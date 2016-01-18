package com.platypii.baseline.data.measurements;

/** Copies an android SensorEvent */
public class MAccel extends MSensor {

    public MAccel(long nano, float a) {
        this.nano = nano;
        // this.accuracy = event.accuracy;
        this.acc = a;
    }

    public float x() {
        return acc;
    }
    public float y() {
        return Float.NaN;
    }
    public float z() {
        return Float.NaN;
    }

    @Override
    public String toRow() {
        // millis,nano,sensor,pressure,lat,lon,hMSL,velN,velE,numSV,gX,gY,gZ,rotX,rotY,rotZ,acc
        return String.format(",%d,acc,,,,,,,,,,,,,,%f", nano, acc);
    }

}
