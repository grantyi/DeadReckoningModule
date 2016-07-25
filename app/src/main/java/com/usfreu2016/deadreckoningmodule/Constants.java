package com.usfreu2016.deadreckoningmodule;


public class Constants {
    public static final String STEP_RESULT_KEY = "stepResult";
    public static final String VERT_THRESHOLD_KEY = "vertAccValue";
    public static final String HORI_THRESHOLD_KEY = "horiAccValue";
    public static final String STEP_TIME_THRESHOLD_KEY = "stepTimeThreshold";
    public static final String ANGLE_KEY = "angle";
    public static final int REGISTER_MESSENGER_MSG = 1;
    public static final int START_STEP_DETECTION_MSG = 2;
    public static final int START_ORIENTATION_TRACKING_MSG = 3;
    public static final int STEP_DETECTED_MSG = 4;
    public static final int ORIENTATION_CHANGED_MSG = 5;
    public static final int SET_INITIAL_ORIENTATION_MSG = 6;
    public static final int SET_VERT_ACC_THRESHOLD_MSG = 7;
    public static final int SET_HORI_ACC_THRESHOLD_MSG = 8;
    public static final int SET_STEP_TIME_THRESHOLD_MSG = 9;
}
