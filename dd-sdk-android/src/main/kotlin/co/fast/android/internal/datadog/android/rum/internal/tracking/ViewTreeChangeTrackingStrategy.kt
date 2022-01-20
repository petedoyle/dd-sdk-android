/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.rum.internal.tracking

import android.app.Activity
import android.view.ViewTreeObserver
import co.fast.android.internal.datadog.android.rum.GlobalRum
import co.fast.android.internal.datadog.android.rum.internal.domain.Time
import co.fast.android.internal.datadog.android.rum.internal.monitor.AdvancedRumMonitor
import co.fast.android.internal.datadog.android.rum.tracking.ActivityLifecycleTrackingStrategy
import co.fast.android.internal.datadog.android.rum.tracking.TrackingStrategy

internal class ViewTreeChangeTrackingStrategy :
    ActivityLifecycleTrackingStrategy(),
    TrackingStrategy,
    ViewTreeObserver.OnGlobalLayoutListener {

    // region ActivityLifecycleTrackingStrategy

    override fun onActivityStarted(activity: Activity) {
        super.onActivityStarted(activity)

        val viewTreeObserver = getViewTreeObserver(activity)
        viewTreeObserver?.addOnGlobalLayoutListener(this)
    }

    override fun onActivityStopped(activity: Activity) {
        super.onActivityStopped(activity)

        val viewTreeObserver = getViewTreeObserver(activity)
        viewTreeObserver?.removeOnGlobalLayoutListener(this)
    }

    // endregion

    // region ViewTreeObserver.OnGlobalLayoutListener

    override fun onGlobalLayout() {
        val now = Time()
        (GlobalRum.get() as? AdvancedRumMonitor)?.viewTreeChanged(now)
    }

    // endregion

    // region Internal

    private fun getViewTreeObserver(activity: Activity): ViewTreeObserver? {
        val window = activity.window ?: return null
        return window.decorView.viewTreeObserver
    }

    // endregion
}
