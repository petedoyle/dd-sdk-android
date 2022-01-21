/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.monitoring.internal

import android.content.Context
import co.fast.android.internal.datadog.android.core.configuration.Configuration
import co.fast.android.internal.datadog.android.core.internal.CoreFeature
import co.fast.android.internal.datadog.android.core.internal.SdkFeature
import co.fast.android.internal.datadog.android.core.internal.net.DataUploader
import co.fast.android.internal.datadog.android.core.internal.persistence.PersistenceStrategy
import co.fast.android.internal.datadog.android.core.internal.utils.rebuildSdkLogger
import co.fast.android.internal.datadog.android.core.internal.utils.sdkLogger
import co.fast.android.internal.datadog.android.log.Logger
import co.fast.android.internal.datadog.android.log.internal.logger.NoOpLogHandler
import co.fast.android.internal.datadog.android.log.internal.net.LogsOkHttpUploaderV2
import co.fast.android.internal.datadog.android.log.model.LogEvent

internal object InternalLogsFeature : SdkFeature<LogEvent, Configuration.Feature.InternalLogs>() {

    internal const val SERVICE_NAME = "dd-sdk-android"
    internal const val ENV_NAME = "prod"
    private const val APPLICATION_PACKAGE_KEY = "application.name"

    // region SdkFeature

    override fun onPostInitialized(context: Context) {
        // The sdk logger might have already been initialized
        // while the feature was not yet initialized
        rebuildSdkLogger()
        sdkLogger.addAttribute(APPLICATION_PACKAGE_KEY, CoreFeature.packageName)
    }

    override fun onPostStopped() {
        rebuildSdkLogger()
    }

    override fun createPersistenceStrategy(
        context: Context,
        configuration: Configuration.Feature.InternalLogs
    ): PersistenceStrategy<LogEvent> {
        return InternalLogFilePersistenceStrategy(
            CoreFeature.trackingConsentProvider,
            context,
            CoreFeature.persistenceExecutorService,
            Logger(NoOpLogHandler())
        )
    }

    override fun createUploader(configuration: Configuration.Feature.InternalLogs): DataUploader {
        return LogsOkHttpUploaderV2(
            configuration.endpointUrl,
            configuration.internalClientToken,
            CoreFeature.sourceName,
            CoreFeature.sdkVersion,
            CoreFeature.okHttpClient,
            Logger(NoOpLogHandler())
        )
    }

    // endregion
}