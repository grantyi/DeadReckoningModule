package com.usfreu2016.deadreckoningmodule;


import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.usfreu2016.deadreckoningmodule.deadreckoning.DeadReckoningManager;
import com.usfreu2016.deadreckoningmodule.deadreckoning.Step;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    final int REQUEST_CODE_ASK_PERMISSIONS = 1;

    final double VERT_ACC_THRESHOLD = 2.0;
    final double HORI_ACC_THRESHOLD = 1.0;
    final long STEP_TIME_THRESHOLD = 500;

    final long TIMER_DELAY = 500;

    final String TAG = "MainActivity";

    /** Dead reckoning manager */
    DeadReckoningManager mDeadReckoningManager;

    /** Timer and task for recording position */
    Timer timer;
    TimerTask task;

    /** Number of steps */
    int numSteps;

    /** position to be recorded */
    double xHat;
    double yHat;
    int bearing;
    double radBearing;
    long timeStamp;
    String positionData;

    /** Used to write the two files */
    BufferedWriter positionWriter;
    BufferedWriter timeWriter;

    /** View for activity */
    Button startButton;
    Button setOrientationButton;
    Button recordTimeButton;
    TextView numStepsTextView;

    /** Directory name and file name */
    String dirName = "DR Data";

    boolean recordingData;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        init();
        timer = new Timer();

        recordingData = false;
        numSteps = 0;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mDeadReckoningManager.disconnect();
    }

    private void checkPermissions() {
        int hasWriteExternalStoragePermission = checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasWriteExternalStoragePermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_CODE_ASK_PERMISSIONS);
            return;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE_ASK_PERMISSIONS:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                }
                else {
                    Toast.makeText(this, "Permissions Denied", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void recordPosition() {
        if (recordingData) {
            long time = Calendar.getInstance().getTimeInMillis();
            String data = String.format("%.2f,%.2f,%d,%d", xHat, yHat, bearing, time);
            try {
                positionWriter.append(data);
                positionWriter.append("\n");
            } catch (FileNotFoundException e) {
                Log.e("Error", "File not found exception");
            } catch (IOException e) {
                Log.e("Error", "IOException");
            }
        }
    }

    private void recordTime() {
        if (recordingData) {
            try {
                timeWriter.append(String.valueOf(Calendar.getInstance().getTimeInMillis()));
                timeWriter.append("\n");
            } catch (FileNotFoundException e) {
                Log.e("Error", "File not found exception");
            } catch (IOException e) {
                Log.e("Error", "IOException");
            }
        }
    }

    private void init() {

        startButton = (Button) findViewById(R.id.startButton);
        setOrientationButton = (Button) findViewById(R.id.setOrientationButton);
        recordTimeButton = (Button) findViewById(R.id.recordTimeButton);
        numStepsTextView = (TextView) findViewById(R.id.numStepsTextView);
        numStepsTextView.setText(String.valueOf(numSteps));

        /** Set button listeners */
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (startButton.getText().toString().equals("Start")) {
                    /** Initial Position */
                    xHat = 0;
                    yHat = 0;

                    openFileWriters();
                    setUpDRManager();
                    task = new TimerTask() {
                        @Override
                        public void run() {
                            if (recordingData) {
                                recordPosition();
                            }
                        }
                    };
                    timer.schedule(task, 0, TIMER_DELAY);
                    startButton.setText("Stop");
                } else {
                    closeFileWriters();
                    task.cancel();
                    startButton.setText("Start");
                    recordingData = false;
                }
            }
        });
        setOrientationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDeadReckoningManager.setInitialOrientation();
                recordingData = true;
            }
        });
        recordTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                recordTime();
            }
        });

        /** Set up dead reckoning manager */
        mDeadReckoningManager = new DeadReckoningManager(getApplicationContext());
        mDeadReckoningManager.setStepListener(new DeadReckoningManager.StepListener() {
            @Override
            public void onStep(Step step) {
                numSteps++;
                numStepsTextView.setText(String.valueOf(numSteps));
                if (recordingData) {
                    Log.d(TAG, "onstep");
                    xHat = xHat + step.getStepLength() * ((int) Math.sin(radBearing));
                    yHat = yHat + step.getStepLength() * ((int) Math.cos(radBearing));
                }
            }
        });
        mDeadReckoningManager.setOrientationListener(new DeadReckoningManager.OrientationListener() {
            @Override
            public void onChanged(int angle) {
                if (recordingData) {
                    bearing = angle;
                    radBearing = (Math.PI * bearing) / 180;
                }
            }
        });

        mDeadReckoningManager.connect(new DeadReckoningManager.ServiceReadyCallback() {
            @Override
            public void onServiceReady() {

            }
        });

    } // end init

    private void setUpDRManager() {
        mDeadReckoningManager.startStepDetection();
        mDeadReckoningManager.setVertAccThreshold(VERT_ACC_THRESHOLD);
        mDeadReckoningManager.setHoriAccThreshold(HORI_ACC_THRESHOLD);
        mDeadReckoningManager.setStepTimeThreshold(STEP_TIME_THRESHOLD);
        mDeadReckoningManager.startOrientationTracking();
    }

    private void openFileWriters() {
        /** Check for write permissions */
        checkPermissions();

        /** Create the files */
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), dirName);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                Log.e("Error", "Directory not created.");
            }
        }
        File positionFile = new File(dir, "position.csv");
        File timeFile = new File(dir, "time.csv");

        /** Set up the writers */
        try {
            FileWriter writer1 = new FileWriter(positionFile, true);
            FileWriter writer2 = new FileWriter(timeFile, true);
            positionWriter = new BufferedWriter(writer1);
            timeWriter = new BufferedWriter(writer2);
        } catch (FileNotFoundException e) {
            Log.e("Error", "File not found exception");
        } catch (IOException e) {
            Log.e("Error", "IOException");
        }
    }

    private void closeFileWriters() {
        /** close the writers */
        try {
            positionWriter.close();
            timeWriter.close();
        } catch (FileNotFoundException e) {
            Log.e("Error", "File not found exception");
        } catch (IOException e) {
            Log.e("Error", "IOException");
        }
    }
} // end MainActivity
