package com.platypii.baseline;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.platypii.baseline.altimeter.MyAltimeter;
import com.platypii.baseline.data.MySensorManager;
import com.platypii.baseline.data.measurements.MAltitude;
import com.platypii.baseline.data.measurements.MLocation;
import com.platypii.baseline.data.measurements.MSensor;
import com.platypii.baseline.location.MyLocationListener;
import com.platypii.baseline.util.Convert;
import com.platypii.baseline.util.SyncedList;
import com.platypii.baseline.util.Util;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import java.util.Locale;

@SuppressLint("SetTextI18n")
public class SensorActivity extends Activity implements MyLocationListener {

    // Barometer
    private TextView pressureLabel;
    private TextView pressureAltitudeLabel;
    private TextView pressureAltitudeFilteredLabel;
    private TextView altitudeLabel;
    private TextView groundLevelLabel;
    private TextView altitudeAglLabel;
    private TextView fallrateLabel;
    // GPS
    private TextView satelliteLabel;
    private TextView lastFixLabel;
    private TextView latitudeLabel;
    private TextView longitudeLabel;
    private TextView gpsAltitudeLabel;
    private TextView gpsFallrateLabel;
    private TextView hAccLabel;
    private TextView pdopLabel;
    private TextView hdopLabel;
    private TextView vdopLabel;
    private TextView groundSpeedLabel;
    private TextView totalSpeedLabel;
    private TextView glideRatioLabel;
    private TextView glideAngleLabel;
    private TextView bearingLabel;

    // Sensors
    private LinearLayout sensorLayout;
    // private final ArrayList<SensorPlot> plots = new ArrayList<>();

