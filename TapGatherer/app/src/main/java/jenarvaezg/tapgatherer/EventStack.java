package jenarvaezg.tapgatherer;

import android.hardware.SensorEvent;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeMap;


public class EventStack extends TreeMap<Long, SensorEventData> {

    private static final String TAG = "EVENTSTACK";


    public Collection<SensorEventData> getInRange(Long start, Long end){
        try {
            return this.subMap(start, end).values();
        }catch (NullPointerException e){
            Log.d(TAG, Log.getStackTraceString(e));
            return new ArrayList<SensorEventData>();
        }
    }

    public Collection<SensorEventData> getBefore(Long t){
        try{
            return this.headMap(t).values();
        }catch (NullPointerException e){
            Log.d(TAG, Log.getStackTraceString(e));
            return new ArrayList<SensorEventData>();
        }
    }

    public Collection<SensorEventData> getAfter(Long t){
        try{
            return this.tailMap(t).values();
        }catch (NullPointerException e){
            Log.d(TAG, Log.getStackTraceString(e));
            return new ArrayList<SensorEventData>();
        }
    }
}
