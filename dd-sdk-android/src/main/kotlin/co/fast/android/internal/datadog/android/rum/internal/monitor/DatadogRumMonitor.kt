/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.rum.internal.monitor

import android.os.Handler
import co.fast.android.internal.datadog.android.core.internal.net.FirstPartyHostDetector
import co.fast.android.internal.datadog.android.core.internal.persistence.DataWriter
import co.fast.android.internal.datadog.android.core.internal.time.TimeProvider
import co.fast.android.internal.datadog.android.rum.RumActionType
import co.fast.android.internal.datadog.android.rum.RumAttributes
import co.fast.android.internal.datadog.android.rum.RumErrorSource
import co.fast.android.internal.datadog.android.rum.RumMonitor
import co.fast.android.internal.datadog.android.rum.RumResourceKind
import co.fast.android.internal.datadog.android.rum.RumSessionListener
import co.fast.android.internal.datadog.android.rum.internal.RumErrorSourceType
import co.fast.android.internal.datadog.android.rum.internal.domain.Time
import co.fast.android.internal.datadog.android.rum.internal.domain.asTime
import co.fast.android.internal.datadog.android.rum.internal.domain.event.ResourceTiming
import co.fast.android.internal.datadog.android.rum.internal.domain.scope.RumApplicationScope
import co.fast.android.internal.datadog.android.rum.internal.domain.scope.RumRawEvent
import co.fast.android.internal.datadog.android.rum.internal.domain.scope.RumScope
import co.fast.android.internal.datadog.android.rum.internal.vitals.VitalMonitor
import co.fast.android.internal.datadog.android.rum.model.ViewEvent
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

