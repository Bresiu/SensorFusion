public class SensorSingleData {

    private int generation; // Measurement number
    private long timestamp; // Timestamp in millis, where first measurement = 0

    // Accelerometer
    private float accX;
    private float accY;
    private float accZ;

    // Gyroscope
    private float gyroX;
    private float gyroY;
    private float gyroZ;

    // Magnetometer
    private float magnX;
    private float magnY;
    private float magnZ;

    public SensorSingleData(int generation, long timestamp, float accX, float accY, float accZ,
                            float gyroX, float gyroY, float gyroZ, float magnX, float magnY, float magnZ) {
        this.generation = generation;
        this.timestamp = timestamp;
        this.accX = accX;
        this.accY = accY;
        this.accZ = accZ;
        this.gyroX = gyroX;
        this.gyroY = gyroY;
        this.gyroZ = gyroZ;
        this.magnX = magnX;
        this.magnY = magnY;
        this.magnZ = magnZ;
    }

    public int getGeneration() {
        return generation;
    }

    public void setGeneration(int generation) {
        this.generation = generation;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public float getAccX() {
        return accX;
    }

    public void setAccX(float accX) {
        this.accX = accX;
    }

    public float getAccY() {
        return accY;
    }

    public void setAccY(float accY) {
        this.accY = accY;
    }

    public float getAccZ() {
        return accZ;
    }

    public void setAccZ(float accZ) {
        this.accZ = accZ;
    }

    public float getGyroX() {
        return gyroX;
    }

    public void setGyroX(float gyroX) {
        this.gyroX = gyroX;
    }

    public float getGyroY() {
        return gyroY;
    }

    public void setGyroY(float gyroY) {
        this.gyroY = gyroY;
    }

    public float getGyroZ() {
        return gyroZ;
    }

    public void setGyroZ(float gyroZ) {
        this.gyroZ = gyroZ;
    }

    public float getMagnX() {
        return magnX;
    }

    public void setMagnX(float magnX) {
        this.magnX = magnX;
    }

    public float getMagnY() {
        return magnY;
    }

    public void setMagnY(float magnY) {
        this.magnY = magnY;
    }

    public float getMagnZ() {
        return magnZ;
    }

    public void setMagnZ(float magnZ) {
        this.magnZ = magnZ;
    }

    @Override
    public String toString() {
        return generation + " " + timestamp + " " + accX + " " + accY + " " + accZ +
                gyroX + " " + gyroY + " " + gyroZ + " " + magnX + " " + magnY + " " + magnZ;
    }
}
