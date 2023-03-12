package com.ustadmobile.httpoveripc.server

import rawhttp.core.RawHttp
import rawhttp.core.RawHttpRequest
import rawhttp.core.RawHttpResponse
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class EchoHttpOverIpcServer: AbstractHttpOverIpcServer() {

    private val rawHttp = RawHttp()

    override fun handleRequest(request: RawHttpRequest): RawHttpResponse<*> {
        val dateString = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC))
        val bodyContentLen = request.headers["content-length"]?.firstOrNull()?.toLong()
        val requestBody = if(bodyContentLen != null) {
            request.body.get().decodeBodyToString(Charsets.UTF_8)
        }else {
            ""
        }

        val body = request.uri.toString() + requestBody


        val response = rawHttp.parseResponse(
            "HTTP/1.1 200 OK\r\n" +
                    "Content-Type: plain/text\r\n" +
                    "Content-Length: " + body.length + "\r\n" +
                    "Server: RawHTTP\r\n" +
                    "Date: " + dateString + "\r\n" +
                    "\r\n" +
                    body
        )

        return response
    }
}