/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.core.internal.persistence.file.advanced

import co.fast.android.internal.datadog.android.core.internal.persistence.file.FileOrchestrator
import co.fast.android.internal.datadog.android.privacy.TrackingConsent

internal interface DataMigrator {

    fun migrateData(
        previousConsent: TrackingConsent?,
        previousFileOrchestrator: FileOrchestrator,
        newConsent: TrackingConsent,
        newFileOrchestrator: FileOrchestrator
    )
}