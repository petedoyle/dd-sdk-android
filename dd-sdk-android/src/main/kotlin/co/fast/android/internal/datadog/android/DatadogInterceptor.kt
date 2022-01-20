/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android

import co.fast.android.internal.datadog.android.core.configuration.Configuration
import co.fast.android.internal.datadog.android.core.internal.CoreFeature
import co.fast.android.internal.datadog.android.core.internal.net.FirstPartyHostDetector
import co.fast.android.internal.datadog.android.core.internal.net.identifyRequest
import co.fast.android.internal.datadog.android.core.internal.utils.devLogger
import co.fast.android.internal.datadog.android.rum.GlobalRum
import co.fast.android.internal.datadog.android.rum.NoOpRumResourceAttributesProvider
import co.fast.android.internal.datadog.android.rum.RumAttributes
import co.fast.android.internal.datadog.android.rum.RumErrorSource
import co.fast.android.internal.datadog.android.rum.RumInterceptor
import co.fast.android.internal.datadog.android.rum.RumMonitor
import co.fast.android.internal.datadog.android.rum.RumResourceAttributesProvider
import co.fast.android.internal.datadog.android.rum.RumResourceKind
import co.fast.android.internal.datadog.android.rum.internal.RumFeature
import co.fast.android.internal.datadog.android.rum.tracking.ViewTrackingStrategy
import co.fast.android.internal.datadog.android.tracing.AndroidTracer
import co.fast.android.internal.datadog.android.tracing.NoOpTracedRequestListener
import co.fast.android.internal.datadog.android.tracing.TracedRequestListener
import co.fast.android.internal.datadog.android.tracing.TracingInterceptor
import io.opentracing.Span
import io.opentracing.Tracer
import java.util.Locale
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * Provides automatic integration for [OkHttpClient] by way of the [Interceptor] system.
 *
 * This interceptor will combine the effects of the [TracingInterceptor] and the
 * [RumInterceptor].
 *
 * From [RumInterceptor]: this interceptor will log the request as a RUM Resource, and fill the
 * request information (url, method, status code, optional error). Note that RUM Resources are only
 * tracked when a view is active. You can use one of the existing [ViewTrackingStrategy] when
 * configuring the SDK (see [Configuration.Builder.useViewTrackingStrategy]) or start a view
 * manually (see [RumMonitor.startView]).
 *
 * From [TracingInterceptor]: This interceptor will create a [Span] around the request and fill the
 * request information (url, method, status code, optional error). It will also propagate the span
 * and trace information in the request header to link it with backend spans.
 *
 * Note: If you want to get more insights on the network requests (such as redirections), you can also add
 * this interceptor as a Network level interceptor.
 *
 * To use:
 * ```
 *     val tracedHosts = listOf("example.com", "example.eu")
 *     OkHttpClient client = new OkHttpClient.Builder()
 *         .addInterceptor(new DatadogInterceptor(tracedHosts))
 *         // Optionally to get information about redirections and retries
 *         // .addNetworkInterceptor(new TracingInterceptor(tracedHosts))
 *         .build();
 * ```
 *
 * @param tracedHosts a list of all the hosts that you want to be automatically tracked
 * by our APM [TracingInterceptor]. If no host provided the interceptor won't trace
 * any OkHttpRequest, nor propagate tracing information to the backend.
 * Please note that the host constraint will only be applied on the [TracingInterceptor] and we will
 * continue to dispatch RUM Resource events for each request without applying any host filtering.
 * @param tracedRequestListener which listens on the intercepted [okhttp3.Request] and offers
 * the possibility to modify the created [io.opentracing.Span].
 * @param rumResourceAttributesProvider which listens on the intercepted [okhttp3.Request]
 * and offers the possibility to add custom attributes to the RUM resource events.
 */
