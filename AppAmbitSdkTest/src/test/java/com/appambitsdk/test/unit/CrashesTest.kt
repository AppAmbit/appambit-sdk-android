package com.appambitsdk.test.unit

import com.appambit.sdk.services.interfaces.ApiService
import com.appambit.sdk.services.interfaces.Storable
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.RelaxedMockK
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.ExecutorService


class CrashesTest {

    @RelaxedMockK
    private lateinit var mockStorable: Storable

    @RelaxedMockK
    private lateinit var mockExecutorService: ExecutorService

    @RelaxedMockK
    private lateinit var mockApiService: ApiService

    @Before
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `log error persists error log`() {

    }

    @Test
    fun `log error_from exception persists crash file path`() {

    }

    @Test
    fun `send batch logs with api success removes logs from storage`() {

    }

    @Test
    fun `send batch logs with backdated logs still sends batch`() {

    }


}