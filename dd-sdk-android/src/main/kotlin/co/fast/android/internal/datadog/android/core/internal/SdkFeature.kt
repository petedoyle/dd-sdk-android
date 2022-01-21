/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.core.internal

import android.content.Context
import co.fast.android.internal.datadog.android.core.configuration.Configuration
import co.fast.android.internal.datadog.android.core.internal.data.upload.DataUploadScheduler
import co.fast.android.internal.datadog.android.core.internal.data.upload.NoOpUploadScheduler
import co.fast.android.internal.datadog.android.core.internal.data.upload.UploadScheduler
import co.fast.android.internal.datadog.android.core.internal.net.DataUploader
import co.fast.android.internal.datadog.android.core.internal.net.NoOpDataUploader
import co.fast.android.internal.datadog.android.core.internal.persistence.NoOpPersistenceStrategy
import co.fast.android.internal.datadog.android.core.internal.persistence.PersistenceStrategy
import co.fast.android.internal.datadog.android.core.internal.privacy.ConsentProvider
import co.fast.android.internal.datadog.android.plugin.DatadogPlugin
import co.fast.android.internal.datadog.android.plugin.DatadogPluginConfig
import java.util.concurrent.atomic.AtomicBoolean

@Suppress("TooManyFunctions")
internal abstract class SdkFeature<T : Any, C : Configuration.Feature> {

    internal val initialized = AtomicBoolean(false)

    internal var persistenceStrategy: PersistenceStrategy<T> = NoOpPersistenceStrategy()
    internal var uploader: DataUploader = NoOpDataUploader()
    internal var uploadScheduler: UploadScheduler = NoOpUploadScheduler()
    internal val featurePlugins: MutableList<DatadogPlugin> = mutableListOf()

    // region SDK Feature

    fun initialize(context: Context, configuration: C) {
        if (initialized.get()) {
            return
        }

        persistenceStrategy = createPersistenceStrategy(context, configuration)

        setupUploader(configuration)

        registerPlugins(
            configuration.plugins,
            DatadogPluginConfig(
                context = context,
                envName = CoreFeature.envName,
                serviceName = CoreFeature.serviceName,
                trackingConsent = CoreFeature.trackingConsentProvider.getConsent()
            ),
            CoreFeature.trackingConsentProvider
        )

        onInitialize(context, configuration)

        initialized.set(true)

        onPostInitialized(context)
    }

    fun isInitialized(): Boolean {
        return initialized.get()
    }

    fun clearAllData() {
        persistenceStrategy.getReader().dropAll()
    }

    fun stop() {
        if (initialized.get()) {
            unregisterPlugins()
            uploadScheduler.stopScheduling()
            persistenceStrategy = NoOpPersistenceStrategy()
            uploadScheduler = NoOpUploadScheduler()

            onStop()

            initialized.set(false)
            onPostStopped()
        }
    }

    fun getPlugins(): List<DatadogPlugin> {
        return featurePlugins
    }

    fun flushStoredData() {
        persistenceStrategy.getFlusher().flush(uploader)
    }

    // endregion

    // region Abstract

    open fun onInitialize(context: Context, configuration: C) {}

    open fun onPostInitialized(context: Context) {}

    open fun onStop() {}

    open fun onPostStopped() {}

    abstract fun createPersistenceStrategy(
        context: Context,
        configuration: C
    ): PersistenceStrategy<T>

    abstract fun createUploader(configuration: C): DataUploader

    // endregion

    // region Internal

    private fun registerPlugins(
        plugins: List<DatadogPlugin>,
        config: DatadogPluginConfig,
        trackingConsentProvider: ConsentProvider
    ) {
        plugins.forEach {
            featurePlugins.add(it)
            it.register(config)
            trackingConsentProvider.registerCallback(it)
        }
    }

    private fun unregisterPlugins() {
        featurePlugins.forEach {
            it.unregister()
        }
        featurePlugins.clear()
    }

    private fun setupUploader(configuration: C) {
        uploadScheduler = if (CoreFeature.isMainProcess) {
            uploader = createUploader(configuration)
            DataUploadScheduler(
                persistenceStrategy.getReader(),
                uploader,
                CoreFeature.networkInfoProvider,
                CoreFeature.systemInfoProvider,
                CoreFeature.uploadFrequency,
                CoreFeature.uploadExecutorService
            )
        } else {
            NoOpUploadScheduler()
        }
        uploadScheduler.startScheduling()
    }

    // endregion
}