open class DatadogInterceptor
internal constructor(
    tracedHosts: List<String>,
    tracedRequestListener: TracedRequestListener,
    firstPartyHostDetector: FirstPartyHostDetector,
    private val rumResourceAttributesProvider: RumResourceAttributesProvider =
        NoOpRumResourceAttributesProvider(),
    localTracerFactory: () -> Tracer
) : TracingInterceptor(
    tracedHosts,
    tracedRequestListener,
    firstPartyHostDetector,
    ORIGIN_RUM,
    localTracerFactory
) {

    /**
     * Creates a [TracingInterceptor] to automatically create a trace around OkHttp [Request]s, and
     * track RUM Resources.
     *
     * @param firstPartyHosts the list of first party hosts.
     * Requests made to a URL with any one of these hosts (or any subdomain) will:
     * - be considered a first party RUM Resource and categorised as such in your RUM dashboard;
     * - be wrapped in a Span and have trace id injected to get a full flame-graph in APM.
     * If no host provided the interceptor won't trace any OkHttp [Request], nor propagate tracing
     * information to the backend, but RUM Resource events will still be sent for each request.
     * @param tracedRequestListener which listens on the intercepted [okhttp3.Request] and offers
     * the possibility to modify the created [io.opentracing.Span].
     * @param rumResourceAttributesProvider which listens on the intercepted [okhttp3.Request]
     * and offers the possibility to add custom attributes to the RUM resource events.
     */
    @JvmOverloads
    constructor(
        firstPartyHosts: List<String>,
        tracedRequestListener: TracedRequestListener = NoOpTracedRequestListener(),
        rumResourceAttributesProvider: RumResourceAttributesProvider =
            NoOpRumResourceAttributesProvider()
    ) : this(
        firstPartyHosts,
        tracedRequestListener,
        CoreFeature.firstPartyHostDetector,
        rumResourceAttributesProvider,
        { AndroidTracer.Builder().build() }
    )

    /**
     * Creates a [TracingInterceptor] to automatically create a trace around OkHttp [Request]s, and
     * track RUM Resources.
     *
     * @param tracedRequestListener which listens on the intercepted [okhttp3.Request] and offers
     * the possibility to modify the created [io.opentracing.Span].
     * @param rumResourceAttributesProvider which listens on the intercepted [okhttp3.Request]
     * and offers the possibility to add custom attributes to the RUM resource events.
     */
    @JvmOverloads
    constructor(
        tracedRequestListener: TracedRequestListener = NoOpTracedRequestListener(),
        rumResourceAttributesProvider: RumResourceAttributesProvider =
            NoOpRumResourceAttributesProvider()
    ) : this(
        emptyList(),
        tracedRequestListener,
        CoreFeature.firstPartyHostDetector,
        rumResourceAttributesProvider,
        { AndroidTracer.Builder().build() }
    )

    // region Interceptor

    /** @inheritdoc */
    override fun intercept(chain: Interceptor.Chain): Response {
        if (RumFeature.isInitialized()) {
            val request = chain.request()
            val url = request.url().toString()
            val method = request.method()
            val requestId = identifyRequest(request)

            GlobalRum.get().startResource(requestId, method, url)
        } else {
            devLogger.w(WARN_RUM_DISABLED)
        }
        return super.intercept(chain)
    }

    // endregion

    // region TracingInterceptor

    /** @inheritdoc */
    override fun onRequestIntercepted(
        request: Request,
        span: Span?,
        response: Response?,
        throwable: Throwable?
    ) {
        super.onRequestIntercepted(request, span, response, throwable)

        if (RumFeature.isInitialized()) {
            if (throwable != null) {
                handleThrowable(request, throwable)
            } else {
                handleResponse(request, response, span)
            }
        }
    }

    /** @inheritdoc */
    override fun canSendSpan(): Boolean {
        return !RumFeature.isInitialized()
    }

    // endregion

    // region Internal

    private fun handleResponse(
        request: Request,
        response: Response?,
        span: Span?
    ) {
        val requestId = identifyRequest(request)
        val statusCode = response?.code()
        val mimeType = response?.header(HEADER_CT)
        val kind = when {
            mimeType == null -> RumResourceKind.NATIVE
            else -> RumResourceKind.fromMimeType(mimeType)
        }
        val attributes = if (span == null) {
            emptyMap<String, Any?>()
        } else {
            mapOf(
                RumAttributes.TRACE_ID to span.context().toTraceId(),
                RumAttributes.SPAN_ID to span.context().toSpanId()
            )
        }
        GlobalRum.get().stopResource(
            requestId,
            statusCode,
            getBodyLength(response),
            kind,
            attributes + rumResourceAttributesProvider.onProvideAttributes(request, response, null)
        )
    }

    private fun handleThrowable(
        request: Request,
        throwable: Throwable
    ) {
        val requestId = identifyRequest(request)
        val method = request.method()
        val url = request.url().toString()
        GlobalRum.get().stopResourceWithError(
            requestId,
            null,
            ERROR_MSG_FORMAT.format(Locale.US, method, url),
            RumErrorSource.NETWORK,
            throwable,
            rumResourceAttributesProvider.onProvideAttributes(request, null, throwable)
        )
    }

    private fun getBodyLength(response: Response?): Long? {
        val body = response?.peekBody(MAX_BODY_PEEK)
        val contentLength = body?.contentLength()
        return if (contentLength == 0L) null else contentLength
    }

    // endregion

    companion object {

        internal const val WARN_RUM_DISABLED =
            "You set up a DatadogInterceptor, but RUM features are disabled." +
                "Make sure you initialized the Datadog SDK with a valid Application Id, " +
                "and that RUM features are enabled."

        internal const val ERROR_MSG_FORMAT = "OkHttp request error %s %s"

        internal const val ORIGIN_RUM = "rum"

        // We need to limit this value as the body will be loaded in memory
        private const val MAX_BODY_PEEK: Long = 32 * 1024L * 1024L
    }
}
