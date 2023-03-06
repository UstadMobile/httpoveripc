package com.ustadmobile.httpoveripc.core.ext

import com.ustadmobile.httpoveripc.core.SimpleTextRequest
import com.ustadmobile.httpoveripc.core.asSimpleTextRequest
import org.junit.Assert
import org.junit.Test
import rawhttp.core.RawHttp

class RawHttpRequestExtTest {

    @Test
    fun givenValidRawHttpRequest_whenConvertedToSimpleRequest_thenShouldMatch() {
        val rawHttp = RawHttp()
        val rawHttpRequest = rawHttp.parseRequest(
            "GET /hello?param=value HTTP/1.1\r\n" +
                    "Host: headers.jsontest.com\r\n" +
                    "User-Agent: RawHTTP\r\n" +
                    "Accept: application/json")

        val simpleRequest = rawHttpRequest.asSimpleTextRequest()
        Assert.assertEquals("Path matches", "/hello", simpleRequest.url.encodedPath)
        Assert.assertEquals("Param matches", "value",
            simpleRequest.url.parameters["param"])
        Assert.assertEquals("Host matches", "headers.jsontest.com",
            simpleRequest.url.host)
        Assert.assertEquals("Method matches", SimpleTextRequest.Method.GET,
            simpleRequest.method)
    }

}