    // Periodic UI updates    
    private final Handler handler = new Handler();
    private final int updateInterval = 100; // in milliseconds
    private Runnable updateRunnable;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_sensors);

        // Find UI elements:
        // Barometer
        pressureLabel = (TextView)findViewById(R.id.pressureLabel);
        pressureAltitudeLabel = (TextView)findViewById(R.id.pressureAltitudeLabel);
        pressureAltitudeFilteredLabel = (TextView)findViewById(R.id.pressureAltitudeFilteredLabel);
        altitudeLabel = (TextView)findViewById(R.id.altitudeLabel);
        groundLevelLabel = (TextView)findViewById(R.id.groundLevelLabel);
        altitudeAglLabel = (TextView)findViewById(R.id.altitudeAglLabel);
        fallrateLabel = (TextView)findViewById(R.id.fallrateLabel);

        // GPS
        satelliteLabel = (TextView)findViewById(R.id.satelliteLabel);
        lastFixLabel = (TextView)findViewById(R.id.lastFixLabel);
        latitudeLabel = (TextView)findViewById(R.id.latitudeLabel);
        longitudeLabel = (TextView)findViewById(R.id.longitudeLabel);
        gpsAltitudeLabel = (TextView)findViewById(R.id.gpsAltitudeLabel);
        gpsFallrateLabel = (TextView)findViewById(R.id.gpsFallrateLabel);
        hAccLabel = (TextView)findViewById(R.id.hAccLabel);
        pdopLabel = (TextView)findViewById(R.id.pdopLabel);
        hdopLabel = (TextView)findViewById(R.id.hdopLabel);
        vdopLabel = (TextView)findViewById(R.id.vdopLabel);
        groundSpeedLabel = (TextView)findViewById(R.id.groundSpeedLabel);
        totalSpeedLabel = (TextView)findViewById(R.id.totalSpeedLabel);
        glideRatioLabel = (TextView)findViewById(R.id.glideRatioLabel);
        glideAngleLabel = (TextView)findViewById(R.id.glideAngleLabel);
        bearingLabel = (TextView)findViewById(R.id.bearingLabel);

        // Sensors
        sensorLayout = (LinearLayout)findViewById(R.id.sensorLayout);
        // TextView sensorsLabel = (TextView)findViewById(R.id.sensorsLabel);
        // sensorsLabel.setText("Sensors: \n" + MySensorManager.getSensorsString());
        
        if(MySensorManager.gravity != null) {
            addPlot("Gravity", MySensorManager.gravity);
        }
        if(MySensorManager.rotation != null) {
            addPlot("Rotation", MySensorManager.rotation);
        }

        // addPlot("Magnetic", MySensorManager.getHistory(Sensor.TYPE_MAGNETIC_FIELD));
        // addPlot("Accelerometer", MySensorManager.history.get(Sensor.TYPE_ACCELEROMETER));
        // addPlot("Gyro", MySensorManager.gyroHistory, 0, 10, 100);
        // addPlot("Gyro (int)", MySensorManager.gyroHistory, 1, 10, 100);
        // addPlot("Linear Acceleration", MySensorManager.linearAccelHistory, 0, 10, 100); // Linear Acceleration = Accel - Gravity
        // addPlot("Linear Velocity", MySensorManager.linearAccelHistory, 1, 20, 100);
        // addPlot("Linear Position", MySensorManager.linearAccelHistory, 2, 90, 120);

        // Set up sensor logging
        MySensorManager.accel.setMaxSize(300);
        MySensorManager.gravity.setMaxSize(300);
        MySensorManager.rotation.setMaxSize(300);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Start GPS updates
        Services.location.addListener(this);
        updateGPS(Services.location.lastLoc);

        // Start altitude updates
        EventBus.getDefault().register(this);
        updateAltimeter();

        // Periodic UI updates
        updateRunnable = new Runnable() {
            public void run() {
                update();
                handler.postDelayed(this, updateInterval);
            }
        };
        handler.post(updateRunnable);
        update();
    }

    @Override
    public void onPause() {
        super.onPause();
        handler.removeCallbacks(updateRunnable);
        updateRunnable = null;
        EventBus.getDefault().unregister(this);
        Services.location.removeListener(this);
    }

    private void addPlot(String label, SyncedList<MSensor> history) {
        if(history != null) {
            final TextView textView = new TextView(this);
            textView.setText(label);
            sensorLayout.addView(textView);

            final SensorPlot plot = new SensorPlot(this, null);
            plot.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, 180));
            plot.loadHistory(history);

            sensorLayout.addView(plot);
            // plots.add(plot);
        }
    }

    private void updateAltimeter() {
        pressureLabel.setText(String.format(Locale.getDefault(), "Pressure: %s (%.2fHz)", Convert.pressure(MyAltimeter.pressure), MyAltimeter.refreshRate));
        pressureAltitudeLabel.setText("Pressure altitude raw: " + Convert.distance(MyAltimeter.pressure_altitude_raw, 2, true));
        pressureAltitudeFilteredLabel.setText("Pressure altitude filtered: " + Convert.distance(MyAltimeter.pressure_altitude_filtered, 2, true) + " +/- " + Convert.distance(Math.sqrt(MyAltimeter.model_error.var()), 2, true));
        altitudeLabel.setText("Altitude (gps corrected): " + Convert.distance(MyAltimeter.altitude, 2, true));
        groundLevelLabel.setText("Ground level: " + Convert.distance(MyAltimeter.groundLevel(), 2, true) + " pressure alt");
        altitudeAglLabel.setText("Altitude AGL: " + Convert.distance(MyAltimeter.altitudeAGL(), 2, true) + " AGL");
        fallrateLabel.setText("Fallrate: " + Convert.speed(-MyAltimeter.climb, 2, true));
    }

    private void updateGPS(MLocation loc) {
        satelliteLabel.setText("Satellites: " + Services.location.satellitesInView + " visible, " + Services.location.satellitesUsed + " used in fix");
        if(loc != null) {
            if (Util.isReal(loc.latitude)) {
                latitudeLabel.setText(String.format(Locale.getDefault(), "Lat: %.6f", loc.latitude));
            } else {
                latitudeLabel.setText("Lat: ");
            }
            if (Util.isReal(loc.latitude)) {
                longitudeLabel.setText(String.format(Locale.getDefault(), "Long: %.6f", loc.longitude));
            } else {
                longitudeLabel.setText("Long: ");
            }
            gpsAltitudeLabel.setText("GPS altitude: " + Convert.distance(loc.altitude_gps, 2, true));
            gpsFallrateLabel.setText("GPS fallrate: " + Convert.speed(Services.location.vD, 2, true));
            hAccLabel.setText("hAcc: " + Convert.distance(loc.hAcc));
            pdopLabel.setText(String.format(Locale.getDefault(), "pdop: %.1f", loc.pdop));
            hdopLabel.setText(String.format(Locale.getDefault(), "hdop: %.1f", loc.hdop));
            vdopLabel.setText(String.format(Locale.getDefault(), "vdop: %.1f", loc.vdop));
            groundSpeedLabel.setText("Ground speed: " + Convert.speed(loc.groundSpeed(), 2, true));
            totalSpeedLabel.setText("Total speed: " + Convert.speed(loc.totalSpeed(), 2, true));
            glideRatioLabel.setText("Glide ratio: " + loc.glideRatioString());
            glideAngleLabel.setText("Glide angle: " + Convert.angle(loc.glideAngle()));
            bearingLabel.setText("Bearing: " + Convert.bearing2(loc.bearing()));
        }
    }

    /** Updates the UI that refresh continuously, such as sample rates */
    private void update() {
        // Last fix needs to be updated continuously since it shows time since last fix
        final long lastFixDuration = Services.location.lastFixDuration();
        if(lastFixDuration >= 0) {
            // Set text color
            if(lastFixDuration > 2000) {
                // Fade from white to red linearly from 2 -> 5 seconds since last fix
                float frac = (5000f - lastFixDuration) / (3000f);
                frac = Math.max(0, Math.min(frac, 1));
                final int b = (int)(0xb0 * frac); // blue
                final int gb = b + 0x100 * b; // blue + green
                lastFixLabel.setTextColor(0xffb00000 + gb);
            } else {
                lastFixLabel.setTextColor(0xffb0b0b0);
            }
            String lastFix = (lastFixDuration / 1000) + "s";
            if(Services.location.refreshRate > 0) {
                lastFix += String.format(Locale.getDefault(), " (%.2fHz)", Services.location.refreshRate);
            }
            lastFixLabel.setText("Last fix: " + lastFix);
        } else {
            lastFixLabel.setTextColor(0xffb0b0b0);
            lastFixLabel.setText("Last fix: ");
        }
        // Altitude refresh rate
        pressureLabel.setText(String.format(Locale.getDefault(), "Pressure: %s (%.2fHz)", Convert.pressure(MyAltimeter.pressure), MyAltimeter.refreshRate));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        MySensorManager.accel.setMaxSize(0);
    }

    // Listeners
    @Override
    public void onLocationChanged(MLocation loc) {}
    @Override
    public void onLocationChangedPostExecute() {
        updateGPS(Services.location.lastLoc);
    }

    /**
     * Listen for altitude updates
     */
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAltitudeEvent(MAltitude alt) {
        updateAltimeter();
    }

    @Override
    public void onStart() {
        super.onStart();
        // Start flight services
        Services.start(this);
    }
    @Override
    public void onStop() {
        super.onStop();
        // Stop flight services
        Services.stop();
    }
}
