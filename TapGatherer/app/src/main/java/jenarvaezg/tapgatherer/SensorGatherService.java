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

    private  SensorManager mSensorManager;
    private  Sensor mAccelerometer;
    private  Sensor mGyroscope;
    private  Sensor mRotationVector;

    private static PointF target = new PointF(0,0);


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            Log.d(TAG, "GOT ACTION " + action);
            if (ACTION_START.equals(action)) {
                Log.d(TAG, "Go!");
                gatherFromSensors();
            }else if(ACTION_UPDATE.equals(action)) {
                target = new PointF(intent.getFloatExtra("X", -1.0f), intent.getFloatExtra("Y", -1.0f));
                Log.d(TAG+"A", "UPDATING!! " + target.toString());
            }else{
                Log.wtf(TAG, "DUDE WHAT");
            }
        }
    }

    private void gatherFromSensors(){
        try {

            //assume we have external storage, fail miserably otherwise
            File f = getExternalFilesDir(null);

            if (f == null){
                throw new IOException("ExternalFilesDir error");
            }
            if (!f.mkdirs()){
                Log.d(TAG, f.getAbsolutePath() + " not created");
            }
            Log.d(TAG, f.getCanonicalPath());
            csvStream = new DataOutputStream(new BufferedOutputStream(
                    new FileOutputStream(f.getAbsolutePath() + "/test.csv")));
            csvStream.writeChars("sensorType,timestamp,value0,value1,value2,value3,value4,targetX,targetY\n");
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return;
        }

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mRotationVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mRotationVector, SensorManager.SENSOR_DELAY_NORMAL);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] values = new float[5];
        //this way we can use 3d sensors and quaternions
        System.arraycopy(event.values, 0, values, 0, event.values.length);

        try {
            csvStream.writeChars(Integer.toString(event.sensor.getType()) + "," +
                    Long.toString(event.timestamp) + "," + Float.toString(values[0]) +
                    "," + Float.toString(values[1]) + "," + Float.toString(values[2]) +
                    "," + Float.toString(values[3]) + "," + Float.toString(values[4]) +
                    "," + Float.toString(target.x) + "," + Float.toString(target.y) + "\n");
            Log.d(TAG, "Writing! " + Long.toString(event.timestamp) + target.toString());
            csvStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
