package jenarvaezg.tapgatherer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import java.util.Random;

/**
 * Created by jenarvaez on 18/01/2017.
 */

public class DrawView extends SurfaceView implements SurfaceHolder.Callback {
    Paint axisPaint = new Paint();
    final static private String TAG = "DrawView";
    Context context;
    Circle currentCircle;
    Boolean surfaceCreated = false;


    public DrawView(Context context, AttributeSet attrs){
        super(context, attrs);
        this.context = context;
        axisPaint.setColor(Color.WHITE);
        axisPaint.setStrokeWidth(10);
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        surfaceCreated = true;
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        tryDrawing(holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        surfaceCreated = false;
    }

    private void tryDrawing(SurfaceHolder holder) {
        Canvas canvas = holder.lockCanvas();
        if (canvas == null) {
            return;
        }
        drawGrid(canvas);
        if (currentCircle == null){
            currentCircle = new Circle(canvas);
        }
        currentCircle.draw(canvas);
        holder.unlockCanvasAndPost(canvas);
    }

    private void drawGrid(Canvas canvas){
        Point size = new Point(canvas.getWidth(), canvas.getHeight());
        canvas.drawLine(0, size.y/2, size.x, size.y/2, axisPaint);
        canvas.drawLine(size.x/2, 0, size.x/2, size.y, axisPaint);
    }

    private void drawNewCircle(){
        SurfaceHolder holder = this.getHolder();
        Canvas canvas = holder.lockCanvas();
        canvas.drawColor(Color.BLACK);
        drawGrid(canvas);
        currentCircle = new Circle(canvas);
        currentCircle.draw(canvas);
        holder.unlockCanvasAndPost(canvas);
    }

    void testTouch(Point p){
        if(currentCircle == null){
            return;
        }
        if (currentCircle.isPointInside(p)){
            drawNewCircle();
        }
    }


}
