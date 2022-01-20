/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.rum.internal.domain

import co.fast.android.internal.datadog.android.core.internal.persistence.PayloadDecoration
import co.fast.android.internal.datadog.android.core.internal.persistence.Serializer
import co.fast.android.internal.datadog.android.core.internal.persistence.file.FileHandler
import co.fast.android.internal.datadog.android.core.internal.persistence.file.FileOrchestrator
import co.fast.android.internal.datadog.android.core.internal.persistence.file.batch.BatchFileDataWriter
import co.fast.android.internal.datadog.android.log.Logger
import co.fast.android.internal.datadog.android.rum.GlobalRum
import co.fast.android.internal.datadog.android.rum.internal.monitor.AdvancedRumMonitor
import co.fast.android.internal.datadog.android.rum.internal.monitor.EventType
import co.fast.android.internal.datadog.android.rum.model.ActionEvent
import co.fast.android.internal.datadog.android.rum.model.ErrorEvent
import co.fast.android.internal.datadog.android.rum.model.LongTaskEvent
import co.fast.android.internal.datadog.android.rum.model.ResourceEvent
import co.fast.android.internal.datadog.android.rum.model.ViewEvent
import java.io.File

internal class RumDataWriter(
    fileOrchestrator: FileOrchestrator,
    serializer: Serializer<Any>,
    decoration: PayloadDecoration,
    handler: FileHandler,
    internalLogger: Logger,
    private val lastViewEventFile: File
) : BatchFileDataWriter<Any>(
    fileOrchestrator,
    serializer,
    decoration,
    handler,
    internalLogger
) {

    override fun onDataWritten(data: Any, rawData: ByteArray) {
        when (data) {
            is ViewEvent -> persistViewEvent(rawData)
            is ActionEvent -> notifyEventSent(data.view.id, EventType.ACTION)
            is ResourceEvent -> notifyEventSent(data.view.id, EventType.RESOURCE)
            is ErrorEvent -> {
                if (data.error.isCrash != true) {
                    notifyEventSent(data.view.id, EventType.ERROR)
                }
            }
            is LongTaskEvent -> {
                if (data.longTask.isFrozenFrame == true) {
                    notifyEventSent(data.view.id, EventType.FROZEN_FRAME)
                } else {
                    notifyEventSent(data.view.id, EventType.LONG_TASK)
                }
            }
        }
    }

    // endregion

    // region Internal

    private fun persistViewEvent(data: ByteArray) {
        handler.writeData(lastViewEventFile, data)
    }

    private fun notifyEventSent(viewId: String, eventType: EventType) {
        val rumMonitor = GlobalRum.get()
        if (rumMonitor is AdvancedRumMonitor) {
            rumMonitor.eventSent(viewId, eventType)
        }
    }

    // endregion
}
