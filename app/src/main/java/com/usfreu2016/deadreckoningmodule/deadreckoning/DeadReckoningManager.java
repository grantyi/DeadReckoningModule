package com.usfreu2016.deadreckoningmodule.deadreckoning;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.usfreu2016.deadreckoningmodule.Constants;

public class DeadReckoningManager {

    private final String TAG = "DeadReckoningManager";

    private final Context context;
    private InternalServiceConnection serviceConnection;
    private final Messenger incomingMessenger;
    private Messenger serviceMessenger;
    private DeadReckoningManager.ServiceReadyCallback callback;
    private DeadReckoningManager.StepListener stepListener;
    private DeadReckoningManager.OrientationListener orientationListener;

    /**
     * public Constructor
     * @param context
     */
    public DeadReckoningManager(Context context) {
        this.context = context;
        this.serviceConnection = new DeadReckoningManager.InternalServiceConnection();
        this.incomingMessenger = new Messenger(new DeadReckoningManager.IncomingHandler());
    }

    /**
     * Connects DeadReckoningManager to DeadReckoningService
     *
     * @param callback Service ready callback
     */
    public void connect(DeadReckoningManager.ServiceReadyCallback callback) {
        this.callback = callback;
        if(this.isConnectedToService()) {
            callback.onServiceReady();
        }

        boolean bound = this.context.bindService(new Intent(this.context, DeadReckoningService.class), this.serviceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Disconnects DeadReckoningManager from DeadReckoningService
     */
    public void disconnect() {
        if(this.isConnectedToService()) {

            this.context.unbindService(this.serviceConnection);
            this.callback = null;
            this.stepListener = null;
            this.serviceMessenger = null;

        }
    }

    /**
     * Set the vertical acceleration value for step detection
     * @param threshold The vertical acceleration threshold value
     */
    public void setVertAccThreshold(double threshold) {
        if (this.isConnectedToService()) {
            Message setVertThresholdMsg =
                    Message.obtain(null, Constants.SET_VERT_ACC_THRESHOLD_MSG);
            Bundle b = new Bundle();
            b.putDouble(Constants.VERT_THRESHOLD_KEY, threshold);
            setVertThresholdMsg.setData(b);
            try {
                this.serviceMessenger.send(setVertThresholdMsg);
            } catch (RemoteException e) {
                Log.e(TAG, "Error sending set vert acc threshold message to service.");
            }
        }
    }

    /**
     * Set the horizontal acceleration value for step detection
     * @param threshold The horizontal acceleration threshold value
     */
    public void setHoriAccThreshold(double threshold) {
        if (this.isConnectedToService()) {
            Message setHoriThresholdMsg =
                    Message.obtain(null, Constants.SET_HORI_ACC_THRESHOLD_MSG);
            Bundle b = new Bundle();
            b.putDouble(Constants.HORI_THRESHOLD_KEY, threshold);
            setHoriThresholdMsg.setData(b);
            try {
                this.serviceMessenger.send(setHoriThresholdMsg);
            } catch (RemoteException e) {
                Log.e(TAG, "Error sending set hori acc threshold message to service.");
            }
        }
    }

    /**
     * Set the step time threshold value for step detection
     * @param threshold The step time threshold value
     */
    public void setStepTimeThreshold(long threshold) {
        if (this.isConnectedToService()) {
            Message setStepTimeThresholdMsg =
                    Message.obtain(null, Constants.SET_STEP_TIME_THRESHOLD_MSG);
            Bundle b = new Bundle();
            b.putLong(Constants.STEP_TIME_THRESHOLD_KEY, threshold);
            setStepTimeThresholdMsg.setData(b);
            try {
                this.serviceMessenger.send(setStepTimeThresholdMsg);
            } catch (RemoteException e) {
                Log.e(TAG, "Error sending set step time threshold message to service.");
            }
        }
    }

    /**
     * Check if service is connected
     * @return True if connected to service
     */
    private boolean isConnectedToService() {
        return this.serviceMessenger != null;
    }

    /**
     * Sets a step listener
     * @param stepListener Listener for step event
     */
    public void setStepListener(DeadReckoningManager.StepListener stepListener) {
        this.stepListener = stepListener;
    }

    /**
     * Sets orientation listener
     * @param orientationListener Listener for orientation changes
     */
    public void setOrientationListener(DeadReckoningManager.OrientationListener orientationListener) {
        this.orientationListener = orientationListener;
    }

    /**
     * Starts step detection in service
     */
    public void startStepDetection() {
        if (this.isConnectedToService()) {
            Message startMsg = Message.obtain(null, Constants.START_STEP_DETECTION_MSG);

            try {
                this.serviceMessenger.send(startMsg);
            } catch (RemoteException e) {
                Log.e(TAG, "Error sending start step detection message to service.");
            }
        }
    }

    /**
     * Start tracking orientation
     */
    public void startOrientationTracking() {
        if (this.isConnectedToService()) {
            Message startMsg = Message.obtain(null, Constants.START_ORIENTATION_TRACKING_MSG);

            try {
                this.serviceMessenger.send(startMsg);
            } catch (RemoteException e) {
                Log.e(TAG, "Error sending start orientation tracking message to service.");
            }
        }
    }

    /**
     * Set initial orientation
     */
    public void setInitialOrientation() {
        if (this.isConnectedToService()) {
            Message msg = Message.obtain(null, Constants.SET_INITIAL_ORIENTATION_MSG);

            try {
                this.serviceMessenger.send(msg);
            } catch (RemoteException e) {
                Log.e(TAG, "Error sending set initial orientation message to service.");
            }
        }
    }

    /**
     * Registers a step listener in the DeadReckoningService
     */
    private void registerMessengerInService() {
        Message registerMsg = Message.obtain(null, Constants.REGISTER_MESSENGER_MSG);
        registerMsg.replyTo = this.incomingMessenger;

        try {
            this.serviceMessenger.send(registerMsg);
        } catch (RemoteException e) {

        }

    }

    /**
     * Handles incoming messages from DeadReckoningService
     */
    private class IncomingHandler extends Handler {
        private IncomingHandler() {
        }

        public void handleMessage(Message msg) {
            switch(msg.what) {
                case Constants.STEP_DETECTED_MSG:
                    if(DeadReckoningManager.this.stepListener != null) {
                        msg.getData().setClassLoader(Step.class.getClassLoader());
                        Step step = msg.getData().getParcelable(Constants.STEP_RESULT_KEY);
                        DeadReckoningManager.this.stepListener.onStep(step);
                    }
                    break;
                case Constants.ORIENTATION_CHANGED_MSG:
                    if (DeadReckoningManager.this.orientationListener != null) {
                        int angle = msg.getData().getInt(Constants.ANGLE_KEY);
                        DeadReckoningManager.this.orientationListener.onChanged(angle);
                    }
                default:
                    break;
            }

        }
    }

    /**
     * Handles connection to DeadReckoningService
     */
    private class InternalServiceConnection implements ServiceConnection {
        private InternalServiceConnection() {
        }

        public void onServiceConnected(ComponentName name, IBinder service) {
            DeadReckoningManager.this.serviceMessenger = new Messenger(service);

            if(DeadReckoningManager.this.stepListener!= null) {
                DeadReckoningManager.this.registerMessengerInService();
            }

            if(DeadReckoningManager.this.callback != null) {
                DeadReckoningManager.this.callback.onServiceReady();
                DeadReckoningManager.this.callback = null;
            }

        }

        public void onServiceDisconnected(ComponentName name) {
            DeadReckoningManager.this.serviceMessenger = null;
        }
    }

    /**
     * Interface for StepListener
     */
    public interface StepListener {
        void onStep(Step step);
    }

    /**
     * Interface for orientation changed listener
     */
    public interface OrientationListener {
        void onChanged(int angle);
    }

    /**
     * Callback triggered when DeadReckoningService is ready
     */
    public interface ServiceReadyCallback {
        void onServiceReady();
    }
}
