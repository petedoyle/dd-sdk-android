package co.fast.android.internal.datadog.android.core.model

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import java.lang.IllegalStateException
import java.lang.NumberFormatException
import kotlin.Long
import kotlin.String
import kotlin.jvm.JvmStatic
import kotlin.jvm.Throws

/**
 * Structure holding information about the available network when an event was tracked.
 * @param connectivity the type of network connectivity
 * @param carrierName the name of the mobile data carrier
 * @param carrierId the unique id of the mobile data carrier
 * @param upKbps the upload speed in kilobytes per second
 * @param downKbps the download speed in kilobytes per second
 * @param strength the strength of the signal (the unit depends on the type of the signal)
 * @param cellularTechnology the type of cellular technology if known (e.g.: GPRS, LTE, 5G)
 */
public data class NetworkInfo(
    public val connectivity: Connectivity = Connectivity.NETWORK_NOT_CONNECTED,
    public val carrierName: String? = null,
    public val carrierId: Long? = null,
    public val upKbps: Long? = null,
    public val downKbps: Long? = null,
    public val strength: Long? = null,
    public val cellularTechnology: String? = null
) {
    public fun toJson(): JsonElement {
        val json = JsonObject()
        json.add("connectivity", connectivity.toJson())
        carrierName?.let { json.addProperty("carrier_name", it) }
        carrierId?.let { json.addProperty("carrier_id", it) }
        upKbps?.let { json.addProperty("up_kbps", it) }
        downKbps?.let { json.addProperty("down_kbps", it) }
        strength?.let { json.addProperty("strength", it) }
        cellularTechnology?.let { json.addProperty("cellular_technology", it) }
        return json
    }

    public companion object {
        @JvmStatic
        @Throws(JsonParseException::class)
        public fun fromJson(serializedObject: String): NetworkInfo {
            try {
                val jsonObject = JsonParser.parseString(serializedObject).asJsonObject
                val connectivity = jsonObject.get("connectivity").asString.let {
                    Connectivity.fromJson(it)
                }
                val carrierName = jsonObject.get("carrier_name")?.asString
                val carrierId = jsonObject.get("carrier_id")?.asLong
                val upKbps = jsonObject.get("up_kbps")?.asLong
                val downKbps = jsonObject.get("down_kbps")?.asLong
                val strength = jsonObject.get("strength")?.asLong
                val cellularTechnology = jsonObject.get("cellular_technology")?.asString
                return NetworkInfo(connectivity, carrierName, carrierId, upKbps, downKbps, strength,
                        cellularTechnology)
            } catch (e: IllegalStateException) {
                throw JsonParseException(e.message)
            } catch (e: NumberFormatException) {
                throw JsonParseException(e.message)
            }
        }
    }

    /**
     * the type of network connectivity
     */
    public enum class Connectivity(
        private val jsonValue: String
    ) {
        NETWORK_NOT_CONNECTED("network_not_connected"),
        NETWORK_ETHERNET("network_ethernet"),
        NETWORK_WIFI("network_wifi"),
        NETWORK_WIMAX("network_wimax"),
        NETWORK_BLUETOOTH("network_bluetooth"),
        NETWORK_2G("network_2G"),
        NETWORK_3G("network_3G"),
        NETWORK_4G("network_4G"),
        NETWORK_5G("network_5G"),
        NETWORK_MOBILE_OTHER("network_mobile_other"),
        NETWORK_CELLULAR("network_cellular"),
        NETWORK_OTHER("network_other"),
        ;

        public fun toJson(): JsonElement = JsonPrimitive(jsonValue)

        public companion object {
            @JvmStatic
            public fun fromJson(serializedObject: String): Connectivity = values().first {
                it.jsonValue == serializedObject
            }
        }
    }
}
