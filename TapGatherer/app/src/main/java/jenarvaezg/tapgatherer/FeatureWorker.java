package jenarvaezg.tapgatherer;

import android.hardware.SensorEvent;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by jose on 4/15/17.
 */
public class FeatureWorker implements Runnable{


    //private ArrayList<EventWindowFeatures> eventFeatures;
    LinkedBlockingQueue<SensorEventData> externalEventsQueue;
    private ArrayList<SensorEventData> events;

    private Integer WINDOW_SIZE = 20;
    private static Thread thread;

    private String TAG = "FEATURE WORKER";

    @Override
    public void run() {
        NetworkWorker networkWorker = null;

        try {
            networkWorker = new NetworkWorker(NetworkWorker.Urls.PREDICT_TAPS);
            networkWorker.toSendQueue.add(EventWindowFeatures.getCSVHeader());
            for(SensorEventData event = externalEventsQueue.take();; event = externalEventsQueue.take()){
                events.add(event);
                if(events.size() > WINDOW_SIZE){
                    events.remove(0);
                    final SensorEventData[] events_a = events.toArray(new SensorEventData[WINDOW_SIZE]);

                    EventWindowFeatures features = new EventWindowFeatures(events_a, false);


                    networkWorker.toSendQueue.add(features.toCSV());
                }

            }
        } catch (InterruptedException e) {

            Log.d(TAG, Log.getStackTraceString(e));
        } catch (IOException e) {
            Log.d(TAG, Log.getStackTraceString(e));
        } finally {
            networkWorker.stop();
        }
    }

    public FeatureWorker(){
        this.externalEventsQueue = new LinkedBlockingQueue<>();
        //this.eventFeatures = new ArrayList<>();
        this.events = new ArrayList<>();
        this.thread  = new Thread(this);
        this.thread.start();
    }

    public void stop(){
        this.thread.interrupt();

    }
}
/*eventFeatures.add(features);
                    if(eventFeatures.size() >= 500){
                        final EventWindowFeatures[] features_a =
                                eventFeatures.toArray(new EventWindowFeatures[500]);
                        //send features_a via network


                        Thread t = new Thread(new Runnable() {
                            @Override
                            public void run() {

                                //send features via network
                                StringBuilder sb = new StringBuilder();
                                sb.append();
                                for (EventWindowFeatures feature : features_a) {
                                    sb.append(feature.toCSV());
                                }
                                NetworkWorker.sendString(, sb.toString(), false);
                                Log.d(TAG, "SENT PREDICT");
                            }
                        });
                        t.start();

                        eventFeatures = new ArrayList<>();
                    }*/