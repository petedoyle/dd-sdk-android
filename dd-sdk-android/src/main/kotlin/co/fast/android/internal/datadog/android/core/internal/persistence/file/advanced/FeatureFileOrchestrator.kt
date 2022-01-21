/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.core.internal.persistence.file.advanced

import android.content.Context
import co.fast.android.internal.datadog.android.core.internal.persistence.file.FileOrchestrator
import co.fast.android.internal.datadog.android.core.internal.persistence.file.FilePersistenceConfig
import co.fast.android.internal.datadog.android.core.internal.persistence.file.batch.BatchFileHandler
import co.fast.android.internal.datadog.android.core.internal.persistence.file.batch.BatchFileOrchestrator
import co.fast.android.internal.datadog.android.core.internal.privacy.ConsentProvider
import co.fast.android.internal.datadog.android.log.Logger
import java.io.File
import java.util.Locale
import java.util.concurrent.ExecutorService

internal class FeatureFileOrchestrator(
    consentProvider: ConsentProvider,
    pendingOrchestrator: FileOrchestrator,
    grantedOrchestrator: FileOrchestrator,
    dataMigrator: DataMigrator
) : ConsentAwareFileOrchestrator(
    consentProvider,
    pendingOrchestrator,
    grantedOrchestrator,
    dataMigrator
) {

    constructor(
        consentProvider: ConsentProvider,
        context: Context,
        featureName: String,
        executorService: ExecutorService,
        internalLogger: Logger
    ) : this(
        consentProvider,
        BatchFileOrchestrator(
            File(context.filesDir, PENDING_DIR.format(Locale.US, featureName)),
            PERSISTENCE_CONFIG,
            internalLogger
        ),
        BatchFileOrchestrator(
            File(context.filesDir, GRANTED_DIR.format(Locale.US, featureName)),
            PERSISTENCE_CONFIG,
            internalLogger
        ),
        ConsentAwareFileMigrator(
            BatchFileHandler(internalLogger),
            executorService,
            internalLogger
        )
    )

    companion object {
        internal const val VERSION = 1
        private const val PENDING_DIR = "fast-dd-%s-pending-v$VERSION"
        private const val GRANTED_DIR = "fast-dd-%s-v$VERSION"

        private val PERSISTENCE_CONFIG = FilePersistenceConfig()
    }
}
