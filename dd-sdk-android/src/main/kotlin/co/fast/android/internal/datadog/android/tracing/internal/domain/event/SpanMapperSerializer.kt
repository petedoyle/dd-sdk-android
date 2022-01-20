/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.tracing.internal.domain.event

import co.fast.android.internal.datadog.android.core.internal.Mapper
import co.fast.android.internal.datadog.android.core.internal.persistence.Serializer
import co.fast.android.internal.datadog.android.event.EventMapper
import co.fast.android.internal.datadog.android.tracing.model.SpanEvent
import co.fast.android.internal.datadog.opentracing.DDSpan

internal class SpanMapperSerializer(
    private val legacyMapper: Mapper<DDSpan, SpanEvent>,
    private val spanEventMapper: EventMapper<SpanEvent>,
    private val spanSerializer: Serializer<SpanEvent>
) : Serializer<DDSpan> {

    override fun serialize(model: DDSpan): String? {
        val spanEvent = legacyMapper.map(model)
        val mappedEvent = spanEventMapper.map(spanEvent) ?: return null
        return spanSerializer.serialize(mappedEvent)
    }
}
