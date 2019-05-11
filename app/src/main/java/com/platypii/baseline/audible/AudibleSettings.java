package com.platypii.baseline.audible;

import com.platypii.baseline.util.Numbers;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;

/**
 * Static class to store audible settings in memory.
 * Make sure to always update this if the underlying preference changes.
 */
public class AudibleSettings {

    // Defaults
    @NonNull
    public static AudibleMode mode = AudibleModes.get("horizontal_speed");
    public static double min = mode.defaultMin;
    public static double max = mode.defaultMax;
    public static int precision = mode.defaultPrecision;
    public static float speechInterval = 2.5f;
    public static float speechRate = 1.0f;

    /**
     * Load audible settings from android preferences
     */
    static void load(@NonNull SharedPreferences prefs) {
        final String audibleMode = prefs.getString("audible_mode", "horizontal_speed");
        mode = AudibleModes.get(audibleMode);
        min = Numbers.parseDouble(prefs.getString("audible_min", Float.toString(mode.defaultMin)));
        max = Numbers.parseDouble(prefs.getString("audible_max", Float.toString(mode.defaultMax)));
        precision = Numbers.parseInt(prefs.getString("audible_precision", Integer.toString(mode.defaultPrecision)), mode.defaultPrecision);
        speechInterval = Numbers.parseFloat(prefs.getString("audible_interval", "2.5"));
        speechRate = Numbers.parseFloat(prefs.getString("audible_rate", "1.0"));
    }

    /**
     * Change audible mode and set default min,max,precision values
     */
    public static void setAudibleMode(@NonNull String audibleMode) {
        mode = AudibleModes.get(audibleMode);
        min = mode.defaultMin;
        max = mode.defaultMax;
        precision = mode.defaultPrecision;
    }
}
