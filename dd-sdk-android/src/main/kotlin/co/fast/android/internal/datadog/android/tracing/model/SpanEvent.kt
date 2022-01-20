package co.fast.android.internal.datadog.android.tracing.model

import com.datadog.android.core.`internal`.utils.toJsonElement
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import java.lang.IllegalStateException
import java.lang.NumberFormatException
import kotlin.Any
import kotlin.Array
import kotlin.Long
import kotlin.Number
import kotlin.String
import kotlin.collections.Map
import kotlin.jvm.JvmStatic
import kotlin.jvm.Throws

/**
 * Structure holding information about a Span
 * @param traceId The id of the trace this Span belongs to
 * @param spanId The unique id of this Span
 * @param parentId The id this Span's parent or 0 if this is the root Span
 * @param resource The resource name
 * @param name The name of this Span
 * @param service The service name
 * @param duration The duration of this Span in nanoseconds
 * @param start The Span start time in nanoseconds
 * @param error Span error flag. If 1 that means there was an error thrown during the duration of
 * the Span
 * @param metrics The metrics data of this Span event
 * @param meta The metadata of this Span event
 */
public data class SpanEvent(
    public val traceId: String,
    public val spanId: String,
    public val parentId: String,
    public var resource: String,
    public var name: String,
    public val service: String,
    public val duration: Long,
    public val start: Long,
    public val error: Long = 0L,
    public val metrics: Metrics,
    public val meta: Meta
) {
    /**
     * The type of the Span. For Mobile this will always be 'CUSTOM'
     */
    public val type: String = "custom"

    public fun toJson(): JsonElement {
        val json = JsonObject()
        json.addProperty("trace_id", traceId)
        json.addProperty("span_id", spanId)
        json.addProperty("parent_id", parentId)
        json.addProperty("resource", resource)
        json.addProperty("name", name)
        json.addProperty("service", service)
        json.addProperty("duration", duration)
        json.addProperty("start", start)
        json.addProperty("error", error)
        json.addProperty("type", type)
        json.add("metrics", metrics.toJson())
        json.add("meta", meta.toJson())
        return json
    }

    public companion object {
        @JvmStatic
        @Throws(JsonParseException::class)
        public fun fromJson(serializedObject: String): SpanEvent {
            try {
                val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                val traceId = jsonObject.get("trace_id").asString
                val spanId = jsonObject.get("span_id").asString
                val parentId = jsonObject.get("parent_id").asString
                val resource = jsonObject.get("resource").asString
                val name = jsonObject.get("name").asString
                val service = jsonObject.get("service").asString
                val duration = jsonObject.get("duration").asLong
                val start = jsonObject.get("start").asLong
                val error = jsonObject.get("error").asLong
                val metrics = jsonObject.get("metrics").toString().let {
                    Metrics.fromJson(it)
                }
                val meta = jsonObject.get("meta").toString().let {
                    Meta.fromJson(it)
                }
                return SpanEvent(traceId, spanId, parentId, resource, name, service, duration,
                        start, error, metrics, meta)
            } catch (e: IllegalStateException) {
                throw JsonParseException(e.message)
            } catch (e: NumberFormatException) {
                throw JsonParseException(e.message)
            }
        }
    }

    /**
     * The metrics data of this Span event
     * @param topLevel Top level flag. If 1 means that this Span is the root of this Trace
     */
    public data class Metrics(
        public val topLevel: Long? = null,
        public val additionalProperties: Map<String, Number> = emptyMap()
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            topLevel?.let { json.addProperty("_top_level", it) }
            additionalProperties.forEach { (k, v) ->
                if (k !in RESERVED_PROPERTIES) {
                    json.addProperty(k, v)
                }
            }
            return json
        }

        public companion object {
            internal val RESERVED_PROPERTIES: Array<String> = arrayOf("_top_level")

            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): Metrics {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val topLevel = jsonObject.get("_top_level")?.asLong
                    val additionalProperties = mutableMapOf<String, Number>()
                    for (entry in jsonObject.entrySet()) {
                        if (entry.key !in RESERVED_PROPERTIES) {
                            additionalProperties[entry.key] = entry.value.asNumber
                        }
                    }
                    return Metrics(topLevel, additionalProperties)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    /**
     * The metadata of this Span event
     * @param version The client application package version
     * @param usr User properties
     * @param network The network information in the moment the Span was created
     */
    public data class Meta(
        public val version: String,
        public val dd: Dd,
        public val span: Span,
        public val tracer: Tracer,
        public val usr: Usr,
        public val network: Network,
        public val additionalProperties: Map<String, String> = emptyMap()
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            json.addProperty("version", version)
            json.add("_dd", dd.toJson())
            json.add("span", span.toJson())
            json.add("tracer", tracer.toJson())
            json.add("usr", usr.toJson())
            json.add("network", network.toJson())
            additionalProperties.forEach { (k, v) ->
                if (k !in RESERVED_PROPERTIES) {
                    json.addProperty(k, v)
                }
            }
            return json
        }

        public companion object {
            internal val RESERVED_PROPERTIES: Array<String> = arrayOf("version", "_dd", "span",
                    "tracer", "usr", "network")

            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): Meta {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val version = jsonObject.get("version").asString
                    val dd = jsonObject.get("_dd").toString().let {
                        Dd.fromJson(it)
                    }
                    val span = Span()
                    val tracer = jsonObject.get("tracer").toString().let {
                        Tracer.fromJson(it)
                    }
                    val usr = jsonObject.get("usr").toString().let {
                        Usr.fromJson(it)
                    }
                    val network = jsonObject.get("network").toString().let {
                        Network.fromJson(it)
                    }
                    val additionalProperties = mutableMapOf<String, String>()
                    for (entry in jsonObject.entrySet()) {
                        if (entry.key !in RESERVED_PROPERTIES) {
                            additionalProperties[entry.key] = entry.value.asString
                        }
                    }
                    return Meta(version, dd, span, tracer, usr, network, additionalProperties)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    /**
     * @param source The trace source
     */
    public data class Dd(
        public val source: String? = "android"
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            source?.let { json.addProperty("source", it) }
            return json
        }

        public companion object {
            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): Dd {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val source = jsonObject.get("source")?.asString
                    return Dd(source)
                } catch (e: IllegalStateException) {
                    throw JsonParseException(e.message)
                } catch (e: NumberFormatException) {
                    throw JsonParseException(e.message)
                }
            }
        }
    }

    public class Span() {
        /**
         * The type of the Span
         */
        public val kind: String = "client"

        public fun toJson(): JsonElement {
            val json = JsonObject()
            json.addProperty("kind", kind)
            return json
        }
    }

    /**
     * @param version The SDK version name
     */
    public data class Tracer(
        public val version: String
    ) {
        public fun toJson(): JsonElement {
            val json = JsonObject()
            json.addProperty("version", version)
            return json
        }

        public companion object {
            @JvmStatic
            @Throws(JsonParseException::class)
            public fun fromJson(serializedObject: String): Tracer {
                try {
                    val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                    val version = jsonObject.get("version").asString
                    return Tracer(version)
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
     * The network information in the moment the Span was created
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
}
