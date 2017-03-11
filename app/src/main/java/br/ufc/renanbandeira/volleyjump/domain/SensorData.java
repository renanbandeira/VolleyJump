package br.ufc.renanbandeira.volleyjump.domain;


public abstract class SensorData {
    private long timestamp;
    private double x;
    private double y;
    private double z;
    protected String type;

    public SensorData(long timestamp, double x, double y, double z, String type) {
        this.timestamp = timestamp;
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public String toString() {
        return "type=" + type + ", t=" + timestamp + ", x=" + x + ", y=" + y + ", z=" + z;
    }
}
