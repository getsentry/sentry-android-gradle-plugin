package io.sentry.samples.instrumentation

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.core.os.trace
import androidx.room.Room
import io.sentry.SentryEvent
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid
import io.sentry.android.core.SentryAndroidOptions
import io.sentry.samples.instrumentation.data.TracksDatabase
import io.sentry.samples.instrumentation.util.SentryActivityFragmentLifecycleIntegration

class SampleApp : Application() {

    companion object {
        lateinit var database: TracksDatabase
            private set

        lateinit var analytics: SharedPreferences
            private set
    }

    override fun onCreate() {
        super.onCreate()
        trace("initSentry") {
            initSentry(this)
        }
        database = Room.databaseBuilder(this, TracksDatabase::class.java, "tracks.db")
            .createFromAsset("tracks.db")
            .fallbackToDestructiveMigration()
            .build()

        analytics = getSharedPreferences("analytics", Context.MODE_PRIVATE)
    }


    private fun initSentry(context: Context) {
        SentryAndroid.init(context) { options: SentryAndroidOptions ->
            options.apply {
                release = BuildConfig.VERSION_NAME
                dsn = "https://1053864c67cc410aa1ffc9701bd6f93d@o447951.ingest.sentry.io/5428559"
                isEnableNdk = false
                sampleRate = null // Send all errors. No sampling.
                tracesSampleRate = null // Disable tracing.
                tracesSampler = null
                isAnrEnabled = false
                isEnableAutoSessionTracking = true
                sessionTrackingIntervalMillis = 30000 // 30s
                isSendDefaultPii = false
                addInAppInclude("com.meesho.mesh.android")
                addInAppInclude("com.meesho.analytics")
                isEnableAppComponentBreadcrumbs = true
                isEnableSystemEventBreadcrumbs = true
                isEnableAppLifecycleBreadcrumbs = true
                isEnableActivityLifecycleBreadcrumbs = false
                addIntegration(SentryActivityFragmentLifecycleIntegration(context)) // Custom integration.
                isEnableAutoActivityLifecycleTracing = false
                isEnableActivityLifecycleTracingAutoFinish = false
                beforeSend = SentryOptions.BeforeSendCallback { event: SentryEvent, hint: Any? ->
                    return@BeforeSendCallback if (event.isCrashed) {
                        event
                    } else {
                        null // Swallow non-fatal errors.
                    }
                }
            }
        }
    }
}
