/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.rum.internal

import co.fast.android.internal.datadog.android.core.configuration.Configuration
import co.fast.android.internal.datadog.android.core.internal.CoreFeature
import co.fast.android.internal.datadog.android.core.internal.SdkFeatureTest
import co.fast.android.internal.datadog.android.core.internal.event.NoOpEventMapper
import co.fast.android.internal.datadog.android.rum.internal.domain.RumFilePersistenceStrategy
import co.fast.android.internal.datadog.android.rum.internal.net.RumOkHttpUploaderV2
import co.fast.android.internal.datadog.android.rum.internal.tracking.NoOpUserActionTrackingStrategy
import co.fast.android.internal.datadog.android.rum.internal.tracking.UserActionTrackingStrategy
import co.fast.android.internal.datadog.android.rum.internal.vitals.AggregatingVitalMonitor
import co.fast.android.internal.datadog.android.rum.internal.vitals.NoOpVitalMonitor
import co.fast.android.internal.datadog.android.rum.tracking.NoOpTrackingStrategy
import co.fast.android.internal.datadog.android.rum.tracking.NoOpViewTrackingStrategy
import co.fast.android.internal.datadog.android.rum.tracking.TrackingStrategy
import co.fast.android.internal.datadog.android.rum.tracking.ViewTrackingStrategy
import co.fast.android.internal.datadog.android.utils.forge.Configurator
import co.fast.android.internal.datadog.tools.unit.extensions.ApiLevelExtension
import co.fast.android.internal.datadog.tools.unit.extensions.TestConfigurationExtension
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import java.lang.ref.WeakReference
import java.util.concurrent.ScheduledThreadPoolExecutor
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
internal class RumFeatureTest : SdkFeatureTest<Any, Configuration.Feature.RUM, RumFeature>() {

    override fun createTestedFeature(): RumFeature {
        return RumFeature
    }

    override fun forgeConfiguration(forge: Forge): Configuration.Feature.RUM {
        return forge.getForgery()
    }

    @Test
    fun `𝕄 initialize persistence strategy 𝕎 initialize()`() {
        // When
        testedFeature.initialize(appContext.mockInstance, fakeConfigurationFeature)

        // Then
        assertThat(testedFeature.persistenceStrategy)
            .isInstanceOf(RumFilePersistenceStrategy::class.java)
    }

    @Test
    fun `𝕄 create a logs uploader 𝕎 createUploader()`() {
        // When
        val uploader = testedFeature.createUploader(fakeConfigurationFeature)

        // Then
        assertThat(uploader).isInstanceOf(RumOkHttpUploaderV2::class.java)
        val rumUploader = uploader as RumOkHttpUploaderV2
        assertThat(rumUploader.intakeUrl).startsWith(fakeConfigurationFeature.endpointUrl)
        assertThat(rumUploader.intakeUrl).endsWith("/api/v2/rum")
        assertThat(rumUploader.callFactory).isSameAs(CoreFeature.okHttpClient)
    }

    @Test
    fun `𝕄 store sampling rate 𝕎 initialize()`() {
        // When
        testedFeature.initialize(appContext.mockInstance, fakeConfigurationFeature)

        // Then
        assertThat(testedFeature.samplingRate).isEqualTo(fakeConfigurationFeature.samplingRate)
    }

    @Test
    fun `𝕄 store background tracking 𝕎 initialize()`() {
        // When
        testedFeature.initialize(appContext.mockInstance, fakeConfigurationFeature)

        // Then
        assertThat(testedFeature.backgroundEventTracking)
            .isEqualTo(fakeConfigurationFeature.backgroundEventTracking)
    }

    @Test
    fun `𝕄 store and register viewTrackingStrategy 𝕎 initialize()`() {
        // When
        testedFeature.initialize(appContext.mockInstance, fakeConfigurationFeature)

        // Then
        assertThat(testedFeature.viewTrackingStrategy)
            .isEqualTo(fakeConfigurationFeature.viewTrackingStrategy)
        verify(fakeConfigurationFeature.viewTrackingStrategy!!).register(appContext.mockInstance)
    }

    @Test
    fun `𝕄 store userActionTrackingStrategy 𝕎 initialize()`() {
        // When
        testedFeature.initialize(appContext.mockInstance, fakeConfigurationFeature)

        // Then
        assertThat(testedFeature.actionTrackingStrategy)
            .isEqualTo(fakeConfigurationFeature.userActionTrackingStrategy)
        verify(fakeConfigurationFeature.userActionTrackingStrategy!!)
            .register(appContext.mockInstance)
    }

    @Test
    fun `𝕄 store longTaskTrackingStrategy 𝕎 initialize()`() {
        // When
        testedFeature.initialize(appContext.mockInstance, fakeConfigurationFeature)

        // Then
        assertThat(testedFeature.longTaskTrackingStrategy)
            .isEqualTo(fakeConfigurationFeature.longTaskTrackingStrategy)
        verify(fakeConfigurationFeature.longTaskTrackingStrategy!!)
            .register(appContext.mockInstance)
    }

    @Test
    fun `𝕄 use noop viewTrackingStrategy 𝕎 initialize()`() {
        // Given
        val config = fakeConfigurationFeature.copy(viewTrackingStrategy = null)

        // When
        testedFeature.initialize(appContext.mockInstance, config)

        // Then
        assertThat(testedFeature.viewTrackingStrategy)
            .isInstanceOf(NoOpViewTrackingStrategy::class.java)
    }

    @Test
    fun `𝕄 use noop userActionTrackingStrategy 𝕎 initialize()`() {
        // Given
        val config = fakeConfigurationFeature.copy(userActionTrackingStrategy = null)

        // When
        testedFeature.initialize(appContext.mockInstance, config)

        // Then
        assertThat(testedFeature.actionTrackingStrategy)
            .isInstanceOf(NoOpUserActionTrackingStrategy::class.java)
    }

