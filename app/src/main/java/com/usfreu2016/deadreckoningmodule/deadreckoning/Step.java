package com.usfreu2016.deadreckoningmodule.deadreckoning;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Calendar;

public class Step implements Parcelable{

    private double stepLength;
    private long timeStamp;
    public static final Creator<Step> CREATOR = new Creator() {
        @Override
        public Step createFromParcel(Parcel source) {
            ClassLoader classLoader = this.getClass().getClassLoader();
            double stepLength = source.readDouble();
            long timeStamp = source.readLong();
            return new Step(stepLength, timeStamp);
        }

        @Override
        public Step[] newArray(int size) {
            return new Step[size];
        }
    };

    public Step() {
        this.stepLength = 0;
        this.timeStamp = Calendar.getInstance().getTimeInMillis();
    }

    public Step(double stepLength) {
        this.stepLength = stepLength;
        this.timeStamp = Calendar.getInstance().getTimeInMillis();
    }

    public Step(double stepLength, long timeStamp) {
        this.stepLength = stepLength;
        this.timeStamp = timeStamp;
    }

    public Step(Parcel in) {
        this.stepLength = in.readDouble();
        this.timeStamp = in.readLong();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeDouble(this.stepLength);
        dest.writeLong(this.timeStamp);
    }


    public long getTimeStamp() {
        return this.timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public double getStepLength() {
        return this.stepLength;
    }

    public void setStepLength(double stepLength) {
        this.stepLength = stepLength;
    }
}
