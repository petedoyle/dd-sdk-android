/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.error.internal

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.impl.WorkManagerImpl
import co.fast.android.internal.datadog.android.Datadog
import co.fast.android.internal.datadog.android.core.configuration.Configuration
import co.fast.android.internal.datadog.android.core.configuration.Credentials
import co.fast.android.internal.datadog.android.core.internal.CoreFeature
import co.fast.android.internal.datadog.android.core.internal.data.upload.UploadWorker
import co.fast.android.internal.datadog.android.core.internal.net.info.NetworkInfoProvider
import co.fast.android.internal.datadog.android.core.internal.persistence.DataWriter
import co.fast.android.internal.datadog.android.core.internal.thread.waitToIdle
import co.fast.android.internal.datadog.android.core.internal.time.TimeProvider
import co.fast.android.internal.datadog.android.core.internal.utils.TAG_DATADOG_UPLOAD
import co.fast.android.internal.datadog.android.core.internal.utils.UPLOAD_WORKER_NAME
import co.fast.android.internal.datadog.android.core.model.NetworkInfo
import co.fast.android.internal.datadog.android.core.model.UserInfo
import co.fast.android.internal.datadog.android.log.LogAttributes
import co.fast.android.internal.datadog.android.log.assertj.LogEventAssert.Companion.assertThat
import co.fast.android.internal.datadog.android.log.internal.domain.LogGenerator
import co.fast.android.internal.datadog.android.log.internal.logger.LogHandler
import co.fast.android.internal.datadog.android.log.internal.user.UserInfoProvider
import co.fast.android.internal.datadog.android.log.model.LogEvent
import co.fast.android.internal.datadog.android.privacy.TrackingConsent
import co.fast.android.internal.datadog.android.rum.GlobalRum
import co.fast.android.internal.datadog.android.rum.RumErrorSource
import co.fast.android.internal.datadog.android.tracing.AndroidTracer
import co.fast.android.internal.datadog.android.utils.config.ApplicationContextTestConfiguration
import co.fast.android.internal.datadog.android.utils.config.GlobalRumMonitorTestConfiguration
import co.fast.android.internal.datadog.android.utils.config.MainLooperTestConfiguration
import co.fast.android.internal.datadog.android.utils.extension.mockChoreographerInstance
import co.fast.android.internal.datadog.android.utils.forge.Configurator
import co.fast.android.internal.datadog.android.utils.mockDevLogHandler
import co.fast.android.internal.datadog.tools.unit.annotations.TestConfigurationsProvider
import co.fast.android.internal.datadog.tools.unit.extensions.ApiLevelExtension
import co.fast.android.internal.datadog.tools.unit.extensions.TestConfigurationExtension
import co.fast.android.internal.datadog.tools.unit.extensions.config.TestConfiguration
import co.fast.android.internal.datadog.tools.unit.invokeMethod
import co.fast.android.internal.datadog.tools.unit.setStaticValue
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import com.nhaarman.mockitokotlin2.whenever
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.annotation.StringForgeryType
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import io.opentracing.util.GlobalTracer
import java.io.FileNotFoundException
import java.io.IOException
import java.lang.RuntimeException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.mockito.ArgumentMatchers
import org.mockito.Mock
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
internal class DatadogExceptionHandlerTest {

    var originalHandler: Thread.UncaughtExceptionHandler? = null

    lateinit var testedHandler: DatadogExceptionHandler

    @Mock
    lateinit var mockPreviousHandler: Thread.UncaughtExceptionHandler

    @Mock
    lateinit var mockNetworkInfoProvider: NetworkInfoProvider

    @Mock
    lateinit var mockUserInfoProvider: UserInfoProvider

    @Mock
    lateinit var mockTimeProvider: TimeProvider

    @Mock
    lateinit var mockLogWriter: DataWriter<LogEvent>

    @Mock
    lateinit var mockWorkManager: WorkManagerImpl

    @Forgery
    lateinit var fakeThrowable: Throwable

    @Forgery
    lateinit var fakeNetworkInfo: NetworkInfo

    @Forgery
    lateinit var fakeUserInfo: UserInfo

    @StringForgery(StringForgeryType.HEXADECIMAL)
    lateinit var fakeToken: String

