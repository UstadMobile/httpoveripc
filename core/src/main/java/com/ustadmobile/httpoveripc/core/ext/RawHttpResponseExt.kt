package com.ustadmobile.httpoveripc.core.ext

import com.ustadmobile.httpoveripc.core.SimpleTextResponse
import rawhttp.core.RawHttpResponse
import kotlin.jvm.optionals.getOrNull

@OptIn(ExperimentalStdlibApi::class)
fun RawHttpResponse<*>.asSimpleTextResponse() : SimpleTextResponse{
    return SimpleTextResponse(
        statusCode = statusCode,
        contentType = headers["Content-Type"].firstOrNull() ?: throw IllegalArgumentException("No content type"),
        headers = headers.toSimpleMap(),
        responseBody = body.getOrNull()?.decodeBodyToString(Charsets.UTF_8),
    )
}
