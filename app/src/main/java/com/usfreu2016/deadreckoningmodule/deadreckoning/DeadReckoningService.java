package com.usfreu2016.deadreckoningmodule.deadreckoning;
import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.usfreu2016.deadreckoningmodule.Constants;

public class DeadReckoningService extends Service implements SensorEventListener {

    private final String TAG = "DeadReckoningService";

    private final Messenger messenger = new Messenger(new DeadReckoningService.IncomingHandler());
    private Messenger replyTo;
    private SensorManager sensorManager;
    private StepDetector stepDetector;
    private OrientationTracker orientationTracker;
    private float currentDegree;
    private boolean bound;

    public DeadReckoningService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.bound = false;
        this.sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        this.bound = true;
        return this.messenger.getBinder();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_LINEAR_ACCELERATION:
                this.stepDetector.check(event.values);
                break;
            case Sensor.TYPE_ORIENTATION:
                currentDegree = Math.round(event.values[0]);
                this.orientationTracker.check(currentDegree);
                break;
            default:
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    /**
     * Start step detection
     */
    private void startStepDetection() {
        this.registerStepSensors();
        this.stepDetector = new StepDetector();
        this.stepDetector.setStepDetectedListener(new StepDetector.StepDetectedListener() {
            @Override
            public void onStepDetected(Step step) {
                try {
                    Message e = Message.obtain(null, Constants.STEP_DETECTED_MSG);
                    Bundle b = new Bundle();
                    b.putParcelable(Constants.STEP_RESULT_KEY, step);
                    e.setData(b);
                    DeadReckoningService.this.replyTo.send(e);
                } catch (RemoteException e) {
                    Log.e(TAG, "Error sending step detected message");
                }
            }
        });
    }

    /**
     * Start orientation tracking
     */
    private void startOrientationTracking() {
        this.registerOrientationSensors();
        this.orientationTracker = new OrientationTracker();
        this.orientationTracker.setOrientationChangedListener(new OrientationTracker.OrientationChangedListener() {
            @Override
            public void onChanged(int angle) {
                try {
                    Message e = Message.obtain(null, Constants.ORIENTATION_CHANGED_MSG);
                    Bundle b = new Bundle();
                    b.putInt(Constants.ANGLE_KEY, angle);
                    e.setData(b);
                    DeadReckoningService.this.replyTo.send(e);
                } catch (RemoteException e) {
                    Log.e(TAG, "Error sending orientation changed message");
                }
            }
        });
    }

    private void setInitialOrientation() {
        this.orientationTracker.setInitialOrientation(currentDegree);
    }

    /**
     * Register the sensors used for step detection
     */
    private void registerStepSensors() {
        this.sensorManager.registerListener(
                this, sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION), SensorManager.SENSOR_DELAY_FASTEST);
    }

    /**
     * Register the sensors used for orientation tracking
     */
    private void registerOrientationSensors() {
        this.sensorManager.registerListener(
                this, sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     * Unregister sensors used by the service
     */
    private void unregisterSensors() {
        this.sensorManager.unregisterListener(this);
    }

    /**
     * Handles incoming messages from DeadReckoningManager
     */
    private class IncomingHandler extends Handler {
        private IncomingHandler() {
        }

        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.REGISTER_MESSENGER_MSG:
                    DeadReckoningService.this.replyTo = msg.replyTo;
                    break;
                case Constants.START_STEP_DETECTION_MSG:
                    DeadReckoningService.this.startStepDetection();
                    break;
                case Constants.SET_VERT_ACC_THRESHOLD_MSG:
                    double vertAccThreshold = msg.getData().getDouble(Constants.VERT_THRESHOLD_KEY);
                    DeadReckoningService.this.stepDetector.setVertAccThreshold(vertAccThreshold);
                    break;
                case Constants.SET_HORI_ACC_THRESHOLD_MSG:
                    double horiAccThreshold = msg.getData().getDouble(Constants.HORI_THRESHOLD_KEY);
                    DeadReckoningService.this.stepDetector.setHoriAccThreshold(horiAccThreshold);
                    break;
                case Constants.SET_STEP_TIME_THRESHOLD_MSG:
                    long stepTimeThreshold = msg.getData().getLong(Constants.STEP_TIME_THRESHOLD_KEY);
                    DeadReckoningService.this.stepDetector.setStepTimeThreshold(stepTimeThreshold);
                    break;
                case Constants.START_ORIENTATION_TRACKING_MSG:
                    DeadReckoningService.this.startOrientationTracking();
                    break;
                case Constants.SET_INITIAL_ORIENTATION_MSG:
                    DeadReckoningService.this.setInitialOrientation();
                default:
                    break;
            }

        }
    }
}
