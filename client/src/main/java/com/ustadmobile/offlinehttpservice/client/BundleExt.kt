package com.ustadmobile.offlinehttpservice.client

import android.os.Bundle
import rawhttp.core.RawHttp
import rawhttp.core.RawHttpRequest
import rawhttp.core.RawHttpResponse
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

fun Bundle.getRawHttpResponse(
    key: String,
    rawHttp: RawHttp,
): RawHttpResponse<*> {
    return ByteArrayInputStream(getByteArray(key)).use {
        rawHttp.parseResponse(it)
    }
}

fun Bundle.getRawHttpRequest(
    key: String,
    rawHttp: RawHttp
): RawHttpRequest {
    return ByteArrayInputStream(getByteArray(key)).use {
        rawHttp.parseRequest(it)
    }
}

fun Bundle.putRawHttpRequest(
    key: String,
    request: RawHttpRequest,
) {
    putByteArray(key, ByteArrayOutputStream().also {
        request.writeTo(it)
        it.flush()
    }.toByteArray())
}

fun Bundle.putRawHttpResponse(
    key: String,
    response: RawHttpResponse<*>,
) {
    putByteArray(key, ByteArrayOutputStream().also {
        response.writeTo(it)
        it.flush()
    }.toByteArray())
}

