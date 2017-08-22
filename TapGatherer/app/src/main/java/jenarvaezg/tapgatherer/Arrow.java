package jenarvaezg.tapgatherer;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.util.Log;

import java.util.Random;


public class Arrow implements Shape{

    private Float x;
    private Float y;
    private Float length;
    private Float width;
    private Paint paint;
    private Boolean vertical;
    private Integer orientation;
    private RectF rect;
    private RectF rectBorder;
    private Path arrowPath;
    private static final String TAG = "ARROW";

    Arrow(Canvas c){
        Point size = new Point(c.getWidth(), c.getHeight());
        Random random = new Random(System.currentTimeMillis());

        this.vertical = random.nextInt() % 2 == 0;
        this.orientation = random.nextInt() % 2;

        this.length = vertical ? size.y * 0.75f : size.x * 0.7f;
        this.width = vertical ? size.x / 10f : size.y / 12f;

        if(vertical){
            this.x =  (random.nextFloat() * (size.x - 20) + 20);
            this.y = size.y * 0.125f;
        }else{
            this.y = (random.nextFloat() * (size.y - 20) + 20);
            this.x = size.x * 0.125f;
        }

        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(255, random.nextInt(255), random.nextInt(255), random.nextInt(255)));
        arrowPath = new Path();
        if(vertical){
            rect = new RectF(x, y, x+width, y+length);
            rectBorder = new RectF(x-5, y-5, x+width+5, y+length+5);
            if(orientation == 0){
                arrowPath.moveTo(x, y);
                arrowPath.rLineTo(-width / 2, 0);
                arrowPath.rLineTo(width, -width);
                arrowPath.rLineTo(width, width);
                arrowPath.rLineTo(-width/2, 0);
            }else{
                arrowPath.moveTo(x, y+length);
                arrowPath.rLineTo(-width/2, 0);
                arrowPath.rLineTo(width, width);
                arrowPath.rLineTo(width, -width);
                arrowPath.rLineTo(-width/2, 0);
            }
        }else{
            rect = new RectF(x, y, x+length, y+width);
            rectBorder = new RectF(x-5, y-5, x+length+5, y+width+5);
            if(orientation == 0){
                arrowPath.moveTo(x, y);
                arrowPath.rLineTo(0, -width/2);
                arrowPath.rLineTo(-width, width);
                arrowPath.rLineTo(width, width);
                arrowPath.rLineTo(0, -width/2);
            }else{
                arrowPath.moveTo(x+length, y);
                arrowPath.rLineTo(0, -width/2);
                arrowPath.rLineTo(width, width);
                arrowPath.rLineTo(-width, width);
                arrowPath.rLineTo(0, -width/2);
            }
        }

    }

    public void draw(Canvas c){
        Paint borderPaint = new Paint();
        borderPaint.setStyle(Paint.Style.FILL);
        borderPaint.setColor(Color.WHITE);
        c.drawRect(rectBorder, borderPaint);
        c.drawRect(rect, paint);
        c.drawPath(arrowPath, borderPaint);

    }

    public boolean isPointInside(Point p){
        RectF rectF = new RectF();
        arrowPath.computeBounds(rectF, true);
        Region r = new Region();
        r.setPath(arrowPath, new Region((int) rectF.left, (int) rectF.top, (int) rectF.right, (int) rectF.bottom));

        return rect.contains(p.x, p.y) || r.contains(p.x, p.y);
    }



    public String toString(){
        return "(" + Float.toString(x) + ", " + Float.toString(y) + ") Vertical?" +
                Boolean.toString(vertical) + ", Orientation? " + Integer.toString(orientation);
    }
}
