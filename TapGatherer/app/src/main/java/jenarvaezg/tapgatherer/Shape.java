package jenarvaezg.tapgatherer;

import android.graphics.Canvas;
import android.graphics.Point;

/**
 * Created by jose on 6/4/17.
 */
interface Shape {

    boolean isPointInside(Point p);
    void draw(Canvas c);

}
