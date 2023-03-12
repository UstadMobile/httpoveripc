package com.ustadmobile.httpoveripc.client

import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import rawhttp.core.RawHttp
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream
import java.io.SequenceInputStream

/**
 * This proxy server can receive http requests and pass them over the IPC bridge using the
 * HttpOverIpcClient. This allows clients to interact with a REST service provided by another app
 * on the device via http in (almost) the same way as connecting over the Internet.
 *
 * Clients need to add an "ipc-host" header to indicate the "real" host header that should be set
 * when the request is forwarded.
 *
 */
class HttpOverIpcProxy(
    private val httpOverIpcClient: IHttpOverIpcClient,
    private val rawHttp: RawHttp,
    private val timeout: Long = 5000,
    hostName: String? = null,
    port: Int
) : NanoHTTPD(hostName, port) {

    override fun serve(session: IHTTPSession): Response {
        val realHost = session.headers["ipc-host"] ?: session.headers["host"]
            ?: return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Bad request: No Host Header")
        val requestStart = "${session.method.name} ${session.uri} HTTP/1.1\r\n" +
                "Host: $realHost\r\n" +
                session.headers.entries.filter { it.key != "host" }.joinToString(separator = "") {
                    "${it.key}: ${it.value}\r\n"
                } +
                "\r\n"


        var requestBodyInputStream: InputStream? = null
        try {
            requestBodyInputStream = session.inputStream
            val requestStartInputStream = ByteArrayInputStream(requestStart.encodeToByteArray())
            val parseInputStream = if(requestBodyInputStream != null){
                SequenceInputStream(requestStartInputStream, requestBodyInputStream)
            }else {
                requestStartInputStream
            }

            val rawHttpRequest = rawHttp.parseRequest(parseInputStream)
            val rawHttpResponse = runBlocking {
                withTimeout(timeMillis = timeout) {
                    httpOverIpcClient.send(rawHttpRequest)
                }
            }

            var contentLength = rawHttpRequest.headers["content-length"].firstOrNull()?.toInt()
            val responseInputStream: InputStream
            if(contentLength != null && contentLength > 0) {
                responseInputStream = rawHttpResponse.body.get().asRawStream()
            }else if(rawHttpResponse.body.isPresent){
                val responseBodyByteArr = rawHttpResponse.body.get().asRawBytes()
                responseInputStream = ByteArrayInputStream(responseBodyByteArr)
                contentLength = responseBodyByteArr.size
            }else {
                responseInputStream = ByteArrayInputStream(byteArrayOf())
                contentLength = 0
            }

            return newFixedLengthResponse(
                Response.Status.lookup(rawHttpResponse.statusCode),
                rawHttpResponse.headers["content-type"].firstOrNull() ?: "text/plain",
                responseInputStream,
                contentLength.toLong()
            ).also { nanoHttpdResponse ->
                rawHttpResponse.headers.asMap().filter {
                    !it.key.equals("content-type", true)
                }.forEach { headerEntry ->
                    headerEntry.value.forEach {
                        nanoHttpdResponse.addHeader(headerEntry.key, it)
                    }
                }
            }

        }catch(e: IOException) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain",
                "Exception: $e")
        }finally {
            requestBodyInputStream?.close()
        }

    }
}