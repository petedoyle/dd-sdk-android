/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.tracing.internal

import android.content.Context
import co.fast.android.internal.datadog.android.core.configuration.Configuration
import co.fast.android.internal.datadog.android.core.internal.CoreFeature
import co.fast.android.internal.datadog.android.core.internal.SdkFeature
import co.fast.android.internal.datadog.android.core.internal.net.DataUploader
import co.fast.android.internal.datadog.android.core.internal.persistence.PersistenceStrategy
import co.fast.android.internal.datadog.android.core.internal.utils.sdkLogger
import co.fast.android.internal.datadog.android.tracing.internal.domain.TracesFilePersistenceStrategy
import co.fast.android.internal.datadog.android.tracing.internal.net.TracesOkHttpUploaderV2
import co.fast.android.internal.datadog.opentracing.DDSpan

internal object TracesFeature : SdkFeature<DDSpan, Configuration.Feature.Tracing>() {

    // region SdkFeature

    override fun createPersistenceStrategy(
        context: Context,
        configuration: Configuration.Feature.Tracing
    ): PersistenceStrategy<DDSpan> {
        return TracesFilePersistenceStrategy(
            CoreFeature.trackingConsentProvider,
            context,
            CoreFeature.persistenceExecutorService,
            CoreFeature.timeProvider,
            CoreFeature.networkInfoProvider,
            CoreFeature.userInfoProvider,
            CoreFeature.envName,
            sdkLogger,
            configuration.spanEventMapper
        )
    }

    override fun createUploader(configuration: Configuration.Feature.Tracing): DataUploader {
        return TracesOkHttpUploaderV2(
            configuration.endpointUrl,
            CoreFeature.clientToken,
            CoreFeature.sourceName,
            CoreFeature.sdkVersion,
            CoreFeature.okHttpClient
        )
    }

    // endregion
}