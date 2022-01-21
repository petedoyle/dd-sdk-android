/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.rum.internal.domain.event

import android.util.Log
import co.fast.android.internal.datadog.android.event.EventMapper
import co.fast.android.internal.datadog.android.log.internal.logger.LogHandler
import co.fast.android.internal.datadog.android.rum.internal.monitor.EventType
import co.fast.android.internal.datadog.android.rum.model.ActionEvent
import co.fast.android.internal.datadog.android.rum.model.ErrorEvent
import co.fast.android.internal.datadog.android.rum.model.LongTaskEvent
import co.fast.android.internal.datadog.android.rum.model.ResourceEvent
import co.fast.android.internal.datadog.android.rum.model.ViewEvent
import co.fast.android.internal.datadog.android.utils.config.GlobalRumMonitorTestConfiguration
import co.fast.android.internal.datadog.android.utils.forge.Configurator
import co.fast.android.internal.datadog.android.utils.forge.aRumEvent
import co.fast.android.internal.datadog.android.utils.mockDevLogHandler
import co.fast.android.internal.datadog.android.utils.mockSdkLogHandler
import co.fast.android.internal.datadog.android.utils.restoreSdkLogHandler
import co.fast.android.internal.datadog.tools.unit.annotations.TestConfigurationsProvider
import co.fast.android.internal.datadog.tools.unit.extensions.TestConfigurationExtension
import co.fast.android.internal.datadog.tools.unit.extensions.config.TestConfiguration
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import java.util.Locale
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
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
internal class RumEventMapperTest {
    lateinit var testedRumEventMapper: RumEventMapper

    @Mock
    lateinit var mockResourceEventMapper: EventMapper<ResourceEvent>

    @Mock
    lateinit var mockActionEventMapper: EventMapper<ActionEvent>

    @Mock
    lateinit var mockErrorEventMapper: EventMapper<ErrorEvent>

    @Mock
    lateinit var mockViewEventMapper: EventMapper<ViewEvent>

    @Mock
    lateinit var mockLongTaskEventMapper: EventMapper<LongTaskEvent>

    @Mock
    lateinit var mockSdkLogHandler: LogHandler

    lateinit var mockDevLogHandler: LogHandler

    lateinit var originalLogHandler: LogHandler

    @BeforeEach
    fun `set up`() {
        originalLogHandler = mockSdkLogHandler(mockSdkLogHandler)
        mockDevLogHandler = mockDevLogHandler()

        whenever(mockViewEventMapper.map(any())).thenAnswer { it.arguments[0] }

        testedRumEventMapper = RumEventMapper(
            actionEventMapper = mockActionEventMapper,
            viewEventMapper = mockViewEventMapper,
            resourceEventMapper = mockResourceEventMapper,
            errorEventMapper = mockErrorEventMapper,
            longTaskEventMapper = mockLongTaskEventMapper
        )
    }

    @AfterEach
    fun `tear down`() {
        restoreSdkLogHandler(originalLogHandler)
    }

    @Test
    fun `M map the bundled event W map { ViewEvent }`(forge: Forge) {
        // GIVEN
        val fakeRumEvent = forge.getForgery<ViewEvent>()
        whenever(mockViewEventMapper.map(fakeRumEvent)).thenReturn(fakeRumEvent)

        // WHEN
        val mappedRumEvent = testedRumEventMapper.map(fakeRumEvent)

        // THEN
        assertThat(mappedRumEvent).isNotNull()
        assertThat(mappedRumEvent).isEqualTo(fakeRumEvent)
    }

    @Test
    fun `M map the bundled event W map { ResourceEvent }`(forge: Forge) {
        // GIVEN
        val fakeRumEvent = forge.getForgery<ResourceEvent>()
        whenever(mockResourceEventMapper.map(fakeRumEvent)).thenReturn(fakeRumEvent)

        // WHEN
        val mappedRumEvent = testedRumEventMapper.map(fakeRumEvent)

        // THEN
        assertThat(mappedRumEvent).isNotNull()
        assertThat(mappedRumEvent).isEqualTo(fakeRumEvent)
    }

    @Test
    fun `M map the bundled event W map { ErrorEvent }`(forge: Forge) {
        // GIVEN
        val fakeRumEvent = forge.getForgery<ErrorEvent>()
        whenever(mockErrorEventMapper.map(fakeRumEvent)).thenReturn(fakeRumEvent)

        // WHEN
        val mappedRumEvent = testedRumEventMapper.map(fakeRumEvent)

        // THEN
        assertThat(mappedRumEvent).isNotNull()
        assertThat(mappedRumEvent).isEqualTo(fakeRumEvent)
    }

