/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.log.internal.user

import co.fast.android.internal.datadog.android.core.internal.persistence.Serializer
import co.fast.android.internal.datadog.android.core.model.UserInfo

internal class UserInfoSerializer :
    Serializer<UserInfo> {

    override fun serialize(model: UserInfo): String {
        return model.toJson().asJsonObject.toString()
    }
}