    @StringForgery(regex = "[a-zA-Z0-9_:./-]{0,195}[a-zA-Z0-9_./-]")
    lateinit var fakeEnvName: String

    lateinit var mockDevLogHandler: LogHandler

    @BeforeEach
    fun `set up`() {
        mockDevLogHandler = mockDevLogHandler()
        mockChoreographerInstance()

        whenever(mockNetworkInfoProvider.getLatestNetworkInfo()) doReturn fakeNetworkInfo
        whenever(mockUserInfoProvider.getUserInfo()) doReturn fakeUserInfo

        _root_ide_package_.co.fast.android.internal.datadog.android.Datadog.initialize(
            appContext.mockInstance,
            Credentials(fakeToken, fakeEnvName, Credentials.NO_VARIANT, null),
            Configuration.Builder(
                logsEnabled = true,
                tracesEnabled = true,
                crashReportsEnabled = true,
                rumEnabled = true
            ).build(),
            TrackingConsent.GRANTED
        )

        originalHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(mockPreviousHandler)
        testedHandler = DatadogExceptionHandler(
            LogGenerator(
                CoreFeature.serviceName,
                DatadogExceptionHandler.LOGGER_NAME,
                mockNetworkInfoProvider,
                mockUserInfoProvider,
                mockTimeProvider,
                CoreFeature.sdkVersion,
                CoreFeature.envName,
                CoreFeature.packageVersion
            ),
            writer = mockLogWriter,
            appContext = appContext.mockInstance
        )
        testedHandler.register()
    }

    @AfterEach
    fun `tear down`() {
        Thread.setDefaultUncaughtExceptionHandler(originalHandler)
        WorkManagerImpl::class.java.setStaticValue("sDefaultInstance", null)
        _root_ide_package_.co.fast.android.internal.datadog.android.Datadog.invokeMethod("stop")
        GlobalTracer::class.java.setStaticValue("isRegistered", false)
    }

    @Test
    fun `M log exception W caught with no previous handler`() {
        Thread.setDefaultUncaughtExceptionHandler(null)
        testedHandler.register()
        val currentThread = Thread.currentThread()

        val now = System.currentTimeMillis()
        testedHandler.uncaughtException(currentThread, fakeThrowable)

        argumentCaptor<LogEvent> {
            verify(mockLogWriter).write(capture())
            assertThat(lastValue)
                .hasThreadName(currentThread.name)
                .hasMessage(fakeThrowable.message!!)
                .hasStatus(LogEvent.Status.EMERGENCY)
                .hasError(fakeThrowable.asLogError())
                .hasNetworkInfo(fakeNetworkInfo)
                .hasUserInfo(fakeUserInfo)
                .hasDateAround(now)
                .hasExactlyTags(
                    listOf(
                        "${LogAttributes.ENV}:$fakeEnvName",
                        "${LogAttributes.APPLICATION_VERSION}:${appContext.fakeVersionName}"
                    )
                )
                .hasExactlyAttributes(
                    mapOf(
                        LogAttributes.RUM_APPLICATION_ID to rumMonitor.context.applicationId,
                        LogAttributes.RUM_SESSION_ID to rumMonitor.context.sessionId,
                        LogAttributes.RUM_VIEW_ID to rumMonitor.context.viewId,
                        LogAttributes.RUM_ACTION_ID to rumMonitor.context.actionId
                    )
                )
        }
        verifyZeroInteractions(mockPreviousHandler)
    }

    @Test
    fun `M wait for the executor to idle W exception caught`() {
        val mockScheduledThreadExecutor: ThreadPoolExecutor = mock()
        CoreFeature.persistenceExecutorService = mockScheduledThreadExecutor
        Thread.setDefaultUncaughtExceptionHandler(null)
        testedHandler.register()
        val currentThread = Thread.currentThread()

        testedHandler.uncaughtException(currentThread, fakeThrowable)

        verify(mockScheduledThreadExecutor)
            .waitToIdle(DatadogExceptionHandler.MAX_WAIT_FOR_IDLE_TIME_IN_MS)
        verify(mockDevLogHandler, never()).handleLog(
            Log.WARN,
            DatadogExceptionHandler.EXECUTOR_NOT_IDLED_WARNING_MESSAGE
        )
    }

