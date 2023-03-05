package com.ustadmobile.httpoveripc.core.ext

import org.junit.Assert
import org.junit.Test
import rawhttp.core.RawHttp


class RawHttpResponseExtTest {

    @Test
    fun givenValidRawHttpResponse_whenConverted_thenShouldMatch() {
        val body = "Hello World"
        val rawHttp = RawHttp()
        val response = rawHttp.parseResponse(
            "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: plain/text\r\n" +
                    "Content-Length: " + body.length + "\r\n" +
                    "Server: RawHTTP\r\n" +
                    "\r\n" +
                    body
        )

        val simpleResponse = response.asSimpleTextResponse()
        Assert.assertEquals(response.statusCode, simpleResponse.statusCode)
        Assert.assertEquals(body.length.toString(),
            simpleResponse.headers.entries.firstOrNull {
                it.key.equals("content-length", true)
            }?.value)
        Assert.assertEquals(body, simpleResponse.responseBody)
    }

}