    @Test
    fun `𝕄 use noop longTaskTrackingStrategy 𝕎 initialize()`() {
        // Given
        val config = fakeConfigurationFeature.copy(longTaskTrackingStrategy = null)

        // When
        testedFeature.initialize(appContext.mockInstance, config)

        // Then
        assertThat(testedFeature.longTaskTrackingStrategy)
            .isInstanceOf(NoOpTrackingStrategy::class.java)
    }

    @Test
    fun `𝕄 register viewTreeStrategy 𝕎 initialize()`() {
        // When
        val mockViewTreeStrategy: TrackingStrategy = mock()
        testedFeature.viewTreeTrackingStrategy = mockViewTreeStrategy
        testedFeature.initialize(appContext.mockInstance, fakeConfigurationFeature)

        // Then
        verify(mockViewTreeStrategy).register(appContext.mockInstance)
    }

    @Test
    fun `𝕄 store eventMapper 𝕎 initialize()`() {
        // When
        testedFeature.initialize(appContext.mockInstance, fakeConfigurationFeature)

        // Then
        assertThat(testedFeature.rumEventMapper).isSameAs(fakeConfigurationFeature.rumEventMapper)
    }

    @Test
    fun `𝕄 setup vital monitors 𝕎 initialize()`() {
        // When
        testedFeature.initialize(appContext.mockInstance, fakeConfigurationFeature)

        // Then
        assertThat(testedFeature.cpuVitalMonitor)
            .isInstanceOf(AggregatingVitalMonitor::class.java)
        assertThat(testedFeature.memoryVitalMonitor)
            .isInstanceOf(AggregatingVitalMonitor::class.java)
        assertThat(testedFeature.frameRateVitalMonitor)
            .isInstanceOf(AggregatingVitalMonitor::class.java)
    }

    @Test
    fun `𝕄 use noop viewTrackingStrategy 𝕎 stop()`() {
        // Given
        testedFeature.initialize(appContext.mockInstance, fakeConfigurationFeature)

        // When
        testedFeature.stop()

        // Then
        assertThat(testedFeature.viewTrackingStrategy)
            .isInstanceOf(NoOpViewTrackingStrategy::class.java)
    }

    @Test
    fun `𝕄 use noop userActionTrackingStrategy 𝕎 stop()`() {
        // Given
        testedFeature.initialize(appContext.mockInstance, fakeConfigurationFeature)

        // When
        testedFeature.stop()

        // Then
        assertThat(testedFeature.actionTrackingStrategy)
            .isInstanceOf(NoOpUserActionTrackingStrategy::class.java)
    }

    @Test
    fun `𝕄 unregister strategies 𝕎 stop()`() {
        // Given
        testedFeature.initialize(appContext.mockInstance, fakeConfigurationFeature)
        CoreFeature.contextRef = WeakReference(appContext.mockInstance)
        val mockActionTrackingStrategy: UserActionTrackingStrategy = mock()
        val mockViewTrackingStrategy: ViewTrackingStrategy = mock()
        val mockViewTreeTrackingStrategy: TrackingStrategy = mock()
        val mockLongTaskTrackingStrategy: TrackingStrategy = mock()
        testedFeature.actionTrackingStrategy = mockActionTrackingStrategy
        testedFeature.viewTrackingStrategy = mockViewTrackingStrategy
        testedFeature.viewTreeTrackingStrategy = mockViewTreeTrackingStrategy
        testedFeature.longTaskTrackingStrategy = mockLongTaskTrackingStrategy

        // When
        testedFeature.stop()

        // Then
        verify(mockActionTrackingStrategy).unregister(appContext.mockInstance)
        verify(mockViewTrackingStrategy).unregister(appContext.mockInstance)
        verify(mockViewTreeTrackingStrategy).unregister(appContext.mockInstance)
        verify(mockLongTaskTrackingStrategy).unregister(appContext.mockInstance)
    }

    @Test
    fun `𝕄 reset eventMapper 𝕎 stop()`() {
        // Given
        testedFeature.initialize(appContext.mockInstance, fakeConfigurationFeature)

        // When
        testedFeature.stop()

        // Then
        assertThat(testedFeature.rumEventMapper).isInstanceOf(NoOpEventMapper::class.java)
    }

    @Test
    fun `𝕄 initialize vital executor 𝕎 initialize()`() {
        // When
        testedFeature.initialize(appContext.mockInstance, fakeConfigurationFeature)

        // Then
        val scheduledRunnables = testedFeature.vitalExecutorService.shutdownNow()
        assertThat(scheduledRunnables).isNotEmpty
    }

    @Test
    fun `𝕄 shut down vital executor 𝕎 stop()`() {
        // Given
        testedFeature.initialize(appContext.mockInstance, fakeConfigurationFeature)
        val mockVitalExecutorService: ScheduledThreadPoolExecutor = mock()
        RumFeature.vitalExecutorService = mockVitalExecutorService

        // When
        testedFeature.stop()

        // Then
        verify(mockVitalExecutorService).shutdownNow()
    }

    @Test
    fun `𝕄 reset vital monitors 𝕎 stop()`() {
        // Given
        testedFeature.initialize(appContext.mockInstance, fakeConfigurationFeature)

        // When
        testedFeature.stop()

        // Then
        assertThat(testedFeature.cpuVitalMonitor).isInstanceOf(NoOpVitalMonitor::class.java)
        assertThat(testedFeature.memoryVitalMonitor).isInstanceOf(NoOpVitalMonitor::class.java)
        assertThat(testedFeature.frameRateVitalMonitor).isInstanceOf(NoOpVitalMonitor::class.java)
    }
}