package com.platypii.baseline.location;

import com.platypii.baseline.R;
import com.platypii.baseline.Services;
import com.platypii.baseline.bluetooth.BluetoothService;
import com.platypii.baseline.util.StringBuilderUtil;
import android.util.Log;

/**
 * Represents the current state of GPS, including bluetooth info
 */
public class LocationStatus {
    private static final String TAG = "LocationStatus";

    public static CharSequence message;
    public static int icon;

    // A buffer to be used for formatted strings, to avoid allocation
    private static final StringBuilder sb = new StringBuilder();

    /**
     * Get GPS status info from services
     */
    public static void updateStatus() {
        // GPS signal status
        if (Services.bluetooth.preferences.preferenceEnabled && Services.bluetooth.getState() != BluetoothService.BT_CONNECTED) {
            // Bluetooth enabled, but not connected
            icon = R.drawable.warning;
            switch (Services.bluetooth.getState()) {
                case BluetoothService.BT_CONNECTING:
                    message = "GPS bluetooth connecting...";
                    break;
                case BluetoothService.BT_DISCONNECTED:
                    message = "GPS bluetooth not connected";
                    break;
                default:
                    message = "GPS bluetooth not connected";
                    Log.e(TAG, "Bluetooth inconsistent state: preference enabled, state = " + Services.bluetooth.getState());
            }
        } else {
            // Internal GPS, or bluetooth connected:
            if (Services.location.lastFixDuration() < 0) {
                // No fix yet
                message = "GPS searching...";
                icon = R.drawable.status_red;
            } else {
                final long lastFixDuration = Services.location.lastFixDuration();
                // TODO: Use better method to determine signal.
                // Take into account acc and dop
                // How many of the last X expected fixes have we missed?
                if (lastFixDuration > 10000) {
                    message = "GPS last fix " + lastFixDuration / 1000L + "s";
                    icon = R.drawable.status_red;
                } else if (lastFixDuration > 2000) {
                    message = "GPS last fix " + lastFixDuration / 1000L + "s";
                    icon = R.drawable.status_yellow;
                } else {
                    sb.setLength(0);
                    if (Services.bluetooth.preferences.preferenceEnabled && Services.bluetooth.getState() == BluetoothService.BT_CONNECTED) {
                        sb.append("GPS bluetooth ");
                        icon = R.drawable.status_blue;
                    } else {
                        sb.append("GPS ");
                        icon = R.drawable.status_green;
                    }
                    StringBuilderUtil.format2f(sb, Services.location.refreshRate);
                    sb.append("Hz");
                    message = sb;
                }
            }
        }

        // Barometer status
        if (!Services.bluetooth.preferences.preferenceEnabled && Services.alti.barometerEnabled && Services.alti.baro_sample_count == 0) {
            message = message + " (no baro)";
        }
    }

}
