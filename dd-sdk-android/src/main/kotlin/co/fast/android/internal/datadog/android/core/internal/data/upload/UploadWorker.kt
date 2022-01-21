/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.core.internal.data.upload

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import co.fast.android.internal.datadog.android.Datadog
import co.fast.android.internal.datadog.android.core.internal.net.DataUploader
import co.fast.android.internal.datadog.android.core.internal.net.UploadStatus
import co.fast.android.internal.datadog.android.core.internal.persistence.Batch
import co.fast.android.internal.datadog.android.core.internal.persistence.DataReader
import co.fast.android.internal.datadog.android.core.internal.utils.devLogger
import co.fast.android.internal.datadog.android.core.internal.utils.sdkLogger
import co.fast.android.internal.datadog.android.error.internal.CrashReportsFeature
import co.fast.android.internal.datadog.android.log.internal.LogsFeature
import co.fast.android.internal.datadog.android.rum.internal.RumFeature
import co.fast.android.internal.datadog.android.tracing.internal.TracesFeature

internal class UploadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : Worker(appContext, workerParams) {

    // region Worker

    override fun doWork(): Result {
        if (!Datadog.isInitialized()) {
            devLogger.e(Datadog.MESSAGE_NOT_INITIALIZED)
            return Result.success()
        }

        // Upload Crash reports
        uploadAllBatches(
            CrashReportsFeature.persistenceStrategy.getReader(),
            CrashReportsFeature.uploader
        )

        // Upload Logs
        uploadAllBatches(
            LogsFeature.persistenceStrategy.getReader(),
            LogsFeature.uploader
        )

        // Upload Traces
        uploadAllBatches(
            TracesFeature.persistenceStrategy.getReader(),
            TracesFeature.uploader
        )

        // Upload RUM
        uploadAllBatches(
            RumFeature.persistenceStrategy.getReader(),
            RumFeature.uploader
        )

        return Result.success()
    }

    private fun uploadAllBatches(
        reader: DataReader,
        uploader: DataUploader
    ) {
        val failedBatches = mutableListOf<Batch>()
        var batch: Batch?
        do {
            batch = reader.lockAndReadNext()
            if (batch != null) {
                if (consumeBatch(batch, uploader)) {
                    reader.drop(batch)
                } else {
                    failedBatches.add(batch)
                }
            }
        } while (batch != null)

        failedBatches.forEach {
            reader.release(it)
        }
    }

    // endregion

    // region Internal

    private fun consumeBatch(
        batch: Batch,
        uploader: DataUploader
    ): Boolean {
        val status = uploader.upload(batch.data)
        status.logStatus(uploader.javaClass.simpleName, batch.data.size, devLogger, false)
        status.logStatus(uploader.javaClass.simpleName, batch.data.size, sdkLogger, true)
        return status == UploadStatus.SUCCESS
    }

    // endregion

    companion object {
        private const val TAG = "UploadWorker"
    }
}
