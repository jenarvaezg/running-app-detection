package jenarvaezg.tapgatherer;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;


public class SensorGatherService extends IntentService implements SensorEventListener {


    public SensorGatherService() {
        super("SensorGatherService");
    }
    //vars need to be static since there is only a thread but there can be different instances
    private static DataOutputStream csvStream;

    private static final String TAG = "Service";

    protected static final String ACTION_START = "START";
    protected static final String ACTION_UPDATE = "UPDATE";

    private static SensorManager mSensorManager;
    private static Sensor mAccelerometer;
    private static Sensor mGyroscope;

    private static boolean canWrite = true;
    private static PointF target = new PointF(0,0);


    @Override
    public void onCreate(){
        super.onCreate();
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        try {
            File f = getExternalFilesDir(null);
            if (f == null || !f.mkdirs()){
                throw new IOException("ExternalFilesDir error");
            }
            csvStream = new DataOutputStream(new BufferedOutputStream(
                    new FileOutputStream(f.getAbsolutePath() + "/test.csv")));
            csvStream.writeUTF("sensorType,timestamp,sensorX,sensorY,sensorZ,targetX,targetY\n");
        } catch (IOException e) {
            canWrite = false;
            e.printStackTrace();
        }
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (!canWrite){
            stopSelf();
            return;
        }
        if (intent != null) {
            final String action = intent.getAction();
            Log.d(TAG+"A", "GOT ACTION " + action);
            if (ACTION_START.equals(action)) {
                Log.d(TAG, "Go!");
                gatherFromSensors();
            }else if(ACTION_UPDATE.equals(action)) {

                target = new PointF(intent.getFloatExtra("X", -1), intent.getFloatExtra("Y", -1));
                Log.d(TAG+"A", "UPDATING!! " + target.toString());
            }else{
                Log.wtf(TAG, "DUDE WHAT");
            }
        }
    }

    private void gatherFromSensors(){
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
    }

    int prevSensor = Sensor.TYPE_ACCELEROMETER;
    long prevTimestamp = 0;
    int sameCtr = 0;
    int diffCtr = 0;
    int accCtr = 0;
    int gyroCtr = 0;
    @Override
    public void onSensorChanged(SensorEvent event) {
        switch(event.sensor.getType()){
            case Sensor.TYPE_GYROSCOPE:
                Log.d(TAG, "Gyroscope: " + Long.toString(event.timestamp));
                gyroCtr++;
                break;
            case Sensor.TYPE_ACCELEROMETER:
                Log.d(TAG, "Accelerometer: " + Long.toString(event.timestamp));
                accCtr++;
        }
        if (prevSensor == event.sensor.getType()){
            Log.d(TAG, "Same sensor, timestamp delta: " + Long.toString(event.timestamp - prevTimestamp));
            sameCtr++;
        }else{
            Log.d(TAG, "Different sensor, timestamp delta: " + Long.toString(event.timestamp - prevTimestamp));
            diffCtr++;
            prevSensor = event.sensor.getType();
        }
        prevTimestamp = event.timestamp;

        Log.d(TAG, "acc: " + Integer.toString(accCtr) + ", gyro: " + Integer.toString(gyroCtr) +
        ", diff: " + Integer.toString(diffCtr) + ", same: " + Integer.toString(sameCtr));

        try {
            csvStream.write((Integer.toString(event.sensor.getType()) + "," + Long.toString(event.timestamp) + "," +
                    Float.toString(event.values[0]) +
                "," + Float.toString(event.values[1]) + "," + Float.toString(event.values[1]) +
                    "," + Float.toString(target.x) + "," + Float.toString(target.y) + "\n").getBytes());
            Log.d(TAG, "Writing! " + Long.toString(event.timestamp) + target.toString());
            csvStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "ACCuracy changed! " + Integer.toString(accuracy));
    }
}
