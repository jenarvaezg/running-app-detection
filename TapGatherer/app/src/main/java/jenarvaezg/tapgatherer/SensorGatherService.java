package jenarvaezg.tapgatherer;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class SensorGatherService extends IntentService implements SensorEventListener {


    public SensorGatherService() {
        super("SensorGatherService");
    }

    private static DataOutputStream csvStream;

    private static final String TAG = "Service";

    protected static final String ACTION_START = "START";
    protected static final String ACTION_UPDATE = "UPDATE";
    protected static final String ACTION_STOP = "STOP";
    protected static final String ACTION_STORE_TOUCH_EVENT = "STORE";

    private  SensorManager mSensorManager;

    private static EventStack eventStack;

    static Map<Integer, TimeBase> sensorOffsets = new HashMap<>();

    class TimeBase{
        Long dateBase;
        Long timestampBase;

        TimeBase(Long dateBase, Long timeStampBase){
            this.dateBase = dateBase;
            this.timestampBase = timeStampBase;
        }
    }


    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            Log.d(TAG, "GOT ACTION " + action);
            if (ACTION_START.equals(action)) {
                Log.d(TAG, "Go!");
                startSensors();
            }else if(ACTION_STORE_TOUCH_EVENT.equals(action)){
                Bundle bundle = intent.getExtras();
                storeTouchEvent(bundle);
            }else if(ACTION_STOP.equals(action)) {
                stopSensors();
            }else{
                Log.wtf(TAG, "DUDE WHAT '" + action + "'");
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
            csvStream.writeChars("timestamp,sensor,value0,value1,value2,value3,value4,when,type,action\n");
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return;
        }

        eventStack = new EventStack();

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        Sensor mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor mRotationVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        Sensor mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Sensor mGameRotationVector = mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mRotationVector, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGameRotationVector, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void stopSensors(){
        mSensorManager.unregisterListener(this);
    }



    /*The timestamps are not defined as being the Unix time;
    they're just "a time" that's only valid for a given sensor.
     This means that timestamps can only be compared if they come from the same sensor.
     O boi*/

    @Override
    public void onSensorChanged(SensorEvent event) {
        TimeBase offset = sensorOffsets.get(event.sensor.getType());
        if(offset == null){
            offset = new TimeBase(System.currentTimeMillis(), event.timestamp);
            sensorOffsets.put(event.sensor.getType(), offset);
        }
        synchronized (EventStack.class) {
            eventStack.put((event.timestamp - offset.timestampBase) / 1000000L + offset.dateBase,
                    new SensorEventData(event, offset));
        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    private void storeTouchEvent(Bundle b){
        Long start = b.getLong("START");
        Long end = b.getLong("END");
        Collection<SensorEventData> before, during, after;
        synchronized (EventStack.class) {
            before = eventStack.getBefore(start);
            during = eventStack.getInRange(start, end);
        }
        try {
            Thread.sleep((end-start) * 2); //wait a bit to collect after touch events
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //noise after event
        synchronized (EventStack.class){
            after = eventStack.getAfter(end);
            eventStack = new EventStack();
        }
        writeToCSVFile(b.getString("TYPE"), b.getString("ACTION"),  before, during, after);

    }

    private synchronized void writeToCSVFile(String eventType, String eventAction,
                                Collection<SensorEventData> before,
                                Collection<SensorEventData> during,
                                Collection<SensorEventData> after) {
        StringBuilder sb = new StringBuilder();
        Log.d(TAG, "Before " + Integer.toString(before.size()) + " DURING" +
                Integer.toString(during.size()) + " AFTER" + Integer.toString(after.size()));
        for(SensorEventData e : before){
            sb.append(e.toCSV());
            sb.append("BEFORE,");
            sb.append(eventType).append(",").append(eventAction).append("\n");
        }
        for(SensorEventData e : during){
            sb.append(e.toCSV());
            sb.append("DURING,");
            sb.append(eventType).append(",").append(eventAction).append("\n");
        }
        for(SensorEventData e : after){
            sb.append(e.toCSV());
            sb.append("AFTER,");
            sb.append(eventType).append(",").append(eventAction).append("\n");
        }
        try {
            csvStream.writeChars(sb.toString());
            csvStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }


}
