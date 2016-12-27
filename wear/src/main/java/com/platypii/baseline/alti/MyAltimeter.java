package com.platypii.baseline.alti;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;
import org.greenrobot.eventbus.EventBus;

/**
 * The main Altimeter class.
 * This class integrates sensor readings from barometer and GPS to model the altitude of the phone.
 * Altitude is measured both AGL and AMSL. Ground level is set to zero on initialization.
 * Kalman filter is used to smooth barometer data.
 *
 * TODO: Correct barometer drift with GPS
 */
class MyAltimeter {
    private static final String TAG = "MyAltimeter";

    private SensorManager sensorManager;
    private SharedPreferences prefs;

    // Pressure data
    private float pressure = Float.NaN; // hPa (millibars)
    private double pressure_altitude_raw = Double.NaN; // pressure converted to altitude under standard conditions (unfiltered)
    double pressure_altitude_filtered = Double.NaN; // kalman filtered pressure altitude

    // Pressure altitude kalman filter
    private final FilterKalman filter = new FilterKalman(); // Unfiltered(), AlphaBeta(), MovingAverage(), etc

    // Official altitude data
    private double altitude = Double.NaN; // Meters AMSL
    private double climb = Double.NaN; // Rate of climb m/s
    // public double verticalAcceleration = Double.NaN;

    // Ground level
    // Save ground level for 12 hours (in milliseconds)
    private static final double GROUND_LEVEL_TTL = 12 * 60 * 60 * 1000;
    private boolean ground_level_initialized = false;
    private double ground_level = Double.NaN;

    private long lastFixNano; // nanoseconds
    private long lastFixMillis; // milliseconds

    // Stats
    // Model error is the difference between our filtered output and the raw pressure altitude
    // Model error should approximate the sensor variance, even when in motion
    private final Stat model_error = new Stat();
    private float refreshRate = 0; // Moving average of refresh rate in Hz
    private long n = 0; // number of samples

