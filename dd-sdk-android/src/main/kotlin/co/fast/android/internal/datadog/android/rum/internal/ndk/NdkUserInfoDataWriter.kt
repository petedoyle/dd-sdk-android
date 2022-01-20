/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.rum.internal.ndk

import android.content.Context
import co.fast.android.internal.datadog.android.core.internal.persistence.file.FileHandler
import co.fast.android.internal.datadog.android.core.internal.persistence.file.advanced.ConsentAwareFileMigrator
import co.fast.android.internal.datadog.android.core.internal.persistence.file.advanced.ConsentAwareFileOrchestrator
import co.fast.android.internal.datadog.android.core.internal.persistence.file.single.SingleFileOrchestrator
import co.fast.android.internal.datadog.android.core.internal.persistence.file.single.SingleItemDataWriter
import co.fast.android.internal.datadog.android.core.internal.privacy.ConsentProvider
import co.fast.android.internal.datadog.android.core.model.UserInfo
import co.fast.android.internal.datadog.android.log.Logger
import co.fast.android.internal.datadog.android.log.internal.user.UserInfoSerializer
import java.util.concurrent.ExecutorService

internal class NdkUserInfoDataWriter(
    context: Context,
    consentProvider: ConsentProvider,
    executorService: ExecutorService,
    fileHandler: FileHandler,
    internalLogger: Logger
) : SingleItemDataWriter<UserInfo>(
    ConsentAwareFileOrchestrator(
        consentProvider = consentProvider,
        pendingOrchestrator = SingleFileOrchestrator(
            DatadogNdkCrashHandler.getPendingUserInfoFile(context)
        ),
        grantedOrchestrator = SingleFileOrchestrator(
            DatadogNdkCrashHandler.getGrantedUserInfoFile(context)
        ),
        dataMigrator = ConsentAwareFileMigrator(
            fileHandler,
            executorService,
            internalLogger
        )
    ),
    UserInfoSerializer(),
    fileHandler,
    internalLogger
)
