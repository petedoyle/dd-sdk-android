/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */
package co.fast.android.internal.datadog.android.rum.internal.tracking

import co.fast.android.internal.datadog.android.rum.internal.instrumentation.gestures.GesturesTracker
import co.fast.android.internal.datadog.android.rum.tracking.TrackingStrategy
import co.fast.android.internal.datadog.tools.annotation.NoOpImplementation

/**
 * A TrackingStrategy dedicated to user actions tracking.
 */
@NoOpImplementation
internal interface UserActionTrackingStrategy : TrackingStrategy {
    fun getGesturesTracker(): GesturesTracker
}
