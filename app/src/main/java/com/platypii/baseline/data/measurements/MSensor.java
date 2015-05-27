package com.platypii.baseline.data.measurements;

/** Copies an android SensorEvent */
public abstract class MSensor extends Measurement {

    // Sensors
    public float gX = Float.NaN;
    public float gY = Float.NaN;
    public float gZ = Float.NaN;
    public float rotX = Float.NaN;
    public float rotY = Float.NaN;
    public float rotZ = Float.NaN;
    public float acc = Float.NaN;

//    public SensorMeasurement(MySensorEvent event) {
//        // Store sensor data
//        this.timeMillis = event.timeMillis;
//        // this.accuracy = event.accuracy;
//        if(event.sensor == Sensor.TYPE_ACCELEROMETER) {
//            this.sensor = "Acc";
//            this.acc = event.x * event.x + event.y * event.y + event.z + event.z;
//        } else if(event.sensor == Sensor.TYPE_GRAVITY) {
//            this.sensor = "Grv";
//            this.gX = event.x;
//            this.gY = event.y;
//            this.gZ = event.z;
//        } else if(event.sensor == Sensor.TYPE_ROTATION_VECTOR) {
//            this.sensor = "Rot";
//            this.rotX = event.x;
//            this.rotY = event.y;
//            this.rotZ = event.z;
//        } else {
//            Log.e("SensorMeasurement", "Unknown sensor type");
//        }
//    }

    public abstract float x();
    public abstract float y();
    public abstract float z();

    @Override
    public String toRow() {
        // timeMillis, sensor, altitude, climb, pressure, latitude, longitude, altitude_gps, vN, vE, gX, gY, gZ, rotX, rotY, rotZ, acc
        return String.format("%d,acc,,,,,,,,,%f,%f,%f,%f,%f,%f,%f", timeMillis, gX, gY, gZ, rotX, rotY, rotZ, acc);
    }

}