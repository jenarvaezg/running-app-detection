package jenarvaezg.tapgatherer;

import android.content.Intent;
import android.graphics.Point;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent intent = new Intent(this, SensorGatherService.class);
        intent.putExtra("X", 0);
        intent.putExtra("Y", 0);
        intent.setAction(SensorGatherService.ACTION_UPDATE);
        startService(intent);
        intent = new Intent(this, SensorGatherService.class);
        intent.setAction(SensorGatherService.ACTION_START);
        startService(intent);
    }

    @Override
    protected void onPause(){
        super.onPause();
        stopService(new Intent(getApplicationContext(), SensorGatherService.class));
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        float eventX = event.getX();
        float eventY = event.getY();

        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                textView.setText("X: " + Float.toString(eventX) + ", Y: " + Float.toString(eventY));
                int[] location = new int[2];
                drawView.getLocationInWindow(location);
                drawView.testTouch(new Point((int) eventX - location[0], (int) eventY-location[1]));
                Intent intent = new Intent(this, SensorGatherService.class);
                intent.putExtra("X", eventX);
                intent.putExtra("Y", eventY);
                intent.setAction(SensorGatherService.ACTION_UPDATE);
                startService(intent);
                return true;
            default:
                return false;
        }
    }

}