    @Test
    fun `M log warning message W exception caught { executor could not be idled }`() {
        val mockScheduledThreadExecutor: ThreadPoolExecutor = mock {
            whenever(it.taskCount).thenReturn(2)
            whenever(it.completedTaskCount).thenReturn(0)
        }
        CoreFeature.persistenceExecutorService = mockScheduledThreadExecutor
        Thread.setDefaultUncaughtExceptionHandler(null)
        testedHandler.register()
        val currentThread = Thread.currentThread()

        testedHandler.uncaughtException(currentThread, fakeThrowable)

        verify(mockDevLogHandler).handleLog(
            Log.WARN,
            DatadogExceptionHandler.EXECUTOR_NOT_IDLED_WARNING_MESSAGE
        )
    }

    @Test
    fun `M schedule the worker W logging an exception`() {

        whenever(
            mockWorkManager.enqueueUniqueWork(
                ArgumentMatchers.anyString(),
                any(),
                any<OneTimeWorkRequest>()
            )
        ) doReturn mock()

        WorkManagerImpl::class.java.setStaticValue("sDefaultInstance", mockWorkManager)
        Thread.setDefaultUncaughtExceptionHandler(null)
        testedHandler.register()
        val currentThread = Thread.currentThread()

        testedHandler.uncaughtException(currentThread, fakeThrowable)

        verify(mockWorkManager)
            .enqueueUniqueWork(
                eq(UPLOAD_WORKER_NAME),
                eq(ExistingWorkPolicy.REPLACE),
                argThat<OneTimeWorkRequest> {
                    this.workSpec.workerClassName == UploadWorker::class.java.canonicalName &&
                        this.tags.contains(TAG_DATADOG_UPLOAD)
                }
            )
    }

    @Test
    fun `M log exception W caught { exception with message }`() {
        val currentThread = Thread.currentThread()
        val now = System.currentTimeMillis()

        testedHandler.uncaughtException(currentThread, fakeThrowable)

        argumentCaptor<LogEvent> {
            verify(mockLogWriter).write(capture())
            assertThat(lastValue)
                .hasThreadName(currentThread.name)
                .hasMessage(fakeThrowable.message!!)
                .hasStatus(LogEvent.Status.EMERGENCY)
                .hasError(fakeThrowable.asLogError())
                .hasNetworkInfo(fakeNetworkInfo)
                .hasUserInfo(fakeUserInfo)
                .hasDateAround(now)
                .hasExactlyTags(
                    listOf(
                        "${LogAttributes.ENV}:$fakeEnvName",
                        "${LogAttributes.APPLICATION_VERSION}:${appContext.fakeVersionName}"
                    )
                )
                .hasExactlyAttributes(
                    mapOf(
                        LogAttributes.RUM_APPLICATION_ID to rumMonitor.context.applicationId,
                        LogAttributes.RUM_SESSION_ID to rumMonitor.context.sessionId,
                        LogAttributes.RUM_VIEW_ID to rumMonitor.context.viewId,
                        LogAttributes.RUM_ACTION_ID to rumMonitor.context.actionId
                    )
                )
        }
        verify(mockPreviousHandler).uncaughtException(currentThread, fakeThrowable)
    }

    @Test
    fun `M log exception W caught { exception without message }`(forge: Forge) {
        val currentThread = Thread.currentThread()
        val now = System.currentTimeMillis()
        val throwable = forge.aThrowableWithoutMessage()

        testedHandler.uncaughtException(currentThread, throwable)

        argumentCaptor<LogEvent> {
            verify(mockLogWriter).write(capture())
            assertThat(lastValue)
                .hasThreadName(currentThread.name)
                .hasMessage("Application crash detected: ${throwable.javaClass.canonicalName}")
                .hasStatus(LogEvent.Status.EMERGENCY)
                .hasError(throwable.asLogError())
                .hasNetworkInfo(fakeNetworkInfo)
                .hasUserInfo(fakeUserInfo)
                .hasDateAround(now)
                .hasExactlyTags(
                    listOf(
                        "${LogAttributes.ENV}:$fakeEnvName",
                        "${LogAttributes.APPLICATION_VERSION}:${appContext.fakeVersionName}"
                    )
                )
                .hasExactlyAttributes(
                    mapOf(
                        LogAttributes.RUM_APPLICATION_ID to rumMonitor.context.applicationId,
                        LogAttributes.RUM_SESSION_ID to rumMonitor.context.sessionId,
                        LogAttributes.RUM_VIEW_ID to rumMonitor.context.viewId,
                        LogAttributes.RUM_ACTION_ID to rumMonitor.context.actionId
                    )
                )
        }
        verify(mockPreviousHandler).uncaughtException(currentThread, throwable)
    }

