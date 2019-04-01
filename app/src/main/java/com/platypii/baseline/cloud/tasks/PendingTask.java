package com.platypii.baseline.cloud.tasks;

import android.support.annotation.NonNull;

public class PendingTask {
    @NonNull
    public String name;
    @NonNull
    String json;

    PendingTask(@NonNull String name, @NonNull String json) {
        this.name = name;
        this.json = json;
    }
}
