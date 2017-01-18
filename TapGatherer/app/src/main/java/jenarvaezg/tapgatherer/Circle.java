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

class Circle {
    private Float x;
    private Float y;
    private Float r;
    private Paint paint;
    private Integer minRadiusFraction = 20;
    private Integer maxRadiusFraction = 10;

    Circle(Canvas c){
        Point size = new Point(c.getWidth(), c.getHeight());
        Random random = new Random(System.currentTimeMillis());
        this.x = random.nextFloat() * (size.x);
        this.y = random.nextFloat() * (size.y);
        //Area = pi * r^2
        //r = sqrt(Area/pi)
        Integer canvasArea = size.x * size.y;
        Double equivalentAreaRadius = Math.sqrt(canvasArea / Math.PI);
        Double minRadius = equivalentAreaRadius / minRadiusFraction;
        Double maxRadius = equivalentAreaRadius / maxRadiusFraction;

        this.r = (float) (random.nextFloat() * (maxRadius - minRadius) + minRadius);
        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(255, random.nextInt(255), random.nextInt(255), random.nextInt(255)));
    }

    void draw(Canvas c){
        c.drawCircle(x, y, r, paint);
    }

    boolean isPointInside(Point p){
        Double distance = Math.sqrt(Math.pow(p.x - this.x, 2) + Math.pow(p.y - this.y, 2));
        return distance <= this.r;
    }

    public String toString(){
        return "(" + Float.toString(x) + ", " + Float.toString(y) + ", " + Float.toString(r) + ")";
    }
}
