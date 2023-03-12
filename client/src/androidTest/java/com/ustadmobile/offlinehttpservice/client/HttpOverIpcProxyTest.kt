package com.ustadmobile.offlinehttpservice.client

import com.ustadmobile.httpoveripc.client.HttpOverIpcProxy
import com.ustadmobile.httpoveripc.client.IHttpOverIpcClient
import com.ustadmobile.httpoveripc.core.ext.rawPathAndQuery
import fi.iki.elonen.NanoHTTPD
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.*
import rawhttp.core.RawHttp
import java.io.ByteArrayOutputStream

class HttpOverIpcProxyTest {

    @Test
    fun givenValidRequestWithNoBody_whenServeCalled_thenShouldReturnResponse() {
        val requestHeaders = mapOf(
            "ipc-host" to "localhost:8087",
            "x-header" to "42"
        )

        val mockSession = mock<NanoHTTPD.IHTTPSession> {
            on { uri }.thenReturn("http://localhost:8000/some/page")
            on { headers }.thenReturn(requestHeaders)
            on { method }.thenReturn(NanoHTTPD.Method.GET)
            on { inputStream }.thenReturn(null)
        }

        val rawHttp = RawHttp()

        val mockIpcClient = mock<IHttpOverIpcClient> {
            onBlocking { send(any()) }.thenAnswer {
                val body = "Hello Reply"
                rawHttp.parseResponse(
                    "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/html\r\n" +
                            "Content-Length: " + body.length + "\r\n" +
                            "Server: RawHTTP\r\n" +
                            "\r\n" +
                            body
                )
            }
        }

        val proxy = HttpOverIpcProxy(
            mockIpcClient, RawHttp(), port = 0
        )

        val response = proxy.serve(mockSession)

        verifyBlocking(mockIpcClient) {
            send(argWhere {
                it.uri.rawPathAndQuery == "/some/page" &&
                        it.headers["host"].first() == "localhost:8087" &&
                        it.headers["x-header"].first() == "42"
            })
        }

        val responseBodyStr = ByteArrayOutputStream().use {
            response.data.copyTo(it)
            it.flush()
            it.toByteArray()
        }.decodeToString()

        assertEquals("Hello Reply", responseBodyStr)
        assertEquals("text/html", response.mimeType)
        assertEquals("RawHTTP", response.getHeader("Server"))
    }

}