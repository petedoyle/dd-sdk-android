/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.rum.internal.domain.scope

import co.fast.android.internal.datadog.android.core.internal.net.FirstPartyHostDetector
import co.fast.android.internal.datadog.android.core.internal.persistence.DataWriter
import co.fast.android.internal.datadog.android.core.internal.time.TimeProvider
import co.fast.android.internal.datadog.android.rum.RumSessionListener
import co.fast.android.internal.datadog.android.rum.internal.domain.RumContext
import co.fast.android.internal.datadog.android.rum.internal.vitals.VitalMonitor

internal class RumApplicationScope(
    applicationId: String,
    internal val samplingRate: Float,
    internal val backgroundTrackingEnabled: Boolean,
    firstPartyHostDetector: FirstPartyHostDetector,
    cpuVitalMonitor: VitalMonitor,
    memoryVitalMonitor: VitalMonitor,
    frameRateVitalMonitor: VitalMonitor,
    timeProvider: TimeProvider,
    sessionListener: RumSessionListener?
) : RumScope {

    private val rumContext = RumContext(applicationId = applicationId)
    internal val childScope: RumScope = RumSessionScope(
        this,
        samplingRate,
        backgroundTrackingEnabled,
        firstPartyHostDetector,
        cpuVitalMonitor,
        memoryVitalMonitor,
        frameRateVitalMonitor,
        timeProvider,
        sessionListener
    )

    // region RumScope

    override fun handleEvent(
        event: RumRawEvent,
        writer: DataWriter<Any>
    ): RumScope {
        childScope.handleEvent(event, writer)
        return this
    }

    override fun getRumContext(): RumContext {
        return rumContext
    }

    // endregion
}