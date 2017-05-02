package jenarvaezg.tapgatherer;

import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingQueue;


public class FeatureWorker implements Runnable{


    LinkedBlockingQueue<SensorEventData> externalEventsQueue;
    private ArrayList<SensorEventData> events;

    private static final Integer WINDOW_SIZE = 20;
    private Thread thread;
    private NetworkWorker networkWorker;

    private static final String TAG = "FEATURE WORKER";
    private NetworkWorker.Urls url;
    private String CSVheaders;
    private String app;

    @Override
    public void run() {
        try {
            networkWorker = new NetworkWorker(url);
            networkWorker.toSendQueue.add(CSVheaders);
            for(SensorEventData event = externalEventsQueue.take();; event = externalEventsQueue.take()){
                events.add(event);
                if(events.size() > WINDOW_SIZE){
                    events.remove(0);
                    final SensorEventData[] events_a = events.toArray(new SensorEventData[WINDOW_SIZE]);
                    EventWindowFeatures features = new EventWindowFeatures(events_a, false, app);
                    networkWorker.toSendQueue.add(features.toCSV());
                }
            }
        } catch (InterruptedException e) {
            Log.d(TAG, Log.getStackTraceString(e));
        } catch (IOException e) {
            Log.d(TAG, Log.getStackTraceString(e));
        }
    }

    public FeatureWorker(NetworkWorker.Urls url, String CSVheaders, String app){
        this.externalEventsQueue = new LinkedBlockingQueue<>();
        this.events = new ArrayList<>();
        this.thread  = new Thread(this);
        this.thread.start();
        this.url = url;
        this.CSVheaders = CSVheaders;
        this.app = app;
    }

    public void stop(){
        if(this.networkWorker != null) this.networkWorker.stop();
        this.thread.interrupt();

    }
}