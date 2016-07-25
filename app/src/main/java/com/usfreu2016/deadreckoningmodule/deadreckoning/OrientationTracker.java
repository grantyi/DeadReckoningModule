package com.usfreu2016.deadreckoningmodule.deadreckoning;
import android.util.Log;

public class OrientationTracker {

    private final String TAG = "OrientationTracker";

    private OrientationChangedListener orientationChangedListener;
    private final float northEast = 45f;
    private final float southEast = 135f;
    private final float southWest = 225f;
    private final float northWest = 315f;
    private final float padding = 10f;
    private float initialAngle;
    int currentAngle;

    public void check(float degree) {
        float angle = degree - initialAngle;
        if (angle < 0) {
            angle = 360 + angle;
        }


        // check if north
        if (currentAngle != 0 && (angle >= (northWest + padding) || angle <= (northEast - padding))) {
            currentAngle = 0;
            orientationChangedListener.onChanged(currentAngle);
        }
        else if (currentAngle != 90 && (angle >= (northEast + padding) && angle <= (southEast - padding))) {
            currentAngle = 90;
            orientationChangedListener.onChanged(currentAngle);
        }
        else if (currentAngle != 180 && (angle >= (southEast + padding) && angle <= (southWest - padding))) {
            currentAngle = 180;
            orientationChangedListener.onChanged(currentAngle);
        }
        else if (currentAngle != 270 && (angle >= (southWest + padding) && angle <= (northWest - padding))) {
            currentAngle = 270;
            orientationChangedListener.onChanged(currentAngle);
        }

    }

    public void setInitialOrientation(float degree) {
        initialAngle = degree;
    }

    public void setOrientationChangedListener(OrientationChangedListener orientationChangedListener) {
        this.orientationChangedListener = orientationChangedListener;
    }

    public interface OrientationChangedListener {
        void onChanged(int angle);

    }
}
