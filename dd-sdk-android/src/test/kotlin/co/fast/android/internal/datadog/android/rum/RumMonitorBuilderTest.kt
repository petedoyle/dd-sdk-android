/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.rum

import android.content.Context
import android.os.Looper
import android.util.Log
import co.fast.android.internal.datadog.android.core.configuration.Configuration
import co.fast.android.internal.datadog.android.log.internal.logger.LogHandler
import co.fast.android.internal.datadog.android.rum.internal.RumFeature
import co.fast.android.internal.datadog.android.rum.internal.domain.scope.RumApplicationScope
import co.fast.android.internal.datadog.android.rum.internal.domain.scope.RumSessionScope
import co.fast.android.internal.datadog.android.rum.internal.monitor.DatadogRumMonitor
import co.fast.android.internal.datadog.android.utils.config.ApplicationContextTestConfiguration
import co.fast.android.internal.datadog.android.utils.config.CoreFeatureTestConfiguration
import co.fast.android.internal.datadog.android.utils.forge.Configurator
import co.fast.android.internal.datadog.android.utils.mockDevLogHandler
import co.fast.android.internal.datadog.tools.unit.annotations.TestConfigurationsProvider
import co.fast.android.internal.datadog.tools.unit.extensions.TestConfigurationExtension
import co.fast.android.internal.datadog.tools.unit.extensions.config.TestConfiguration
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import fr.xgouchet.elmyr.annotation.FloatForgery
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class),
    ExtendWith(TestConfigurationExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(Configurator::class)
internal class RumMonitorBuilderTest {

    lateinit var testedBuilder: RumMonitor.Builder

    @Forgery
    lateinit var fakeConfig: Configuration.Feature.RUM

    lateinit var mockDevLogHandler: LogHandler

    @BeforeEach
    fun `set up`() {
        mockDevLogHandler = mockDevLogHandler()
        RumFeature.initialize(appContext.mockInstance, fakeConfig)

        testedBuilder = RumMonitor.Builder()
    }

    @AfterEach
    fun `tear down`() {
        RumFeature.stop()
    }

    @Test
    fun `𝕄 builds a default RumMonitor 𝕎 build()`() {
        // When
        val monitor = testedBuilder.build()

        // Then
        check(monitor is DatadogRumMonitor)
        assertThat(monitor.rootScope).isInstanceOf(RumApplicationScope::class.java)
        assertThat(monitor.rootScope)
            .overridingErrorMessage(
                "Expecting root scope to have applicationId ${coreFeature.fakeRumApplicationId}"
            )
            .matches {
                (it as RumApplicationScope)
                    .getRumContext()
                    .applicationId == coreFeature.fakeRumApplicationId
            }
        assertThat(monitor.handler.looper).isSameAs(Looper.getMainLooper())
        assertThat(monitor.samplingRate).isEqualTo(fakeConfig.samplingRate)
        assertThat(monitor.backgroundTrackingEnabled).isEqualTo(fakeConfig.backgroundEventTracking)
    }

    @Test
    fun `𝕄 builds a RumMonitor with custom sampling 𝕎 build()`(
        @FloatForgery(0f, 100f) samplingRate: Float
    ) {
        // When
        val monitor = testedBuilder
            .sampleRumSessions(samplingRate)
            .build()

        // Then
        check(monitor is DatadogRumMonitor)
        assertThat(monitor.rootScope).isInstanceOf(RumApplicationScope::class.java)
        assertThat(monitor.rootScope)
            .overridingErrorMessage(
                "Expecting root scope to have applicationId ${coreFeature.fakeRumApplicationId}"
            )
            .matches {
                (it as RumApplicationScope)
                    .getRumContext()
                    .applicationId == coreFeature.fakeRumApplicationId
            }
        assertThat(monitor.handler.looper).isSameAs(Looper.getMainLooper())
        assertThat(monitor.samplingRate).isEqualTo(samplingRate)
    }

    @Test
    fun `𝕄 builds a RumMonitor without session listener 𝕎 build()`() {
        // When
        val monitor = testedBuilder.build()

        // Then
        check(monitor is DatadogRumMonitor)
        assertThat(monitor.rootScope).isInstanceOf(RumApplicationScope::class.java)
        val appScope = monitor.rootScope as RumApplicationScope
        assertThat(appScope.childScope).isInstanceOf(RumSessionScope::class.java)
        val sessionScope = appScope.childScope as RumSessionScope
        assertThat(sessionScope.sessionListener).isNull()
    }

    @Test
    fun `𝕄 builds a RumMonitor with session callback 𝕎 setSessionListener() + build()`() {
        // When
        val mockListener: RumSessionListener = mock()
        val monitor = testedBuilder
            .setSessionListener(mockListener)
            .build()

        // Then
        check(monitor is DatadogRumMonitor)
        assertThat(monitor.rootScope).isInstanceOf(RumApplicationScope::class.java)
        val appScope = monitor.rootScope as RumApplicationScope
        assertThat(appScope.childScope).isInstanceOf(RumSessionScope::class.java)
        val sessionScope = appScope.childScope as RumSessionScope
        assertThat(sessionScope.sessionListener).isSameAs(mockListener)
    }

    @Test
    fun `𝕄 builds nothing 𝕎 build() and RumFeature is not initialized`() {
        // Given
        RumFeature.stop()

        // When
        val monitor = testedBuilder.build()

        // Then
        verify(mockDevLogHandler).handleLog(
            Log.ERROR,
            RumMonitor.Builder.RUM_NOT_ENABLED_ERROR_MESSAGE
        )
        check(monitor is NoOpRumMonitor)
    }

    companion object {
        val appContext = ApplicationContextTestConfiguration(Context::class.java)
        val coreFeature = CoreFeatureTestConfiguration(appContext)

        @TestConfigurationsProvider
        @JvmStatic
        fun getTestConfigurations(): List<TestConfiguration> {
            return listOf(appContext, coreFeature)
        }
    }
}