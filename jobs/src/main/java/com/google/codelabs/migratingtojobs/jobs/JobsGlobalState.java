package com.google.codelabs.migratingtojobs.jobs;

import android.app.Application;

import com.google.codelabs.migratingtojobs.shared.AppModule;
import com.google.codelabs.migratingtojobs.shared.EventBus;
import com.google.codelabs.migratingtojobs.shared.SharedInitializer;

import javax.inject.Inject;

public class JobsGlobalState {
    private static JobsGlobalState sInstance;

    public static JobsComponent get(Application app) {
        if (sInstance == null) {
            synchronized (JobsGlobalState.class) {
                if (sInstance == null) {
                    sInstance = new JobsGlobalState(app);

                    sInstance.init();
                }
            }
        }

        return sInstance.get();
    }

    private final JobsComponent component;

    @Inject
    SharedInitializer sharedInitializer;

    @Inject
    EventBus bus;

    @Inject
    JobSchedulingErrorListener jobSchedulingErrorListener;

    private JobsGlobalState(Application app) {
        component = DaggerJobsComponent.builder()
                .appModule(new AppModule(app))
                .build();

        component.inject(this);
    }

    private void init() {
        sharedInitializer.init();
        bus.register(jobSchedulingErrorListener);
    }

    public JobsComponent get() {
        return component;
    }

}