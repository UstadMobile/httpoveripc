package com.ustadmobile.httpoveripc.core

import io.ktor.http.*
import rawhttp.core.RawHttp
import rawhttp.core.RawHttpRequest

/**
 * This represents a simple text http request. It is deliberately simplified (e.g. duplicate headers
 * and query params are not allowed). Query param key names and values will be url encoded.
 */
data class SimpleTextRequest(
    val method: Method,
    val url: Url,
    val headers: Map<String, String>,
    val requestBody: String? = null,
) {

    @Suppress("unused")
    enum class Method {
        GET, POST, PUT, DELETE
    }

    fun toRawHttpRequest(rawHttp: RawHttp): RawHttpRequest {
        val allHeaders = headers.toMutableMap()
        allHeaders["Host"] = if(url.specifiedPort > 0) {
            url.hostWithPort
        }else {
            url.host
        }
        val contentBytes = requestBody?.encodeToByteArray()
        if(contentBytes != null){
            allHeaders["Content-Length"] = contentBytes.size.toString()
        }

        return rawHttp.parseRequest(
            "${method.name} ${url.encodedPathAndQuery} HTTP/1.1\r\n" +
                    allHeaders.entries.joinToString(separator = "\r\n") { header ->
                        "${header.key}: ${header.value}"
                    } +
                    "\r\n" +
                    "\r\n" +
                    (requestBody ?: "")
        )
    }
}