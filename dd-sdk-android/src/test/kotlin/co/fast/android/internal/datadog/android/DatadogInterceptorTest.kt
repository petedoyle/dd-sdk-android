/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android

import co.fast.android.internal.datadog.android.core.configuration.Configuration
import co.fast.android.internal.datadog.android.core.internal.net.identifyRequest
import co.fast.android.internal.datadog.android.rum.RumAttributes
import co.fast.android.internal.datadog.android.rum.RumErrorSource
import co.fast.android.internal.datadog.android.rum.RumResourceAttributesProvider
import co.fast.android.internal.datadog.android.rum.RumResourceKind
import co.fast.android.internal.datadog.android.rum.internal.RumFeature
import co.fast.android.internal.datadog.android.tracing.TracingInterceptor
import co.fast.android.internal.datadog.android.tracing.TracingInterceptorNotSendingSpanTest
import co.fast.android.internal.datadog.android.utils.config.GlobalRumMonitorTestConfiguration
import co.fast.android.internal.datadog.android.utils.forge.Configurator
import co.fast.android.internal.datadog.android.utils.forge.exhaustiveAttributes
import com.datadog.tools.unit.annotations.TestConfigurationsProvider
import com.datadog.tools.unit.extensions.TestConfigurationExtension
import com.datadog.tools.unit.extensions.config.TestConfiguration
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.doThrow
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.whenever
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.annotation.IntForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import io.opentracing.Tracer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
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
internal class DatadogInterceptorTest : TracingInterceptorNotSendingSpanTest() {

    @Mock
    lateinit var mockRumAttributesProvider: RumResourceAttributesProvider

    @Forgery
    lateinit var fakeRumConfig: Configuration.Feature.RUM

    private lateinit var fakeAttributes: Map<String, Any?>

    override fun instantiateTestedInterceptor(
        tracedHosts: List<String>,
        factory: () -> Tracer
    ): TracingInterceptor {
        RumFeature.initialize(
            appContext.mockInstance,
            fakeRumConfig
        )
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

    @BeforeEach
    override fun `set up`(forge: Forge) {
        super.`set up`(forge)
        fakeAttributes = forge.exhaustiveAttributes()
        whenever(
            mockRumAttributesProvider.onProvideAttributes(
                any(),
                anyOrNull(),
                anyOrNull()
            )
        ) doReturn fakeAttributes
    }

    @AfterEach
    override fun `tear down`() {
        super.`tear down`()
        RumFeature.stop()
    }

    @Test
    fun `𝕄 start and stop RUM Resource 𝕎 intercept() for successful request`(
        @IntForgery(min = 200, max = 300) statusCode: Int
    ) {
        // Given
        stubChain(mockChain, statusCode)
        val expectedStartAttrs = emptyMap<String, Any?>()
        val expectedStopAttrs = mapOf(
            RumAttributes.TRACE_ID to fakeTraceId,
            RumAttributes.SPAN_ID to fakeSpanId
        ) + fakeAttributes
        val requestId = identifyRequest(fakeRequest)
        val mimeType = fakeMediaType?.type()
        val kind = when {
            mimeType != null -> RumResourceKind.fromMimeType(mimeType)
            else -> RumResourceKind.NATIVE
        }

        // When
        testedInterceptor.intercept(mockChain)

        // Then
        inOrder(rumMonitor.mockInstance) {
            verify(rumMonitor.mockInstance).startResource(
                requestId,
                fakeMethod,
                fakeUrl,
                expectedStartAttrs
            )
            verify(rumMonitor.mockInstance).stopResource(
                requestId,
                statusCode,
                fakeResponseBody.toByteArray().size.toLong(),
                kind,
                expectedStopAttrs
            )
        }
    }

    @Test
    fun `𝕄 start and stop RUM Resource 𝕎 intercept() for failing request`(
        @IntForgery(min = 400, max = 500) statusCode: Int
    ) {
        // Given
        stubChain(mockChain, statusCode)
        val expectedStartAttrs = emptyMap<String, Any?>()
        val expectedStopAttrs = mapOf(
            RumAttributes.TRACE_ID to fakeTraceId,
            RumAttributes.SPAN_ID to fakeSpanId
        ) + fakeAttributes
        val requestId = identifyRequest(fakeRequest)
        val mimeType = fakeMediaType?.type()
        val kind = when {
            mimeType != null -> RumResourceKind.fromMimeType(mimeType)
            else -> RumResourceKind.NATIVE
        }

        // When
        testedInterceptor.intercept(mockChain)

        // Then
        inOrder(rumMonitor.mockInstance) {
            verify(rumMonitor.mockInstance).startResource(
                requestId,
                fakeMethod,
                fakeUrl,
                expectedStartAttrs
            )
            verify(rumMonitor.mockInstance).stopResource(
                requestId,
                statusCode,
                fakeResponseBody.toByteArray().size.toLong(),
                kind,
                expectedStopAttrs
            )
        }
    }

    @Test
    fun `𝕄 start and stop RUM Resource 𝕎 intercept() for throwing request`(
        @Forgery throwable: Throwable
    ) {
        // Given
        val expectedStartAttrs = emptyMap<String, Any?>()
        val requestId = identifyRequest(fakeRequest)
        whenever(mockChain.request()) doReturn fakeRequest
        whenever(mockChain.proceed(any())) doThrow throwable

        // When
        assertThrows<Throwable>(throwable.message.orEmpty()) {
            testedInterceptor.intercept(mockChain)
        }

        // Then
        inOrder(rumMonitor.mockInstance) {
            verify(rumMonitor.mockInstance).startResource(
                requestId,
                fakeMethod,
                fakeUrl,
                expectedStartAttrs
            )
            verify(rumMonitor.mockInstance).stopResourceWithError(
                requestId,
                null,
                "OkHttp request error $fakeMethod $fakeUrl",
                RumErrorSource.NETWORK,
                throwable,
                fakeAttributes
            )
        }
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
