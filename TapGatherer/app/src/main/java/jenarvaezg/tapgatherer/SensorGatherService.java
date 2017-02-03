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
import java.text.DateFormat;
import java.util.Date;


public class SensorGatherService extends IntentService implements SensorEventListener {


    public SensorGatherService() {
        super("SensorGatherService");
    }

    private static DataOutputStream csvStream;

    private static final String TAG = "Service";

    protected static final String ACTION_START = "START";
    protected static final String ACTION_UPDATE = "UPDATE";
    protected static final String ACTION_STOP = "STOP";

    private  SensorManager mSensorManager;


    private static PointF target = new PointF(0,0);

    private EventStack eventStack;


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            Log.d(TAG, "GOT ACTION " + action);
            if (ACTION_START.equals(action)) {
                Log.d(TAG, "Go!");
                startSensors();
            }else if(ACTION_UPDATE.equals(action)) {
                target = new PointF(intent.getFloatExtra("X", -1.0f), intent.getFloatExtra("Y", -1.0f));
            }else if(ACTION_STOP.equals(action)) {
                stopSensors();
            }else{
                Log.wtf(TAG, "DUDE WHAT");
            }
        }
    }

    private void startSensors(){
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
            DateFormat format = new java.text.SimpleDateFormat("yy-MM-dd.HH:mm:ss");
            String dateString = format.format(new Date(System.currentTimeMillis()));
            Log.d(TAG, dateString);
            csvStream = new DataOutputStream(new BufferedOutputStream(
                    new FileOutputStream(f.getAbsolutePath() + "/"+dateString+".csv")));
            csvStream.writeChars("sensorType,timestamp,value0,value1,value2,value3,value4,targetX,targetY\n");
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return;
        }

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        Sensor mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor mRotationVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        Sensor mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mRotationVector, SensorManager.SENSOR_DELAY_NORMAL);
        eventStack = new EventStack();
    }

    private void stopSensors(){
        mSensorManager.unregisterListener(this);
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        eventStack.put(event.timestamp, event);
    }
        //float[] values = new float[5];
        /*if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR ||
                event.sensor.getType() == Sensor.TYPE_GAME_ROTATION_VECTOR ){
            values = quaternionToEuler(event.values);
        }else{
            values = event.values;
        }*/

        //this way we can use 3d sensors and quaternions
        /*System.arraycopy(event.values, 0, values, 0, event.values.length);

        StringBuilder sb = new StringBuilder();
        sb.append(event.sensor.getType()).append(",");
        sb.append(event.timestamp).append(",");
        for(float value : values){
            sb.append(value).append(",");
        }
        sb.append(target.x).append(",").append(target.y).append("\n");
        try {
            csvStream.writeChars(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, Log.getStackTraceString(e));
        }
        Log.d(TAG, "Writing!:  " + sb.toString());*/

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    /*private float[] quaternionToEuler(float[] quaternion){
        float theta = (float) (2*Math.acos(quaternion[4]));
        float sinTheta = (float) Math.sin(theta);
        float[] euler = new float[3];
        for(int i = 0; i < 3; i++){
            euler[i] = quaternion[i] / sinTheta;
        }
        return euler;
    }*/


    /*private void getMemory(){
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);

        double availableMegs = mi.availMem / 0x100000L;
        Log.d(TAG, "Available memory: " + Double.toString(availableMegs) + "MB");
        //Percentage can be calculated for API 16+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
            double percentAvail = mi.availMem *100 / mi.totalMem;
            Log.d(TAG, "Percentage available: " + Double.toString(percentAvail) + "% total: " +
                    mi.totalMem / 0x100000L + "MB");
        }
    }*/

    /*private void getInstalledApps(){
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> pkgAppsList = context.getPackageManager().queryIntentActivities( mainIntent, 0);
        for(ResolveInfo r: pkgAppsList){
            ApplicationInfo appInfo = r.activityInfo.applicationInfo;
            final String applicationName = (String) (appInfo != null ? context.getPackageManager().
                    getApplicationLabel(appInfo) : "(unknown)");
            Log.d(TAG, r.activityInfo.applicationInfo.processName + " " +
                    applicationName );
        }
    }*/


}
