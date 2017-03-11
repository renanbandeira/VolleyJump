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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.UUID;

import br.ufc.renanbandeira.volleyjump.domain.AccelData;
import br.ufc.renanbandeira.volleyjump.domain.GyroscopeData;
import br.ufc.renanbandeira.volleyjump.domain.SensorData;

public class MainActivity extends Activity implements SensorEventListener,
        OnClickListener {
    private SensorManager sensorManager;
    Sensor accelerometer, gyroscope;
    private Button btnStart, btnStop;
    private boolean started = false;
    private ArrayList sensorData;
    private DatabaseReference mDatabase;
    String eventID;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        sensorData = new ArrayList();

        btnStart = (Button) findViewById(R.id.btnStart);
        btnStop = (Button) findViewById(R.id.btnStop);
        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        mDatabase = FirebaseDatabase.getInstance().getReference();
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (started) {
            double x = event.values[0];
            double y = event.values[1];
            double z = event.values[2];
            long timestamp = System.currentTimeMillis();
            SensorData data;
            if (event.sensor.equals(accelerometer)) {
                data = new AccelData(timestamp, x, y, z);
            } else {
                data = new GyroscopeData(timestamp, x, y, z);
            }
            Log.i("Data", data.toString());
            sensorData.add(data);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btnStart:
                eventID = UUID.randomUUID().toString();
                btnStart.setEnabled(false);
                btnStop.setEnabled(true);
                sensorData = new ArrayList();
                // save prev data if available
                started = true;
                sensorManager.registerListener(this, accelerometer,
                        SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
                sensorManager.registerListener(this, gyroscope,
                        SensorManager.SENSOR_STATUS_ACCURACY_HIGH);
                break;
            case R.id.btnStop:
                btnStart.setEnabled(true);
                btnStop.setEnabled(false);
                started = false;
                sensorManager.unregisterListener(this);
                mDatabase.child("events").child(eventID).setValue(sensorData);
                sensorData.clear();
                eventID = null;
                break;
            default:
                break;
        }

    }
}