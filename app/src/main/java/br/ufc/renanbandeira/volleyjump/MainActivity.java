package br.ufc.renanbandeira.volleyjump;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.UUID;

import br.ufc.renanbandeira.volleyjump.domain.SensorData;

public class MainActivity extends Activity implements SensorEventListener,
        OnClickListener {
    private SensorManager sensorManager;
    Sensor accelerometer, gyroscope, linearAcc;
    private Button btnStart, btnStop;
    private boolean started = false;
    private ArrayList<SensorData> accelerometerData;
    private ArrayList<SensorData> linearAccelerationData;
    private ArrayList<SensorData> gyroscopeData;
    private ArrayList<Integer> jumpsHeight;
    private DatabaseReference mDatabase;
    String eventID;
    double maxValue = -1;
    long maxValueTimestamp;
    double minValue = 50;
    long minValueTimestamp;
    boolean isJumping;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometerData = new ArrayList<>();
        gyroscopeData = new ArrayList<>();
        linearAccelerationData = new ArrayList<>();
        jumpsHeight = new ArrayList<>();

        btnStart = (Button) findViewById(R.id.btnStart);
        btnStop = (Button) findViewById(R.id.btnStop);
        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        linearAcc = sensorManager
                .getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        accelerometer = sensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (started == true) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.w("SENSOR", "ACCURACY CHANGED!!!");
    }

    void analyzeData() {
        for(int i = 0; i < accelerometerData.size(); i++) {
            double y = accelerometerData.get(i).getY();
            long timestamp = accelerometerData.get(i).getTimestamp();
            if ( y < 0 && !isJumping && maxValue > 0) {
                isJumping = true;
            }
            if (isJumping) {
                if (y < minValue) {
                    minValue = y;
                    minValueTimestamp = timestamp;
                } else if (y > minValue) {
                    finishJump();
                }
            } else {
                if (y > maxValue) {
                    maxValue = y;
                    maxValueTimestamp = timestamp;
                }
            }
        }
    }

    void finishJump() {
        isJumping = false;
        float dt = ((float) (minValueTimestamp - maxValueTimestamp))/1000;
        double da = minValue + maxValue;
        Log.d("MIN_MAX", "" + minValue);
        Log.d("MAX_MIN", "" + maxValue);
        Log.d("MIN_MAX_TIMESTAMP", "" + minValueTimestamp);
        Log.d("MAX_MIN_TIMESTAMP", "" + maxValueTimestamp);
        Log.d("MAX_MIN_DT_TIMESTAMP", "" + dt);
        int height = (int) ((da*dt*dt*100));
        minValue = 50;
        maxValue = -1;
        maxValueTimestamp = 0;
        minValueTimestamp = 0;
        if (height <= 0) {
            return;
        }
        jumpsHeight.add(height);
        Toast.makeText(this, "Salto: " + height + " cm", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (started) {
            double x = event.values[0];
            double y = event.values[1];
            double z = event.values[2];
            long timestamp = System.currentTimeMillis();
            SensorData data = new SensorData(timestamp, x, y, z);
            if (event.sensor.equals(accelerometer)) {
                accelerometerData.add(data);
            } else if (event.sensor.equals(gyroscope)){
                gyroscopeData.add(data);
            } else {
                linearAccelerationData.add(data);
            }
            Log.i("Data", data.toString());
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnStart:
                Calendar cal = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yy--HH-mm-ss-SSS");
                eventID = sdf.format(cal.getTime());
                v.setKeepScreenOn(true);
                btnStart.setEnabled(false);
                btnStop.setEnabled(true);
                accelerometerData.clear();
                gyroscopeData.clear();
                started = true;
                sensorManager.registerListener(this, linearAcc,
                        SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
                sensorManager.registerListener(this, accelerometer,
                        SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
                sensorManager.registerListener(this, gyroscope,
                        SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
                break;
            case R.id.btnStop:
                btnStart.setEnabled(true);
                btnStop.setEnabled(false);
                started = false;
                v.setKeepScreenOn(false);
                sensorManager.unregisterListener(this);
                Log.e("Event", eventID);
                analyzeData();
                mDatabase.child("events").child(eventID).child("acc").setValue(accelerometerData);
                mDatabase.child("events").child(eventID).child("linear").setValue(linearAccelerationData);
                mDatabase.child("events").child(eventID).child("gyro").setValue(gyroscopeData);
                mDatabase.child("events").child(eventID).child("heights").setValue(jumpsHeight);

                linearAccelerationData.clear();
                accelerometerData.clear();
                gyroscopeData.clear();
                jumpsHeight.clear();
                eventID = null;
                break;
            default:
                break;
        }

    }
}