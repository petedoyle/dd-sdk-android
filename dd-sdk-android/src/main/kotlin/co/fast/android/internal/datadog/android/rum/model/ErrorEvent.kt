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
 * @param context User provided context
 * @param error Error properties
 * @param action Action properties
 */
public data class ErrorEvent(
    public val date: Long,
    public val application: Application,
    public val service: String? = null,
    public val session: ErrorEventSession,
    public val view: View,
    public val usr: Usr? = null,
    public val connectivity: Connectivity? = null,
    public val synthetics: Synthetics? = null,
    public val dd: Dd,
    public val context: Context? = null,
    public val error: Error,
    public val action: Action? = null
) {
    /**
     * RUM event type
     */
    public val type: String = "error"

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
        json.add("error", error.toJson())
        action?.let { json.add("action", it.toJson()) }
        return json
    }

    public companion object {
        @JvmStatic
        @Throws(JsonParseException::class)
        public fun fromJson(serializedObject: String): ErrorEvent {
            try {
                val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                val date = jsonObject.get("date").asLong
                val application = jsonObject.get("application").toString().let {
                    Application.fromJson(it)
                }
                val service = jsonObject.get("service")?.asString
                val session = jsonObject.get("session").toString().let {
                    ErrorEventSession.fromJson(it)
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
                val error = jsonObject.get("error").toString().let {
                    Error.fromJson(it)
                }
                val action = jsonObject.get("action")?.toString()?.let {
                    Action.fromJson(it)
                }
                return ErrorEvent(date, application, service, session, view, usr, connectivity,
                        synthetics, dd, context, error, action)
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
    public data class ErrorEventSession(
        public val id: String,
        public val type: ErrorEventSessionType,
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
            public fun fromJson(serializedObject: String): ErrorEventSession {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val id = jsonObject.get("id").asString
                    val type = jsonObject.get("type").asString.let {
                        ErrorEventSessionType.fromJson(it)
                    }
                    val hasReplay = jsonObject.get("has_replay")?.asBoolean
                    return ErrorEventSession(id, type, hasReplay)
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
     * @param inForeground Is the error starting in the foreground (focus in browser)
     */
    public data class View(
        public val id: String,
        public var referrer: String? = null,
        public var url: String,
        public var name: String? = null,
        public val inForeground: Boolean? = null
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            json.addProperty("id", id)
            referrer?.let { json.addProperty("referrer", it) }
            json.addProperty("url", url)
            name?.let { json.addProperty("name", it) }
            inForeground?.let { json.addProperty("in_foreground", it) }
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
                    val inForeground = jsonObject.get("in_foreground")?.asBoolean
                    return View(id, referrer, url, name, inForeground)
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
     * @param session Session-related internal properties
     */
    public data class Dd(
        public val session: DdSession? = null
    ) {
        /**
         * Version of the RUM event format
         */
        public val formatVersion: Long = 2L

        public fun toJson(): JsonElement {
            val json = JsonObject()
            json.addProperty("format_version", formatVersion)
            session?.let { json.add("session", it.toJson()) }
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
                    return Dd(session)
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
     * Error properties
     * @param id UUID of the error
     * @param message Error message
     * @param source Source of the error
     * @param stack Stacktrace of the error
     * @param isCrash Whether this error crashed the host application
     * @param type The type of the error
     * @param handling Whether the error has been handled manually in the source code or not
     * @param handlingStack Handling call stack
     * @param sourceType Source type of the error (the language or platform impacting the error
     * stacktrace format)
     * @param resource Resource properties of the error
     */
    public data class Error(
        public val id: String? = null,
        public var message: String,
        public val source: Source,
        public var stack: String? = null,
        public val isCrash: Boolean? = null,
        public val type: String? = null,
        public val handling: Handling? = null,
        public val handlingStack: String? = null,
        public val sourceType: SourceType? = null,
        public val resource: Resource? = null
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            id?.let { json.addProperty("id", it) }
            json.addProperty("message", message)
            json.add("source", source.toJson())
            stack?.let { json.addProperty("stack", it) }
            isCrash?.let { json.addProperty("is_crash", it) }
            type?.let { json.addProperty("type", it) }
            handling?.let { json.add("handling", it.toJson()) }
            handlingStack?.let { json.addProperty("handling_stack", it) }
            sourceType?.let { json.add("source_type", it.toJson()) }
            resource?.let { json.add("resource", it.toJson()) }
            return json
        }

        public companion object {
            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): Error {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val id = jsonObject.get("id")?.asString
                    val message = jsonObject.get("message").asString
                    val source = jsonObject.get("source").asString.let {
                        Source.fromJson(it)
                    }
                    val stack = jsonObject.get("stack")?.asString
                    val isCrash = jsonObject.get("is_crash")?.asBoolean
                    val type = jsonObject.get("type")?.asString
                    val handling = jsonObject.get("handling")?.asString?.let {
                        Handling.fromJson(it)
                    }
                    val handlingStack = jsonObject.get("handling_stack")?.asString
                    val sourceType = jsonObject.get("source_type")?.asString?.let {
                        SourceType.fromJson(it)
                    }
                    val resource = jsonObject.get("resource")?.toString()?.let {
                        Resource.fromJson(it)
                    }
                    return Error(id, message, source, stack, isCrash, type, handling, handlingStack,
                            sourceType, resource)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    /**
     * Action properties
     * @param id UUID of the action
     */
    public data class Action(
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
            public fun fromJson(serializedObject: String): Action {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val id = jsonObject.get("id").asString
                    return Action(id)
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
     * Resource properties of the error
     * @param method HTTP method of the resource
     * @param statusCode HTTP Status code of the resource
     * @param url URL of the resource
     * @param provider The provider for this resource
     */
    public data class Resource(
        public val method: Method,
        public val statusCode: Long,
        public var url: String,
        public val provider: Provider? = null
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            json.add("method", method.toJson())
            json.addProperty("status_code", statusCode)
            json.addProperty("url", url)
            provider?.let { json.add("provider", it.toJson()) }
            return json
        }

        public companion object {
            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): Resource {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val method = jsonObject.get("method").asString.let {
                        Method.fromJson(it)
                    }
                    val statusCode = jsonObject.get("status_code").asLong
                    val url = jsonObject.get("url").asString
                    val provider = jsonObject.get("provider")?.toString()?.let {
                        Provider.fromJson(it)
                    }
                    return Resource(method, statusCode, url, provider)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    /**
     * The provider for this resource
     * @param domain The domain name of the provider
     * @param name The user friendly name of the provider
     * @param type The type of provider
     */
    public data class Provider(
        public val domain: String? = null,
        public val name: String? = null,
        public val type: ProviderType? = null
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            domain?.let { json.addProperty("domain", it) }
            name?.let { json.addProperty("name", it) }
            type?.let { json.add("type", it.toJson()) }
            return json
        }

        public companion object {
            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): Provider {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val domain = jsonObject.get("domain")?.asString
                    val name = jsonObject.get("name")?.asString
                    val type = jsonObject.get("type")?.asString?.let {
                        ProviderType.fromJson(it)
                    }
                    return Provider(domain, name, type)
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
    public enum class ErrorEventSessionType(
        private val jsonValue: String
    ) {
        USER("user"),
        SYNTHETICS("synthetics"),
        ;

        public fun toJson(): JsonElement = JsonPrimitive(jsonValue)

        public companion object {
            @JvmStatic
            public fun fromJson(serializedObject: String): ErrorEventSessionType = values().first {
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
     * Source of the error
     */
    public enum class Source(
        private val jsonValue: String
    ) {
        NETWORK("network"),
        SOURCE("source"),
        CONSOLE("console"),
        LOGGER("logger"),
        AGENT("agent"),
        WEBVIEW("webview"),
        CUSTOM("custom"),
        ;

        public fun toJson(): JsonElement = JsonPrimitive(jsonValue)

        public companion object {
            @JvmStatic
            public fun fromJson(serializedObject: String): Source = values().first {
                it.jsonValue == serializedObject
            }
        }
    }

    /**
     * Whether the error has been handled manually in the source code or not
     */
    public enum class Handling(
        private val jsonValue: String
    ) {
        HANDLED("handled"),
        UNHANDLED("unhandled"),
        ;

        public fun toJson(): JsonElement = JsonPrimitive(jsonValue)

        public companion object {
            @JvmStatic
            public fun fromJson(serializedObject: String): Handling = values().first {
                it.jsonValue == serializedObject
            }
        }
    }

    /**
     * Source type of the error (the language or platform impacting the error stacktrace format)
     */
    public enum class SourceType(
        private val jsonValue: String
    ) {
        ANDROID("android"),
        BROWSER("browser"),
        IOS("ios"),
        REACT_NATIVE("react-native"),
        ;

        public fun toJson(): JsonElement = JsonPrimitive(jsonValue)

        public companion object {
            @JvmStatic
            public fun fromJson(serializedObject: String): SourceType = values().first {
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

    /**
     * HTTP method of the resource
     */
    public enum class Method(
        private val jsonValue: String
    ) {
        POST("POST"),
        GET("GET"),
        HEAD("HEAD"),
        PUT("PUT"),
        DELETE("DELETE"),
        PATCH("PATCH"),
        ;

        public fun toJson(): JsonElement = JsonPrimitive(jsonValue)

        public companion object {
            @JvmStatic
            public fun fromJson(serializedObject: String): Method = values().first {
                it.jsonValue == serializedObject
            }
        }
    }

    /**
     * The type of provider
     */
    public enum class ProviderType(
        private val jsonValue: String
    ) {
        AD("ad"),
        ADVERTISING("advertising"),
        ANALYTICS("analytics"),
        CDN("cdn"),
        CONTENT("content"),
        CUSTOMER_SUCCESS("customer-success"),
        FIRST_PARTY("first party"),
        HOSTING("hosting"),
        MARKETING("marketing"),
        OTHER("other"),
        SOCIAL("social"),
        TAG_MANAGER("tag-manager"),
        UTILITY("utility"),
        VIDEO("video"),
        ;

        public fun toJson(): JsonElement = JsonPrimitive(jsonValue)

        public companion object {
            @JvmStatic
            public fun fromJson(serializedObject: String): ProviderType = values().first {
                it.jsonValue == serializedObject
            }
        }
    }
}
