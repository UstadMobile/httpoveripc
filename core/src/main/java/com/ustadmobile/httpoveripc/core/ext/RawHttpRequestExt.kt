package com.ustadmobile.httpoveripc.core

import com.ustadmobile.httpoveripc.core.ext.toSimpleMap
import io.ktor.http.*
import io.ktor.utils.io.charsets.Charsets
import rawhttp.core.RawHttpRequest

fun RawHttpRequest.asSimpleTextRequest(): SimpleTextRequest {
    return SimpleTextRequest(
        method = SimpleTextRequest.Method.valueOf(method.uppercase()),
        url = Url(uri),
        headers = headers.toSimpleMap(),
        requestBody = if(body.isPresent) {
            body.get().decodeBodyToString(Charsets.UTF_8)
        } else {
            null
        }
    )
}
