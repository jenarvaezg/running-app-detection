package jenarvaezg.tapgatherer;

import android.hardware.SensorEvent;

import java.util.Collection;
import java.util.TreeMap;


public class EventStack extends TreeMap<Long, SensorEvent> {

    public Collection<SensorEvent> getInRange(Long start, Long end){
        return this.subMap(start, end).values();
    }

}
