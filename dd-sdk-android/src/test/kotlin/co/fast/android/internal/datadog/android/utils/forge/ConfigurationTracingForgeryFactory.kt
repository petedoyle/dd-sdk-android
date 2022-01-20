/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.utils.forge

import co.fast.android.internal.datadog.android.core.configuration.Configuration
import co.fast.android.internal.datadog.android.plugin.DatadogPlugin
import com.nhaarman.mockitokotlin2.mock
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.ForgeryFactory

internal class ConfigurationTracingForgeryFactory :
    ForgeryFactory<Configuration.Feature.Tracing> {
    override fun getForgery(forge: Forge): Configuration.Feature.Tracing {
        return Configuration.Feature.Tracing(
            endpointUrl = forge.aStringMatching("http(s?)://[a-z]+\\.com/\\w+"),
            plugins = forge.aList { mock<DatadogPlugin>() },
            spanEventMapper = mock()
        )
    }
}
