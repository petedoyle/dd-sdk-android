/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.rum.internal.instrumentation.gestures

import android.content.res.Resources
import co.fast.android.internal.datadog.android.core.internal.CoreFeature
import co.fast.android.internal.datadog.android.rum.tracking.InteractionPredicate

internal fun resolveTargetName(
    interactionPredicate: InteractionPredicate,
    target: Any,
): String {
    val customTargetName = interactionPredicate.getTargetName(target)
    return if (!customTargetName.isNullOrEmpty()) {
        customTargetName
    } else {
        ""
    }
}

internal fun resourceIdName(id: Int): String {
    @Suppress("SwallowedException")
    return try {
        CoreFeature.contextRef.get()?.resources?.getResourceEntryName(id)
            ?: idAsStringHexa(id)
    } catch (e: Resources.NotFoundException) {
        idAsStringHexa(id)
    }
}

private fun idAsStringHexa(id: Int) = "0x${id.toString(16)}"
