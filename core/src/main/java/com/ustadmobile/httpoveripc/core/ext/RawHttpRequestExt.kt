package com.ustadmobile.httpoveripc.core

import com.ustadmobile.httpoveripc.core.ext.toSimpleMap
import io.ktor.http.*
import rawhttp.core.RawHttpRequest

fun RawHttpRequest.asSimpleTextRequest(): SimpleTextRequest {
    return SimpleTextRequest(
        method = SimpleTextRequest.Method.valueOf(method.uppercase()),
        url = Url(uri),
        headers = headers.toSimpleMap(),
    )
}
