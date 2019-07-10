package com.platypii.baseline.cloud.tasks;

import com.platypii.baseline.BaseService;
import com.platypii.baseline.cloud.AuthException;
import com.platypii.baseline.util.ABundle;
import com.platypii.baseline.util.Exceptions;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.firebase.analytics.FirebaseAnalytics;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import javax.net.ssl.SSLException;

/**
 * Queue for tasks such as uploading to the cloud.
 * This class will persist tasks to shared preferences so they can be resumed later.
 * This class tracks state of task execution.
 * Only one task will execute at a time.
 */
public class Tasks implements BaseService {
    private static final String TAG = "Tasks";

    private Context context;

    @NonNull
    final List<Task> pending = new ArrayList<>();
    @Nullable
    private Task running = null;

    @Override
    public void start(@NonNull Context context) {
        this.context = context;
        // Start pending work
        tendQueue();
    }

    public void add(@NonNull Task task) {
        Log.i(TAG, "Adding task " + task);
        synchronized (pending) {
            if (!pending.contains(task)) {
                pending.add(task);
            } else {
                Log.w(TAG, "Skipping duplicate task " + task);
            }
        }
        tendQueue();
    }

    public void tendQueue() {
        synchronized (pending) {
            Log.i(TAG, "Tending task queue: " + pending.size() + " tasks");
            Log.d(TAG, "Tending task queue: " + TextUtils.join(",", pending));
            if (running == null && !pending.isEmpty()) {
                // Start first pending task
                running = pending.get(0);
                runAsync(running);
            }
        }
    }

    /**
     * Run task in a new thread
     */
    private void runAsync(@NonNull Task task) {
        Log.i(TAG, "Running task: " + task);
        new Thread(() -> {
            FirebaseAnalytics.getInstance(context).logEvent("task_run", ABundle.of("task_name", task.toString()));
            // Run in background
            try {
                task.run(context);
                // Success
                runSuccess(task);
            } catch (AuthException | SocketException | SSLException | UnknownHostException e) {
                // Wait for sign in or network availability
                runFailed(task, e, true);
            } catch (Exception e) {
                runFailed(task, e, false);
                // TODO: Try again later
            }
        }).start();
    }

    /**
     * Called when a task completed successfully
     */
    private void runSuccess(@NonNull Task task) {
        Log.i(TAG, "Task success: " + task);
        FirebaseAnalytics.getInstance(context).logEvent("task_success", ABundle.of("task_name", task.toString()));
        synchronized (pending) {
            if (running != task) {
                Exceptions.report(new IllegalStateException("Invalid pop: " + running + " != " + task));
            }
            final Task removed = pending.remove(0);
            if (running != removed) {
                Exceptions.report(new IllegalStateException("Invalid pop: " + running + " != " + removed));
            }
            running = null;
        }
        // Check for next pending task
        tendQueue();
    }

    /**
     * Called when a task failed
     */
    private void runFailed(@NonNull Task task, @NonNull Exception e, boolean networkError) {
        Log.e(TAG, "Task failed: " + task, e);
        FirebaseAnalytics.getInstance(context).logEvent("task_failed", ABundle.of("task_name", task.toString()));
        if (!networkError) {
            Exceptions.report(e);
        }
        synchronized (pending) {
            running = null;
        }
    }

    /**
     * Remove all tasks of a given type
     */
    public void removeType(@NonNull TaskType taskType) {
        synchronized (pending) {
            final ListIterator<Task> it = pending.listIterator();
            while (it.hasNext()) {
                final Task task = it.next();
                if (task.taskType().name().equals(taskType.name())) {
                    it.remove();
                }
            }
        }
    }

    @Override
    public void stop() {
        synchronized (pending) {
            pending.clear();
        }
    }

}
