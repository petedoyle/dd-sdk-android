/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.log.internal.logger

import android.util.Log as AndroidLog
import co.fast.android.internal.datadog.android.core.internal.persistence.DataWriter
import co.fast.android.internal.datadog.android.core.internal.sampling.RateBasedSampler
import co.fast.android.internal.datadog.android.core.internal.sampling.Sampler
import co.fast.android.internal.datadog.android.log.internal.domain.LogGenerator
import co.fast.android.internal.datadog.android.log.model.LogEvent
import co.fast.android.internal.datadog.android.rum.GlobalRum
import co.fast.android.internal.datadog.android.rum.RumErrorSource

internal class DatadogLogHandler(
    internal val logGenerator: LogGenerator,
    internal val writer: DataWriter<LogEvent>,
    internal val bundleWithTraces: Boolean = true,
    internal val bundleWithRum: Boolean = true,
    internal val sampler: Sampler = RateBasedSampler(1.0f)
) : LogHandler {

    // region LogHandler

    override fun handleLog(
        level: Int,
        message: String,
        throwable: Throwable?,
        attributes: Map<String, Any?>,
        tags: Set<String>,
        timestamp: Long?
    ) {
        val resolvedTimeStamp = timestamp ?: System.currentTimeMillis()
        if (sampler.sample()) {
            val log = createLog(level, message, throwable, attributes, tags, resolvedTimeStamp)
            writer.write(log)
        }

        if (level >= AndroidLog.ERROR) {
            GlobalRum.get().addError(message, RumErrorSource.LOGGER, throwable, attributes)
        }
    }

    // endregion

    // region Internal

    @Suppress("LongParameterList")
    private fun createLog(
        level: Int,
        message: String,
        throwable: Throwable?,
        attributes: Map<String, Any?>,
        tags: Set<String>,
        timestamp: Long
    ): LogEvent {
        return logGenerator.generateLog(
            level,
            message,
            throwable,
            attributes,
            tags,
            timestamp,
            bundleWithRum = bundleWithRum,
            bundleWithTraces = bundleWithTraces
        )
    }

    // endregion
}
