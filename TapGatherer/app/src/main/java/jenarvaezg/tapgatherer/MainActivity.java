package jenarvaezg.tapgatherer;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.media.Image;
import android.os.SystemClock;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {
    TextView textView;
    DrawView drawView;

    private static final String TAG = "MAIN";
    private static final Long uptimestamp =  System.currentTimeMillis() - SystemClock.uptimeMillis();
    private static Integer nActions = 0;
    private static Switch startStopSwitch;
    static Boolean started = false;
    static Boolean training = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.drawViewText);
        textView.setText(Integer.toString(nActions));
        drawView = (DrawView) findViewById(R.id.drawView);
        drawView.setVisibility(View.GONE);
        startStopSwitch =(Switch) findViewById(R.id.startstop_button);
        Log.d(TAG, "Setting check to " + Boolean.toString(started));
        startStopSwitch.setChecked(started);

        startStopSwitch.setOnCheckedChangeListener(new Switch.OnCheckedChangeListener() {

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (!isChecked) {
                    started = false;
                    Intent intent = new Intent(MainActivity.this, SensorGatherService.class);
                    intent.setAction(SensorGatherService.ACTION_STOP);
                    startService(intent);
                    drawView.setVisibility(View.GONE);
                    MainActivity.training = false;
                } else {
                    if (started) {
                        Log.d(TAG, "Already started");
                        return;
                    }
                    started = true;
                    Intent intent = new Intent(MainActivity.this, SensorGatherService.class);
                    intent.setAction(SensorGatherService.ACTION_START);
                    startService(intent);

                    setupDialogs();

                }
            }
        });
    }


    void setupDialogs(){

        final Dialog appsDialog = new Dialog(this);
        appsDialog.setTitle(R.string.apps_chooser);


        AlertDialog.Builder appsOrTapsBuilder = new AlertDialog.Builder(MainActivity.this);
        appsOrTapsBuilder.setMessage(R.string.dialog_apps_taps);
        appsOrTapsBuilder.setPositiveButton(R.string.apps, new DialogInterface.OnClickListener() {

            final private void callService(String app) {
                Intent intent = new Intent(MainActivity.this, SensorGatherService.class);
                intent.setAction(SensorGatherService.ACTION_TRAIN);
                intent.putExtra("APP", app);
                intent.setType("APPS");
                startService(intent);
            }

            private void setupAppsDialog(){

                appsDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                appsDialog.setContentView(R.layout.apps_layout);
                appsDialog.setCanceledOnTouchOutside(false);
                appsDialog.setCancelable(false);

                Spinner appsSpinner = (Spinner) appsDialog.findViewById(R.id.apps_spinner);

                final String[] apps = new String[] {
                        "app to launch", "facebook", "whatsapp"
                };
                ArrayAdapter<String> adapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_spinner_item, apps);
                appsSpinner.setAdapter(adapter);

                appsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                    Boolean first = true;
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        Log.d(TAG, "FIRST? " + Boolean.toString(first) + " position: " + Integer.toString(position));
                        if (first){
                            first = false;
                            return;
                        }
                        Log.d(TAG, Integer.toString(position) +  " " + apps[position]);
                        callService(apps[position]);
                        appsDialog.dismiss();
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        Log.wtf(TAG, "NOTHING");
                    }
                });


                appsDialog.show();

            }

            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(MainActivity.training){
                    setupAppsDialog();
                }else {
                    Intent intent = new Intent(MainActivity.this, SensorGatherService.class);
                    intent.setAction(SensorGatherService.ACTION_PREDICT);
                    intent.setType("APPS");
                    startService(intent);
                }
                drawView.setVisibility(View.VISIBLE);
            }
        });

        appsOrTapsBuilder.setNegativeButton(R.string.taps, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent(MainActivity.this, SensorGatherService.class);
                intent.setAction(MainActivity.training ? SensorGatherService.ACTION_TRAIN : SensorGatherService.ACTION_PREDICT);
                intent.setType("TAPS");
                startService(intent);
                drawView.setVisibility(View.VISIBLE);
            }
        });
        final AlertDialog appsOrTapsDialog = appsOrTapsBuilder.create();
        appsOrTapsDialog.setCancelable(false);
        appsOrTapsDialog.setCanceledOnTouchOutside(false);


        AlertDialog.Builder builderTrainPredict = new AlertDialog.Builder(MainActivity.this);
        builderTrainPredict.setMessage(R.string.dialog_message).setTitle(R.string.dialog_title);
        builderTrainPredict.setPositiveButton(R.string.dialog_predict, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                appsOrTapsDialog.show();
            }
        });
        builderTrainPredict.setNegativeButton(R.string.dialog_train, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                MainActivity.training = true;
                appsOrTapsDialog.show();
            }
        });

        AlertDialog dialogTrainPredict = builderTrainPredict.create();
        dialogTrainPredict.setCancelable(false);
        dialogTrainPredict.setCanceledOnTouchOutside(false);
        dialogTrainPredict.show();
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause(){
        super.onPause();
        //stopService(new Intent(getApplicationContext(), SensorGatherService.class));
    }


    private static TouchEvent currentMotionEvent;

    @Override
    public boolean onTouchEvent(MotionEvent event){
        float eventX = event.getX();
        float eventY = event.getY();
        Intent intent;
        textView.setText(Integer.toString(nActions));
        switch (event.getAction()){

            case MotionEvent.ACTION_DOWN:
                nActions++;
                currentMotionEvent = new TouchEvent(event);
                int[] location = new int[2];
                drawView.getLocationInWindow(location);
                drawView.testTouch(new Point((int) eventX - location[0], (int) eventY-location[1]));
                return true;
            case MotionEvent.ACTION_UP:
                currentMotionEvent.add(event);
                intent = new Intent(this, SensorGatherService.class);
                intent.putExtra("TYPE", currentMotionEvent.getType());
                intent.putExtra("ACTION", currentMotionEvent.getAction(getWindowManager()));
                intent.putExtra("START", currentMotionEvent.getFirst().getEventTime() + uptimestamp);
                intent.putExtra("END", currentMotionEvent.getLast().getEventTime() + uptimestamp);
                intent.setAction(SensorGatherService.ACTION_STORE_TOUCH_EVENT);
                startService(intent);
                Log.d(TAG, intent.getStringExtra("TYPE") + " " + intent.getStringExtra("ACTION"));
                return true;
            case MotionEvent.ACTION_MOVE:
                currentMotionEvent.add(event);
                return true;
            default:
                return false;

        }
    }

}
