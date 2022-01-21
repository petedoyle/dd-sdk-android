/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.utils.forge

import co.fast.android.internal.datadog.android.rum.model.ActionEvent
import co.fast.android.internal.datadog.android.rum.model.ErrorEvent
import co.fast.android.internal.datadog.android.rum.model.ResourceEvent
import co.fast.android.internal.datadog.android.rum.model.ViewEvent
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.jvm.ext.aTimestamp
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import org.junit.jupiter.api.Assumptions.assumeTrue

/**
 * Will generate a map with different value types with a possibility to filter out given keys,
 * see [aFilteredMap] for details on filtering.
 */
internal fun Forge.exhaustiveAttributes(
    excludedKeys: Set<String> = emptySet(),
    filterThreshold: Float = 0.5f
): Map<String, Any?> {
    val map = generateMapWithExhaustiveValues(this).toMutableMap()

    map[""] = anHexadecimalString()
    map[aWhitespaceString()] = anHexadecimalString()
    map[anAlphabeticalString()] = generateMapWithExhaustiveValues(this).toMutableMap().apply {
        this[anAlphabeticalString()] = generateMapWithExhaustiveValues(this@exhaustiveAttributes)
    }

    val filtered = map.filterKeys { it !in excludedKeys }

    assumeDifferenceIsNoMore(filtered.size, map.size, filterThreshold)

    return filtered
}

/**
 * Creates a map just like [Forge#aMap], but it won't include given keys.
 * @param excludedKeys Keys to exclude from generated map.
 * @param filterThreshold Max ratio of keys removed from originally generated map. If ratio
 * is more than that, [Assume] mechanism will be used.
 */
internal fun <K, V> Forge.aFilteredMap(
    size: Int = -1,
    excludedKeys: Set<K>,
    filterThreshold: Float = 0.5f,
    forging: Forge.() -> Pair<K, V>
): Map<K, V> {

    val base = aMap(size, forging)

    val filtered = base.filterKeys { it !in excludedKeys }

    if (base.isNotEmpty()) {
        assumeDifferenceIsNoMore(filtered.size, base.size, filterThreshold)
    }

    return filtered
}

internal fun Forge.aFormattedTimestamp(format: String = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"): String {
    val simpleDateFormat = SimpleDateFormat(format, Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    return simpleDateFormat.format(this.aTimestamp())
}

internal fun Forge.aRumEvent(): Any {
    return this.anElementFrom(
        this.getForgery<ViewEvent>(),
        this.getForgery<ActionEvent>(),
        this.getForgery<ResourceEvent>(),
        this.getForgery<ErrorEvent>()
    )
}

private fun assumeDifferenceIsNoMore(result: Int, base: Int, maxDifference: Float) {

    check(result <= base) {
        "Number of elements after filtering cannot exceed the number of original elements."
    }

    val diff = (base - result).toFloat() / base
    assumeTrue(
        diff <= maxDifference,
        "Too many elements removed, condition cannot be satisfied."
    )
}

private fun generateMapWithExhaustiveValues(forge: Forge): Map<String, Any?> {
    return forge.run {
        listOf(
            aBool(),
            anInt(),
            aLong(),
            // TODO RUMM-1531 put it back once proper JSON assertions are ready
            // aFloat(),
            aDouble(),
            anAsciiString(),
            getForgery<Date>(),
            getForgery<Locale>(),
            getForgery<TimeZone>(),
            getForgery<File>(),
            getForgery<JsonObject>(),
            getForgery<JsonArray>(),
            aList { anAlphabeticalString() },
            aList { aDouble() },
            null
        )
            .map { anAlphaNumericalString() to it }
            .toMap()
    }
}