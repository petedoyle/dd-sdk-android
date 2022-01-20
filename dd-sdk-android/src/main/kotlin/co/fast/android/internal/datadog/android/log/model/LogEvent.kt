package co.fast.android.internal.datadog.android.log.model

import com.datadog.android.core.`internal`.utils.toJsonElement
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import java.lang.IllegalStateException
import java.lang.NumberFormatException
import kotlin.Any
import kotlin.Array
import kotlin.String
import kotlin.collections.Map
import kotlin.jvm.JvmStatic
import kotlin.jvm.Throws

/**
 * Structure holding information about a Log
 * @param status The severity of this log
 * @param service The service name
 * @param message The log message
 * @param date The date when the log is fired as an ISO-8601 String
 * @param logger Information about the logger that produced this log.
 * @param usr User properties
 * @param network The network information in the moment the log was created
 * @param error The additional error information in case this log is marked as an error
 * @param ddtags The list of tags joined into a String and divided by ',' 
 */
public data class LogEvent(
    public var status: Status,
    public val service: String,
    public var message: String,
    public val date: String,
    public val logger: Logger,
    public val usr: Usr? = null,
    public val network: Network? = null,
    public val error: Error? = null,
    public var ddtags: String,
    public val additionalProperties: Map<String, Any?> = emptyMap()
) {
    public fun toJson(): JsonElement {
        val json = JsonObject()
        json.add("status", status.toJson())
        json.addProperty("service", service)
        json.addProperty("message", message)
        json.addProperty("date", date)
        json.add("logger", logger.toJson())
        usr?.let { json.add("usr", it.toJson()) }
        network?.let { json.add("network", it.toJson()) }
        error?.let { json.add("error", it.toJson()) }
        json.addProperty("ddtags", ddtags)
        additionalProperties.forEach { (k, v) ->
            if (k !in RESERVED_PROPERTIES) {
                json.add(k, v.toJsonElement())
            }
        }
        return json
    }

    public companion object {
        internal val RESERVED_PROPERTIES: Array<String> = arrayOf("status", "service", "message",
                "date", "logger", "usr", "network", "error", "ddtags")

        @JvmStatic
        @Throws(JsonParseException::class)
        public fun fromJson(serializedObject: String): LogEvent {
            try {
                val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                val status = jsonObject.get("status").asString.let {
                    Status.fromJson(it)
                }
                val service = jsonObject.get("service").asString
                val message = jsonObject.get("message").asString
                val date = jsonObject.get("date").asString
                val logger = jsonObject.get("logger").toString().let {
                    Logger.fromJson(it)
                }
                val usr = jsonObject.get("usr")?.toString()?.let {
                    Usr.fromJson(it)
                }
                val network = jsonObject.get("network")?.toString()?.let {
                    Network.fromJson(it)
                }
                val error = jsonObject.get("error")?.toString()?.let {
                    Error.fromJson(it)
                }
                val ddtags = jsonObject.get("ddtags").asString
                val additionalProperties = mutableMapOf<String, Any?>()
                for (entry in jsonObject.entrySet()) {
                    if (entry.key !in RESERVED_PROPERTIES) {
                        additionalProperties[entry.key] = entry.value
                    }
                }
                return LogEvent(status, service, message, date, logger, usr, network, error, ddtags,
                        additionalProperties)
            } catch (e: IllegalStateException) {
                throw JsonParseException(e.message)
            } catch (e: NumberFormatException) {
                throw JsonParseException(e.message)
            }
        }
    }

    /**
     * Information about the logger that produced this log.
     * @param name The name of the logger
     * @param threadName The thread name on which the log event was created
     * @param version The SDK version name
     */
    public data class Logger(
        public var name: String,
        public val threadName: String? = null,
        public val version: String
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            json.addProperty("name", name)
            threadName?.let { json.addProperty("thread_name", it) }
            json.addProperty("version", version)
            return json
        }

        public companion object {
            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): Logger {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val name = jsonObject.get("name").asString
                    val threadName = jsonObject.get("thread_name")?.asString
                    val version = jsonObject.get("version").asString
                    return Logger(name, threadName, version)
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
     * The network information in the moment the log was created
     */
    public data class Network(
        public val client: Client
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            json.add("client", client.toJson())
            return json
        }

        public companion object {
            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): Network {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val client = jsonObject.get("client").toString().let {
                        Client.fromJson(it)
                    }
                    return Network(client)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    /**
     * The additional error information in case this log is marked as an error
     * @param kind The kind of this error. It is resolved from the throwable class name
     * @param message The error message
     * @param stack The error stack trace
     */
    public data class Error(
        public var kind: String? = null,
        public var message: String? = null,
        public var stack: String? = null
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            kind?.let { json.addProperty("kind", it) }
            message?.let { json.addProperty("message", it) }
            stack?.let { json.addProperty("stack", it) }
            return json
        }

        public companion object {
            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): Error {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val kind = jsonObject.get("kind")?.asString
                    val message = jsonObject.get("message")?.asString
                    val stack = jsonObject.get("stack")?.asString
                    return Error(kind, message, stack)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    /**
     * @param connectivity The active network
     */
    public data class Client(
        public val simCarrier: SimCarrier? = null,
        public val signalStrength: String? = null,
        public val downlinkKbps: String? = null,
        public val uplinkKbps: String? = null,
        public val connectivity: String
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            simCarrier?.let { json.add("sim_carrier", it.toJson()) }
            signalStrength?.let { json.addProperty("signal_strength", it) }
            downlinkKbps?.let { json.addProperty("downlink_kbps", it) }
            uplinkKbps?.let { json.addProperty("uplink_kbps", it) }
            json.addProperty("connectivity", connectivity)
            return json
        }

        public companion object {
            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): Client {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val simCarrier = jsonObject.get("sim_carrier")?.toString()?.let {
                        SimCarrier.fromJson(it)
                    }
                    val signalStrength = jsonObject.get("signal_strength")?.asString
                    val downlinkKbps = jsonObject.get("downlink_kbps")?.asString
                    val uplinkKbps = jsonObject.get("uplink_kbps")?.asString
                    val connectivity = jsonObject.get("connectivity").asString
                    return Client(simCarrier, signalStrength, downlinkKbps, uplinkKbps,
                            connectivity)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    public data class SimCarrier(
        public val id: String? = null,
        public val name: String? = null
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            id?.let { json.addProperty("id", it) }
            name?.let { json.addProperty("name", it) }
            return json
        }

        public companion object {
            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): SimCarrier {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val id = jsonObject.get("id")?.asString
                    val name = jsonObject.get("name")?.asString
                    return SimCarrier(id, name)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    /**
     * The severity of this log
     */
    public enum class Status(
        private val jsonValue: String
    ) {
        CRITICAL("critical"),
        ERROR("error"),
        WARN("warn"),
        INFO("info"),
        DEBUG("debug"),
        TRACE("trace"),
        EMERGENCY("emergency"),
        ;

        public fun toJson(): JsonElement = JsonPrimitive(jsonValue)

        public companion object {
            @JvmStatic
            public fun fromJson(serializedObject: String): Status = values().first {
                it.jsonValue == serializedObject
            }
        }
    }
}
