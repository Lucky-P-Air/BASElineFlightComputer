package com.platypii.baseline.location;

import com.platypii.baseline.R;
import com.platypii.baseline.Services;
import com.platypii.baseline.bluetooth.BluetoothService;
import android.util.Log;
import java.util.Locale;

/**
 * Represents the current state of GPS, including bluetooth info
 */
public class LocationStatus {
    private static final String TAG = "LocationStatus";

    public final String message;
    public final int icon;

    private LocationStatus(String message, int icon) {
        this.message = message;
        this.icon = icon;
    }

    /**
     * Get GPS status info from services
     */
    public static LocationStatus getStatus() {
        String message;
        int icon;

        // GPS signal status
        if(Services.bluetooth.preferences.preferenceEnabled && Services.bluetooth.getState() != BluetoothService.BT_CONNECTED) {
            // Bluetooth enabled, but not connected
            icon = R.drawable.warning;
            switch(Services.bluetooth.getState()) {
                case BluetoothService.BT_CONNECTING:
                    message = "BT connecting...";
                    break;
                case BluetoothService.BT_DISCONNECTED:
                    message = "BT not connected";
                    break;
                default:
                    message = "BT not connected";
                    Log.e(TAG, "Bluetooth inconsistent state: preference enabled, state = " + Services.bluetooth.getState());
            }
        } else {
            // Internal GPS, or bluetooth connected:
            if(Services.location.lastFixDuration() < 0) {
                // No fix yet
                message = "Searching...";
                icon = R.drawable.status_red;
            } else {
                final long lastFixDuration = Services.location.lastFixDuration();
                // TODO: Use better method to determine signal.
                // Take into account acc and dop
                // How many of the last X expected fixes have we missed?
                if (lastFixDuration > 10000) {
                    message = String.format(Locale.getDefault(), "Last fix %ds", lastFixDuration / 1000L);
                    icon = R.drawable.status_red;
                } else if (lastFixDuration > 2000) {
                    message = String.format(Locale.getDefault(), "Last fix %ds", lastFixDuration / 1000L);
                    icon = R.drawable.status_yellow;
                } else if (Services.bluetooth.preferences.preferenceEnabled && Services.bluetooth.getState() == BluetoothService.BT_CONNECTED) {
                    message = String.format(Locale.getDefault(), "GPS %.2fHz", Services.location.refreshRate);
                    icon = R.drawable.status_blue;
                } else {
                    message = String.format(Locale.getDefault(), "GPS %.2fHz", Services.location.refreshRate);
                    icon = R.drawable.status_green;
                }
            }
        }

        return new LocationStatus(message, icon);
    }

}
