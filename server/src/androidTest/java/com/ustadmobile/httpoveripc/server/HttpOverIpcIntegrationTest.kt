package com.ustadmobile.httpoveripc.server

import android.content.Intent
import android.os.IBinder
import androidx.test.core.app.ApplicationProvider
import androidx.test.rule.ServiceTestRule
import com.ustadmobile.httpoveripc.client.HttpOverIpcClient
import com.ustadmobile.httpoveripc.client.HttpOverIpcProxy
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import rawhttp.core.RawHttp
import java.nio.charset.StandardCharsets


class HttpOverIpcIntegrationTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    @Test
    fun givenValidService_whenClientSendsRequest_thenCanGetResponseFromServer() {
        val rawHttp = RawHttp()
        val serviceIntent = Intent(
            ApplicationProvider.getApplicationContext(),
            EchoHttpOverIpcServer::class.java
        )

        val binder: IBinder = serviceRule.bindService(serviceIntent)
        val client = HttpOverIpcClient(binder)

        val request = rawHttp.parseRequest(
            "GET /hello HTTP/1.1\r\n" +
                    "Host: headers.jsontest.com\r\n" +
                    "User-Agent: RawHTTP\r\n" +
                    "Accept: application/json")

        runBlocking {
            val response = client.send(request)
            assertEquals(200, response.statusCode)
        }
    }

    @Test
    fun givenValidService_whenClientsSendRequestConcurrently_thenCanGetResponseFromServer() {
        val rawHttp = RawHttp()
        val serviceIntent = Intent(
            ApplicationProvider.getApplicationContext(),
            EchoHttpOverIpcServer::class.java
        )

        val binder: IBinder = serviceRule.bindService(serviceIntent)
        val client = HttpOverIpcClient(binder)

        runBlocking {
            val requests = (1..10).map { index ->
                async {
                    val response = client.send(rawHttp.parseRequest(
                        "GET /hello${index} HTTP/1.1\r\n" +
                                "Host: headers.jsontest.com\r\n" +
                                "User-Agent: RawHTTP\r\n" +
                                "Accept: application/json"))
                    assertEquals(200, response.statusCode)
                    assertTrue(response.body.get().asRawString(StandardCharsets.UTF_8)
                        .endsWith("/hello${index}"))
                }
            }

            awaitAll(*requests.toTypedArray())
        }
        client.close()
    }

    @Test
    fun givenValidService_whenClientSendsGetRequestUsingHttpProxy_thenCanGetResponseFromSErver() {
        val rawHttp = RawHttp()
        val serviceIntent = Intent(
            ApplicationProvider.getApplicationContext(),
            EchoHttpOverIpcServer::class.java
        )

        val binder: IBinder = serviceRule.bindService(serviceIntent)
        val ipcClient = HttpOverIpcClient(binder)
        val proxyServer = HttpOverIpcProxy(ipcClient, rawHttp, port = 0)
        proxyServer.start()

        val httpClient = HttpClient()
        val httpResponse = runBlocking {
            httpClient.get("http://localhost:${proxyServer.listeningPort}/helloHttp?param=value")
        }
        val body = runBlocking { httpResponse.bodyAsText() }

        assertEquals("http://localhost:${proxyServer.listeningPort}/helloHttp?param=value", body)

        proxyServer.stop()
        ipcClient.close()
    }

    @Test
    fun givenValidService_whenCLientSendPostRequestWithBody_thenCanGetResponseFromServer() {
        val rawHttp = RawHttp()
        val serviceIntent = Intent(
            ApplicationProvider.getApplicationContext(),
            EchoHttpOverIpcServer::class.java
        )

        val binder: IBinder = serviceRule.bindService(serviceIntent)
        val ipcClient = HttpOverIpcClient(binder)
        val proxyServer = HttpOverIpcProxy(ipcClient, rawHttp, port = 0)
        proxyServer.start()

        val httpClient = HttpClient()
        val httpResponse = runBlocking {
            httpClient.post("http://localhost:${proxyServer.listeningPort}/helloHttp?param=value") {
                header("Content-type", "text/plain")
                setBody("Body Content")
            }
        }
        val body = runBlocking { httpResponse.bodyAsText() }
        assertTrue(body.startsWith("http://localhost:${proxyServer.listeningPort}/helloHttp?param=value"))
        assertTrue(body.endsWith("Body Content"))
    }

}