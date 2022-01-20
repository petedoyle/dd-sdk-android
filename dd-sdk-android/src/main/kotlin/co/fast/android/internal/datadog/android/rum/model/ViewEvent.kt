/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.rum.model

import co.fast.android.internal.datadog.android.core.`internal`.utils.toJsonElement
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import java.lang.IllegalStateException
import java.lang.NumberFormatException
import kotlin.Any
import kotlin.Array
import kotlin.Boolean
import kotlin.Long
import kotlin.Number
import kotlin.String
import kotlin.collections.ArrayList
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.jvm.JvmStatic
import kotlin.jvm.Throws

/**
 * Schema of common properties of RUM events
 * @param date Start of the event in ms from epoch
 * @param application Application properties
 * @param service The service name for this application
 * @param session Session properties
 * @param view View properties
 * View properties
 * @param usr User properties
 * @param connectivity Device connectivity properties
 * @param synthetics Synthetics properties
 * @param dd Internal properties
 * Internal properties
 * @param context User provided context
 */
public data class ViewEvent(
    public val date: Long,
    public val application: Application,
    public val service: String? = null,
    public val session: ViewEventSession,
    public val view: View,
    public val usr: Usr? = null,
    public val connectivity: Connectivity? = null,
    public val synthetics: Synthetics? = null,
    public val dd: Dd,
    public val context: Context? = null
) {
    /**
     * RUM event type
     */
    public val type: String = "view"

    public fun toJson(): JsonElement {
        val json = JsonObject()
        json.addProperty("date", date)
        json.add("application", application.toJson())
        service?.let { json.addProperty("service", it) }
        json.add("session", session.toJson())
        json.add("view", view.toJson())
        usr?.let { json.add("usr", it.toJson()) }
        connectivity?.let { json.add("connectivity", it.toJson()) }
        synthetics?.let { json.add("synthetics", it.toJson()) }
        json.add("_dd", dd.toJson())
        context?.let { json.add("context", it.toJson()) }
        json.addProperty("type", type)
        return json
    }

    public companion object {
        @JvmStatic
        @Throws(JsonParseException::class)
        public fun fromJson(serializedObject: String): ViewEvent {
            try {
                val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                val date = jsonObject.get("date").asLong
                val application = jsonObject.get("application").toString().let {
                    Application.fromJson(it)
                }
                val service = jsonObject.get("service")?.asString
                val session = jsonObject.get("session").toString().let {
                    ViewEventSession.fromJson(it)
                }
                val view = jsonObject.get("view").toString().let {
                    View.fromJson(it)
                }
                val usr = jsonObject.get("usr")?.toString()?.let {
                    Usr.fromJson(it)
                }
                val connectivity = jsonObject.get("connectivity")?.toString()?.let {
                    Connectivity.fromJson(it)
                }
                val synthetics = jsonObject.get("synthetics")?.toString()?.let {
                    Synthetics.fromJson(it)
                }
                val dd = jsonObject.get("_dd").toString().let {
                    Dd.fromJson(it)
                }
                val context = jsonObject.get("context")?.toString()?.let {
                    Context.fromJson(it)
                }
                return ViewEvent(date, application, service, session, view, usr, connectivity,
                        synthetics, dd, context)
            } catch (e: IllegalStateException) {
                throw JsonParseException(e.message)
            } catch (e: NumberFormatException) {
                throw JsonParseException(e.message)
            }
        }
    }

    /**
     * Application properties
     * @param id UUID of the application
     */
    public data class Application(
        public val id: String
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            json.addProperty("id", id)
            return json
        }

        public companion object {
            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): Application {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val id = jsonObject.get("id").asString
                    return Application(id)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    /**
     * Session properties
     * @param id UUID of the session
     * @param type Type of the session
     * @param hasReplay Whether this session has a replay
     */
    public data class ViewEventSession(
        public val id: String,
        public val type: Type,
        public val hasReplay: Boolean? = null
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            json.addProperty("id", id)
            json.add("type", type.toJson())
            hasReplay?.let { json.addProperty("has_replay", it) }
            return json
        }

        public companion object {
            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): ViewEventSession {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val id = jsonObject.get("id").asString
                    val type = jsonObject.get("type").asString.let {
                        Type.fromJson(it)
                    }
                    val hasReplay = jsonObject.get("has_replay")?.asBoolean
                    return ViewEventSession(id, type, hasReplay)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    /**
     * View properties
     * View properties
     * @param id UUID of the view
     * @param referrer URL that linked to the initial view of the page
     * @param url URL of the view
     * @param name User defined name of the view
     * @param loadingTime Duration in ns to the view is considered loaded
     * @param loadingType Type of the loading of the view
     * @param timeSpent Time spent on the view in ns
     * @param firstContentfulPaint Duration in ns to the first rendering
     * @param largestContentfulPaint Duration in ns to the largest contentful paint
     * @param firstInputDelay Duration in ns of the first input event delay
     * @param firstInputTime Duration in ns to the first input
     * @param cumulativeLayoutShift Total layout shift score that occured on the view
     * @param domComplete Duration in ns to the complete parsing and loading of the document and its
     * sub resources
     * @param domContentLoaded Duration in ns to the complete parsing and loading of the document
     * without its sub resources
     * @param domInteractive Duration in ns to the end of the parsing of the document
     * @param loadEvent Duration in ns to the end of the load event handler execution
     * @param customTimings User custom timings of the view. As timing name is used as facet path,
     * it must contain only letters, digits, or the characters - _ . @ $
     * @param isActive Whether the View corresponding to this event is considered active
     * @param isSlowRendered Whether the View had a low average refresh rate
     * @param action Properties of the actions of the view
     * @param error Properties of the errors of the view
     * @param crash Properties of the crashes of the view
     * @param longTask Properties of the long tasks of the view
     * @param frozenFrame Properties of the frozen frames of the view
     * @param resource Properties of the resources of the view
     * @param inForegroundPeriods List of the periods of time the user had the view in foreground
     * (focused in the browser)
     * @param memoryAverage Average memory used during the view lifetime (in bytes)
     * @param memoryMax Peak memory used during the view lifetime (in bytes)
     * @param cpuTicksCount Total number of cpu ticks during the view’s lifetime
     * @param cpuTicksPerSecond Average number of cpu ticks per second during the view’s lifetime
     * @param refreshRateAverage Average refresh rate during the view’s lifetime (in frames per
     * second)
     * @param refreshRateMin Minimum refresh rate during the view’s lifetime (in frames per second)
     */
    public data class View(
        public val id: String,
        public var referrer: String? = null,
        public var url: String,
        public var name: String? = null,
        public val loadingTime: Long? = null,
        public val loadingType: LoadingType? = null,
        public val timeSpent: Long,
        public val firstContentfulPaint: Long? = null,
        public val largestContentfulPaint: Long? = null,
        public val firstInputDelay: Long? = null,
        public val firstInputTime: Long? = null,
        public val cumulativeLayoutShift: Number? = null,
        public val domComplete: Long? = null,
        public val domContentLoaded: Long? = null,
        public val domInteractive: Long? = null,
        public val loadEvent: Long? = null,
        public val customTimings: CustomTimings? = null,
        public val isActive: Boolean? = null,
        public val isSlowRendered: Boolean? = null,
        public val action: Action,
        public val error: Error,
        public val crash: Crash? = null,
        public val longTask: LongTask? = null,
        public val frozenFrame: FrozenFrame? = null,
        public val resource: Resource,
        public val inForegroundPeriods: List<InForegroundPeriod>? = null,
        public val memoryAverage: Number? = null,
        public val memoryMax: Number? = null,
        public val cpuTicksCount: Number? = null,
        public val cpuTicksPerSecond: Number? = null,
        public val refreshRateAverage: Number? = null,
        public val refreshRateMin: Number? = null
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            json.addProperty("id", id)
            referrer?.let { json.addProperty("referrer", it) }
            json.addProperty("url", url)
            name?.let { json.addProperty("name", it) }
            loadingTime?.let { json.addProperty("loading_time", it) }
            loadingType?.let { json.add("loading_type", it.toJson()) }
            json.addProperty("time_spent", timeSpent)
            firstContentfulPaint?.let { json.addProperty("first_contentful_paint", it) }
            largestContentfulPaint?.let { json.addProperty("largest_contentful_paint", it) }
            firstInputDelay?.let { json.addProperty("first_input_delay", it) }
            firstInputTime?.let { json.addProperty("first_input_time", it) }
            cumulativeLayoutShift?.let { json.addProperty("cumulative_layout_shift", it) }
            domComplete?.let { json.addProperty("dom_complete", it) }
            domContentLoaded?.let { json.addProperty("dom_content_loaded", it) }
            domInteractive?.let { json.addProperty("dom_interactive", it) }
            loadEvent?.let { json.addProperty("load_event", it) }
            customTimings?.let { json.add("custom_timings", it.toJson()) }
            isActive?.let { json.addProperty("is_active", it) }
            isSlowRendered?.let { json.addProperty("is_slow_rendered", it) }
            json.add("action", action.toJson())
            json.add("error", error.toJson())
            crash?.let { json.add("crash", it.toJson()) }
            longTask?.let { json.add("long_task", it.toJson()) }
            frozenFrame?.let { json.add("frozen_frame", it.toJson()) }
            json.add("resource", resource.toJson())
            inForegroundPeriods?.let { temp ->
                val inForegroundPeriodsArray = JsonArray(temp.size)
                temp.forEach { inForegroundPeriodsArray.add(it.toJson()) }
                json.add("in_foreground_periods", inForegroundPeriodsArray)
            }
            memoryAverage?.let { json.addProperty("memory_average", it) }
            memoryMax?.let { json.addProperty("memory_max", it) }
            cpuTicksCount?.let { json.addProperty("cpu_ticks_count", it) }
            cpuTicksPerSecond?.let { json.addProperty("cpu_ticks_per_second", it) }
            refreshRateAverage?.let { json.addProperty("refresh_rate_average", it) }
            refreshRateMin?.let { json.addProperty("refresh_rate_min", it) }
            return json
        }

        public companion object {
            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): View {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val id = jsonObject.get("id").asString
                    val referrer = jsonObject.get("referrer")?.asString
                    val url = jsonObject.get("url").asString
                    val name = jsonObject.get("name")?.asString
                    val loadingTime = jsonObject.get("loading_time")?.asLong
                    val loadingType = jsonObject.get("loading_type")?.asString?.let {
                        LoadingType.fromJson(it)
                    }
                    val timeSpent = jsonObject.get("time_spent").asLong
                    val firstContentfulPaint = jsonObject.get("first_contentful_paint")?.asLong
                    val largestContentfulPaint = jsonObject.get("largest_contentful_paint")?.asLong
                    val firstInputDelay = jsonObject.get("first_input_delay")?.asLong
                    val firstInputTime = jsonObject.get("first_input_time")?.asLong
                    val cumulativeLayoutShift = jsonObject.get("cumulative_layout_shift")?.asNumber
                    val domComplete = jsonObject.get("dom_complete")?.asLong
                    val domContentLoaded = jsonObject.get("dom_content_loaded")?.asLong
                    val domInteractive = jsonObject.get("dom_interactive")?.asLong
                    val loadEvent = jsonObject.get("load_event")?.asLong
                    val customTimings = jsonObject.get("custom_timings")?.toString()?.let {
                        CustomTimings.fromJson(it)
                    }
                    val isActive = jsonObject.get("is_active")?.asBoolean
                    val isSlowRendered = jsonObject.get("is_slow_rendered")?.asBoolean
                    val action = jsonObject.get("action").toString().let {
                        Action.fromJson(it)
                    }
                    val error = jsonObject.get("error").toString().let {
                        Error.fromJson(it)
                    }
                    val crash = jsonObject.get("crash")?.toString()?.let {
                        Crash.fromJson(it)
                    }
                    val longTask = jsonObject.get("long_task")?.toString()?.let {
                        LongTask.fromJson(it)
                    }
                    val frozenFrame = jsonObject.get("frozen_frame")?.toString()?.let {
                        FrozenFrame.fromJson(it)
                    }
                    val resource = jsonObject.get("resource").toString().let {
                        Resource.fromJson(it)
                    }
                    val inForegroundPeriods =
                            jsonObject.get("in_foreground_periods")?.asJsonArray?.let { jsonArray ->
                        val collection = ArrayList<InForegroundPeriod>(jsonArray.size())
                        jsonArray.forEach {
                            collection.add(InForegroundPeriod.fromJson(it.toString()))
                        }
                        collection
                    }
                    val memoryAverage = jsonObject.get("memory_average")?.asNumber
                    val memoryMax = jsonObject.get("memory_max")?.asNumber
                    val cpuTicksCount = jsonObject.get("cpu_ticks_count")?.asNumber
                    val cpuTicksPerSecond = jsonObject.get("cpu_ticks_per_second")?.asNumber
                    val refreshRateAverage = jsonObject.get("refresh_rate_average")?.asNumber
                    val refreshRateMin = jsonObject.get("refresh_rate_min")?.asNumber
                    return View(id, referrer, url, name, loadingTime, loadingType, timeSpent,
                            firstContentfulPaint, largestContentfulPaint, firstInputDelay,
                            firstInputTime, cumulativeLayoutShift, domComplete, domContentLoaded,
                            domInteractive, loadEvent, customTimings, isActive, isSlowRendered,
                            action, error, crash, longTask, frozenFrame, resource,
                            inForegroundPeriods, memoryAverage, memoryMax, cpuTicksCount,
                            cpuTicksPerSecond, refreshRateAverage, refreshRateMin)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    /**
     * User properties
     * @param id Identifier of the user
     * @param name Name of the user
     * @param email Email of the user
     */
    public data class Usr(
        public val id: String? = null,
        public val name: String? = null,
        public val email: String? = null,
        public val additionalProperties: Map<String, Any?> = emptyMap()
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            id?.let { json.addProperty("id", it) }
            name?.let { json.addProperty("name", it) }
            email?.let { json.addProperty("email", it) }
            additionalProperties.forEach { (k, v) ->
                if (k !in RESERVED_PROPERTIES) {
                    json.add(k, v.toJsonElement())
                }
            }
            return json
        }

        public companion object {
            internal val RESERVED_PROPERTIES: Array<String> = arrayOf("id", "name", "email")

            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): Usr {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val id = jsonObject.get("id")?.asString
                    val name = jsonObject.get("name")?.asString
                    val email = jsonObject.get("email")?.asString
                    val additionalProperties = mutableMapOf<String, Any?>()
                    for (entry in jsonObject.entrySet()) {
                        if (entry.key !in RESERVED_PROPERTIES) {
                            additionalProperties[entry.key] = entry.value
                        }
                    }
                    return Usr(id, name, email, additionalProperties)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    /**
     * Device connectivity properties
     * @param status Status of the device connectivity
     * @param interfaces The list of available network interfaces
     * @param cellular Cellular connectivity properties
     */
    public data class Connectivity(
        public val status: Status,
        public val interfaces: List<Interface>,
        public val cellular: Cellular? = null
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            json.add("status", status.toJson())
            val interfacesArray = JsonArray(interfaces.size)
            interfaces.forEach { interfacesArray.add(it.toJson()) }
            json.add("interfaces", interfacesArray)
            cellular?.let { json.add("cellular", it.toJson()) }
            return json
        }

        public companion object {
            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): Connectivity {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val status = jsonObject.get("status").asString.let {
                        Status.fromJson(it)
                    }
                    val interfaces = jsonObject.get("interfaces").asJsonArray.let { jsonArray ->
                        val collection = ArrayList<Interface>(jsonArray.size())
                        jsonArray.forEach {
                            collection.add(Interface.fromJson(it.asString))
                        }
                        collection
                    }
                    val cellular = jsonObject.get("cellular")?.toString()?.let {
                        Cellular.fromJson(it)
                    }
                    return Connectivity(status, interfaces, cellular)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    /**
     * Synthetics properties
     * @param testId The identifier of the current Synthetics test
     * @param resultId The identifier of the current Synthetics test results
     */
    public data class Synthetics(
        public val testId: String,
        public val resultId: String
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            json.addProperty("test_id", testId)
            json.addProperty("result_id", resultId)
            return json
        }

        public companion object {
            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): Synthetics {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val testId = jsonObject.get("test_id").asString
                    val resultId = jsonObject.get("result_id").asString
                    return Synthetics(testId, resultId)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    /**
     * Internal properties
     * Internal properties
     * @param session Session-related internal properties
     * @param documentVersion Version of the update of the view event
     */
    public data class Dd(
        public val session: DdSession? = null,
        public val documentVersion: Long
    ) {
        /**
         * Version of the RUM event format
         */
        public val formatVersion: Long = 2L

        public fun toJson(): JsonElement {
            val json = JsonObject()
            json.addProperty("format_version", formatVersion)
            session?.let { json.add("session", it.toJson()) }
            json.addProperty("document_version", documentVersion)
            return json
        }

        public companion object {
            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): Dd {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val session = jsonObject.get("session")?.toString()?.let {
                        DdSession.fromJson(it)
                    }
                    val documentVersion = jsonObject.get("document_version").asLong
                    return Dd(session, documentVersion)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    /**
     * User provided context
     */
    public data class Context(
        public val additionalProperties: Map<String, Any?> = emptyMap()
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            additionalProperties.forEach { (k, v) ->
                json.add(k, v.toJsonElement())
            }
            return json
        }

        public companion object {
            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): Context {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val additionalProperties = mutableMapOf<String, Any?>()
                    for (entry in jsonObject.entrySet()) {
                        additionalProperties[entry.key] = entry.value
                    }
                    return Context(additionalProperties)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    /**
     * User custom timings of the view. As timing name is used as facet path, it must contain only
     * letters, digits, or the characters - _ . @ $
     */
    public data class CustomTimings(
        public val additionalProperties: Map<String, Long> = emptyMap()
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            additionalProperties.forEach { (k, v) ->
                json.addProperty(k, v)
            }
            return json
        }

        public companion object {
            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): CustomTimings {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val additionalProperties = mutableMapOf<String, Long>()
                    for (entry in jsonObject.entrySet()) {
                        additionalProperties[entry.key] = entry.value.asLong
                    }
                    return CustomTimings(additionalProperties)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    /**
     * Properties of the actions of the view
     * @param count Number of actions that occurred on the view
     */
    public data class Action(
        public val count: Long
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            json.addProperty("count", count)
            return json
        }

        public companion object {
            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): Action {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val count = jsonObject.get("count").asLong
                    return Action(count)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    /**
     * Properties of the errors of the view
     * @param count Number of errors that occurred on the view
     */
    public data class Error(
        public val count: Long
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            json.addProperty("count", count)
            return json
        }

        public companion object {
            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): Error {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val count = jsonObject.get("count").asLong
                    return Error(count)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    /**
     * Properties of the crashes of the view
     * @param count Number of crashes that occurred on the view
     */
    public data class Crash(
        public val count: Long
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            json.addProperty("count", count)
            return json
        }

        public companion object {
            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): Crash {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val count = jsonObject.get("count").asLong
                    return Crash(count)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    /**
     * Properties of the long tasks of the view
     * @param count Number of long tasks that occurred on the view
     */
    public data class LongTask(
        public val count: Long
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            json.addProperty("count", count)
            return json
        }

        public companion object {
            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): LongTask {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val count = jsonObject.get("count").asLong
                    return LongTask(count)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    /**
     * Properties of the frozen frames of the view
     * @param count Number of frozen frames that occurred on the view
     */
    public data class FrozenFrame(
        public val count: Long
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            json.addProperty("count", count)
            return json
        }

        public companion object {
            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): FrozenFrame {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val count = jsonObject.get("count").asLong
                    return FrozenFrame(count)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    /**
     * Properties of the resources of the view
     * @param count Number of resources that occurred on the view
     */
    public data class Resource(
        public val count: Long
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            json.addProperty("count", count)
            return json
        }

        public companion object {
            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): Resource {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val count = jsonObject.get("count").asLong
                    return Resource(count)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    /**
     * Properties of the foreground period of the view
     * @param start Duration in ns between start of the view and start of foreground period
     * @param duration Duration in ns of the view foreground period
     */
    public data class InForegroundPeriod(
        public val start: Long,
        public val duration: Long
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            json.addProperty("start", start)
            json.addProperty("duration", duration)
            return json
        }

        public companion object {
            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): InForegroundPeriod {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val start = jsonObject.get("start").asLong
                    val duration = jsonObject.get("duration").asLong
                    return InForegroundPeriod(start, duration)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    /**
     * Cellular connectivity properties
     * @param technology The type of a radio technology used for cellular connection
     * @param carrierName The name of the SIM carrier
     */
    public data class Cellular(
        public val technology: String? = null,
        public val carrierName: String? = null
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            technology?.let { json.addProperty("technology", it) }
            carrierName?.let { json.addProperty("carrier_name", it) }
            return json
        }

        public companion object {
            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): Cellular {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val technology = jsonObject.get("technology")?.asString
                    val carrierName = jsonObject.get("carrier_name")?.asString
                    return Cellular(technology, carrierName)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    /**
     * Session-related internal properties
     * @param plan Session plan: 1 is the 'lite' plan, 2 is the 'replay' plan
     */
    public data class DdSession(
        public val plan: Plan
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            json.add("plan", plan.toJson())
            return json
        }

        public companion object {
            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): DdSession {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val plan = jsonObject.get("plan").asString.let {
                        Plan.fromJson(it)
                    }
                    return DdSession(plan)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    /**
     * Type of the session
     */
    public enum class Type(
        private val jsonValue: String
    ) {
        USER("user"),
        SYNTHETICS("synthetics"),
        ;

        public fun toJson(): JsonElement = JsonPrimitive(jsonValue)

        public companion object {
            @JvmStatic
            public fun fromJson(serializedObject: String): Type = values().first {
                it.jsonValue == serializedObject
            }
        }
    }

    /**
     * Type of the loading of the view
     */
    public enum class LoadingType(
        private val jsonValue: String
    ) {
        INITIAL_LOAD("initial_load"),
        ROUTE_CHANGE("route_change"),
        ACTIVITY_DISPLAY("activity_display"),
        ACTIVITY_REDISPLAY("activity_redisplay"),
        FRAGMENT_DISPLAY("fragment_display"),
        FRAGMENT_REDISPLAY("fragment_redisplay"),
        VIEW_CONTROLLER_DISPLAY("view_controller_display"),
        VIEW_CONTROLLER_REDISPLAY("view_controller_redisplay"),
        ;

        public fun toJson(): JsonElement = JsonPrimitive(jsonValue)

        public companion object {
            @JvmStatic
            public fun fromJson(serializedObject: String): LoadingType = values().first {
                it.jsonValue == serializedObject
            }
        }
    }

    /**
     * Status of the device connectivity
     */
    public enum class Status(
        private val jsonValue: String
    ) {
        CONNECTED("connected"),
        NOT_CONNECTED("not_connected"),
        MAYBE("maybe"),
        ;

        public fun toJson(): JsonElement = JsonPrimitive(jsonValue)

        public companion object {
            @JvmStatic
            public fun fromJson(serializedObject: String): Status = values().first {
                it.jsonValue == serializedObject
            }
        }
    }

    public enum class Interface(
        private val jsonValue: String
    ) {
        BLUETOOTH("bluetooth"),
        CELLULAR("cellular"),
        ETHERNET("ethernet"),
        WIFI("wifi"),
        WIMAX("wimax"),
        MIXED("mixed"),
        OTHER("other"),
        UNKNOWN("unknown"),
        NONE("none"),
        ;

        public fun toJson(): JsonElement = JsonPrimitive(jsonValue)

        public companion object {
            @JvmStatic
            public fun fromJson(serializedObject: String): Interface = values().first {
                it.jsonValue == serializedObject
            }
        }
    }

    /**
     * Session plan: 1 is the 'lite' plan, 2 is the 'replay' plan
     */
    public enum class Plan(
        private val jsonValue: Number
    ) {
        PLAN_1(1),
        PLAN_2(2),
        ;

        public fun toJson(): JsonElement = JsonPrimitive(jsonValue)

        public companion object {
            @JvmStatic
            public fun fromJson(serializedObject: String): Plan = values().first {
                it.jsonValue.toString() == serializedObject
            }
        }
    }
}
