/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.tracing.internal.domain

import android.content.Context
import co.fast.android.internal.datadog.android.core.internal.net.info.NetworkInfoProvider
import co.fast.android.internal.datadog.android.core.internal.persistence.PayloadDecoration
import co.fast.android.internal.datadog.android.core.internal.persistence.file.advanced.FeatureFileOrchestrator
import co.fast.android.internal.datadog.android.core.internal.persistence.file.batch.BatchFilePersistenceStrategy
import co.fast.android.internal.datadog.android.core.internal.privacy.ConsentProvider
import co.fast.android.internal.datadog.android.core.internal.time.TimeProvider
import co.fast.android.internal.datadog.android.event.SpanEventMapper
import co.fast.android.internal.datadog.android.log.Logger
import co.fast.android.internal.datadog.android.log.internal.user.UserInfoProvider
import co.fast.android.internal.datadog.android.tracing.internal.domain.event.DdSpanToSpanEventMapper
import co.fast.android.internal.datadog.android.tracing.internal.domain.event.SpanEventMapperWrapper
import co.fast.android.internal.datadog.android.tracing.internal.domain.event.SpanEventSerializer
import co.fast.android.internal.datadog.android.tracing.internal.domain.event.SpanMapperSerializer
import co.fast.android.internal.datadog.opentracing.DDSpan
import java.util.concurrent.ExecutorService

internal class TracesFilePersistenceStrategy(
    consentProvider: ConsentProvider,
    context: Context,
    executorService: ExecutorService,
    timeProvider: TimeProvider,
    networkInfoProvider: NetworkInfoProvider,
    userInfoProvider: UserInfoProvider,
    envName: String,
    internalLogger: Logger,
    spanEventMapper: SpanEventMapper
) : BatchFilePersistenceStrategy<DDSpan>(
    FeatureFileOrchestrator(
        consentProvider,
        context,
        "tracing",
        executorService,
        internalLogger
    ),
    executorService,
    SpanMapperSerializer(
        DdSpanToSpanEventMapper(
            timeProvider,
            networkInfoProvider,
            userInfoProvider
        ),
        SpanEventMapperWrapper(spanEventMapper),
        SpanEventSerializer(envName)
    ),
    PayloadDecoration.NEW_LINE_DECORATION,
    internalLogger
)
