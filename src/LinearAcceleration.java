/*
 * Copyright 2013, Kaleb Kircher - Boki Software, Kircher Electronics
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Copyright (c) 2012 Paul Lawitzki
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the "Software"),
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE
 * OR OTHER DEALINGS IN THE SOFTWARE.
 */

import com.google.common.eventbus.Subscribe;

public class LinearAcceleration {
    public static final float FILTER_COEFFICIENT = 0.5f;
    public static final float EPSILON = 0.000000001f;
    private static final float NS2S = 1.0f / 1000000000.0f;

    private boolean hasOrientation = false;

    private float[] components = new float[3]; // The gravity components of the acceleration signal.
    private float[] linearAcceleration = new float[]{0, 0, 0};
    private float[] gravity = new float[]{0, 0, 0};
    private float[] gyroscope = new float[3]; // angular speeds from gyro
    private float[] gyroMatrix = new float[9]; // rotation matrix from gyro data
    private float[] gyroOrientation = new float[3]; // orientation angles from gyro matrix
    private float[] magnetic = new float[3]; // magnetic field vector
    private float[] acceleration = new float[3]; // accelerometer vector
    private float[] orientation = new float[3]; // orientation angles from accel and magnet
    private float[] fusedOrientation = new float[3]; // final orientation angles from sensor fusion
    private float[] rotationMatrix = new float[9]; // accelerometer and magnetometer based rotation matrix
    private float[] absoluteFrameOrientation = new float[3];
    private float[] gravityOrientation = new float[3]; // gravity on x, y, z axis
    private float[] deltaRotationVector = new float[4]; // convert the raw gyro data into a rotation vector
    private float[] deltaMatrix = new float[9]; // convert rotation vector into rotation matrix

    private long timeStamp;
    private boolean initState = false;

    private MeanFilter meanFilterGravity;
    private MeanFilter meanFilterMagnetic;
    private MeanFilter meanFilterAcceleration;
    private MeanFilter meanFilterLinearAcceleration;

    private Exporter exporter;
    private SensorSingleData singleData;

    /**
     * Initialize a singleton instance.
     * gravitySubject the gravity subject.
     * gyroscopeSubject the gyroscope subject.
     * magneticSubject the magnetic subject.
     */
    public LinearAcceleration() {
        super();
        meanFilterGravity = new MeanFilter();
        meanFilterGravity.setWindowSize(10);

        meanFilterMagnetic = new MeanFilter();
        meanFilterMagnetic.setWindowSize(10);

        meanFilterAcceleration = new MeanFilter();
        meanFilterAcceleration.setWindowSize(10);

        meanFilterLinearAcceleration = new MeanFilter();
        meanFilterLinearAcceleration.setWindowSize(10);

        gyroOrientation[0] = 0.0f;
        gyroOrientation[1] = 0.0f;
        gyroOrientation[2] = 0.0f;

        // Initialize gyroMatrix with identity matrix
        gyroMatrix[0] = 1.0f;
        gyroMatrix[1] = 0.0f;
        gyroMatrix[2] = 0.0f;
        gyroMatrix[3] = 0.0f;
        gyroMatrix[4] = 1.0f;
        gyroMatrix[5] = 0.0f;
        gyroMatrix[6] = 0.0f;
        gyroMatrix[7] = 0.0f;
        gyroMatrix[8] = 1.0f;

        exporter = new Exporter();
        registerBus();
    }

    private void registerBus() {
        BusProvider.getInstance().register(this);
    }

    public void prepareToExport() {
        singleData.setAccX(linearAcceleration[0]);
        singleData.setAccY(linearAcceleration[1]);
        singleData.setAccZ(linearAcceleration[2]);
        exportNewSensorData(singleData);
    }

    @Subscribe
    public void onSensorUpdate(SensorSingleData singleData) {
        startProcess(singleData);
    }

