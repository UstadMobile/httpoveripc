package com.ustadmobile.httpoveripc.core

import rawhttp.core.RawHttp
import rawhttp.core.RawHttpResponse

data class SimpleTextResponse(
    val statusCode: Int,
    val contentType: String,
    val headers: Map<String, String> = emptyMap(),
    val responseBody: String? = null,
) {

    fun toRawResponse(rawHttp: RawHttp): RawHttpResponse<*> {

        return rawHttp.parseResponse(
            "HTTP/1.1 $statusCode ${STATUS_RESPONSES[statusCode] ?: ""}\r\n" +
            "Content-Type: $contentType\r\n" +
            headers.entries.joinToString(separator = "") {
                "${it.key}: ${it.value}\r\n"
            } +
            (responseBody?.let { "\r\n$responseBody" } ?: "")
        )

    }

    companion object {

        //As per 6.1.1
        // https://www.w3.org/Protocols/rfc2616/rfc2616-sec6.html
        val STATUS_RESPONSES = mapOf(
            100 to "Continue",
            101 to "Switching Protocols",
            200 to "OK",
            201 to "Created",
            202 to "Accepted",
            203 to "Non-Authoritative Information",
            204 to "No Content",
            205 to "Reset Content",
            206 to "Partial Content",
            300 to "Multiple Choices",
            301 to "Moved Permanently",
            302 to "Found",
            303 to "See Other",
            304 to "Not Modified",
            305 to "Use Proxy",
            307 to "Temporary Redirect",
            400 to "Bad Request",
            401 to "Unauthorized",
            402 to "Payment Required",
            403 to "Forbidden",
            404 to "Not Found",
            405 to "Method Not Allowed",
            406 to "Not Acceptable",
            407 to "Proxy Authentication Required",
            408 to "Request Time-out",
            409 to "Confict",
            410 to "Gone",
            411 to "Length Required",
            412 to "Precondition failed",
            413 to "Request Entity Too Large",
            414 to "Request-URI Too Large",
            415 to "Unsupported Media Type",
            416 to "Requested range not satisfiable",
            417 to "Expectation Failed",
            500 to "Internal Server Error",
            501 to "Not Implemented",
            502 to "Bad Gateway",
            503 to "Service Unavailable",
            504 to "Gateway Time-out",
            505 to "HTTP Version not supported"
        )
    }

}