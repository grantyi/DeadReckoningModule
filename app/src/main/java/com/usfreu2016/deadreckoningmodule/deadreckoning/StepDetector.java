package com.usfreu2016.deadreckoningmodule.deadreckoning;
import java.util.Calendar;

public class StepDetector {

    private String TAG = "StepDetector";

    private StepDetectedListener stepDetectedListener;
    private Step previousStep;
    private double vertAccThreshold;
    private double horiAccThreshold;
    private double stepLength;
    private double stepLengthConstant;
    private double peakVertAcc;
    private double valleyVertAcc;
    private double horiPeakAcc;
    private double horiValleyAcc;
    private long stepTimeThreshold;
    private boolean stepDetected;
    private boolean peakVertAccRec;
    private boolean startValleyVertAccRec;
    private boolean valleyVertAccRec;

    public StepDetector() {
        this.vertAccThreshold = 2.0;
        this.horiAccThreshold = 1.0;
        this.stepLengthConstant = 0.53;
        this.peakVertAcc = 0;
        this.valleyVertAcc = 0;
        this.horiPeakAcc = 0;
        this.horiValleyAcc = 0;
        this.stepDetected = false;
        this.peakVertAccRec = false;
        this.startValleyVertAccRec = false;
        this.valleyVertAccRec = false;
        this.stepTimeThreshold = 500;
    }

    /**
     * check values from a linear acceleration sensor
     *
     * @param values x, y, z values of the linear acceleration virtual sensor
     */
    public void check(float[] values) {

        float yA = values[1];
        float zA = values[2];

        /** check for peak or valley hori values */
        if (yA > horiPeakAcc) {
            horiPeakAcc = yA;
        }
        else if (yA < horiValleyAcc) {
            horiValleyAcc = yA;
        }

        /** if peak and valley acceleration are recorded and the current value
         * is greater greater than threshold, then step detected
         */
        if (this.peakVertAccRec
                && this.valleyVertAccRec
                && (zA > this.vertAccThreshold)
                && (horiPeakAcc - horiValleyAcc) > horiAccThreshold) {

            /** Check if enough time has passed between steps */
            if (previousStep != null) {
                long currentTime = Calendar.getInstance().getTimeInMillis();
                long stepTime = currentTime - this.previousStep.getTimeStamp();
                if (stepTime < this.stepTimeThreshold) {
                    return;
                }
            }

            /** Compute step length */
            this.stepLength = computeStepLength(this.peakVertAcc, this.valleyVertAcc);

            this.previousStep = new Step(this.stepLength, Calendar.getInstance().getTimeInMillis());

            /** Fire step detected event chain */
            this.stepDetectedListener.onStepDetected(previousStep);

            /** Reset for next step */
            this.peakVertAcc = 0;
            this.valleyVertAcc = 0;
            this.horiPeakAcc = 0;
            this.horiValleyAcc = 0;
            this.peakVertAccRec = false;
            this.startValleyVertAccRec = false;
            this.valleyVertAccRec = false;

        }

        /** If value is greater than the vertical acceleration threshold */
        if (zA > this.vertAccThreshold) {

            // record peak acceleration
            if (zA > this.peakVertAcc) {
                this.peakVertAcc = zA;
                this.peakVertAccRec = true;
            }
        }

        /** if peak is recorded and acceleration is decreasing */
        if (this.peakVertAccRec && (zA - this.peakVertAcc) < 0) {
            startValleyVertAccRec = true;
        }

        /** if currently recording valley of vertical acceleration */
        if (startValleyVertAccRec && zA < valleyVertAcc) {
            valleyVertAcc = zA;
            valleyVertAccRec = true;
        }
    }

    /**
     * Set the threshold for vertical acceleration step detection
     * @param threshold The threshold value
     */
    public void setVertAccThreshold(double threshold) {
        this.vertAccThreshold = threshold;
    }

    public void setHoriAccThreshold(double threshold) {
        this.horiAccThreshold = threshold;
    }

    public void setStepTimeThreshold(long threshold) {
        this.stepTimeThreshold = threshold;
    }

    /**
     * Set a listener for step detection
     * @param stepDetectedListener
     */
    public void setStepDetectedListener(StepDetectedListener stepDetectedListener) {
        this.stepDetectedListener = stepDetectedListener;
    }

    /**
     * Interface for onStepDetected callback
     */
    public interface StepDetectedListener {
        void onStepDetected(Step step);
    }

    private double computeStepLength(double max, double min) {
        double diff = max - min;
        double result = stepLengthConstant * (float) Math.pow(diff, 1.0 / 4);
        return result;
    }
}
