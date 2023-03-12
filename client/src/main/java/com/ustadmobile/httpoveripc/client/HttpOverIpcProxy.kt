package com.ustadmobile.httpoveripc.client

import fi.iki.elonen.NanoHTTPD
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import rawhttp.core.RawHttp
import java.io.ByteArrayInputStream
import java.io.InputStream

/**
 * This proxy server can receive http requests and pass them over the IPC bridge using the
 * HttpOverIpcClient. This allows clients to interact with a REST service provided by another app
 * on the device via http in (almost) the same way as connecting over the Internet.
 *
 * Clients need to add an "ipc-host" header to indicate the "real" host header that should be set
 * when the request is forwarded.
 *
 * @param httpOverIpcClient The HttpOverIpcClient that will be used to send requests
 * @param rawHttp RawHTTP instance used to parse/encode http requests
 * @param timeout the maximum time to allow for a response to be received before responding with an internal server error
 * @param hostName if not null, the hostname on which to bind as per NanoHTTPD param
 * @param port: if 0, then auto allocate, otherwise, try the specific port. As per NanoHTTPD param.
 */
class HttpOverIpcProxy(
    private val httpOverIpcClient: IHttpOverIpcClient,
    private val rawHttp: RawHttp,
    private val timeout: Long = 5000,
    hostName: String? = null,
    port: Int = 0,
) : NanoHTTPD(hostName, port) {

    override fun serve(session: IHTTPSession): Response {
        try {
            val rawHttpRequest = session.toRawRequest(rawHttp)
            val rawHttpResponse = runBlocking {
                withTimeout(timeMillis = timeout) {
                    httpOverIpcClient.send(rawHttpRequest)
                }
            }

            var contentLength = rawHttpResponse.headers["content-length"].firstOrNull()?.toInt()
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

        }catch(e: Exception) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain",
                "Exception: $e")
        }
    }
}