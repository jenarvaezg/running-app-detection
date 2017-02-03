package jenarvaezg.tapgatherer;

import android.content.Intent;
import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {
    TextView textView;
    DrawView drawView;

    private static final String TAG = "MAIN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = (TextView) findViewById(R.id.drawViewText);
        drawView = (DrawView) findViewById(R.id.drawView);
        Intent intent = new Intent(this, SensorGatherService.class);
        intent.putExtra("X", 0f);
        intent.putExtra("Y", 0f);
        intent.setAction(SensorGatherService.ACTION_UPDATE);
        startService(intent);
        intent = new Intent(this, SensorGatherService.class);
        intent.setAction(SensorGatherService.ACTION_START);
        startService(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onPause(){
        super.onPause();
        stopService(new Intent(getApplicationContext(), SensorGatherService.class));
    }


    private static TouchEvent currentMotionEvent;

    @Override
    public boolean onTouchEvent(MotionEvent event){
        float eventX = event.getX();
        float eventY = event.getY();
        Intent intent;

        textView.setText("X: " + Float.toString(eventX) + ", Y: " + Float.toString(eventY));
        /*Log.d(TAG, event.toString());
        Log.d(TAG, currentMotionEvent.toString());*/
        switch (event.getAction()){

            case MotionEvent.ACTION_DOWN:
                currentMotionEvent = new TouchEvent(event);

                int[] location = new int[2];
                drawView.getLocationInWindow(location);
                drawView.testTouch(new Point((int) eventX - location[0], (int) eventY-location[1]));
                /*intent = new Intent(this, SensorGatherService.class);
                intent.putExtra("X", eventX);
                intent.putExtra("Y", eventY);
                intent.setAction(SensorGatherService.ACTION_UPDATE);
                startService(intent);*/
                return true;
            case MotionEvent.ACTION_UP:
                currentMotionEvent.push(event);
                intent = new Intent(this, SensorGatherService.class);
                intent.putExtra("TYPE", currentMotionEvent.getType());
                intent.putExtra("ACTION", currentMotionEvent.getAction(getWindowManager()));
                intent.putExtra("START", currentMotionEvent.getFirst().getEventTime());
                intent.putExtra("END", currentMotionEvent.getLast().getEventTime());
                intent.setAction(SensorGatherService.ACTION_UPDATE);
                startService(intent);
                Log.d(TAG, intent.getStringExtra("TYPE") + " " + intent.getStringExtra("ACTION"));
                return true;
            case MotionEvent.ACTION_MOVE:
                currentMotionEvent.push(event);
                return true;
            default:
                return false;

        }
    }

}
