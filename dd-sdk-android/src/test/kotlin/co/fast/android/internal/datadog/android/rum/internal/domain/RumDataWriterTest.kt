/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.rum.internal.domain

import co.fast.android.internal.datadog.android.core.internal.persistence.PayloadDecoration
import co.fast.android.internal.datadog.android.core.internal.persistence.Serializer
import co.fast.android.internal.datadog.android.core.internal.persistence.file.FileHandler
import co.fast.android.internal.datadog.android.core.internal.persistence.file.FileOrchestrator
import co.fast.android.internal.datadog.android.log.Logger
import co.fast.android.internal.datadog.android.log.internal.logger.LogHandler
import co.fast.android.internal.datadog.android.rum.internal.monitor.EventType
import co.fast.android.internal.datadog.android.rum.model.ActionEvent
import co.fast.android.internal.datadog.android.rum.model.ErrorEvent
import co.fast.android.internal.datadog.android.rum.model.LongTaskEvent
import co.fast.android.internal.datadog.android.rum.model.ResourceEvent
import co.fast.android.internal.datadog.android.rum.model.ViewEvent
import co.fast.android.internal.datadog.android.utils.config.GlobalRumMonitorTestConfiguration
import co.fast.android.internal.datadog.android.utils.forge.Configurator
import co.fast.android.internal.datadog.tools.unit.annotations.TestConfigurationsProvider
import co.fast.android.internal.datadog.tools.unit.extensions.TestConfigurationExtension
import co.fast.android.internal.datadog.tools.unit.extensions.config.TestConfiguration
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyZeroInteractions
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import java.io.File
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
internal class RumDataWriterTest {

    lateinit var testedWriter: RumDataWriter

    @Mock
    lateinit var mockSerializer: Serializer<Any>

    @Mock
    lateinit var mockOrchestrator: FileOrchestrator

    @Mock
    lateinit var mockFileHandler: FileHandler

    @Mock
    lateinit var mockLogHandler: LogHandler

    @Forgery
    lateinit var fakeDecoration: PayloadDecoration

    @StringForgery
    lateinit var fakeSerializedEvent: String
    lateinit var fakeSerializedData: ByteArray

    @Forgery
    lateinit var fakeLastViewEventFile: File

    @BeforeEach
    fun `set up`() {
        fakeSerializedData = fakeSerializedEvent.toByteArray(Charsets.UTF_8)

        testedWriter = RumDataWriter(
            mockOrchestrator,
            mockSerializer,
            fakeDecoration,
            mockFileHandler,
            Logger(mockLogHandler),
            fakeLastViewEventFile
        )
    }

    @Test
    fun `𝕄 do not notify the RumMonitor 𝕎 onDataWritten() { ViewEvent }`(
        @Forgery viewEvent: ViewEvent
    ) {

        // When
        testedWriter.onDataWritten(viewEvent, fakeSerializedData)

        // Then
        verifyZeroInteractions(rumMonitor.mockInstance)
    }

    @Test
    fun `𝕄 persist the event into the NDK crash folder 𝕎 onDataWritten() { ViewEvent }`(
        @Forgery viewEvent: ViewEvent
    ) {
        // When
        testedWriter.onDataWritten(viewEvent, fakeSerializedData)

        // Then
        verify(mockFileHandler)
            .writeData(fakeLastViewEventFile, fakeSerializedData)
    }

    @Test
    fun `𝕄 do not notify the RumMonitor 𝕎 onDataWriteFailed() { ViewEvent }`(
        @Forgery viewEvent: ViewEvent
    ) {
        // When
        testedWriter.onDataWriteFailed(viewEvent)

        // Then
        verifyZeroInteractions(rumMonitor.mockInstance, mockFileHandler)
    }

    @Test
    fun `𝕄 notify the RumMonitor 𝕎 onDataWritten() { ActionEvent }`(
        @Forgery actionEvent: ActionEvent
    ) {
        // When
        testedWriter.onDataWritten(actionEvent, fakeSerializedData)

        // Then
        verify(rumMonitor.mockInstance).eventSent(actionEvent.view.id, EventType.ACTION)
        verifyZeroInteractions(mockFileHandler)
    }

    @Test
    fun `𝕄 do not notify the RumMonitor 𝕎 onDataWriteFailed() { ActionEvent }`(
        @Forgery actionEvent: ActionEvent
    ) {
        // When
        testedWriter.onDataWriteFailed(actionEvent)

        // Then
        verifyZeroInteractions(rumMonitor.mockInstance, mockFileHandler)
    }

