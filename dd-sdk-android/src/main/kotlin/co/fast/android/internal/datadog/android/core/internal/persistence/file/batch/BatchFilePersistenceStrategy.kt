/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.core.internal.persistence.file.batch

import co.fast.android.internal.datadog.android.core.internal.data.upload.DataFlusher
import co.fast.android.internal.datadog.android.core.internal.data.upload.Flusher
import co.fast.android.internal.datadog.android.core.internal.persistence.DataReader
import co.fast.android.internal.datadog.android.core.internal.persistence.DataWriter
import co.fast.android.internal.datadog.android.core.internal.persistence.PayloadDecoration
import co.fast.android.internal.datadog.android.core.internal.persistence.PersistenceStrategy
import co.fast.android.internal.datadog.android.core.internal.persistence.Serializer
import co.fast.android.internal.datadog.android.core.internal.persistence.file.FileOrchestrator
import co.fast.android.internal.datadog.android.core.internal.persistence.file.advanced.ScheduledWriter
import co.fast.android.internal.datadog.android.log.Logger
import java.util.concurrent.ExecutorService

internal open class BatchFilePersistenceStrategy<T : Any>(
    private val fileOrchestrator: FileOrchestrator,
    private val executorService: ExecutorService,
    serializer: Serializer<T>,
    private val payloadDecoration: PayloadDecoration,
    internalLogger: Logger
) : PersistenceStrategy<T> {

    internal val fileHandler = BatchFileHandler(internalLogger)

    private val fileWriter: DataWriter<T> by lazy {
        createWriter(
            fileOrchestrator,
            executorService,
            serializer,
            payloadDecoration,
            internalLogger
        )
    }

    private val fileReader = BatchFileDataReader(
        fileOrchestrator,
        payloadDecoration,
        fileHandler,
        internalLogger
    )

    // region PersistenceStrategy

    override fun getWriter(): DataWriter<T> {
        return fileWriter
    }

    override fun getReader(): DataReader {
        return fileReader
    }

    override fun getFlusher(): Flusher {
        return DataFlusher(fileOrchestrator, payloadDecoration, fileHandler)
    }

    // endregion

    // region Open

    internal open fun createWriter(
        fileOrchestrator: FileOrchestrator,
        executorService: ExecutorService,
        serializer: Serializer<T>,
        payloadDecoration: PayloadDecoration,
        internalLogger: Logger
    ): DataWriter<T> {
        return ScheduledWriter(
            BatchFileDataWriter(
                fileOrchestrator,
                serializer,
                payloadDecoration,
                fileHandler,
                internalLogger
            ),
            executorService,
            internalLogger
        )
    }

    //
}
