public class SensorSingleData {
    // Generation,Timestamp,AX,AY,AZ,lAX,lAY,lAZ,

    private int generation; // Measurement nunber
    private long timestamp; // Timestamp in millis, where first measurement = 0
    private double aX;      // X axle - raw data
    private double aY;      // Y axle - raw data
    private double aZ;      // Z axle - raw data
    private double laX;     // X axle - fused data
    private double laY;     // Y axle - fused data
    private double laZ;     // Z axle - fused data

    public SensorSingleData(int generation, long timestamp, double aX, double aY, double aZ, double laX, double laY, double laZ) {
        this.generation = generation;
        this.timestamp = timestamp;
        this.aX = aX;
        this.aY = aY;
        this.aZ = aZ;
        this.laX = laX;
        this.laY = laY;
        this.laZ = laZ;
    }

    public int getGeneration() {
        return generation;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getaX() {
        return aX;
    }

    public double getaY() {
        return aY;
    }

    public double getaZ() {
        return aZ;
    }

    public double getLaX() {
        return laX;
    }

    public double getLaY() {
        return laY;
    }

    public double getLaZ() {
        return laZ;
    }

    @Override
    public String toString() {
        return generation + " " + timestamp + " " + aX + " " + aY + " " + aZ + " " + laX + " " + laY + " " + laZ;
    }
}
