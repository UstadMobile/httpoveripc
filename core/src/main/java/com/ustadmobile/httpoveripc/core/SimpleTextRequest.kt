package com.ustadmobile.httpoveripc.core

import rawhttp.core.RawHttp
import rawhttp.core.RawHttpRequest
import java.net.URLEncoder

/**
 * This represents a simple text http request. It is deliberately simplified (e.g. duplicate headers
 * and query params are not allowed). Query param key names and values will be url encoded.
 */
data class SimpleTextRequest(
    val method: Method,
    val protocol: String,
    val host: String,
    val path: String,
    val queryParams: Map<String, String>,
    val headers: Map<String, String>,
    val requestBody: String? = null,
) {

    @Suppress("unused")
    enum class Method {
        GET, POST, PUT, DELETE
    }

    fun toRawHttpRequest(rawHttp: RawHttp): RawHttpRequest {
        val searchSuffix = if(queryParams.isNotEmpty()) {
            "?" + queryParams.entries.joinToString(separator = "&") { entry ->
                "${URLEncoder.encode(entry.key, "UTF-8")}=${URLEncoder.encode(entry.value, "UTF-8")}"
            }
        }else {
            ""
        }

        val allHeaders = headers.toMutableMap()
        allHeaders["Host"] = host
        val contentBytes = requestBody?.encodeToByteArray()
        if(contentBytes != null){
            allHeaders["Content-Length"] = contentBytes.size.toString()
        }

        return rawHttp.parseRequest(
            "${method.name} $path$searchSuffix HTTP/1.1\r\n" +
                    allHeaders.entries.joinToString(separator = "\r\n") { header ->
                        "${header.key}: ${header.value}"
                    } +
                    "\r\n" +
                    "\r\n" +
                    (requestBody ?: "")
        )
    }
}