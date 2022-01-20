/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.core.model

import co.fast.android.internal.datadog.android.core.`internal`.utils.toJsonElement
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import java.lang.IllegalStateException
import java.lang.NumberFormatException
import kotlin.Any
import kotlin.Array
import kotlin.String
import kotlin.collections.Map
import kotlin.jvm.JvmStatic
import kotlin.jvm.Throws

/**
 * Structure holding information about the current user.
 * @param id a (unique) user identifier
 * @param name the user name or alias
 * @param email the user email
 */
public data class UserInfo(
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
        public fun fromJson(serializedObject: String): UserInfo {
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
                return UserInfo(id, name, email, additionalProperties)
            } catch (e: IllegalStateException) {
                throw JsonParseException(e.message)
            } catch (e: NumberFormatException) {
                throw JsonParseException(e.message)
            }
        }
    }
}
