package io.sentry.samples.instrumentation.util;

import android.annotation.SuppressLint
import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import io.sentry.Breadcrumb
import io.sentry.IHub
import io.sentry.Integration
import io.sentry.SentryLevel
import io.sentry.SentryOptions

class SentryActivityFragmentLifecycleIntegration(
    private val context: Context
) : Application.ActivityLifecycleCallbacks,
    FragmentManager.FragmentLifecycleCallbacks(),
    Integration {
    @SuppressLint("StrictLateinit") // Guaranteed to be initialised in #register(IHub, SentryOptions).
    private lateinit var hub: IHub

    override fun register(hub: IHub, options: SentryOptions) {
        this.hub = hub
        (context as Application).registerActivityLifecycleCallbacks(this)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        addActivityBreadcrumb(activity, "created")
        (activity as? FragmentActivity)
            ?.supportFragmentManager
            ?.registerFragmentLifecycleCallbacks(this, true)
    }

    override fun onActivityStarted(activity: Activity) {
        addActivityBreadcrumb(activity, "started")
    }

    override fun onActivityResumed(activity: Activity) {
        addActivityBreadcrumb(activity, "resumed")
    }

    override fun onActivityPaused(activity: Activity) {
        addActivityBreadcrumb(activity, "paused")
    }

    override fun onActivityStopped(activity: Activity) {
        addActivityBreadcrumb(activity, "stopped")
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        addActivityBreadcrumb(activity, "save instance state")
    }

    override fun onActivityDestroyed(activity: Activity) {
        addActivityBreadcrumb(activity, "destroyed")
    }

    override fun onFragmentAttached(fm: FragmentManager, fragment: Fragment, context: Context) {
        addFragmentBreadcrumb(fragment, "attached")
    }

    override fun onFragmentCreated(fm: FragmentManager, fragment: Fragment, savedInstanceState: Bundle?) {
        addFragmentBreadcrumb(fragment, "created")
    }

    override fun onFragmentViewCreated(
        fm: FragmentManager,
        fragment: Fragment,
        view: View,
        savedInstanceState: Bundle?
    ) {
        addFragmentBreadcrumb(fragment, "view created")
    }

    override fun onFragmentStarted(fm: FragmentManager, fragment: Fragment) {
        addFragmentBreadcrumb(fragment, "started")
    }

    override fun onFragmentResumed(fm: FragmentManager, fragment: Fragment) {
        addFragmentBreadcrumb(fragment, "resumed")
    }

    override fun onFragmentPaused(fm: FragmentManager, fragment: Fragment) {
        addFragmentBreadcrumb(fragment, "paused")
    }

    override fun onFragmentStopped(fm: FragmentManager, fragment: Fragment) {
        addFragmentBreadcrumb(fragment, "stopped")
    }

    override fun onFragmentSaveInstanceState(fm: FragmentManager, fragment: Fragment, outState: Bundle) {
        addFragmentBreadcrumb(fragment, "save instance state")
    }

    override fun onFragmentViewDestroyed(fm: FragmentManager, fragment: Fragment) {
        addFragmentBreadcrumb(fragment, "view destroyed")
    }

    override fun onFragmentDestroyed(fm: FragmentManager, fragment: Fragment) {
        addFragmentBreadcrumb(fragment, "destroyed")
    }

    override fun onFragmentDetached(fm: FragmentManager, fragment: Fragment) {
        addFragmentBreadcrumb(fragment, "detached")
    }

    private fun addFragmentBreadcrumb(fragment: Fragment, state: String) {
        val breadcrumb = Breadcrumb().apply {
            type = "navigation"
            setData("state", state)
            setData("screen", fragment.javaClass.name) // Use fragment's full class name.
            category = "ui.fragment.lifecycle"
            level = SentryLevel.INFO
        }
        hub.addBreadcrumb(breadcrumb)
    }

    private fun addActivityBreadcrumb(activity: Activity, state: String) {
        val breadcrumb = Breadcrumb().apply {
            type = "navigation"
            setData("state", state)
            setData("screen", activity.javaClass.simpleName)
            category = "ui.lifecycle"
            level = SentryLevel.INFO
        }
        hub.addBreadcrumb(breadcrumb)
    }
}
