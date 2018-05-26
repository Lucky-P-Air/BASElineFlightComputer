package com.platypii.baseline.tracks;

import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Manage track files on the device.
 * Files are saved in either the external files dir, or the internal files.
 * Unsynced track files are stored in the top level directory.
 * Synced track files are moved to the "synced" directory.
 */
public class TrackFiles {
    private static final String TAG = "TrackFiles";

    @NonNull
    static List<TrackFile> getTracks(@NonNull Context context) {
        final List<TrackFile> tracks = new ArrayList<>();
        // Load jumps from disk
        final File logDir = getTrackDirectory(context);
        if (logDir != null) {
            final File[] files = logDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    final String filename = file.getName();
                    final TrackFile trackFile = new TrackFile(file);
                    // Tracks look like "track_(yyyy-MM-dd_HH-mm-ss).csv.gz"
                    final boolean matchesFilenamePattern = filename.startsWith("track_") && filename.endsWith(".csv.gz");
                    if (matchesFilenamePattern) {
                        tracks.add(trackFile);
                    }
                }
            } else {
                Log.e(TAG, "Failed to list track files: " + logDir + " " + logDir.exists());
            }
            return tracks;
        } else {
            Log.e(TAG, "Track storage directory not available");
            return tracks;
        }
    }

    public static File getTrackDirectory(@NonNull Context context) {
        final String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return context.getExternalFilesDir(null);
        } else {
            Log.w(TAG, "External storage directory not available, falling back to internal storage");
            return context.getFilesDir();
        }
    }

}
