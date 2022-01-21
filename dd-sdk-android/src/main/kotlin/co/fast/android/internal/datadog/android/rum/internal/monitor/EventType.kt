/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.rum.internal.monitor

internal enum class EventType {
    VIEW,
    ACTION,
    RESOURCE,
    ERROR,
    LONG_TASK,
    FROZEN_FRAME
}