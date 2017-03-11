package br.ufc.renanbandeira.volleyjump.domain;


public class GyroscopeData extends SensorData {
    public GyroscopeData(long timestamp, double x, double y, double z) {
        super(timestamp, x, y, z, "Gyro");
    }
}
