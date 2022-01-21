/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.log.internal.user

import co.fast.android.internal.datadog.android.core.internal.persistence.Deserializer
import co.fast.android.internal.datadog.android.core.model.UserInfo
import co.fast.android.internal.datadog.android.log.Logger
import com.google.gson.JsonParseException
import java.util.Locale

internal class UserInfoDeserializer(
    private val internalLogger: Logger
) : Deserializer<UserInfo> {

    override fun deserialize(model: String): UserInfo? {
        return try {
            UserInfo.fromJson(model)
        } catch (e: JsonParseException) {
            internalLogger.e(DESERIALIZE_ERROR_MESSAGE_FORMAT.format(Locale.US, model), e)
            null
        } catch (e: IllegalStateException) {
            internalLogger.e(DESERIALIZE_ERROR_MESSAGE_FORMAT.format(Locale.US, model), e)
            null
        }
    }

    companion object {
        const val DESERIALIZE_ERROR_MESSAGE_FORMAT =
            "Error while trying to deserialize the serialized UserInfo: %s"
    }
}