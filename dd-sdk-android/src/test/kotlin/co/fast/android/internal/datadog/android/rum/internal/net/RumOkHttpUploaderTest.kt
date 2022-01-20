/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.rum.internal.net

import android.app.Application
import co.fast.android.internal.datadog.android.BuildConfig
import co.fast.android.internal.datadog.android.core.internal.CoreFeature
import co.fast.android.internal.datadog.android.core.internal.net.DataOkHttpUploader
import co.fast.android.internal.datadog.android.core.internal.net.DataOkHttpUploaderTest
import co.fast.android.internal.datadog.android.rum.RumAttributes
import co.fast.android.internal.datadog.android.utils.config.ApplicationContextTestConfiguration
import co.fast.android.internal.datadog.android.utils.config.CoreFeatureTestConfiguration
import co.fast.android.internal.datadog.android.utils.forge.Configurator
import com.datadog.tools.unit.annotations.TestConfigurationsProvider
import com.datadog.tools.unit.extensions.TestConfigurationExtension
import com.datadog.tools.unit.extensions.config.TestConfiguration
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import okhttp3.Call
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
internal class RumOkHttpUploaderTest : DataOkHttpUploaderTest<RumOkHttpUploader>() {

    override fun uploader(callFactory: Call.Factory): RumOkHttpUploader {
        return RumOkHttpUploader(
            fakeEndpoint,
            fakeToken,
            callFactory
        )
    }

    override fun expectedPath(): String {
        return "/v1/input/$fakeToken"
    }

    override fun expectedQueryParams(): Map<String, String> {
        val tags = mutableListOf(
            "${RumAttributes.SERVICE_NAME}:${coreFeature.fakeServiceName}",
            "${RumAttributes.APPLICATION_VERSION}:${appContext.fakeVersionName}",
            "${RumAttributes.SDK_VERSION}:${BuildConfig.SDK_VERSION_NAME}",
            "${RumAttributes.ENV}:${coreFeature.fakeEnvName}"
        )

        if (appContext.fakeVariant.isNotBlank()) {
            tags.add("${RumAttributes.VARIANT}:${appContext.fakeVariant}")
        }

        return mapOf(
            DataOkHttpUploader.QP_SOURCE to CoreFeature.sourceName,
            RumOkHttpUploader.QP_TAGS to tags.joinToString(",")
        )
    }

    companion object {
        val appContext = ApplicationContextTestConfiguration(Application::class.java)
        val coreFeature = CoreFeatureTestConfiguration(appContext)

        @TestConfigurationsProvider
        @JvmStatic
        fun getTestConfigurations(): List<TestConfiguration> {
            return listOf(appContext, coreFeature)
        }
    }
}
