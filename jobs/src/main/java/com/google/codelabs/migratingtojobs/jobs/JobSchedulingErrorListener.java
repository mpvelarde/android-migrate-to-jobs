package com.google.codelabs.migratingtojobs.jobs;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.codelabs.migratingtojobs.shared.BaseEventListener;
import com.google.codelabs.migratingtojobs.shared.CatalogItem;

import java.util.concurrent.ExecutorService;

import javax.inject.Inject;
import javax.inject.Named;

public class JobSchedulingErrorListener extends BaseEventListener {
    public final static int DOWNLOAD_JOB_ID = 1;

    private static final String TAG = "JS D/L Handler";

    private final ExecutorService executorService;
    /**
     * All calls to the JobScheduler go through IPC, so we do it all on a worker thread in this
     * Runnable.
     */
    private final Runnable scheduleRunnable;
    /**
     * Whether we've already scheduled a job. We should avoid making too many expensive IPC calls,
     * so check here (synchronize on `this`) first.
     */
    private boolean jobScheduled = false;

    @Inject
    public JobSchedulingErrorListener(@NonNull Context appContext,
                                      @Named("worker") ExecutorService executorService) {
        this.scheduleRunnable = new ScheduleRunnable(appContext);
        this.executorService = executorService;
    }

    @Override
    public void onItemDownloadFailed(CatalogItem item) {
        // most checks shouldn't have to wait for the synch lock
        if (!jobScheduled) {
            synchronized (this) {
                if (!jobScheduled) {
                    executorService.submit(scheduleRunnable);
                    jobScheduled = true;
                }
            }
        }
    }

    @Override
    public void handle(Message msg) {
        if (msg.what == JobEvents.DOWNLOAD_JOB_FINISHED) {
            synchronized (this) {
                jobScheduled = false;
            }
        }
    }

    private static class ScheduleRunnable implements Runnable {
        private final Context appContext;
        private final JobInfo jobToSchedule;

        public ScheduleRunnable(Context appContext) {
            this.appContext = appContext;
            this.jobToSchedule = new JobInfo.Builder(
                    DOWNLOAD_JOB_ID, new ComponentName(appContext, DownloaderJobService.class))
                    // Normally you'd want this be NETWORK_TYPE_UNMETERED, but the
                    // ConnectivityManager hack we're using in Downloader only checks for "a"
                    // connection, so let's emulate that here.
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .build();
        }

        @Override
        public void run() {
            JobScheduler js = (JobScheduler) appContext
                    .getSystemService(Context.JOB_SCHEDULER_SERVICE);

            if (js == null) {
                Log.e(TAG, "unable to retrieve JobScheduler. What's going on?");
                return;
            }

            Log.i(TAG, "scheduling new job");
            if (js.schedule(jobToSchedule) == JobScheduler.RESULT_FAILURE) {
                Log.e(TAG, "encountered unknown error scheduling job");
            }
        }
    }
}