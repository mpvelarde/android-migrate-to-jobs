package com.google.codelabs.migratingtojobs.jobs;

import com.google.codelabs.migratingtojobs.shared.AppModule;
import com.google.codelabs.migratingtojobs.shared.RootComponent;

import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component(modules = {AppModule.class, JobsModule.class})
public interface JobsComponent extends RootComponent {
    public void inject(JobsGlobalState globalState);
    public void inject(JobsCatalogListActivity activity);
    public void inject(DownloaderJobService jobService);
}