internal class DatadogRumMonitor(
    applicationId: String,
    internal val samplingRate: Float,
    internal val backgroundTrackingEnabled: Boolean,
    private val writer: DataWriter<Any>,
    internal val handler: Handler,
    firstPartyHostDetector: FirstPartyHostDetector,
    cpuVitalMonitor: VitalMonitor,
    memoryVitalMonitor: VitalMonitor,
    frameRateVitalMonitor: VitalMonitor,
    timeProvider: TimeProvider,
    sessionListener: RumSessionListener?,
    private val executorService: ExecutorService = Executors.newSingleThreadExecutor()
) : RumMonitor, AdvancedRumMonitor {

    internal val rootScope: RumScope = RumApplicationScope(
        applicationId,
        samplingRate,
        backgroundTrackingEnabled,
        firstPartyHostDetector,
        cpuVitalMonitor,
        memoryVitalMonitor,
        frameRateVitalMonitor,
        timeProvider,
        sessionListener
    )

    internal val keepAliveRunnable = Runnable {
        handleEvent(RumRawEvent.KeepAlive())
    }

    init {
        handler.postDelayed(keepAliveRunnable, KEEP_ALIVE_MS)
    }

    // region RumMonitor

    override fun startView(key: Any, name: String, attributes: Map<String, Any?>) {
        val eventTime = getEventTime(attributes)
        handleEvent(
            RumRawEvent.StartView(key, name, attributes, eventTime)
        )
    }

    override fun stopView(key: Any, attributes: Map<String, Any?>) {
        val eventTime = getEventTime(attributes)
        handleEvent(
            RumRawEvent.StopView(key, attributes, eventTime)
        )
    }

    override fun addUserAction(type: RumActionType, name: String, attributes: Map<String, Any?>) {
        val eventTime = getEventTime(attributes)
        handleEvent(
            RumRawEvent.StartAction(type, name, false, attributes, eventTime)
        )
    }

    override fun startUserAction(type: RumActionType, name: String, attributes: Map<String, Any?>) {
        val eventTime = getEventTime(attributes)
        handleEvent(
            RumRawEvent.StartAction(type, name, true, attributes, eventTime)
        )
    }

    override fun stopUserAction(attributes: Map<String, Any?>) {
        handleEvent(
            RumRawEvent.StopAction(null, null, attributes)
        )
    }

    override fun stopUserAction(
        type: RumActionType,
        name: String,
        attributes: Map<String, Any?>
    ) {
        val eventTime = getEventTime(attributes)
        handleEvent(
            RumRawEvent.StopAction(type, name, attributes, eventTime)
        )
    }

    override fun startResource(
        key: String,
        method: String,
        url: String,
        attributes: Map<String, Any?>
    ) {
        val eventTime = getEventTime(attributes)
        handleEvent(
            RumRawEvent.StartResource(key, url, method, attributes, eventTime)
        )
    }

    override fun stopResource(
        key: String,
        statusCode: Int?,
        size: Long?,
        kind: RumResourceKind,
        attributes: Map<String, Any?>
    ) {
        val eventTime = getEventTime(attributes)
        handleEvent(
            RumRawEvent.StopResource(key, statusCode?.toLong(), size, kind, attributes, eventTime)
        )
    }

    override fun stopResourceWithError(
        key: String,
        statusCode: Int?,
        message: String,
        source: RumErrorSource,
        throwable: Throwable,
        attributes: Map<String, Any?>
    ) {
        handleEvent(
            RumRawEvent.StopResourceWithError(
                key,
                statusCode?.toLong(),
                message,
                source,
                throwable,
                attributes
            )
        )
    }

    override fun addError(
        message: String,
        source: RumErrorSource,
        throwable: Throwable?,
        attributes: Map<String, Any?>
    ) {
        val eventTime = getEventTime(attributes)
        val errorType = getErrorType(attributes)
        handleEvent(
            RumRawEvent.AddError(
                message,
                source,
                throwable,
                null,
                false,
                attributes,
                eventTime,
                errorType
            )
        )
    }

    override fun addErrorWithStacktrace(
        message: String,
        source: RumErrorSource,
        stacktrace: String?,
        attributes: Map<String, Any?>
    ) {
        val eventTime = getEventTime(attributes)
        val errorType = getErrorType(attributes)
        val errorSourceType = getErrorSourceType(attributes) ?: RumErrorSourceType.ANDROID
        handleEvent(
            RumRawEvent.AddError(
                message,
                source,
                null,
                stacktrace,
                false,
                attributes,
                eventTime,
                errorType,
                errorSourceType
            )
        )
    }

    // endregion

    // region AdvancedRumMonitor

    override fun resetSession() {
        handleEvent(
            RumRawEvent.ResetSession()
        )
    }

    override fun viewTreeChanged(eventTime: Time) {
        handleEvent(
            RumRawEvent.ViewTreeChanged(eventTime)
        )
    }

    override fun waitForResourceTiming(key: String) {
        handleEvent(
            RumRawEvent.WaitForResourceTiming(key)
        )
    }

    override fun addResourceTiming(key: String, timing: ResourceTiming) {
        handleEvent(
            RumRawEvent.AddResourceTiming(key, timing)
        )
    }

    override fun addCrash(message: String, source: RumErrorSource, throwable: Throwable) {
        handleEvent(
            RumRawEvent.AddError(message, source, throwable, null, true, emptyMap())
        )
    }

    override fun updateViewLoadingTime(
        key: Any,
        loadingTimeInNs: Long,
        type: ViewEvent.LoadingType
    ) {
        handleEvent(
            RumRawEvent.UpdateViewLoadingTime(key, loadingTimeInNs, type)
        )
    }

    override fun addTiming(name: String) {
        handleEvent(
            RumRawEvent.AddCustomTiming(name)
        )
    }

    override fun addLongTask(durationNs: Long, target: String) {
        handleEvent(
            RumRawEvent.AddLongTask(durationNs, target)
        )
    }

    override fun eventSent(viewId: String, type: EventType) {
        when (type) {
            EventType.ACTION -> handleEvent(RumRawEvent.ActionSent(viewId))
            EventType.RESOURCE -> handleEvent(RumRawEvent.ResourceSent(viewId))
            EventType.ERROR -> handleEvent(RumRawEvent.ErrorSent(viewId))
            EventType.LONG_TASK -> handleEvent(RumRawEvent.LongTaskSent(viewId, false))
            EventType.FROZEN_FRAME -> handleEvent(RumRawEvent.LongTaskSent(viewId, true))
            EventType.VIEW -> {
                // Nothing to do
            }
        }
    }

    override fun eventDropped(viewId: String, type: EventType) {
        when (type) {
            EventType.ACTION -> handleEvent(RumRawEvent.ActionDropped(viewId))
            EventType.RESOURCE -> handleEvent(RumRawEvent.ResourceDropped(viewId))
            EventType.ERROR -> handleEvent(RumRawEvent.ErrorDropped(viewId))
            EventType.LONG_TASK -> handleEvent(RumRawEvent.LongTaskDropped(viewId, false))
            EventType.FROZEN_FRAME -> handleEvent(RumRawEvent.LongTaskDropped(viewId, true))
            EventType.VIEW -> {
                // Nothing to do
            }
        }
    }

    // endregion

    // region Internal

    internal fun drainExecutorService() {
        val tasks = arrayListOf<Runnable>()
        (executorService as? ThreadPoolExecutor)
            ?.queue
            ?.drainTo(tasks)
        executorService.shutdown()
        executorService.awaitTermination(10, TimeUnit.SECONDS)
        tasks.forEach {
            it.run()
        }
    }

    internal fun handleEvent(event: RumRawEvent) {
        if (event is RumRawEvent.AddError && event.isFatal) {
            rootScope.handleEvent(event, writer)
        } else {
            handler.removeCallbacks(keepAliveRunnable)
            // avoid trowing a RejectedExecutionException
            if (!executorService.isShutdown) {
                executorService.submit {
                    synchronized(rootScope) {
                        rootScope.handleEvent(event, writer)
                    }
                    handler.postDelayed(keepAliveRunnable, KEEP_ALIVE_MS)
                }
            }
        }
    }

    internal fun stopKeepAliveCallback() {
        handler.removeCallbacks(keepAliveRunnable)
    }

    private fun getEventTime(attributes: Map<String, Any?>): Time {
        return (attributes[RumAttributes.INTERNAL_TIMESTAMP] as? Long)?.asTime() ?: Time()
    }

    private fun getErrorType(attributes: Map<String, Any?>): String? {
        return attributes[RumAttributes.INTERNAL_ERROR_TYPE] as? String
    }

    private fun getErrorSourceType(attributes: Map<String, Any?>): RumErrorSourceType? {
        val sourceType = attributes[RumAttributes.INTERNAL_ERROR_SOURCE_TYPE] as? String

        return if (sourceType == null) {
            return null
        } else {
            when (sourceType.lowercase(Locale.US)) {
                "android" -> RumErrorSourceType.ANDROID
                "react-native" -> RumErrorSourceType.REACT_NATIVE
                "browser" -> RumErrorSourceType.BROWSER
                else -> RumErrorSourceType.ANDROID
            }
        }
    }

    // endregion

    companion object {
        internal val KEEP_ALIVE_MS = TimeUnit.MINUTES.toMillis(5)
    }
}