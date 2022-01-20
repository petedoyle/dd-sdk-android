/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.rum.tracking

import android.content.Context
import co.fast.android.internal.datadog.tools.annotation.NoOpImplementation

/**
 * The TrackingStrategy interface.
 */
@NoOpImplementation
interface TrackingStrategy {

    /**
     * This method will register the tracking strategy to the current Context.
     * @param context as [Context]
     */
    fun register(context: Context)

    /**
     * This method will unregister the tracking strategy from the current Context.
     * @param context as [Context]
     */
    fun unregister(context: Context?)
}
