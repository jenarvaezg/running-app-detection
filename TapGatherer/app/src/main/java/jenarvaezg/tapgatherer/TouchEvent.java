package jenarvaezg.tapgatherer;


import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;

import java.util.LinkedList;

class TouchEvent extends LinkedList<MotionEvent>{

    private static final String TAG = "TOUCH_EVENT";

    private static final Integer TOUCH_THRESHOLD = 10;

    TouchEvent(MotionEvent event){
        push(event);
    }

    @Override
    public void push(MotionEvent event){
        super.push(MotionEvent.obtain(event));
    }

    private Boolean isTouch(){
        return size() <= TOUCH_THRESHOLD;
    }


    public Boolean isHorizontal(){
        MotionEvent first = getFirst();
        MotionEvent last = getLast();
        return Math.abs(first.getX() - last.getX()) > Math.abs(first.getY() - last.getY());
    }

    String getType(){
        return isTouch()? "TOUCH" : "SWIPE";
    }

    String getAction(WindowManager windowManager){
        if (isTouch()){
            return getTouchRegion(windowManager);
        }
        return getSwipeOrientation();
    }

    private String getSwipeOrientation() {
        if (isHorizontal()){
            return getFirst().getX() > getLast().getX() ? "RIGHT->LEFT" : "LEFT->RIGHT";
        }
        return getFirst().getY() > getLast().getY() ? "BOTTOM->TOP" : "TOP->BOTTOM";
    }

    private String getTouchRegion(WindowManager windowManager){
        DisplayMetrics displaymetrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(displaymetrics);
        int height = displaymetrics.heightPixels;
        int width = displaymetrics.widthPixels;
        //try to divide screen in 9 sectors, so we have center
        //if buggy, switch to 4(?)
        Integer X = (int) (getFirst().getX() + getLast().getX()) / 2;
        Integer Y = (int) (getFirst().getY() + getLast().getY()) / 2;
        String s = "";
        if(Y < height/3){
            s += "TOP";
        }else if(Y < height *2 / 3){
            s += "CENTER";
        }else{
            s += "BOTTOM";
        }
        if(X < width / 3){
            s += "-LEFT";
        }else if(X < width*2/3){
            s += s.equals("CENTER") ? "" : "-CENTER";
        }else{
            s+="-RIGHT";
        }

        return s;
    }
}
