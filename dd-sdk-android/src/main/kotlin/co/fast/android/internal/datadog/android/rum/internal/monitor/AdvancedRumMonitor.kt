/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.rum.internal.monitor

import co.fast.android.internal.datadog.android.rum.RumErrorSource
import co.fast.android.internal.datadog.android.rum.RumMonitor
import co.fast.android.internal.datadog.android.rum.internal.domain.Time
import co.fast.android.internal.datadog.android.rum.internal.domain.event.ResourceTiming
import co.fast.android.internal.datadog.android.rum.model.ViewEvent
import co.fast.android.internal.datadog.tools.annotation.NoOpImplementation

@NoOpImplementation
internal interface AdvancedRumMonitor : RumMonitor {

    fun resetSession()

    fun viewTreeChanged(eventTime: Time)

    fun waitForResourceTiming(key: String)

    fun updateViewLoadingTime(key: Any, loadingTimeInNs: Long, type: ViewEvent.LoadingType)

    fun addResourceTiming(key: String, timing: ResourceTiming)

    fun addLongTask(durationNs: Long, target: String)

    fun addCrash(
        message: String,
        source: RumErrorSource,
        throwable: Throwable
    )

    fun eventSent(viewId: String, type: EventType)

    fun eventDropped(viewId: String, type: EventType)
}
