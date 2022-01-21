/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android

import android.util.Log
import co.fast.android.internal.datadog.android.rum.RumResourceAttributesProvider
import co.fast.android.internal.datadog.android.tracing.TracingInterceptor
import co.fast.android.internal.datadog.android.tracing.TracingInterceptorTest
import co.fast.android.internal.datadog.android.utils.config.GlobalRumMonitorTestConfiguration
import co.fast.android.internal.datadog.android.utils.forge.Configurator
import co.fast.android.internal.datadog.tools.unit.annotations.TestConfigurationsProvider
import co.fast.android.internal.datadog.tools.unit.extensions.TestConfigurationExtension
import co.fast.android.internal.datadog.tools.unit.extensions.config.TestConfiguration
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.annotation.IntForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import io.opentracing.Tracer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.Mock
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
internal class DatadogInterceptorWithoutRumTest : TracingInterceptorTest() {

    @Mock
    lateinit var mockRumAttributesProvider: RumResourceAttributesProvider

    override fun instantiateTestedInterceptor(
        tracedHosts: List<String>,
        factory: () -> Tracer
    ): TracingInterceptor {
        return DatadogInterceptor(
            tracedHosts,
            mockRequestListener,
            mockDetector,
            mockRumAttributesProvider,
            factory
        )
    }

    override fun getExpectedOrigin(): String {
        return DatadogInterceptor.ORIGIN_RUM
    }

    @Test
    fun `𝕄 warn that RUM is not enabled 𝕎 intercept()`(
        @IntForgery(min = 200, max = 300) statusCode: Int
    ) {
        // Given
        stubChain(mockChain, statusCode)
        // When
        testedInterceptor.intercept(mockChain)

        // Then
        verify(mockDevLogHandler).handleLog(
            Log.WARN,
            DatadogInterceptor.WARN_RUM_DISABLED
        )
    }

    @Test
    fun `𝕄 do nothing 𝕎 intercept() for successful request`(
        @IntForgery(min = 200, max = 300) statusCode: Int
    ) {
        // Given
        stubChain(mockChain, statusCode)

        // When
        testedInterceptor.intercept(mockChain)

        // Then
        verifyZeroInteractions(rumMonitor.mockInstance)
        verifyZeroInteractions(mockRumAttributesProvider)
    }

    @Test
    fun `𝕄 do nothing RUM Resource 𝕎 intercept() for failing request`(
        @IntForgery(min = 400, max = 500) statusCode: Int
    ) {
        // Given
        stubChain(mockChain, statusCode)

        // When
        testedInterceptor.intercept(mockChain)

        // Then
        verifyZeroInteractions(rumMonitor.mockInstance)
        verifyZeroInteractions(mockRumAttributesProvider)
    }

    @Test
    fun `𝕄 do nothing 𝕎 intercept() for throwing request`(
        @Forgery throwable: Throwable
    ) {
        // Given
        whenever(mockChain.request()) doReturn fakeRequest
        whenever(mockChain.proceed(any())) doThrow throwable

        // When
        assertThrows<Throwable>(throwable.message.orEmpty()) {
            testedInterceptor.intercept(mockChain)
        }

        // Then
        verifyZeroInteractions(rumMonitor.mockInstance)
        verifyZeroInteractions(mockRumAttributesProvider)
    }

    companion object {
        val rumMonitor = GlobalRumMonitorTestConfiguration()

        @TestConfigurationsProvider
        @JvmStatic
        fun getTestConfigurations(): List<TestConfiguration> {
            return listOf(appContext, coreFeature, rumMonitor)
        }
    }
}