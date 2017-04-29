package jenarvaezg.tapgatherer;

import android.app.IntentService;
import android.content.Intent;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;


public class SensorGatherService extends IntentService implements SensorEventListener {


    public SensorGatherService() {
        super("SensorGatherService");
    }

    private static FeatureWorker featureWorker;
    private static final String TAG = "Service";

    protected static final String ACTION_START = "START";
    protected static final String ACTION_PREDICT = "PREDICT";
    protected static final String ACTION_TRAIN = "TRAIN";
    protected static final String ACTION_STOP = "STOP";
    protected static final String ACTION_STORE_TOUCH_EVENT = "STORE";
    private static Boolean predicting = false;
    private static Boolean training = false;

    private static SensorManager mSensorManager;

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

        NetworkWorker.externalFilesDir = getExternalFilesDir(null);
        if (intent != null) {
            final String action = intent.getAction();
            Log.d(TAG, "GOT ACTION " + action);
            if (ACTION_START.equals(action)) {
                Log.d(TAG, "Go!");
            }else if(ACTION_STORE_TOUCH_EVENT.equals(action)){
                Bundle bundle = intent.getExtras();
                storeTouchEvent(bundle);
            }else if(ACTION_STOP.equals(action)) {
                stopSensors();
                if(training){
                    sendTrainCommand();
                }else if(predicting){
                    featureWorker.stop();
                }

                // TODO events = new ArrayList<>();
                // eventFeatures = new ArrayList<>();
                training = false;
                predicting = false;
            }else if(ACTION_TRAIN.equals(action)) {
                startSensors(SensorManager.SENSOR_DELAY_FASTEST);
                startTraining();
            }else if(ACTION_PREDICT.equals(action)){
                startSensors(SensorManager.SENSOR_DELAY_FASTEST);
                featureWorker = new FeatureWorker();
                predicting = true;
            }else{
                Log.wtf(TAG, "DUDE WHAT '" + action + "'");
            }
        }
    }

    private void startSensors(Integer delay){

        eventStack = new EventStack();

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        Sensor mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor mGyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        mSensorManager.registerListener(this, mAccelerometer, delay);
        mSensorManager.registerListener(this, mGyroscope, delay);
    }

    private void startTraining(){
        training = true;
    }

    private void stopSensors(){
        mSensorManager.unregisterListener(this);
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
            eventStack.put((event.timestamp - offset.timestampBase) / 1000000L + offset.dateBase,
                        new SensorEventData(event, offset));
        }else if(predicting){
            featureWorker.externalEventsQueue.add(new SensorEventData(event, offset));

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
            before = eventStack.getInRange(start - ((end-start) ), start);
            //before = eventStack.getBefore(start); too much noise
            during = eventStack.getInRange(start, end);
        }
        try {
            Thread.sleep((end-start)); //wait a bit to collect after touch events
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //noise after event
        synchronized (EventStack.class){
            after = eventStack.getAfter(end);
            eventStack = new EventStack();
        }
        final String type = b.getString("TYPE");
        final String action = b.getString("ACTION");
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                EventWindowFeatures[] features = extractTrainFeatures(type, action,
                        before, during, after);
                //send features via network
                StringBuilder sb = new StringBuilder();
                sb.append(EventWindowFeatures.getTrainingCSVHeader());
                for(EventWindowFeatures feature: features){
                    sb.append(feature.toCSV());
                }
                NetworkWorker.sendString(NetworkWorker.Urls.UPDATE_TAPS, sb.toString(), false);
                Log.d(TAG, "SENT");
            }
        });
        t.start();

    }

    private EventWindowFeatures[] extractTrainFeatures(String eventType, String eventAction,
                                                       Collection<SensorEventData> before,
                                                       Collection<SensorEventData> during,
                                                       Collection<SensorEventData> after){

        Integer WINDOW_SIZE = 20;
        Log.d(TAG, "BEFORE: " + Integer.toString(before.size())  + " DURING: " + Integer.toString(during.size()) +  " AFTER: " + Integer.toString(after.size()));
        Integer startDuringPos = before.size();
        Integer endDuringPos = startDuringPos + during.size();
        SensorEventData[] events = new SensorEventData[endDuringPos + after.size()];
        System.arraycopy(before.toArray(new SensorEventData[startDuringPos]), 0, events, 0, startDuringPos);
        System.arraycopy(during.toArray(new SensorEventData[during.size()]), 0, events, startDuringPos,
                endDuringPos - startDuringPos);
        System.arraycopy(after.toArray(new SensorEventData[after.size()]), 0, events, endDuringPos, after.size());
        EventWindowFeatures[] features = new EventWindowFeatures[events.length - WINDOW_SIZE];
        for(int i = 0; i < events.length - WINDOW_SIZE; i++){
            String when = i + WINDOW_SIZE - 1 < startDuringPos ? "BEFORE" : i + WINDOW_SIZE - 1 <
                    endDuringPos ? "DURING" : "AFTER";
            SensorEventData[] thisWindowEvents = Arrays.copyOfRange(events, i, i+WINDOW_SIZE);
            EventWindowFeatures thisWindowFeatures = new EventWindowFeatures(thisWindowEvents, true);
            thisWindowFeatures.setLabels(when, eventAction, eventType);
            features[i] = thisWindowFeatures;
        }

        Log.d(TAG, "N FEATURES " + Integer.toString(features.length));

        return features;
    }


    private void sendTrainCommand(){
        Log.d(TAG, "SENDING TRAIN TAPS");
        NetworkWorker.sendString(NetworkWorker.Urls.TRAIN_TAPS, "", true);
        Log.d(TAG, "MODEL READY");
    }

}