    @Test
    fun `M log exception W caught { exception without message or class }`() {
        val currentThread = Thread.currentThread()

        val now = System.currentTimeMillis()

        val throwable = object : RuntimeException() {}

        testedHandler.uncaughtException(currentThread, throwable)

        argumentCaptor<LogEvent> {
            verify(mockLogWriter).write(capture())
            assertThat(lastValue)
                .hasThreadName(currentThread.name)
                .hasMessage("Application crash detected: ${throwable.javaClass.simpleName}")
                .hasStatus(LogEvent.Status.EMERGENCY)
                .hasError(throwable.asLogError())
                .hasNetworkInfo(fakeNetworkInfo)
                .hasUserInfo(fakeUserInfo)
                .hasDateAround(now)
                .hasExactlyTags(
                    listOf(
                        "${LogAttributes.ENV}:$fakeEnvName",
                        "${LogAttributes.APPLICATION_VERSION}:${appContext.fakeVersionName}"
                    )
                )
                .hasExactlyAttributes(
                    mapOf(
                        LogAttributes.RUM_APPLICATION_ID to rumMonitor.context.applicationId,
                        LogAttributes.RUM_SESSION_ID to rumMonitor.context.sessionId,
                        LogAttributes.RUM_VIEW_ID to rumMonitor.context.viewId,
                        LogAttributes.RUM_ACTION_ID to rumMonitor.context.actionId
                    )
                )
        }
        verify(mockPreviousHandler).uncaughtException(currentThread, throwable)
    }

    @Test
    fun `M log exception W caught on background thread`(forge: Forge) {
        val latch = CountDownLatch(1)
        val threadName = forge.anAlphabeticalString()
        val thread = Thread(
            {
                testedHandler.uncaughtException(Thread.currentThread(), fakeThrowable)
                latch.countDown()
            },
            threadName
        )

        val now = System.currentTimeMillis()
        thread.start()
        latch.await(1, TimeUnit.SECONDS)

        argumentCaptor<LogEvent> {
            verify(mockLogWriter).write(capture())
            assertThat(lastValue)
                .hasThreadName(threadName)
                .hasMessage(fakeThrowable.message!!)
                .hasStatus(LogEvent.Status.EMERGENCY)
                .hasError(fakeThrowable.asLogError())
                .hasNetworkInfo(fakeNetworkInfo)
                .hasUserInfo(fakeUserInfo)
                .hasDateAround(now)
                .hasExactlyTags(
                    listOf(
                        "${LogAttributes.ENV}:$fakeEnvName",
                        "${LogAttributes.APPLICATION_VERSION}:${appContext.fakeVersionName}"
                    )
                )
                .hasExactlyAttributes(
                    mapOf(
                        LogAttributes.RUM_APPLICATION_ID to rumMonitor.context.applicationId,
                        LogAttributes.RUM_SESSION_ID to rumMonitor.context.sessionId,
                        LogAttributes.RUM_VIEW_ID to rumMonitor.context.viewId,
                        LogAttributes.RUM_ACTION_ID to rumMonitor.context.actionId
                    )
                )
        }
        verify(mockPreviousHandler).uncaughtException(thread, fakeThrowable)
    }