    @Test
    fun `𝕄 notify the RumMonitor 𝕎 onDataWritten() { ResourceEvent }`(
        @Forgery resourceEvent: ResourceEvent
    ) {
        // When
        testedWriter.onDataWritten(resourceEvent, fakeSerializedData)

        // Then
        verify(rumMonitor.mockInstance).eventSent(resourceEvent.view.id, EventType.RESOURCE)
        verifyZeroInteractions(mockFileHandler)
    }

    @Test
    fun `𝕄 do not notify the RumMonitor 𝕎 onDataWriteFailed() { ResourceEvent }`(
        @Forgery resourceEvent: ResourceEvent
    ) {
        // When
        testedWriter.onDataWriteFailed(resourceEvent)

        // Then
        verifyZeroInteractions(rumMonitor.mockInstance, mockFileHandler)
    }

    @Test
    fun `𝕄 notify the RumMonitor 𝕎 onDataWritten() { ErrorEvent isCrash=false }`(
        @Forgery fakeEvent: ErrorEvent
    ) {
        // Given
        val errorEvent = fakeEvent.copy(error = fakeEvent.error.copy(isCrash = false))

        // When
        testedWriter.onDataWritten(errorEvent, fakeSerializedData)

        // Then
        verify(rumMonitor.mockInstance).eventSent(fakeEvent.view.id, EventType.ERROR)
        verifyZeroInteractions(mockFileHandler)
    }

    @Test
    fun `𝕄 not notify the RumMonitor 𝕎 onDataWritten() { ErrorEvent isCrash=true }`(
        @Forgery fakeEvent: ErrorEvent
    ) {
        // Given
        val errorEvent = fakeEvent.copy(error = fakeEvent.error.copy(isCrash = true))

        // When
        testedWriter.onDataWritten(errorEvent, fakeSerializedData)

        // Then
        verify(rumMonitor.mockInstance, never()).eventSent(eq(fakeEvent.view.id), any())
        verifyZeroInteractions(mockFileHandler)
    }

    @Test
    fun `𝕄 do not notify the RumMonitor 𝕎 onDataWriteFailed() { ErrorEvent }`(
        @Forgery fakeEvent: ErrorEvent
    ) {
        // When
        testedWriter.onDataWriteFailed(fakeEvent)

        // Then
        verifyZeroInteractions(rumMonitor.mockInstance, mockFileHandler)
    }

    @Test
    fun `𝕄 notify the RumMonitor 𝕎 onDataWritten() { LongTaskEvent }`(
        @Forgery fakeEvent: LongTaskEvent
    ) {
        // Given
        val longTaskEvent = fakeEvent.copy(
            longTask = LongTaskEvent.LongTask(
                id = fakeEvent.longTask.id,
                duration = fakeEvent.longTask.duration,
                isFrozenFrame = false
            )
        )

        // When
        testedWriter.onDataWritten(longTaskEvent, fakeSerializedData)

        // Then
        verify(rumMonitor.mockInstance).eventSent(longTaskEvent.view.id, EventType.LONG_TASK)
        verifyZeroInteractions(mockFileHandler)
    }

    @Test
    fun `𝕄 notify the RumMonitor 𝕎 onDataWritten() { FrozenFrame Event }`(
        @Forgery fakeEvent: LongTaskEvent
    ) {
        // Given
        val frozenFrameEvent = fakeEvent.copy(
            longTask = LongTaskEvent.LongTask(
                id = fakeEvent.longTask.id,
                duration = fakeEvent.longTask.duration,
                isFrozenFrame = true
            )
        )

        // When
        testedWriter.onDataWritten(frozenFrameEvent, fakeSerializedData)

        // Then
        verify(rumMonitor.mockInstance).eventSent(frozenFrameEvent.view.id, EventType.FROZEN_FRAME)
        verifyZeroInteractions(mockFileHandler)
    }

    @Test
    fun `𝕄 do not notify the RumMonitor 𝕎 onDataWriteFailed() { LongTaskEvent }`(
        @Forgery fakeEvent: LongTaskEvent
    ) {
        // When
        testedWriter.onDataWriteFailed(fakeEvent)

        // Then
        verifyZeroInteractions(rumMonitor.mockInstance, mockFileHandler)
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