    /**
     * Initializes altimeter services, if not already running.
     * Starts async in a background thread
     * @param context The Application context
     */
    synchronized void start(@NonNull final Context context) {
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                // Get a new preference manager
                prefs = PreferenceManager.getDefaultSharedPreferences(context);
                if(sensorManager == null) {
                    // Load ground level from preferences
                    loadGroundLevel();

                    // Add sensor listener
                    sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
                    final Sensor sensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
                    if (sensor != null) {
                        // Start sensor updates
                        sensorManager.registerListener(sensorEventListener, sensor, SensorManager.SENSOR_DELAY_FASTEST);
                    } else {
                        Log.e(TAG, "No pressure sensor found");
                    }
                } else {
                    Log.e(TAG, "MyAltimeter already started");
                }
            }
        });
    }

    double altitudeAGL() {
        return pressure_altitude_filtered - ground_level;
    }

    /**
     * Load ground level from preferences
     */
    private void loadGroundLevel() {
        final long groundLevelTime = prefs.getLong("altimeter_ground_level_time", -1L);
        if(groundLevelTime != -1 && System.currentTimeMillis() - groundLevelTime < GROUND_LEVEL_TTL) {
            ground_level = prefs.getFloat("altimeter_ground_level", 0);
            ground_level_initialized = true;
            Log.i(TAG, "Restoring ground level from preferences: " + Convert.distance(ground_level, 2, true));
        }
    }

    /**
     * Set ground level, based on pressure altitude.
     * Should not be called until altimeter is initialized.
     * @param groundLevel the pressure altitude at ground level (0m AGL)
     */
    void setGroundLevel(double groundLevel) {
        ground_level = groundLevel;
        ground_level_initialized = true;
        // Save to preferences
        final SharedPreferences.Editor edit = prefs.edit();
        edit.putFloat("altimeter_ground_level", (float) ground_level);
        edit.putLong("altimeter_ground_level_time", System.currentTimeMillis());
        edit.apply();
    }

    // Sensor Event Listener
    private final SensorEventListener sensorEventListener = new AltimeterSensorEventListener();
    private class AltimeterSensorEventListener implements SensorEventListener {
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        public void onSensorChanged(@NonNull SensorEvent event) {
            long millis = System.currentTimeMillis(); // Record time as soon as possible
            // assert event.sensor.getType() == Sensor.TYPE_PRESSURE;
            // Log.w(TAG, "values[] = " + event.values[0] + ", " + event.values[1] + ", " + event.values[2]);
            updateBarometer(millis, event);
        }
    }

    /**
     * Process new barometer reading
     */
    private void updateBarometer(long millis, SensorEvent event) {
        if(event == null || event.values.length == 0 || Double.isNaN(event.values[0]))
            return;

        if(lastFixNano == event.timestamp) {
            Log.e(TAG, "Double update: " + lastFixNano);
        }

        final double prevAltitude = altitude;
        // final double prevClimb = climb;
        final long prevLastFixNano = lastFixNano;

        pressure = event.values[0];
        lastFixNano = event.timestamp;
        lastFixMillis = millis;

        // Barometer refresh rate
        final long deltaTime = lastFixNano - prevLastFixNano; // time since last refresh
        if(deltaTime > 0 && prevLastFixNano > 0) {
            final float newRefreshRate = 1E9f / (float) (deltaTime); // Refresh rate based on last 2 samples
            if(refreshRate == 0) {
                refreshRate = newRefreshRate;
            } else {
                refreshRate += (newRefreshRate - refreshRate) * 0.5f; // Moving average
            }
            if (Double.isNaN(refreshRate)) {
                Log.e(TAG, "Refresh rate is NaN, deltaTime = " + deltaTime + " refreshTime = " + newRefreshRate);
                refreshRate = 0;
            }
        }

        // Convert pressure to altitude
        pressure_altitude_raw = pressureToAltitude(pressure);

        // Apply kalman filter to pressure altitude, to produce smooth barometric pressure altitude.
        final double dt = Double.isNaN(prevAltitude)? 0 : (lastFixNano - prevLastFixNano) * 1E-9;
        filter.update(pressure_altitude_raw, dt);
        pressure_altitude_filtered = filter.x;
        climb = filter.v;

        // Compute model error
        model_error.addSample(pressure_altitude_filtered - pressure_altitude_raw);

        // Compute GPS corrected altitude AMSL
        altitude = pressure_altitude_filtered;
        if(Double.isNaN(altitude)) {
            Log.w(TAG, "Altitude should not be NaN: altitude = " + altitude);
        }

        // Adjust for ground level
        if(!ground_level_initialized) {
            if (n == 0) {
                // First pressure reading. Calibrate ground level.
                ground_level = pressure_altitude_raw;
            } else if (n < 30) {
                // Average the first N raw samples
                ground_level += (pressure_altitude_raw - ground_level) / (n + 1);
            } else {
                setGroundLevel(ground_level);
            }
        }

        n++;
        updateAltitude();
    }

    /**
     * Saves an official altitude measurement
     */
    private void updateAltitude() {
        // Log.d(TAG, "Altimeter Update Time: " + System.currentTimeMillis() + " " + System.nanoTime() + " " + lastFixMillis + " " + lastFixNano);
        // Create the measurement
        final MAltitude myAltitude = new MAltitude(lastFixMillis, lastFixNano, altitude, climb, pressure);
        // Notify listeners
        EventBus.getDefault().post(myAltitude);
    }

    // ISA pressure and temperature
    private static final double altitude0 = 0; // ISA height 0 meters
    private static final double pressure0 = SensorManager.PRESSURE_STANDARD_ATMOSPHERE; // ISA pressure 1013.25 hPa
    private static final double temp0 = 288.15; // ISA temperature 15 degrees celcius

    // Physical constants
//    private static final double G = 9.80665; // Gravity (m/s^2)
//    private static final double R = 8.31432; // Universal Gas Constant ((N m)/(mol K))
//    private static final double M = 0.0289644; // Molar Mass of air (kg/mol)
    private static final double L = -0.0065; // Temperature Lapse Rate (K/m)
    private static final double EXP = 0.190263237; // -L * R / (G * M);

    /**
     * Convert air pressure to altitude
     * @param pressure Pressure in hPa
     * @return The pressure altitude in meters
     */
    private static double pressureToAltitude(double pressure) {
        // Barometric formula
        return altitude0 - temp0 * (1 - Math.pow(pressure / pressure0, EXP)) / L;
    }

    void stop() {
        if(sensorManager != null) {
            sensorManager.unregisterListener(sensorEventListener);
            sensorManager = null;
        } else {
            Log.e(TAG, "MyAltimeter.stop() called, but service is already stopped");
        }
        prefs = null;
    }

}