    @Test
    fun `M map the bundled event W map { ActionEvent }`(forge: Forge) {
        // GIVEN
        val fakeRumEvent = forge.getForgery<ActionEvent>()
        whenever(mockActionEventMapper.map(fakeRumEvent)).thenReturn(fakeRumEvent)

        // WHEN
        val mappedRumEvent = testedRumEventMapper.map(fakeRumEvent)

        // THEN
        assertThat(mappedRumEvent).isNotNull()
        assertThat(mappedRumEvent).isEqualTo(fakeRumEvent)
    }

    @Test
    fun `M map the bundled event W map { LongTaskEvent }`(forge: Forge) {
        // GIVEN
        val fakeRumEvent = forge.getForgery<LongTaskEvent>()
        whenever(mockLongTaskEventMapper.map(fakeRumEvent)).thenReturn(fakeRumEvent)

        // WHEN
        val mappedRumEvent = testedRumEventMapper.map(fakeRumEvent)

        // THEN
        assertThat(mappedRumEvent).isNotNull()
        assertThat(mappedRumEvent).isEqualTo(fakeRumEvent)
    }

    @Test
    fun `M return the original event W map { no internal mapper used }`(forge: Forge) {
        // GIVEN
        testedRumEventMapper = RumEventMapper()
        val fakeRumEvent = forge.aRumEvent()

        // WHEN
        val mappedRumEvent = testedRumEventMapper.map(fakeRumEvent)

        // THEN
        assertThat(mappedRumEvent).isNotNull()
        assertThat(mappedRumEvent).isEqualTo(fakeRumEvent)
    }

    @Test
    fun `M return the original event W map { bundled event unknown }`() {
        // GIVEN
        testedRumEventMapper = RumEventMapper()
        val fakeRumEvent = Any()

        // WHEN
        val mappedRumEvent = testedRumEventMapper.map(fakeRumEvent)

        // THEN
        verify(mockSdkLogHandler).handleLog(
            Log.WARN,
            RumEventMapper.NO_EVENT_MAPPER_ASSIGNED_WARNING_MESSAGE
                .format(Locale.US, fakeRumEvent.javaClass.simpleName)
        )
        assertThat(mappedRumEvent).isEqualTo(fakeRumEvent)
    }

    @Test
    fun `M use the original event W map returns null object { ViewEvent }`(forge: Forge) {
        // GIVEN
        val fakeRumEvent = forge.getForgery<ViewEvent>()
        whenever(mockViewEventMapper.map(fakeRumEvent))
            .thenReturn(null)

        // WHEN
        val mappedRumEvent = testedRumEventMapper.map(fakeRumEvent)

        // THEN
        assertThat(mappedRumEvent).isEqualTo(fakeRumEvent)
        verify(mockDevLogHandler).handleLog(
            Log.WARN,
            RumEventMapper.VIEW_EVENT_NULL_WARNING_MESSAGE.format(Locale.US, fakeRumEvent)
        )
    }

    @Test
    fun `M return null event W map returns null object { ResourceEvent }`(forge: Forge) {
        // GIVEN
        val fakeRumEvent = forge.getForgery<ResourceEvent>()
        whenever(mockResourceEventMapper.map(fakeRumEvent))
            .thenReturn(null)

        // WHEN
        val mappedRumEvent = testedRumEventMapper.map(fakeRumEvent)

        // THEN
        assertThat(mappedRumEvent).isNull()
        verify(mockDevLogHandler).handleLog(
            Log.WARN,
            RumEventMapper.EVENT_NULL_WARNING_MESSAGE.format(Locale.US, fakeRumEvent)

        )
    }

    @Test
    fun `M return null event W map returns null object { ErrorEvent }`(forge: Forge) {
        // GIVEN
        val fakeErrorEvent = forge.getForgery<ErrorEvent>()
        val fakeNoCrashEvent =
            fakeErrorEvent.copy(error = fakeErrorEvent.error.copy(isCrash = false))
        whenever(mockErrorEventMapper.map(fakeNoCrashEvent))
            .thenReturn(null)

        // WHEN
        val mappedRumEvent = testedRumEventMapper.map(fakeNoCrashEvent)

        // THEN
        assertThat(mappedRumEvent).isNull()
        verify(mockDevLogHandler).handleLog(
            Log.WARN,
            RumEventMapper.EVENT_NULL_WARNING_MESSAGE.format(Locale.US, fakeNoCrashEvent)

        )
    }

