package com.ustadmobile.httpoveripc.client

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import rawhttp.core.RawHttp
import rawhttp.core.RawHttpHeaders
import rawhttp.core.body.StringBody
import rawhttp.core.server.TcpRawHttpServer
import java.net.InetAddress
import java.net.ServerSocket
import java.util.*

/**
 * This proxy server can receive http requests and pass them over the IPC bridge using the
 * HttpOverIpcClient. This allows clients to interact with a REST service provided by another app
 * on the device via http in (almost) the same way as connecting over the Internet.
 *
 * Clients must add a Forwarded header if they want the receiver to know the original protocol and
 * host (e.g. proto and host)
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
) {

    private var openSocket: ServerSocket? = null

    private val rawServerOptions = TcpRawHttpServer.TcpRawHttpServerOptions {
        val socket = if(hostName != null) {
            ServerSocket(port, SOCKET_BACKLOG_ALLOWED, InetAddress.getByName(hostName))
        }else {
            ServerSocket(port, SOCKET_BACKLOG_ALLOWED)
        }
        openSocket = socket
        socket
    }

    private val rawHttpServer = TcpRawHttpServer(rawServerOptions)

    val listeningPort: Int
        get()  = openSocket?.localPort ?: throw IllegalStateException("Port not open")

    fun start() {
        rawHttpServer.start { request ->
            try {
                val proxyRequest = request.withHeaders(
                    RawHttpHeaders.newBuilder(request.headers)
                        .build()
                )

                runBlocking {
                    withTimeout(timeout) {
                        Optional.of(httpOverIpcClient.send(proxyRequest))
                    }
                }
            }catch(e: Exception) {
                Optional.of(
                    rawHttp.parseResponse("HTTP/1.1 500 Internal Error\r\n" +
                        "Content-Type: text/plain").withBody(StringBody(e.toString())))
            }
        }
    }


    fun stop() {
        rawHttpServer.stop()
        openSocket = null
    }

    companion object {

        const val SOCKET_BACKLOG_ALLOWED = 50
    }
}
