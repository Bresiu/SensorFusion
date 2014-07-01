public class SensorSingleData {
    // Generation,Timestamp,AX,AY,AZ

    private int generation; // Measurement nunber
    private long timestamp; // Timestamp in millis, where first measurement = 0
    private double aX;      // X axle
    private double aY;      // Y axle
    private double aZ;      // Z axle

    public SensorSingleData(int generation, long timestamp, double aX, double aY, double aZ) {
        this.generation = generation;
        this.timestamp = timestamp;
        this.aX = aX;
        this.aY = aY;
        this.aZ = aZ;
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

    public void setGeneration(int generation) {
        this.generation = generation;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setaX(double aX) {
        this.aX = aX;
    }

    public void setaY(double aY) {
        this.aY = aY;
    }

    public void setaZ(double aZ) {
        this.aZ = aZ;
    }

    @Override
    public String toString() {
        return generation + " " + timestamp + " " + aX + " " + aY + " " + aZ;
    }
}