    private void startProcess(SensorSingleData singleData) {
        this.singleData = singleData;

        float[] acceleration = new float[]{this.singleData.getAccX(), this.singleData.getAccY(), this.singleData
                .getAccZ()};
        float[] magnetic = new float[]{this.singleData.getMagnX(), this.singleData.getMagnY(), this.singleData
                .getMagnZ()};
        this.gyroscope = new float[]{this.singleData.getGyroX(), this.singleData.getGyroY(), this.singleData
                .getGyroZ()};

        onAccelerationSensorChanged(acceleration);
        onMagneticSensorChanged(magnetic);
        float dT = (this.singleData.getTimestamp() - this.timeStamp) * NS2S;
        getRotationVectorFromGyro(dT / 2.0f);
        this.timeStamp = this.singleData.getTimestamp();
        getGravityVector(deltaRotationVector);
        onGravitySensorChanged(gravity);
        onGyroscopeSensorChanged(this.gyroscope, this.timeStamp);
    }

    private void exportNewSensorData(SensorSingleData newSensorData) {
        exporter.writeData(newSensorData.toString());
    }

    // Calculates orientation angles from accelerometer and magnetometer output.
    private void calculateOrientation() {
        if (getRotationMatrix(rotationMatrix, null, gravity, magnetic)) {
            getOrientation(rotationMatrix, orientation);
            hasOrientation = true;
        }
    }

    /**
     * Get the rotation matrix from the current orientation. Android Sensor
     * Manager does not provide a method to transform the orientation into a
     * rotation matrix, only the orientation from a rotation matrix. The basic
     * rotations can be found in Wikipedia with the caveat that the rotations
     * are *transposed* relative to what is required for this method.
     * The device orientation.
     * The rotation matrix from the orientation.
     * http://en.wikipedia.org/wiki/Rotation_matrix
     */
    private float[] getRotationMatrixFromOrientation(float[] orientation) {
        float[] xM = new float[9];
        float[] yM = new float[9];
        float[] zM = new float[9];

        float sinX = (float) Math.sin(orientation[1]);
        float cosX = (float) Math.cos(orientation[1]);
        float sinY = (float) Math.sin(orientation[2]);
        float cosY = (float) Math.cos(orientation[2]);
        float sinZ = (float) Math.sin(orientation[0]);
        float cosZ = (float) Math.cos(orientation[0]);

        // rotation about x-axis (pitch)
        xM[0] = 1.0f;
        xM[1] = 0.0f;
        xM[2] = 0.0f;
        xM[3] = 0.0f;
        xM[4] = cosX;
        xM[5] = sinX;
        xM[6] = 0.0f;
        xM[7] = -sinX;
        xM[8] = cosX;

        // rotation about y-axis (roll)
        yM[0] = cosY;
        yM[1] = 0.0f;
        yM[2] = sinY;
        yM[3] = 0.0f;
        yM[4] = 1.0f;
        yM[5] = 0.0f;
        yM[6] = -sinY;
        yM[7] = 0.0f;
        yM[8] = cosY;

        // rotation about z-axis (azimuth)
        zM[0] = cosZ;
        zM[1] = sinZ;
        zM[2] = 0.0f;
        zM[3] = -sinZ;
        zM[4] = cosZ;
        zM[5] = 0.0f;
        zM[6] = 0.0f;
        zM[7] = 0.0f;
        zM[8] = 1.0f;

        // Build the composite rotation... rotation order is y, x, z (roll, pitch, azimuth)
        float[] resultMatrix = matrixMultiplication(xM, yM);
        resultMatrix = matrixMultiplication(zM, resultMatrix);
        return resultMatrix;
    }