    @Test
    fun `M return event W map returns null object { fatal ErrorEvent }`(forge: Forge) {
        // GIVEN
        val fakeErrorEvent = forge.getForgery<ErrorEvent>()
        val fakeCrasEvent = fakeErrorEvent.copy(
            error = fakeErrorEvent.error.copy(isCrash = true)
        )
        whenever(mockErrorEventMapper.map(fakeCrasEvent))
            .thenReturn(null)

        // WHEN
        val mappedRumEvent = testedRumEventMapper.map(fakeCrasEvent)

        // THEN
        assertThat(mappedRumEvent)
            .isSameAs(fakeCrasEvent)
            .isEqualTo(fakeCrasEvent)
    }

    @Test
    fun `M return null event W map returns null object { ActionEvent }`(forge: Forge) {
        // GIVEN
        val fakeRumEvent = forge.getForgery<ActionEvent>()
        whenever(mockActionEventMapper.map(fakeRumEvent))
            .thenReturn(null)

        // WHEN
        val mappedRumEvent = testedRumEventMapper.map(fakeRumEvent)

        // THEN
        assertThat(mappedRumEvent).isNull()
        verify(mockDevLogHandler).handleLog(
            Log.WARN,
            RumEventMapper.EVENT_NULL_WARNING_MESSAGE.format(Locale.US, fakeRumEvent)

        )
    }

    @Test
    fun `M return null event W map returns null object { LongTaskEvent }`(forge: Forge) {
        // GIVEN
        val fakeRumEvent = forge.getForgery<LongTaskEvent>()
        whenever(mockLongTaskEventMapper.map(fakeRumEvent))
            .thenReturn(null)

        // WHEN
        val mappedRumEvent = testedRumEventMapper.map(fakeRumEvent)

        // THEN
        assertThat(mappedRumEvent).isNull()
        verify(mockDevLogHandler).handleLog(
            Log.WARN,
            RumEventMapper.EVENT_NULL_WARNING_MESSAGE.format(Locale.US, fakeRumEvent)

        )
    }

    @Test
    fun `M use the original event W map returns different object { ViewEvent }`(forge: Forge) {
        // GIVEN
        val fakeRumEvent = forge.getForgery<ViewEvent>()
        whenever(mockViewEventMapper.map(fakeRumEvent))
            .thenReturn(forge.getForgery())

        // WHEN
        val mappedRumEvent = testedRumEventMapper.map(fakeRumEvent)

        // THEN
        assertThat(mappedRumEvent).isEqualTo(fakeRumEvent)
        verify(mockDevLogHandler).handleLog(
            Log.WARN,
            RumEventMapper.VIEW_EVENT_NULL_WARNING_MESSAGE.format(Locale.US, fakeRumEvent)
        )
    }

    @Test
    fun `M return null event W map returns different object { ResourceEvent }`(forge: Forge) {
        // GIVEN
        val fakeRumEvent = forge.getForgery<ResourceEvent>()
        whenever(mockResourceEventMapper.map(fakeRumEvent))
            .thenReturn(forge.getForgery())

        // WHEN
        val mappedRumEvent = testedRumEventMapper.map(fakeRumEvent)

        // THEN
        assertThat(mappedRumEvent).isNull()
        verify(mockDevLogHandler).handleLog(
            Log.WARN,
            RumEventMapper.NOT_SAME_EVENT_INSTANCE_WARNING_MESSAGE.format(Locale.US, fakeRumEvent)

        )
    }

    @Test
    fun `M return null event W map returns different object { ErrorEvent }`(forge: Forge) {
        // GIVEN
        val fakeRumEvent = forge.getForgery<ErrorEvent>()
        whenever(mockErrorEventMapper.map(fakeRumEvent))
            .thenReturn(forge.getForgery())

        // WHEN
        val mappedRumEvent = testedRumEventMapper.map(fakeRumEvent)

        // THEN
        assertThat(mappedRumEvent).isNull()
        verify(mockDevLogHandler).handleLog(
            Log.WARN,
            RumEventMapper.NOT_SAME_EVENT_INSTANCE_WARNING_MESSAGE.format(Locale.US, fakeRumEvent)
        )
    }

