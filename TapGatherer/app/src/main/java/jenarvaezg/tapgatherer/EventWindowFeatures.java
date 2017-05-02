package jenarvaezg.tapgatherer;

import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Created by jenarvaez on 02/03/2017.
 */

public class EventWindowFeatures{

    private static final Integer WINDOW_SIZE = 20;
    private static final String TAG ="EVENTWINDOWFEATURES";

    private Integer n_accel = 0;
    private Integer n_gyro = 0;
    private Float[] means = new Float[]{0f,0f,0f,0f,0f,0f};
    private Float[] vars = new Float[]{0f,0f,0f,0f,0f,0f};
    private Float[] medians = new Float[]{0f,0f,0f,0f,0f,0f};
    private Float[] skewnesses = new Float[]{0f,0f,0f,0f,0f,0f};
    private Float[] kurtoses = new Float[]{0f,0f,0f,0f,0f,0f};
    private Float[] diffs = new Float[]{0f,0f,0f,0f,0f,0f};
    private Boolean isLabeled = false;
    private String when, action, type;
    private Integer noise = 0;
    private String app;

    EventWindowFeatures(SensorEventData[] events, Boolean isLabeled, String app){
        this.app = app;
        this.isLabeled = isLabeled;
        final ArrayList<SensorEventData> gyroEvents = new ArrayList<>();
        final ArrayList<SensorEventData> accelEvents = new ArrayList<>();
        for(SensorEventData event : events){
            if(event.sensorName.equals("Accelerometer")){
                accelEvents.add(event);
                n_accel++;
            }else{
                gyroEvents.add(event);
                n_gyro++;
            }
        }
        Thread taccel = new Thread(new Runnable(){
            public void run(){
                extractFeatures("accel", accelEvents);
            }
        });
        Thread tgyro = new Thread(new Runnable(){
            public void run(){
                extractFeatures("gyro", gyroEvents);
            }
        });
        taccel.start();
        tgyro.start();
        try {
            tgyro.join();
            taccel.join();
        } catch (InterruptedException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    void setLabels(String when, String action, String type){
        this.when = when;
        if(!when.equals("DURING")){
            this.action = "NOISE";
            this.type = "NOISE";
            this.noise = 1;
            return;
        }
        this.action = action;
        this.noise = 0;
        this.type = type;
    }

    private void extractFeatures(String sensor, ArrayList<SensorEventData> events){
        Integer offset = sensor.equals("accel") ? 0 : 3;
        Integer size = events.size();
        if(size == 0){
            return; //this is ok since all is initialized to 0
        }
        Float[] sums = new Float[]{0f, 0f, 0f};
        Float[] sorted_x = new Float[size];
        Float[] sorted_y = new Float[size];
        Float[] sorted_z = new Float[size];
        //ugly but I'm trying to reduce unnecessary loops

        for(int i = 0; i < size; i++){
            sorted_x[i] = events.get(i).values[0];
            sorted_y[i] = events.get(i).values[1];
            sorted_z[i] = events.get(i).values[2];
            sums[0] = sums[0] +  sorted_x[i];
            sums[1] += sorted_y[i];
            sums[2] += sorted_z[i];
        }
        Arrays.sort(sorted_x);
        Arrays.sort(sorted_y);
        Arrays.sort(sorted_z);
        diffs[offset] = sorted_x[size-1] - sorted_x[0];
        diffs[1+offset] = sorted_y[size-1] - sorted_y[0];
        diffs[2+offset] = sorted_z[size-1] - sorted_z[0];
        medians[offset] = sorted_x[size/2];
        medians[1+offset] = sorted_y[size/2];
        medians[2+offset] = sorted_z[size/2];
        means[offset] = sums[0] / size;
        means[1+offset] = sums[1] / size;
        means[2+offset] = sums[2] / size;
        Float[] xMinusMeanAccum = new Float[]{0f, 0f, 0f};
        for(int i = 0; i < size; i++) { //get variance, skewness and kurtosis
            xMinusMeanAccum[0] += events.get(i).values[0] - means[offset];
            xMinusMeanAccum[1] += events.get(i).values[1] - means[2+offset];
            xMinusMeanAccum[2] += events.get(i).values[2] - means[1+offset];
        }
        for(int i = 0; i < 3; i++) {
            vars[i + offset] = (float) Math.pow(xMinusMeanAccum[i], 2) / size;
            if (vars[i + offset] != 0) {

                skewnesses[i + offset] = (float) (Math.pow(xMinusMeanAccum[i], 3) /
                        Math.pow(vars[i + offset], (3 / 2)) / size);
                kurtoses[i + offset] = (float) (Math.pow(xMinusMeanAccum[i], 4) /
                        Math.pow(vars[i + offset], (3)) / size);
            }
        }
    }


    public static String getCSVHeader(){
        return "n_accel,n_gyro,"+
                "accel_x_mean,accel_x_median,accel_x_var,"+
                "accel_x_skewness,accel_x_kurtosis,accel_x_diff,"+
                "accel_y_mean,accel_y_median,accel_y_var,"+
                "accel_y_skewness,accel_y_kurtosis,accel_y_diff,"+
                "accel_z_mean,accel_z_median,accel_z_var,"+
                "accel_z_skewness,accel_z_kurtosis,accel_z_diff,"+
                "gyro_x_mean,gyro_x_median,gyro_x_var,"+
                "gyro_x_skewness,gyro_x_kurtosis,gyro_x_diff,"+
                "gyro_y_mean,gyro_y_median,gyro_y_var,"+
                "gyro_y_skewness,gyro_y_kurtosis,gyro_y_diff,"+
                "gyro_z_mean,gyro_z_median,gyro_z_var,"+
                "gyro_z_skewness,gyro_z_kurtosis,gyro_z_diff\n";
    }

    public static String getTrainingTapsCSVHeader() {
        String base = getCSVHeader();
        return base.substring(0, base.length() - 1) + ",when,noise,type,action\n";
    }

    public static String getTrainingAppsCSVHeader() {
        String base = getCSVHeader();
        return base.substring(0, base.length() - 1) + ",app\n";
    }

    public static String getPredictingCSVHeader(){
        return getCSVHeader();
    }

    String toCSV() {
        StringBuilder sb = new StringBuilder();
        sb.append(n_accel).append(",").append(n_gyro);
        for(int i = 0; i < 6; i++){
            sb.append(",").append(means[i]).append(",").append(medians[i]).append(",").
                    append(vars[i]).append(",").append(skewnesses[i]).append(",")
                    .append(kurtoses[i]).append(",").append(diffs[i]);
        }
        if(isLabeled){
            sb.append(",").append(when).append(",").append(noise).append(",").
                    append(type).append(",").append(action);
        }
        if(app != null){
            sb.append(",").append(app);
        }
        sb.append("\n");
        return sb.toString();
    }
}
