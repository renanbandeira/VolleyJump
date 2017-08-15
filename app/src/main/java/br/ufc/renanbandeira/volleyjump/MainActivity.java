package br.ufc.renanbandeira.volleyjump;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.gson.Gson;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import br.ufc.great.sensortag.SensorTag;
import br.ufc.great.sensortag.callbacks.SensorTagListener;
import br.ufc.renanbandeira.volleyjump.domain.SensorData;
import br.ufc.renanbandeira.volleyjump.domain.SensorTagData;

public class MainActivity extends Activity implements
        OnClickListener, SensorTagListener {
    private Button btnStart, btnStop;
    private boolean started = false;
    private ArrayList<SensorData> accelerometerData;
    private ArrayList<SensorData> gyroscopeData;
    private ArrayList<Integer> jumpsHeight;
    private DatabaseReference mDatabase;
    String eventID;
    double maxValue = -1;
    long maxValueTimestamp;
    double minValue = 50;
    long minValueTimestamp;
    boolean isJumping;
    SensorTag sensorTag;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        accelerometerData = new ArrayList<>();
        gyroscopeData = new ArrayList<>();
        jumpsHeight = new ArrayList<>();

        btnStart = (Button) findViewById(R.id.btnStart);
        btnStop = (Button) findViewById(R.id.btnStop);
        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        sensorTag = new SensorTag();
        sensorTag.setPeriod(50);
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
        Log.e("Height", "" + height);
        if (height < 1 || height > 100) {
            return;
        }
        jumpsHeight.add(height);
        Toast.makeText(this, "Salto: " + height + " cm", Toast.LENGTH_LONG).show();
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
                sensorTag.start(this, this);
                break;
            case R.id.btnStop:
                sensorTag.stop();
                btnStart.setEnabled(true);
                btnStop.setEnabled(false);
                started = false;
                v.setKeepScreenOn(false);
                Log.e("Event", eventID);
                analyzeData();
                //mDatabase.child("events").child(eventID).child("acc").setValue(accelerometerData);
                //mDatabase.child("events").child(eventID).child("gyro").setValue(gyroscopeData);
                //mDatabase.child("events").child(eventID).child("heights").setValue(jumpsHeight);

                accelerometerData.clear();
                gyroscopeData.clear();
                jumpsHeight.clear();
                eventID = null;
                break;
            default:
                break;
        }

    }

    @Override
    public void onSensorTagUpdate(String s) {
        if (started) {
            Gson gson = new Gson();
            long timestamp = System.currentTimeMillis();
            SensorTagData data = gson.fromJson(s, SensorTagData.class);
            SensorData accelerometer = data.getAccelData();
            SensorData gyroscope = data.getGyroData();
            if (gyroscope != null) {
                gyroscope.setTimestamp(timestamp);
                //Log.i("GyroData", gyroscope.toString());
                gyroscopeData.add(gyroscope);
            }
            if (accelerometer != null) {
                accelerometer.setY(accelerometer.getY() / 3.1 + 1.1);
                accelerometer.setTimestamp(timestamp);
                accelerometerData.add(accelerometer);
                Log.i("AccelData", accelerometer.toString());
            }

        }
    }
}