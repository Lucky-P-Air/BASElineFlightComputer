package com.platypii.baseline.cloud;

import com.platypii.baseline.Services;
import com.platypii.baseline.events.LoggingEvent;
import com.platypii.baseline.events.SyncEvent;
import com.platypii.baseline.tracks.TrackFile;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import java.util.List;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Manages track uploads.
 * This includes queueing finished tracks, and retrying in the future.
 */
class UploadManager {
    private static final String TAG = "UploadManager";

    private Context context;

    // Used inside the uploadAll loop
    private boolean uploading = false;
    @Nullable
    private TrackFile uploadingTrack = null;

    public void start(Context context) {
        this.context = context;
        // Listen for track completion
        EventBus.getDefault().register(this);
        // Check for queued tracks to upload
        uploadAll();
    }

    private void upload(@NonNull TrackFile trackFile) {
        final boolean networkAvailable = Services.cloud.isNetworkAvailable();
        if (!networkAvailable) {
            Log.w(TAG, "Skipping upload, network unavailable");
            return;
        }
        uploading = true;
        uploadingTrack = trackFile;
        // Mark track as queued for upload
        Services.trackStore.setUploading(trackFile);
        // Start upload thread
        new Thread(new UploadTask(context, trackFile)).start();
    }

    /**
     * Upload the first track waiting to upload
     */
    private void uploadAll() {
        if (AuthState.getUser() == null) {
            // Can't upload if you're not signed in
            return;
        }
        if (!uploading) {
            final List<TrackFile> tracks = Services.trackStore.getLocalTracks();
            if (!tracks.isEmpty()) {
                // Start uploading the first track
                TrackFile firstTrackFile = tracks.get(0);
                Log.i(TAG, "Uploading track from queue " + firstTrackFile);
                upload(firstTrackFile);
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onLoggingEvent(@NonNull LoggingEvent event) {
        if (AuthState.getUser() != null && !event.started) {
            if (!uploading) {
                Log.i(TAG, "Auto syncing track " + event.trackFile);
                upload(event.trackFile);
            }
        }
    }
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onUploadSuccess(@NonNull SyncEvent.UploadSuccess event) {
        if (event.trackFile == uploadingTrack) {
            uploading = false;
            uploadingTrack = null;
            // Upload the next track file
            uploadAll();
        } else {
            Log.e(TAG, "Tracks should not be uploading except through uploadAll loop");
        }
    }
    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onUploadFailure(@NonNull SyncEvent.UploadFailure event) {
        if (event.trackFile == uploadingTrack) {
            uploading = false;
            uploadingTrack = null;
            // TODO: Wait a bit, then try uploading the next track file, even though this one failed
//            Thread.sleep(1000);
//            uploadAll();
        } else {
            Log.e(TAG, "Tracks should not be uploading except through uploadAll loop");
        }
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onSignIn(@NonNull AuthState event) {
        if (event instanceof AuthState.SignedIn) {
            Log.d(TAG, "User signed in, uploading queued tracks");
            uploadAll();
        }
    }

    public void stop() {
        EventBus.getDefault().unregister(this);
    }

}
