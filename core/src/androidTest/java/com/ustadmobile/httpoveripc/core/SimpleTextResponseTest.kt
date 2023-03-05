package com.ustadmobile.httpoveripc.core

import org.junit.Assert
import org.junit.Test
import rawhttp.core.RawHttp

class SimpleTextResponseTest {

    @Test
    fun givenValidResponse_whenConvertedToRawResponse_thenShouldMatch() {
        val simpleResponse = SimpleTextResponse(
            statusCode = 200,
            contentType = "application/json",
            headers = mapOf("auth" to "secret"),
            responseBody = "Hello World"
        )

        val rawHttp = RawHttp()
        val rawResponse = simpleResponse.toRawResponse(rawHttp)

        Assert.assertEquals(simpleResponse.statusCode, rawResponse.statusCode)
        Assert.assertEquals("Hello World",
            rawResponse.body.get().asRawString(Charsets.UTF_8))
        Assert.assertEquals("secret", rawResponse.headers["auth"].first())
        Assert.assertEquals("application/json", rawResponse.headers["content-type"].first())
    }

}