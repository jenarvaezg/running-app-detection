package jenarvaezg.tapgatherer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.util.Log;

import java.util.Random;

/**
 * Created by jenarvaez on 18/01/2017.
 */

class Circle implements Shape {
    private Float x;
    private Float y;
    private Float r;
    private Paint paint;
    private static final Integer MIN_RADIUS_FRACTION = 20;
    private static final Integer MAX_RADIUS_FRACTION = 10;



    Circle(Canvas c){
        Point size = new Point(c.getWidth(), c.getHeight());
        Random random = new Random(System.currentTimeMillis());
        //Area = pi * r^2
        //r = sqrt(Area/pi)
        Integer canvasArea = size.x * size.y;
        Double equivalentAreaRadius = Math.sqrt(canvasArea / Math.PI);
        Double minRadius = equivalentAreaRadius / MIN_RADIUS_FRACTION;
        Double maxRadius = equivalentAreaRadius / MAX_RADIUS_FRACTION;
        this.r = (float) (random.nextFloat() * (maxRadius - minRadius) + minRadius);

        //don't want circle to have parts outside canvas
        this.x = random.nextFloat() * (size.x - r) + r;
        this.y = random.nextFloat() * (size.y - r) + r;

        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(255, random.nextInt(255), random.nextInt(255), random.nextInt(255)));

    }

    public void draw(Canvas c){
        Paint borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.FILL);
        borderPaint.setColor(Color.WHITE);
        c.drawCircle(x, y, r+5, borderPaint);
        c.drawCircle(x, y, r, paint);
    }

    public boolean isPointInside(Point p){
        Double distance = Math.sqrt(Math.pow(p.x - this.x, 2) + Math.pow(p.y - this.y, 2));
        return distance <= this.r;
    }



    public String toString(){
        return "(" + Float.toString(x) + ", " + Float.toString(y) + ", " + Float.toString(r) + ")";
    }
}
