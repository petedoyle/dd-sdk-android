/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.monitoring.internal

import android.content.Context
import co.fast.android.internal.datadog.android.core.internal.persistence.PayloadDecoration
import co.fast.android.internal.datadog.android.core.internal.persistence.file.advanced.FeatureFileOrchestrator
import co.fast.android.internal.datadog.android.core.internal.persistence.file.batch.BatchFilePersistenceStrategy
import co.fast.android.internal.datadog.android.core.internal.privacy.ConsentProvider
import co.fast.android.internal.datadog.android.log.Logger
import co.fast.android.internal.datadog.android.log.internal.domain.event.LogEventSerializer
import co.fast.android.internal.datadog.android.log.model.LogEvent
import java.util.concurrent.ExecutorService

internal class InternalLogFilePersistenceStrategy(
    consentProvider: ConsentProvider,
    context: Context,
    executorService: ExecutorService,
    internalLogger: Logger
) : BatchFilePersistenceStrategy<LogEvent>(
    FeatureFileOrchestrator(
        consentProvider,
        context,
        "internal-logs",
        executorService,
        internalLogger
    ),
    executorService,
    LogEventSerializer(),
    PayloadDecoration.JSON_ARRAY_DECORATION,
    internalLogger
)
