/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.rum.internal.domain.event

import co.fast.android.internal.datadog.android.core.internal.event.NoOpEventMapper
import co.fast.android.internal.datadog.android.core.internal.utils.devLogger
import co.fast.android.internal.datadog.android.core.internal.utils.sdkLogger
import co.fast.android.internal.datadog.android.event.EventMapper
import co.fast.android.internal.datadog.android.rum.GlobalRum
import co.fast.android.internal.datadog.android.rum.internal.monitor.AdvancedRumMonitor
import co.fast.android.internal.datadog.android.rum.internal.monitor.EventType
import co.fast.android.internal.datadog.android.rum.model.ActionEvent
import co.fast.android.internal.datadog.android.rum.model.ErrorEvent
import co.fast.android.internal.datadog.android.rum.model.LongTaskEvent
import co.fast.android.internal.datadog.android.rum.model.ResourceEvent
import co.fast.android.internal.datadog.android.rum.model.ViewEvent
import java.util.Locale

internal data class RumEventMapper(
    val viewEventMapper: EventMapper<ViewEvent> = NoOpEventMapper(),
    val errorEventMapper: EventMapper<ErrorEvent> = NoOpEventMapper(),
    val resourceEventMapper: EventMapper<ResourceEvent> = NoOpEventMapper(),
    val actionEventMapper: EventMapper<ActionEvent> = NoOpEventMapper(),
    val longTaskEventMapper: EventMapper<LongTaskEvent> = NoOpEventMapper()
) : EventMapper<Any> {

    override fun map(event: Any): Any? {
        val mappedEvent = resolveEvent(event)
        if (mappedEvent == null) {
            notifyEventDropped(event)
        }
        return mappedEvent
    }

    // region Internal

    private fun mapRumEvent(event: Any): Any? {
        return when (event) {
            is ViewEvent -> viewEventMapper.map(event)
            is ActionEvent -> actionEventMapper.map(event)
            is ErrorEvent -> {
                if (event.error.isCrash != true) {
                    errorEventMapper.map(event)
                } else {
                    event
                }
            }
            is ResourceEvent -> resourceEventMapper.map(event)
            is LongTaskEvent -> longTaskEventMapper.map(event)
            else -> {
                sdkLogger.w(
                    NO_EVENT_MAPPER_ASSIGNED_WARNING_MESSAGE
                        .format(Locale.US, event.javaClass.simpleName)
                )
                event
            }
        }
    }

    private fun resolveEvent(
        event: Any
    ): Any? {
        val mappedEvent = mapRumEvent(event)

        // we need to check if the returned bundled mapped object is not null and same instance
        // as the original one. Otherwise we will drop the event.
        // In case the event is of type ViewEvent this cannot be null according with the interface
        // but it can happen that if used from Java code to have null values. In this case we will
        // log a warning and we will use the original event.
        return if (event is ViewEvent && (mappedEvent == null || mappedEvent != event)) {
            devLogger.w(
                VIEW_EVENT_NULL_WARNING_MESSAGE.format(Locale.US, event)
            )
            event
        } else if (mappedEvent == null) {
            devLogger.w(
                EVENT_NULL_WARNING_MESSAGE.format(Locale.US, event)
            )
            null
        } else if (mappedEvent !== event) {
            devLogger.w(
                NOT_SAME_EVENT_INSTANCE_WARNING_MESSAGE.format(Locale.US, event)
            )
            null
        } else {
            event
        }
    }

    private fun notifyEventDropped(event: Any) {
        val monitor = (GlobalRum.get() as? AdvancedRumMonitor) ?: return
        when (event) {
            is ActionEvent -> monitor.eventDropped(event.view.id, EventType.ACTION)
            is ResourceEvent -> monitor.eventDropped(event.view.id, EventType.RESOURCE)
            is ErrorEvent -> monitor.eventDropped(event.view.id, EventType.ERROR)
            is LongTaskEvent -> {
                if (event.longTask.isFrozenFrame == true) {
                    monitor.eventDropped(event.view.id, EventType.FROZEN_FRAME)
                } else {
                    monitor.eventDropped(event.view.id, EventType.LONG_TASK)
                }
            }
            else -> {
                // Nothing to do
            }
        }
    }

    // endregion

    companion object {
        internal const val VIEW_EVENT_NULL_WARNING_MESSAGE =
            "RumEventMapper: the returned mapped ViewEvent was null. " +
                "The original event object will be used instead: %s"
        internal const val EVENT_NULL_WARNING_MESSAGE =
            "RumEventMapper: the returned mapped object was null. " +
                "This event will be dropped: %s"
        internal const val NOT_SAME_EVENT_INSTANCE_WARNING_MESSAGE =
            "RumEventMapper: the returned mapped object was not the " +
                "same instance as the original object. This event will be dropped: %s"
        internal const val NO_EVENT_MAPPER_ASSIGNED_WARNING_MESSAGE =
            "RumEventMapper: there was no EventMapper assigned for" +
                " RUM event type: %s"
    }
}
