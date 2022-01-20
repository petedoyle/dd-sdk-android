/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.rum.internal.domain

import android.content.Context
import co.fast.android.internal.datadog.android.core.internal.persistence.DataWriter
import co.fast.android.internal.datadog.android.core.internal.persistence.PayloadDecoration
import co.fast.android.internal.datadog.android.core.internal.persistence.Serializer
import co.fast.android.internal.datadog.android.core.internal.persistence.file.FileOrchestrator
import co.fast.android.internal.datadog.android.core.internal.persistence.file.advanced.FeatureFileOrchestrator
import co.fast.android.internal.datadog.android.core.internal.persistence.file.advanced.ScheduledWriter
import co.fast.android.internal.datadog.android.core.internal.persistence.file.batch.BatchFilePersistenceStrategy
import co.fast.android.internal.datadog.android.core.internal.privacy.ConsentProvider
import co.fast.android.internal.datadog.android.event.EventMapper
import co.fast.android.internal.datadog.android.event.MapperSerializer
import co.fast.android.internal.datadog.android.log.Logger
import co.fast.android.internal.datadog.android.rum.internal.domain.event.RumEventSerializer
import java.io.File
import java.util.concurrent.ExecutorService

internal class RumFilePersistenceStrategy(
    consentProvider: ConsentProvider,
    context: Context,
    eventMapper: EventMapper<Any>,
    executorService: ExecutorService,
    internalLogger: Logger,
    private val lastViewEventFile: File
) : BatchFilePersistenceStrategy<Any>(
    FeatureFileOrchestrator(
        consentProvider,
        context,
        "rum",
        executorService,
        internalLogger
    ),
    executorService,
    MapperSerializer(
        eventMapper,
        RumEventSerializer()
    ),
    PayloadDecoration.NEW_LINE_DECORATION,
    internalLogger
) {

    override fun createWriter(
        fileOrchestrator: FileOrchestrator,
        executorService: ExecutorService,
        serializer: Serializer<Any>,
        payloadDecoration: PayloadDecoration,
        internalLogger: Logger
    ): DataWriter<Any> {
        return ScheduledWriter(
            RumDataWriter(
                fileOrchestrator,
                serializer,
                payloadDecoration,
                fileHandler,
                internalLogger,
                lastViewEventFile
            ),
            executorService,
            internalLogger
        )
    }
}
