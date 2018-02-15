package br.ufc.renanbandeira.volleyjump;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.IdRes;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
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
        OnClickListener, SensorTagListener, SensorEventListener {
    private static final int NEXT_STEPS_TO_CHECK_GRAPH = 5;
    private static final double THRESHOLD = 0.01;
    private static boolean hasConnectedBefore = false;
    private SensorManager sensorManager;
    Sensor accelerometer, gyroscope;
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
    int jumpType = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
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
        // sensorTag = new SensorTag();
        // sensorTag.setPeriod(100);
        accelerometer = sensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (started) {
            sensorManager.unregisterListener(this);
        }
    }

    boolean isIncreasing(int currentIndex) {
        if (accelerometerData.size() > currentIndex + NEXT_STEPS_TO_CHECK_GRAPH) {
            for (int i = currentIndex + 1; i < currentIndex + NEXT_STEPS_TO_CHECK_GRAPH; i++) {
                if (accelerometerData.get(i).getY() + THRESHOLD > accelerometerData.get(currentIndex).getY()) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean isDecreasing(int currentIndex) {
        if (accelerometerData.size() > currentIndex + NEXT_STEPS_TO_CHECK_GRAPH) {
            for (int i = currentIndex + 1; i < currentIndex + NEXT_STEPS_TO_CHECK_GRAPH; i++) {
                if (accelerometerData.get(i).getY() - THRESHOLD < accelerometerData.get(currentIndex).getY()) {
                    return true;
                }
            }
        }
        return false;
    }

    void analyzeData() {
        for(int i = 0; i < accelerometerData.size(); i++) {
            double y = accelerometerData.get(i).getY();
            long timestamp = accelerometerData.get(i).getTimestamp();
            if (!isJumping && isIncreasing(i)) {
                isJumping = true;
                minValue = y;
                minValueTimestamp = timestamp;
            } else if (isJumping && isDecreasing(i)) {
                maxValue = y;
                maxValueTimestamp = timestamp;
                finishJump();
            }
        }
    }

    void askForJumpHeight() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.jump_result_dialog);

        dialog.setTitle("Digite a altura do salto");
        ((RadioGroup) dialog.findViewById(R.id.jump_type)).setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, @IdRes int i) {
               if (i == R.id.jump_atack) {
                   jumpType = 0;
               } else {
                   jumpType = 1;
               }
            }
        });
        (dialog.findViewById(R.id.submit)).setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText height = (EditText) dialog.findViewById(R.id.jump_height);
                mDatabase.child("events").child(eventID).child("height").setValue(height.getText().toString());
                mDatabase.child("events").child(eventID).child("type").setValue(jumpType);
                jumpType = 0;
                eventID = null;
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    void finishJump() {

        isJumping = false;
        float dt = ((float) (minValueTimestamp - maxValueTimestamp));
        double da = minValue + maxValue;
        Log.d("MIN_MAX", "" + minValue);
        Log.d("MAX_MIN", "" + maxValue);
        Log.d("MIN_MAX_TIMESTAMP", "" + minValueTimestamp);
        Log.d("MAX_MIN_TIMESTAMP", "" + maxValueTimestamp);
        Log.d("MAX_MIN_DT_TIMESTAMP", "" + dt);
        int height = (int) ((da*dt*dt*100));
        minValue = 50;
        maxValue = -100;
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
                btnStart.setEnabled(false);
                btnStop.setEnabled(true);
                accelerometerData.clear();
                gyroscopeData.clear();
                started = true;
                if (sensorTag != null) {
                    // sensorTag.start(this, this);
                }
                sensorManager.registerListener(this, accelerometer,
                        SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
                sensorManager.registerListener(this, gyroscope,
                        SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
                break;
            case R.id.btnStop:
                if (sensorTag != null) {
                    sensorTag.stop();
                }
                btnStart.setEnabled(true);
                btnStop.setEnabled(false);
                started = false;
                hasConnectedBefore = false;
                v.setKeepScreenOn(false);
                Log.e("Event", eventID);
                if (accelerometerData.isEmpty() && gyroscopeData.isEmpty()) {
                    break;
                }
                analyzeData();
                mDatabase.child("events").child(eventID).child("acc").setValue(accelerometerData);
                mDatabase.child("events").child(eventID).child("gyro").setValue(gyroscopeData);
                //mDatabase.child("events").child(eventID).child("heights").setValue(jumpsHeight);
                sensorManager.unregisterListener(this);
                askForJumpHeight();
                accelerometerData.clear();
                gyroscopeData.clear();
                jumpsHeight.clear();

                break;
            default:
                break;
        }

    }

    @Override
    public void onSensorTagUpdate(String s) {
        if (!hasConnectedBefore) {
            hasConnectedBefore = true;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(MainActivity.this, "Pode saltar!", Toast.LENGTH_SHORT).show();
                }
            });
        }
        Log.d("SENSOR", s);
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
                accelerometer.setTimestamp(timestamp);
                accelerometerData.add(accelerometer);
                Log.i("AccelData", accelerometer.toString());
            }

        }
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
            }
            Log.i("Data", data.toString());
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}