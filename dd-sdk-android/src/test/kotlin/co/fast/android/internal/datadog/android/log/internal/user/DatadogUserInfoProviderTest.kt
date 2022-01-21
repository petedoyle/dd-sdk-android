/*
 * Unless explicitly stated otherwise all files in this repository are licensed under the Apache License Version 2.0.
 * This product includes software developed at Datadog (https://www.datadoghq.com/).
 * Copyright 2016-Present Datadog, Inc.
 */

package co.fast.android.internal.datadog.android.log.internal.user

import co.fast.android.internal.datadog.android.core.internal.persistence.DataWriter
import co.fast.android.internal.datadog.android.core.model.UserInfo
import co.fast.android.internal.datadog.android.utils.forge.Configurator
import com.nhaarman.mockitokotlin2.verify
import fr.xgouchet.elmyr.annotation.Forgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

@ExtendWith(ForgeExtension::class)
@ForgeConfiguration(Configurator::class)
@MockitoSettings(strictness = Strictness.LENIENT)
internal class DatadogUserInfoProviderTest {

    lateinit var testedProvider: DatadogUserInfoProvider

    @Mock
    lateinit var mockWriter: DataWriter<UserInfo>

    @BeforeEach
    fun `set up`() {
        testedProvider = DatadogUserInfoProvider(mockWriter)
    }

    @Test
    fun `𝕄 return default userInfo 𝕎 getUserInfo()`() {
        // When
        val result = testedProvider.getUserInfo()

        // Then
        assertThat(result).isEqualTo(UserInfo())
    }

    @Test
    fun `𝕄 return saved userInfo 𝕎 setUserInfo() and getUserInfo()`(
        @Forgery userInfo: UserInfo
    ) {
        // When
        testedProvider.setUserInfo(userInfo)
        val result = testedProvider.getUserInfo()

        // Then
        assertThat(result).isEqualTo(userInfo)
    }

    @Test
    fun `M delegate to persister W setUserInfo`(@Forgery userInfo: UserInfo) {
        // When
        testedProvider.setUserInfo(userInfo)

        // Then
        verify(mockWriter).write(userInfo)
    }
}