    @Test
    fun `M return null event W map returns different object { ActionEvent }`(forge: Forge) {
        // GIVEN
        val fakeRumEvent = forge.getForgery<ActionEvent>()
        whenever(mockActionEventMapper.map(fakeRumEvent))
            .thenReturn(forge.getForgery())

        // WHEN
        val mappedRumEvent = testedRumEventMapper.map(fakeRumEvent)

        // THEN
        assertThat(mappedRumEvent).isNull()
        verify(mockDevLogHandler).handleLog(
            Log.WARN,
            RumEventMapper.NOT_SAME_EVENT_INSTANCE_WARNING_MESSAGE.format(Locale.US, fakeRumEvent)

        )
    }

    @Test
    fun `M return null event W map returns different object { LongTaskEvent }`(forge: Forge) {
        // GIVEN
        val fakeRumEvent = forge.getForgery<LongTaskEvent>()
        whenever(mockLongTaskEventMapper.map(fakeRumEvent))
            .thenReturn(forge.getForgery())

        // WHEN
        val mappedRumEvent = testedRumEventMapper.map(fakeRumEvent)

        // THEN
        assertThat(mappedRumEvent).isNull()
        verify(mockDevLogHandler).handleLog(
            Log.WARN,
            RumEventMapper.NOT_SAME_EVENT_INSTANCE_WARNING_MESSAGE.format(Locale.US, fakeRumEvent)

        )
    }

    @Test
    fun `𝕄 warn the RUM Monitor 𝕎 map() {action dropped}`(
        @Forgery fakeRumEvent: ActionEvent
    ) {
        // Given
        whenever(mockActionEventMapper.map(fakeRumEvent)) doReturn null

        // WHEN
        val mappedRumEvent = testedRumEventMapper.map(fakeRumEvent)

        // THEN
        assertThat(mappedRumEvent).isNull()
        verify(rumMonitor.mockInstance).eventDropped(fakeRumEvent.view.id, EventType.ACTION)
    }

    @Test
    fun `𝕄 warn the RUM Monitor 𝕎 map() {resource dropped}`(
        @Forgery fakeRumEvent: ResourceEvent
    ) {
        // Given
        whenever(mockResourceEventMapper.map(fakeRumEvent)) doReturn null

        // WHEN
        val mappedRumEvent = testedRumEventMapper.map(fakeRumEvent)

        // THEN
        assertThat(mappedRumEvent).isNull()
        verify(rumMonitor.mockInstance).eventDropped(fakeRumEvent.view.id, EventType.RESOURCE)
    }

    @Test
    fun `𝕄 warn the RUM Monitor 𝕎 map() {error dropped}`(
        @Forgery fakeRumEvent: ErrorEvent
    ) {
        // Given
        whenever(mockErrorEventMapper.map(fakeRumEvent)) doReturn null

        // WHEN
        val mappedRumEvent = testedRumEventMapper.map(fakeRumEvent)

        // THEN
        assertThat(mappedRumEvent).isNull()
        verify(rumMonitor.mockInstance).eventDropped(fakeRumEvent.view.id, EventType.ERROR)
    }

    @Test
    fun `𝕄 warn the RUM Monitor 𝕎 map() {longTask dropped}`(
        @Forgery longTaskEvent: LongTaskEvent
    ) {
        // Given
        val fakeRumEvent = longTaskEvent.copy(
            longTask = LongTaskEvent.LongTask(
                id = longTaskEvent.longTask.id,
                duration = longTaskEvent.longTask.duration,
                isFrozenFrame = false
            )
        )

        // WHEN
        val mappedRumEvent = testedRumEventMapper.map(fakeRumEvent)

        // THEN
        assertThat(mappedRumEvent).isNull()
        verify(rumMonitor.mockInstance).eventDropped(longTaskEvent.view.id, EventType.LONG_TASK)
    }

    @Test
    fun `𝕄 warn the RUM Monitor 𝕎 map() {frozenFrame dropped}`(
        @Forgery longTaskEvent: LongTaskEvent
    ) {
        // Given
        val fakeRumEvent = longTaskEvent.copy(
            longTask = LongTaskEvent.LongTask(
                id = longTaskEvent.longTask.id,
                duration = longTaskEvent.longTask.duration,
                isFrozenFrame = true
            )
        )

        // WHEN
        val mappedRumEvent = testedRumEventMapper.map(fakeRumEvent)

        // THEN
        assertThat(mappedRumEvent).isNull()
        verify(rumMonitor.mockInstance).eventDropped(longTaskEvent.view.id, EventType.FROZEN_FRAME)
    }

    companion object {
        val rumMonitor = GlobalRumMonitorTestConfiguration()

        @TestConfigurationsProvider
        @JvmStatic
        fun getTestConfigurations(): List<TestConfiguration> {
            return listOf(rumMonitor)
        }
    }
}