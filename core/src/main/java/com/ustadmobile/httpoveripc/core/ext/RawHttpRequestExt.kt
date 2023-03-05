package com.ustadmobile.httpoveripc.core

import com.ustadmobile.httpoveripc.core.ext.toSimpleMap
import rawhttp.core.RawHttpRequest
import java.net.URLDecoder

fun RawHttpRequest.asSimpleTextRequest(): SimpleTextRequest {
    val queryParams = uri.query.split("&").map { param ->
        val parts = param.split("=")
        URLDecoder.decode(parts.first(), "UTF-8") to
                URLDecoder.decode(parts.getOrNull(1) ?: "", "UTF-8")
    }.toMap()

    return SimpleTextRequest(
        method = SimpleTextRequest.Method.valueOf(method.uppercase()),
        protocol = uri.scheme,
        host = headers.get("Host")?.firstOrNull() ?: throw IllegalArgumentException("No Host header!"),
        path = uri.path,
        queryParams = queryParams,
        headers = headers.toSimpleMap(),
    )
}
