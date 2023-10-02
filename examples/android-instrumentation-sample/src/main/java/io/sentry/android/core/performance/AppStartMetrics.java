package io.sentry.android.core.performance;

import android.app.Application;
import android.content.ContentProvider;
import android.util.Log;

import org.jetbrains.annotations.NotNull;

public class AppStartMetrics {

    private static final String TAG = "AppStartMetrics";

    public static void onApplicationCreate(final @NotNull Application application) {
        Log.d(TAG, "onApplicationCreate: " + application.getClass().getName());
    }

    public static void onApplicationPostCreate(final @NotNull Application application) {
        Log.d(TAG, "onApplicationPostCreate: " + application.getClass().getName());
    }

    public static void onContentProviderCreate(final @NotNull ContentProvider contentProvider) {
        Log.d(TAG, "onContentProviderCreate: " + contentProvider.getClass().getName());
    }

    public static void onContentProviderPostCreate(final @NotNull ContentProvider contentProvider) {
        Log.d(TAG, "onContentProviderPostCreate: "+ contentProvider.getClass().getName());
    }
}
