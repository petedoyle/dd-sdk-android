/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.log.internal.domain.event

import co.fast.android.internal.datadog.android.core.internal.constraints.DataConstraints
import co.fast.android.internal.datadog.android.core.internal.constraints.DatadogDataConstraints
import co.fast.android.internal.datadog.android.core.internal.persistence.Serializer
import co.fast.android.internal.datadog.android.log.LogAttributes
import co.fast.android.internal.datadog.android.log.model.LogEvent

internal class LogEventSerializer(
    private val dataConstraints: DataConstraints = DatadogDataConstraints()
) :
    Serializer<LogEvent> {

    override fun serialize(model: LogEvent): String {
        return sanitizeTagsAndAttributes(model).toJson().toString()
    }

    private fun sanitizeTagsAndAttributes(log: LogEvent): LogEvent {
        val sanitizedTags = dataConstraints
            .validateTags(log.ddtags.split(","))
            .joinToString(",")
        val sanitizedAttributes = dataConstraints
            .validateAttributes(log.additionalProperties)
            .filterKeys { it.isNotBlank() }
        val usr = log.usr?.let {
            val sanitizedUserAttributes = dataConstraints.validateAttributes(
                it.additionalProperties,
                keyPrefix = LogAttributes.USR_ATTRIBUTES_GROUP,
                attributesGroupName = USER_EXTRA_GROUP_VERBOSE_NAME
            )
            it.copy(additionalProperties = sanitizedUserAttributes)
        }
        return log.copy(
            ddtags = sanitizedTags,
            additionalProperties = sanitizedAttributes,
            usr = usr
        )
    }

    companion object {
        internal const val USER_EXTRA_GROUP_VERBOSE_NAME = "user extra information"
    }
}
