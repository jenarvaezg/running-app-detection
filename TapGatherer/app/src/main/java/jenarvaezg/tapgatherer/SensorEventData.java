package jenarvaezg.tapgatherer;

import android.hardware.Sensor;
import android.hardware.SensorEvent;

import java.sql.Time;
import java.util.Arrays;

/**
 * Created by jenarvaez on 09/02/2017.
 */

public class SensorEventData  {

    Long timestamp;
    float values[];
    String sensorName;


    SensorEventData(SensorEvent e, SensorGatherService.TimeBase offset){
        //clone values
        this.timestamp = (e.timestamp - offset.timestampBase) / 1000000L + offset.dateBase;
        this.values = e.values.clone();
        switch(e.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                sensorName = "Accelerometer";
                break;
            case Sensor.TYPE_GYROSCOPE:
                sensorName = "Gyroscope";
                break;
            case Sensor.TYPE_GAME_ROTATION_VECTOR:
                sensorName = "GameRotationVector";
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                sensorName = "RotationVector";
                break;
        }
    }

    public String toString(){
        return sensorName + " at " + Long.toString(timestamp) + ": " + Arrays.toString(values);
    }

    public String toCSV() {
        StringBuilder sb = new StringBuilder();
        float[] values = new float[5];
        //this way we can use 3d sensors and quaternions
        System.arraycopy(this.values, 0, values, 0, this.values.length);
        sb.append(timestamp).append(",").append('"' + sensorName + '"').append(",");
        for(float value : values){
            sb.append(value).append(",");
        }
        return sb.toString();
    }
}