    private static boolean getRotationMatrix(float[] R, float[] I, float[] gravity, float[] geomagnetic) {
        float Ax = gravity[0];
        float Ay = gravity[1];
        float Az = gravity[2];
        final float Ex = geomagnetic[0];
        final float Ey = geomagnetic[1];
        final float Ez = geomagnetic[2];
        float Hx = Ey * Az - Ez * Ay;
        float Hy = Ez * Ax - Ex * Az;
        float Hz = Ex * Ay - Ey * Ax;
        final float normH = (float) Math.sqrt(Hx * Hx + Hy * Hy + Hz * Hz);
        if (normH < 0.1f) {
            // device is close to free fall (or in space?), or close to magnetic north pole. Typical values are  > 100.
            return false;
        }
        final float invH = 1.0f / normH;
        Hx *= invH;
        Hy *= invH;
        Hz *= invH;
        final float invA = 1.0f / (float) Math.sqrt(Ax * Ax + Ay * Ay + Az * Az);
        Ax *= invA;
        Ay *= invA;
        Az *= invA;
        final float Mx = Ay * Hz - Az * Hy;
        final float My = Az * Hx - Ax * Hz;
        final float Mz = Ax * Hy - Ay * Hx;
        if (R != null) {
            if (R.length == 9) {
                R[0] = Hx;
                R[1] = Hy;
                R[2] = Hz;
                R[3] = Mx;
                R[4] = My;
                R[5] = Mz;
                R[6] = Ax;
                R[7] = Ay;
                R[8] = Az;
            } else if (R.length == 16) {
                R[0] = Hx;
                R[1] = Hy;
                R[2] = Hz;
                R[3] = 0;
                R[4] = Mx;
                R[5] = My;
                R[6] = Mz;
                R[7] = 0;
                R[8] = Ax;
                R[9] = Ay;
                R[10] = Az;
                R[11] = 0;
                R[12] = 0;
                R[13] = 0;
                R[14] = 0;
                R[15] = 1;
            }
        }
        if (I != null) {
            // compute the inclination matrix by projecting the geomagnetic vector onto the Z (gravity) and X
            // (horizontal component of geomagnetic vector) axes.
            final float invE = 1.0f / (float) Math.sqrt(Ex * Ex + Ey * Ey + Ez * Ez);
            final float c = (Ex * Mx + Ey * My + Ez * Mz) * invE;
            final float s = (Ex * Ax + Ey * Ay + Ez * Az) * invE;
            if (I.length == 9) {
                I[0] = 1;
                I[1] = 0;
                I[2] = 0;
                I[3] = 0;
                I[4] = c;
                I[5] = s;
                I[6] = 0;
                I[7] = -s;
                I[8] = c;
            } else if (I.length == 16) {
                I[0] = 1;
                I[1] = 0;
                I[2] = 0;
                I[4] = 0;
                I[5] = c;
                I[6] = s;
                I[8] = 0;
                I[9] = -s;
                I[10] = c;
                I[3] = I[7] = I[11] = I[12] = I[13] = I[14] = 0;
                I[15] = 1;
            }
        }
        return true;
    }

    private static void getRotationMatrixFromVector(float[] R, float[] rotationVector) {

        float q0;
        float q1 = rotationVector[0];
        float q2 = rotationVector[1];
        float q3 = rotationVector[2];
        if (rotationVector.length == 4) {
            q0 = rotationVector[3];
        } else {
            q0 = 1 - q1 * q1 - q2 * q2 - q3 * q3;
            q0 = (q0 > 0) ? (float) Math.sqrt(q0) : 0;
        }

        float sq_q1 = 2 * q1 * q1;
        float sq_q2 = 2 * q2 * q2;
        float sq_q3 = 2 * q3 * q3;
        float q1_q2 = 2 * q1 * q2;
        float q3_q0 = 2 * q3 * q0;
        float q1_q3 = 2 * q1 * q3;
        float q2_q0 = 2 * q2 * q0;
        float q2_q3 = 2 * q2 * q3;
        float q1_q0 = 2 * q1 * q0;

        if (R.length == 9) {
            R[0] = 1 - sq_q2 - sq_q3;
            R[1] = q1_q2 - q3_q0;
            R[2] = q1_q3 + q2_q0;
            R[3] = q1_q2 + q3_q0;
            R[4] = 1 - sq_q1 - sq_q3;
            R[5] = q2_q3 - q1_q0;
            R[6] = q1_q3 - q2_q0;
            R[7] = q2_q3 + q1_q0;
            R[8] = 1 - sq_q1 - sq_q2;
        } else if (R.length == 16) {
            R[0] = 1 - sq_q2 - sq_q3;
            R[1] = q1_q2 - q3_q0;
            R[2] = q1_q3 + q2_q0;
            R[3] = 0.0f;

            R[4] = q1_q2 + q3_q0;
            R[5] = 1 - sq_q1 - sq_q3;
            R[6] = q2_q3 - q1_q0;
            R[7] = 0.0f;

            R[8] = q1_q3 - q2_q0;
            R[9] = q2_q3 + q1_q0;
            R[10] = 1 - sq_q1 - sq_q2;
            R[11] = 0.0f;
            R[12] = R[13] = R[14] = 0.0f;
            R[15] = 1.0f;
        }
    }