    @Test
    fun `M add current span information W tracer is active`(
        @StringForgery operation: String
    ) {
        val currentThread = Thread.currentThread()
        val tracer = AndroidTracer.Builder().build()
        val span = tracer.buildSpan(operation).start()
        tracer.activateSpan(span)
        GlobalTracer.registerIfAbsent(tracer)

        testedHandler.uncaughtException(currentThread, fakeThrowable)

        argumentCaptor<LogEvent> {
            verify(mockLogWriter).write(capture())

            assertThat(lastValue)
                .hasExactlyAttributes(
                    mapOf(
                        LogAttributes.DD_TRACE_ID to tracer.traceId,
                        LogAttributes.DD_SPAN_ID to tracer.spanId,
                        LogAttributes.RUM_APPLICATION_ID to rumMonitor.context.applicationId,
                        LogAttributes.RUM_SESSION_ID to rumMonitor.context.sessionId,
                        LogAttributes.RUM_VIEW_ID to rumMonitor.context.viewId,
                        LogAttributes.RUM_ACTION_ID to rumMonitor.context.actionId
                    )
                )
        }
        _root_ide_package_.co.fast.android.internal.datadog.android.Datadog.invokeMethod("stop")
    }

    @Test
    fun `M register RUM Error W RumMonitor registered { exception with message }`() {
        val currentThread = Thread.currentThread()

        testedHandler.uncaughtException(currentThread, fakeThrowable)

        verify(rumMonitor.mockInstance).addCrash(
            fakeThrowable.message!!,
            RumErrorSource.SOURCE,
            fakeThrowable
        )
        verify(mockPreviousHandler).uncaughtException(currentThread, fakeThrowable)
    }

    @Test
    fun `M register RUM Error W RumMonitor registered { exception without message }`(
        forge: Forge
    ) {
        val currentThread = Thread.currentThread()
        val throwable = forge.aThrowableWithoutMessage()

        testedHandler.uncaughtException(currentThread, throwable)

        verify(rumMonitor.mockInstance).addCrash(
            "Application crash detected: ${throwable.javaClass.canonicalName}",
            RumErrorSource.SOURCE,
            throwable
        )
        verify(mockPreviousHandler).uncaughtException(currentThread, throwable)
    }

    @Test
    fun `M register RUM Error W RumMonitor registered { exception without message or class }`() {
        val currentThread = Thread.currentThread()
        val throwable = object : RuntimeException() {}

        testedHandler.uncaughtException(currentThread, throwable)

        verify(rumMonitor.mockInstance).addCrash(
            "Application crash detected: ${throwable.javaClass.simpleName}",
            RumErrorSource.SOURCE,
            throwable
        )
        verify(mockPreviousHandler).uncaughtException(currentThread, throwable)
    }

    @Test
    fun `M not add RUM information W no RUM Monitor registered`() {
        val currentThread = Thread.currentThread()
        GlobalRum.isRegistered.set(false)

        testedHandler.uncaughtException(currentThread, fakeThrowable)

        argumentCaptor<LogEvent> {
            verify(mockLogWriter).write(capture())

            assertThat(lastValue)
                .hasExactlyAttributes(emptyMap())
        }
        verify(mockPreviousHandler).uncaughtException(currentThread, fakeThrowable)
    }

    private fun Forge.aThrowableWithoutMessage(): Throwable {
        val exceptionClass = anElementFrom(
            IOException::class.java,
            IllegalStateException::class.java,
            UnknownError::class.java,
            ArrayIndexOutOfBoundsException::class.java,
            NullPointerException::class.java,
            UnsupportedOperationException::class.java,
            FileNotFoundException::class.java
        )

        return if (aBool()) {
            exceptionClass.newInstance()
        } else {
            exceptionClass.constructors
                .first {
                    it.parameterTypes.size == 1 && it.parameterTypes[0] == String::class.java
                }
                .newInstance(anElementFrom("", aWhitespaceString())) as Throwable
        }
    }

    private fun Throwable.asLogError(): LogEvent.Error {
        return LogEvent.Error(
            kind = this.javaClass.canonicalName ?: this.javaClass.simpleName,
            message = this.message,
            stack = this.stackTraceToString()
        )
    }

    companion object {
        val appContext = ApplicationContextTestConfiguration(Context::class.java)
        val rumMonitor = GlobalRumMonitorTestConfiguration()
        val mainLooper = MainLooperTestConfiguration()

        @TestConfigurationsProvider
        @JvmStatic
        fun getTestConfigurations(): List<TestConfiguration> {
            return listOf(appContext, rumMonitor, mainLooper)
        }
    }
}
