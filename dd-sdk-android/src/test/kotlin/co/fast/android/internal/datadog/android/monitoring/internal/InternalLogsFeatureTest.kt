/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.monitoring.internal

import co.fast.android.internal.datadog.android.core.configuration.Configuration
import co.fast.android.internal.datadog.android.core.internal.CoreFeature
import co.fast.android.internal.datadog.android.core.internal.SdkFeatureTest
import co.fast.android.internal.datadog.android.core.internal.utils.sdkLogger
import co.fast.android.internal.datadog.android.log.internal.net.LogsOkHttpUploaderV2
import co.fast.android.internal.datadog.android.log.model.LogEvent
import co.fast.android.internal.datadog.android.utils.forge.Configurator
import co.fast.android.internal.datadog.tools.unit.extensions.ApiLevelExtension
import co.fast.android.internal.datadog.tools.unit.extensions.TestConfigurationExtension
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class),
    ExtendWith(ApiLevelExtension::class),
    ExtendWith(TestConfigurationExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(Configurator::class)
internal class InternalLogsFeatureTest :
    SdkFeatureTest<LogEvent, Configuration.Feature.InternalLogs, InternalLogsFeature>() {

    override fun createTestedFeature(): InternalLogsFeature {
        return InternalLogsFeature
    }

    override fun forgeConfiguration(forge: Forge): Configuration.Feature.InternalLogs {
        return forge.getForgery()
    }

    @Test
    fun `𝕄 initialize persistence strategy 𝕎 initialize()`() {
        // When
        testedFeature.initialize(appContext.mockInstance, fakeConfigurationFeature)

        // Then
        assertThat(testedFeature.persistenceStrategy)
            .isInstanceOf(InternalLogFilePersistenceStrategy::class.java)
    }

    @Test
    fun `𝕄 rebuild the sdkLogger 𝕎 initialize()`() {
        // Given
        val originalHandler = sdkLogger.handler

        // When
        testedFeature.initialize(appContext.mockInstance, fakeConfigurationFeature)

        // Then
        assertThat(sdkLogger.handler).isNotSameAs(originalHandler)
    }

    @Test
    fun `𝕄 rebuild the sdkLogger 𝕎 stop()`() {
        // Given
        val originalHandler = sdkLogger.handler

        // When
        testedFeature.initialize(appContext.mockInstance, fakeConfigurationFeature)
        val initHandler = sdkLogger.handler
        testedFeature.stop()

        // Then
        assertThat(sdkLogger.handler).isNotSameAs(originalHandler)
        assertThat(sdkLogger.handler).isNotSameAs(initHandler)
    }

    @Test
    fun `𝕄 create a logs uploader 𝕎 createUploader()`() {
        // When
        val uploader = testedFeature.createUploader(fakeConfigurationFeature)

        // Then
        assertThat(uploader).isInstanceOf(LogsOkHttpUploaderV2::class.java)
        val logsUploader = uploader as LogsOkHttpUploaderV2
        assertThat(logsUploader.intakeUrl).startsWith(fakeConfigurationFeature.endpointUrl)
        assertThat(logsUploader.intakeUrl).endsWith("/api/v2/logs")
        assertThat(logsUploader.callFactory).isSameAs(CoreFeature.okHttpClient)
    }
}