    /**
     * Calculates a rotation vector from the gyroscope angular speed values.
     * gyroValues
     * deltaRotationVector
     * timeFactor
     */
    private void getRotationVectorFromGyro(float timeFactor) {

        // Calculate the angular speed of the sample
        float omegaMagnitude = (float) Math.sqrt(Math.pow(gyroscope[0], 2)
                + Math.pow(gyroscope[1], 2) + Math.pow(gyroscope[2], 2));

        // Normalize the rotation vector if it's big enough to get the axis
        if (omegaMagnitude > EPSILON) {
            gyroscope[0] /= omegaMagnitude;
            gyroscope[1] /= omegaMagnitude;
            gyroscope[2] /= omegaMagnitude;
        }

        // Integrate around this axis with the angular speed by the timestep
        // in order to get a delta rotation from this sample over the timestep
        // We will convert this axis-angle representation of the delta rotation
        // into a quaternion before turning it into the rotation matrix.
        float thetaOverTwo = omegaMagnitude * timeFactor;
        float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
        float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);

        deltaRotationVector[0] = sinThetaOverTwo * gyroscope[0];
        deltaRotationVector[1] = sinThetaOverTwo * gyroscope[1];
        deltaRotationVector[2] = sinThetaOverTwo * gyroscope[2];
        deltaRotationVector[3] = cosThetaOverTwo;
    }

    private void getGravityVector(float[] deltaRotationVector) {
        float q0 = deltaRotationVector[0];
        float q1 = deltaRotationVector[1];
        float q2 = deltaRotationVector[2];
        float q3 = deltaRotationVector[3];

        float gx = 2 * (q1 * q3 - q0 * q2);
        float gy = 2 * (q0 * q1 + q2 * q3);
        float gz = q0 * q0 - q1 * q1 - q2 * q2 + q3 * q3;

        gravity[0] = gx;
        gravity[1] = gy;
        gravity[2] = gz;
    }

    //  Multiply A by B
    private float[] matrixMultiplication(float[] A, float[] B) {
        float[] result = new float[9];

        result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
        result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
        result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

        result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
        result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
        result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

        result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
        result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
        result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

        return result;
    }

    private void onAccelerationSensorChanged(float[] acceleration) {
        // Get a local copy of the raw magnetic values from the device sensor.
        System.arraycopy(acceleration, 0, this.acceleration, 0,
                acceleration.length);

        this.acceleration = meanFilterAcceleration.filterFloat(this.acceleration);
    }

    private void onMagneticSensorChanged(float[] magnetic) {
        // Get a local copy of the raw magnetic values from the device sensor.
        System.arraycopy(magnetic, 0, this.magnetic, 0, magnetic.length);

        this.magnetic = meanFilterMagnetic.filterFloat(this.magnetic);
    }

    private void onGravitySensorChanged(float[] gravity) {
        // Get a local copy of the raw magnetic values from the device sensor.
        System.arraycopy(gravity, 0, this.gravity, 0, gravity.length);
        this.gravity = meanFilterGravity.filterFloat(this.gravity);
        calculateOrientation();
    }

    private void onGyroscopeSensorChanged(float[] gyroscope, long timeStamp) {
        // don't start until first accelerometer/magnetometer orientation has
        // been acquired
        if (!hasOrientation) {
            return;
        }

        // Initialization of the gyroscope based rotation matrix
        if (!initState) {
            gyroMatrix = matrixMultiplication(gyroMatrix, rotationMatrix);
            initState = true;
        }

        if (this.timeStamp != 0) {
            float dT = (timeStamp - this.timeStamp) * NS2S;

            System.arraycopy(gyroscope, 0, this.gyroscope, 0, 3);
            getRotationVectorFromGyro(dT / 2.0f);
        }

        // measurement done, save current time for next interval
        this.timeStamp = timeStamp;

        // Get the rotation matrix from the gyroscope
        getRotationMatrixFromVector(deltaMatrix, deltaRotationVector);

        // Apply the new rotation interval on the gyroscope based rotation
        // matrix to form a composite rotation matrix. The product of two
        // rotation matricies is a rotation matrix...
        // Multiplication of rotation matrices corresponds to composition of
        // rotations... Which in this case are the rotation matrix from the
        // fused orientation and the rotation matrix from the current gyroscope
        // outputs.
        gyroMatrix = matrixMultiplication(gyroMatrix, deltaMatrix);

        // Get the gyroscope based orientation from the composite rotation
        // matrix. This orientation will be fused via complementary filter with
        // the orientation from the acceleration sensor and magnetic sensor.
        getOrientation(gyroMatrix, gyroOrientation);

        calculateFusedOrientation();
    }

    // Calculate the fused orientation.
    private void calculateFusedOrientation() {
        float oneMinusCoeff = (1.0f - FILTER_COEFFICIENT);

		/*
         * Fix for 179° <--> -179° transition problem: Check whether one of the
		 * two orientation angles (gyro or accMag) is negative while the other
		 * one is positive. If so, add 360° (2 * math.PI) to the negative value,
		 * perform the sensor fusion, and remove the 360° from the result if it
		 * is greater than 180°. This stabilizes the output in
		 * positive-to-negative-transition cases.
		 */

        // azimuth
        if (gyroOrientation[0] < -0.5 * Math.PI && orientation[0] > 0.0) {
            fusedOrientation[0] = (float) (FILTER_COEFFICIENT
                    * (gyroOrientation[0] + 2.0 * Math.PI) + oneMinusCoeff
                    * orientation[0]);
            fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI
                    : 0;
        } else if (orientation[0] < -0.5 * Math.PI && gyroOrientation[0] > 0.0) {
            fusedOrientation[0] = (float) (FILTER_COEFFICIENT
                    * gyroOrientation[0] + oneMinusCoeff
                    * (orientation[0] + 2.0 * Math.PI));
            fusedOrientation[0] -= (fusedOrientation[0] > Math.PI) ? 2.0 * Math.PI
                    : 0;
        } else {
            fusedOrientation[0] = FILTER_COEFFICIENT * gyroOrientation[0]
                    + oneMinusCoeff * orientation[0];
        }

        // pitch
        if (gyroOrientation[1] < -0.5 * Math.PI && orientation[1] > 0.0) {
            fusedOrientation[1] = (float) (FILTER_COEFFICIENT
                    * (gyroOrientation[1] + 2.0 * Math.PI) + oneMinusCoeff
                    * orientation[1]);
            fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI
                    : 0;
        } else if (orientation[1] < -0.5 * Math.PI && gyroOrientation[1] > 0.0) {
            fusedOrientation[1] = (float) (FILTER_COEFFICIENT
                    * gyroOrientation[1] + oneMinusCoeff
                    * (orientation[1] + 2.0 * Math.PI));
            fusedOrientation[1] -= (fusedOrientation[1] > Math.PI) ? 2.0 * Math.PI
                    : 0;
        } else {
            fusedOrientation[1] = FILTER_COEFFICIENT * gyroOrientation[1]
                    + oneMinusCoeff * orientation[1];
        }

        // roll
        if (gyroOrientation[2] < -0.5 * Math.PI && orientation[2] > 0.0) {
            fusedOrientation[2] = (float) (FILTER_COEFFICIENT
                    * (gyroOrientation[2] + 2.0 * Math.PI) + oneMinusCoeff
                    * orientation[2]);
            fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI
                    : 0;
        } else if (orientation[2] < -0.5 * Math.PI && gyroOrientation[2] > 0.0) {
            fusedOrientation[2] = (float) (FILTER_COEFFICIENT
                    * gyroOrientation[2] + oneMinusCoeff
                    * (orientation[2] + 2.0 * Math.PI));
            fusedOrientation[2] -= (fusedOrientation[2] > Math.PI) ? 2.0 * Math.PI
                    : 0;
        } else {
            fusedOrientation[2] = FILTER_COEFFICIENT * gyroOrientation[2]
                    + oneMinusCoeff * orientation[2];
        }

        // overwrite gyro matrix and orientation with fused orientation  to comensate gyro drift
        gyroMatrix = getRotationMatrixFromOrientation(fusedOrientation);

        System.arraycopy(fusedOrientation, 0, gyroOrientation, 0, 3);
        calculateLinearAcceleration();
    }

    private static float[] getOrientation(float[] R, float values[]) {
        /*
        * 4x4 (length=16) case:
        *     R[ 0]   R[ 1]   R[ 2]   0
        *     R[ 4]   R[ 5]   R[ 6]   0
        *     R[ 8]   R[ 9]   R[10]   0
        *         0       0       0   1
        *
        * 3x3 (length=9) case:
        *     R[ 0]   R[ 1]   R[ 2]
        *     R[ 3]   R[ 4]   R[ 5]
        *     R[ 6]   R[ 7]   R[ 8]
        *
        */
        if (R.length == 9) {
            values[0] = (float) Math.atan2(R[1], R[4]);
            values[1] = (float) Math.asin(-R[7]);
            values[2] = (float) Math.atan2(-R[6], R[8]);
        } else {
            values[0] = (float) Math.atan2(R[1], R[5]);
            values[1] = (float) Math.asin(-R[9]);
            values[2] = (float) Math.atan2(-R[8], R[10]);
        }
        return values;
    }

    // TODO TEST IT!
    private void calculateGravity() {
        System.arraycopy(gyroOrientation, 0, gravityOrientation, 0, 3);

        gravity[0] = (float) (Constants.GRAVITY_EARTH
                * -Math.cos(gravityOrientation[1]) * Math
                .sin(gravityOrientation[2]));

        // Find the gravity component of the Y-axis = g*-sin(pitch);
        gravity[1] = (float) (Constants.GRAVITY_EARTH * -Math
                .sin(gravityOrientation[1]));

        // Find the gravity component of the Z-axis = g*cos(pitch)*cos(roll);
        gravity[2] = (float) (Constants.GRAVITY_EARTH
                * Math.cos(gravityOrientation[1]) * Math
                .cos(gravityOrientation[2]));
    }

    private void calculateLinearAcceleration() {
        System.arraycopy(gyroOrientation, 0, absoluteFrameOrientation, 0, 3);

        // values[0]: azimuth, rotation around the Z axis.
        // values[1]: pitch, rotation around the X axis.
        // values[2]: roll, rotation around the Y axis.

        // Find the gravity component of the X-axis = g*-cos(pitch)*sin(roll);
        components[0] = (float) (Constants.GRAVITY_EARTH
                * -Math.cos(absoluteFrameOrientation[1]) * Math
                .sin(absoluteFrameOrientation[2]));

        // Find the gravity component of the Y-axis  g*-sin(pitch);
        components[1] = (float) (Constants.GRAVITY_EARTH * -Math
                .sin(absoluteFrameOrientation[1]));

        // Find the gravity component of the Z-axis = g*cos(pitch)*cos(roll);
        components[2] = (float) (Constants.GRAVITY_EARTH
                * Math.cos(absoluteFrameOrientation[1]) * Math
                .cos(absoluteFrameOrientation[2]));

        // Subtract the gravity component of the signal from the input acceleration signal to get the tilt
        // compensated output.
        linearAcceleration[0] = (this.acceleration[0] - components[0]);
        linearAcceleration[1] = (this.acceleration[1] - components[1]);
        linearAcceleration[2] = (this.acceleration[2] - components[2]);

        this.linearAcceleration = meanFilterLinearAcceleration
                .filterFloat(this.linearAcceleration);

        prepareToExport();
    }

}
