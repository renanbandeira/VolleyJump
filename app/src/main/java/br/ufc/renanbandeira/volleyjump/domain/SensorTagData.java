package br.ufc.renanbandeira.volleyjump.domain;


import com.google.gson.annotations.SerializedName;

public class SensorTagData {
    @SerializedName("AccelData")
    private SensorData accelData;
    @SerializedName("GyroData")
    private SensorData gyroData;

    public SensorTagData() {}

    public SensorData getAccelData() {
        return accelData;
    }

    public void setAccelData(SensorData accelData) {
        this.accelData = accelData;
    }

    public SensorData getGyroData() {
        return gyroData;
    }

    public void setGyroData(SensorData gyroData) {
        this.gyroData = gyroData;
    }
}
