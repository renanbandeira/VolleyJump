package br.ufc.renanbandeira.volleyjump.domain;


public class AccelData extends SensorData {
    public AccelData(long timestamp, double x, double y, double z) {
        super(timestamp, x, y, z, "Acc");
    }
}