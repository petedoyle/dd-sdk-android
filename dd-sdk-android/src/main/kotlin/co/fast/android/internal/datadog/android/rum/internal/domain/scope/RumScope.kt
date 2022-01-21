/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.rum.internal.domain.scope

import co.fast.android.internal.datadog.android.core.internal.persistence.DataWriter
import co.fast.android.internal.datadog.android.rum.internal.domain.RumContext
import co.fast.android.internal.datadog.tools.annotation.NoOpImplementation

@NoOpImplementation
internal interface RumScope {

    /**
     * Handles an incoming event.
     * If needed, writes a RumEvent to the provided writer.
     * @return this instance if this scope is still valid, or null if it no longer can process
     * events
     */
    fun handleEvent(
        event: RumRawEvent,
        writer: DataWriter<Any>
    ): RumScope?

    fun getRumContext(): RumContext
}