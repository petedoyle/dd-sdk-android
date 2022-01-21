/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.tracing.internal.data

import co.fast.android.internal.datadog.android.core.internal.persistence.DataWriter
import co.fast.android.internal.datadog.opentracing.DDSpan
import co.fast.android.internal.datadog.trace.common.writer.Writer

internal class TraceWriter(
    val writer: DataWriter<DDSpan>
) : Writer {

    // region Writer
    override fun start() {
        // NO - OP
    }

    override fun write(trace: MutableList<DDSpan>?) {
        trace?.let {
            writer.write(it)
        }
    }

    override fun close() {
        // NO - OP
    }

    override fun incrementTraceCount() {
        // NO - OP
    }

    // endregion
}