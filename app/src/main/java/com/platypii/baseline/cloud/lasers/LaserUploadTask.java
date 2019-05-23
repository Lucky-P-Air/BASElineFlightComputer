package com.platypii.baseline.cloud.lasers;

import com.platypii.baseline.Services;
import com.platypii.baseline.cloud.AuthState;
import com.platypii.baseline.cloud.RetrofitClient;
import com.platypii.baseline.cloud.tasks.AuthRequiredException;
import com.platypii.baseline.cloud.tasks.Task;
import com.platypii.baseline.cloud.tasks.TaskType;
import com.platypii.baseline.events.LaserSyncEvent;
import com.platypii.baseline.laser.LaserProfile;
import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import java.io.IOException;
import org.greenrobot.eventbus.EventBus;
import retrofit2.Response;

public class LaserUploadTask implements Task {
    private static final String TAG = "LaserUpload";

    @NonNull
    private final LaserProfile laserProfile;

    LaserUploadTask(@NonNull LaserProfile laserProfile) {
        this.laserProfile = laserProfile;
    }

    @Override
    public TaskType taskType() {
        return TaskType.laserUpload;
    }

    @Override
    public void run(@NonNull Context context) throws AuthRequiredException, IOException {
        if (AuthState.getUser() == null) {
            throw new AuthRequiredException();
        }
        Log.i(TAG, "Uploading laser profile " + laserProfile);
        // Make HTTP request
        final LaserApi laserApi = RetrofitClient.getRetrofit(context).create(LaserApi.class);
        final Response<LaserProfile> response = laserApi.post(laserProfile).execute();
        if (response.isSuccessful()) {
            final LaserProfile result = response.body();
            Log.i(TAG, "Laser POST successful, laser profile " + result);
            // Add laser to cache, remove from unsynced, and update list
            Services.cloud.lasers.cache.add(result);
            Services.cloud.lasers.unsynced.remove(laserProfile);
            Services.cloud.lasers.listAsync(context, true);
            // Sneakily replace laser_id
            laserProfile.laser_id = result.laser_id;
            EventBus.getDefault().post(new LaserSyncEvent.UploadSuccess(result));
        } else {
            final String error = response.errorBody().string();
            EventBus.getDefault().post(new LaserSyncEvent.UploadFailure(laserProfile, error));
            throw new IOException("Laser upload failed: " + error);
        }
    }

    @NonNull
    @Override
    public String toString() {
        return "LaserUpload(" + laserProfile.laser_id + ", " + laserProfile.name + ")";
    }

}
