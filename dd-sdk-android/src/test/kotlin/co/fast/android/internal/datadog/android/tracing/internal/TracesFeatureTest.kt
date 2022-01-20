/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.tracing.internal

import co.fast.android.internal.datadog.android.core.configuration.Configuration
import co.fast.android.internal.datadog.android.core.internal.CoreFeature
import co.fast.android.internal.datadog.android.core.internal.SdkFeatureTest
import co.fast.android.internal.datadog.android.core.internal.persistence.file.advanced.ScheduledWriter
import co.fast.android.internal.datadog.android.core.internal.persistence.file.batch.BatchFileDataWriter
import co.fast.android.internal.datadog.android.event.SpanEventMapper
import co.fast.android.internal.datadog.android.tracing.internal.domain.TracesFilePersistenceStrategy
import co.fast.android.internal.datadog.android.tracing.internal.domain.event.SpanEventMapperWrapper
import co.fast.android.internal.datadog.android.tracing.internal.domain.event.SpanMapperSerializer
import co.fast.android.internal.datadog.android.tracing.internal.net.TracesOkHttpUploaderV2
import co.fast.android.internal.datadog.android.utils.forge.Configurator
import co.fast.android.internal.datadog.opentracing.DDSpan
import com.datadog.tools.unit.extensions.ApiLevelExtension
import com.datadog.tools.unit.extensions.TestConfigurationExtension
import com.datadog.tools.unit.getFieldValue
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
internal class TracesFeatureTest :
    SdkFeatureTest<DDSpan, Configuration.Feature.Tracing, TracesFeature>() {

    override fun createTestedFeature(): TracesFeature {
        return TracesFeature
    }

    override fun forgeConfiguration(forge: Forge): Configuration.Feature.Tracing {
        return forge.getForgery()
    }

    @Test
    fun `𝕄 initialize persistence strategy 𝕎 initialize()`() {
        // When
        testedFeature.initialize(appContext.mockInstance, fakeConfigurationFeature)

        // Then
        assertThat(testedFeature.persistenceStrategy)
            .isInstanceOf(TracesFilePersistenceStrategy::class.java)
    }

    @Test
    fun `𝕄 use the eventMapper 𝕎 initialize()`() {
        // When
        testedFeature.initialize(appContext.mockInstance, fakeConfigurationFeature)

        // Then
        val batchFileDataWriter =
            (testedFeature.persistenceStrategy.getWriter() as? ScheduledWriter)
                ?.delegateWriter as? BatchFileDataWriter
        val spanMapperSerializer = batchFileDataWriter?.serializer as? SpanMapperSerializer
        val spanEventMapperWrapper =
            spanMapperSerializer?.getFieldValue<SpanEventMapperWrapper, SpanMapperSerializer>(
                "spanEventMapper"
            )
        val spanEventMapper =
            spanEventMapperWrapper?.getFieldValue<SpanEventMapper, SpanEventMapperWrapper>(
                "wrappedEventMapper"
            )
        assertThat(
            spanEventMapper
        ).isSameAs(
            fakeConfigurationFeature.spanEventMapper
        )
    }

    @Test
    fun `𝕄 create a tracing uploader 𝕎 createUploader()`() {
        // When
        val uploader = testedFeature.createUploader(fakeConfigurationFeature)

        // Then
        assertThat(uploader).isInstanceOf(TracesOkHttpUploaderV2::class.java)
        val tracesUploader = uploader as TracesOkHttpUploaderV2
        assertThat(tracesUploader.intakeUrl).startsWith(fakeConfigurationFeature.endpointUrl)
        assertThat(tracesUploader.intakeUrl).endsWith("/api/v2/spans")
        assertThat(tracesUploader.callFactory).isSameAs(CoreFeature.okHttpClient)
    }
}
