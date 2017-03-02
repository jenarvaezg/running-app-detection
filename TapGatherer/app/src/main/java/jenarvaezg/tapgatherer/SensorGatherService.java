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
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class SensorGatherService extends IntentService implements SensorEventListener {


    public SensorGatherService() {
        super("SensorGatherService");
    }

    private static FileWriter csvWriter;

    private static final String TAG = "Service";

    protected static final String ACTION_START = "START";
    protected static final String ACTION_PREDICT = "PREDICT";
    protected static final String ACTION_TRAIN = "TRAIN";
    protected static final String ACTION_STOP = "STOP";
    protected static final String ACTION_STORE_TOUCH_EVENT = "STORE";
    private static Boolean predicting = false;
    private static Boolean training = false;

    private static SensorManager mSensorManager;
    private static Integer WINDOW_SIZE = 20;

    private static EventStack eventStack;
    private static ArrayList<EventWindowFeatures> eventFeatures = new ArrayList<>();
    private static ArrayList<SensorEventData> events = new ArrayList<>();

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
                training = false;
                predicting = false;
            }else if(ACTION_TRAIN.equals(action)) {
                startTraining();
            }else if(ACTION_PREDICT.equals(action)){
                predicting = true;
            }else{
                Log.wtf(TAG, "DUDE WHAT '" + action + "'");
            }
        }
    }

    private void startSensors(){

        eventStack = new EventStack();

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        Sensor mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mGyroscope, SensorManager.SENSOR_DELAY_FASTEST);
    }

    private void startTraining(){
        training = true;
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
            csvWriter = new FileWriter(new File(f.getAbsolutePath() + "/"+dateString+".csv"));
            csvWriter.write("ID,timestamp,sensor,value0,value1,value2,when,type,action\n");
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            return;
        }
    }

    private void stopSensors(){
        mSensorManager.unregisterListener(this);
        try {
            csvWriter.close();
        } catch (Exception e) {
            //pass
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    //TODO fix training so we get features instead of raw

    /*The timestamps are not defined as being the Unix time;
    they're just "a time" that's only valid for a given sensor.
     This means that timestamps can only be compared if they come from the same sensor.
     ...
     O boi*/

    @Override
    public void onSensorChanged(SensorEvent event) {
        TimeBase offset = sensorOffsets.get(event.sensor.getType());
        if(offset == null){
            offset = new TimeBase(System.currentTimeMillis(), event.timestamp);
            sensorOffsets.put(event.sensor.getType(), offset);
        }
        if (training) {
            synchronized (EventStack.class) {
                eventStack.put((event.timestamp - offset.timestampBase) / 1000000L + offset.dateBase,
                        new SensorEventData(event, offset));
            }
        }else if(predicting){
            events.add(new SensorEventData(event, offset));
            if(events.size() > WINDOW_SIZE){
                events.remove(0);
            }

            if(events.size() == WINDOW_SIZE){
                final SensorEventData[] events_a = events.toArray(new SensorEventData[0]);

                EventWindowFeatures features = new EventWindowFeatures(events_a);
                synchronized (eventFeatures) {
                    eventFeatures.add(features);
                    if(eventFeatures.size() >= 100){
                        final EventWindowFeatures[] features_a =
                                eventFeatures.toArray(new EventWindowFeatures[0]);
                        eventFeatures = new ArrayList<>();
                        //send features_a via network
                        Log.d(TAG, features_a[0].toCSV());
                    }
                }
            }

        }
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    private void storeTouchEvent(Bundle b){
        if(!training){
            return;
        }
        Long start = b.getLong("START");
        Long end = b.getLong("END");
        final Collection<SensorEventData> before, during, after;
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
        final Integer id = before.hashCode();
        final String type = b.getString("TYPE");
        final String action = b.getString("ACTION");
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                EventWindowFeatures[] features = extractTrainFeatures(type, action,
                        before, during, after, id);
                //send features via network
                Log.d(TAG, Integer.toString(features.length));
            }
        });
        t.run();

        //writeToCSVFile(b.getString("TYPE"), b.getString("ACTION"),  before, during, after, id);

    }

    private EventWindowFeatures[] extractTrainFeatures(String eventType, String eventAction,
                                                       Collection<SensorEventData> before,
                                                       Collection<SensorEventData> during,
                                                       Collection<SensorEventData> after,
                                                       Integer id){
        Integer startDuringPos = before.size();
        Integer endDuringPos = startDuringPos + during.size();
        SensorEventData[] events = new SensorEventData[endDuringPos + after.size()];
        System.arraycopy(before.toArray(new SensorEventData[0]), 0, events, 0, startDuringPos);
        System.arraycopy(during.toArray(new SensorEventData[0]), 0, events, startDuringPos,
                endDuringPos - startDuringPos);
        System.arraycopy(after.toArray(new SensorEventData[0]), 0, events, endDuringPos, after.size());
        EventWindowFeatures[] features = new EventWindowFeatures[events.length - WINDOW_SIZE];
        for(int i = 0; i < events.length - WINDOW_SIZE; i++){
            String when = i < startDuringPos ? "BEFORE" : i < endDuringPos ? "DURING" : "AFTER";
            SensorEventData[] thisWindowEvents = Arrays.copyOfRange(events, i, i+WINDOW_SIZE);
            EventWindowFeatures thisWindowFeatures = new EventWindowFeatures(thisWindowEvents);
            thisWindowFeatures.setLabels(when, eventAction, eventType);
            features[i] = thisWindowFeatures;
        }


        return features;
    }

    private synchronized void writeToCSVFile(String eventType, String eventAction,
                                Collection<SensorEventData> before,
                                Collection<SensorEventData> during,
                                Collection<SensorEventData> after,
                                Integer id) {
        StringBuilder sb = new StringBuilder();

        Log.d(TAG, "Before " + Integer.toString(before.size()) + " DURING" +
                Integer.toString(during.size()) + " AFTER" + Integer.toString(after.size()));
        for(SensorEventData e : before){
            sb.append(id).append(",");
            sb.append(e.toCSV());
            sb.append("\"BEFORE\",");
            sb.append('"' +eventType+ '"').append(",").append('"' +eventAction + '"').append("\n");
        }
        for(SensorEventData e : during){
            sb.append(id).append(",");
            sb.append(e.toCSV());
            sb.append("\"DURING\",");
            sb.append('"' +eventType+ '"').append(",").append('"' +eventAction + '"').append("\n");
        }
        for(SensorEventData e : after){
            sb.append(id).append(",");
            sb.append(e.toCSV());
            sb.append("\"AFTER\",");
            sb.append('"' +eventType+ '"').append(",").append('"' +eventAction + '"').append("\n");
        }
        try {
            Log.d(TAG, sb.toString());
            csvWriter.write(sb.toString());
            csvWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }


}
