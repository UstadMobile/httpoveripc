package com.ustadmobile.httpoveripc.core

import org.junit.Assert
import org.junit.Test
import rawhttp.core.RawHttp

class SimpleTextRequestTest {

    @Test
    fun givenValidRequestWithHeaders_whenConvertedToRawRequest_thenShouldParseOk() {
        val simpleTextRequest = SimpleTextRequest(
            method = SimpleTextRequest.Method.PUT,
            protocol = "https",
            host = "dummyserver.com",
            path = "/endpoint",
            queryParams = mapOf("key" to "value"),
            headers = mapOf("auth" to "secret"),
            requestBody = "Hello"
        )

        val rawRequest = simpleTextRequest.toRawHttpRequest(RawHttp())

        Assert.assertEquals("Header as expected", simpleTextRequest.headers["auth"],
            rawRequest.headers.get("auth").first())
        Assert.assertEquals(rawRequest.uri.query, "key=value")
        Assert.assertEquals("Method as expected", "PUT", rawRequest.method)
        Assert.assertEquals("Path as expected", simpleTextRequest.path,
            rawRequest.uri.path)
        Assert.assertEquals("Body as expected", "Hello",
            rawRequest.body.get().asRawString(Charsets.UTF_8))
    }

}