package com.ustadmobile.httpoveripc.client

import fi.iki.elonen.NanoHTTPD.IHTTPSession
import rawhttp.core.RawHttp
import rawhttp.core.RawHttpRequest
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.SequenceInputStream
import kotlin.math.min

private fun InputStream.copyExactlyNBytes(
    dst: OutputStream,
    nBytes: Int,
    bufSize: Int = 8192
) {
    val buf = ByteArray(bufSize)
    var bytesRemaining = nBytes
    var bytesRead = 0

    while(
        bytesRemaining > 0 && read(buf, 0, min(bufSize, bytesRemaining)).also { bytesRead = it } != -1
    ) {
        dst.write(buf, 0, bytesRead)
        bytesRemaining -= bytesRead
    }
}

fun IHTTPSession.toRawRequest(
    rawHttp: RawHttp
): RawHttpRequest {
    val queryPostfix = if(queryParameterString != null && queryParameterString != "") {
        "?$queryParameterString"
    }else {
        ""
    }

    //get the line to send the server
    val requestLine =  "/" + uri.substringAfter("://").substringAfter("/") + queryPostfix
    val realHost = headers["ipc-host"] ?: headers["host"] ?: throw IllegalArgumentException("No host header")

    val requestStart = "${method.name} $requestLine HTTP/1.1\r\n" +
            "Host: $realHost\r\n" +
            headers.entries.filter { it.key != "host" }.joinToString(separator = "") {
                "${it.key}: ${it.value}\r\n"
            } +
            "\r\n"

    val requestStartInputStream = ByteArrayInputStream(requestStart.encodeToByteArray())

    val requestContentLen = headers["content-length"]?.toLong() ?: 0

    val parseInputStream = if(requestContentLen != 0L){
        val requestBodyBytes = ByteArrayOutputStream(requestContentLen.toInt()).use {
            inputStream.copyExactlyNBytes(it, requestContentLen.toInt())
            it.flush()
            it.toByteArray()
        }

        SequenceInputStream(requestStartInputStream, ByteArrayInputStream(requestBodyBytes))
    }else {
        requestStartInputStream
    }

    return rawHttp.parseRequest(parseInputStream)
}
