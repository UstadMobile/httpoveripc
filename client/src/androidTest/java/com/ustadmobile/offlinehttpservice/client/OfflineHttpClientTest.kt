package com.ustadmobile.offlinehttpservice.client

import android.os.Bundle
import android.os.Message
import com.ustadmobile.offlinehttpservice.client.OfflineHttpClient.Companion.HTTP_MSG
import com.ustadmobile.offlinehttpservice.client.OfflineHttpClient.Companion.KEY_REQUEST
import com.ustadmobile.offlinehttpservice.client.OfflineHttpClient.Companion.KEY_RESPONSE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.mockito.kotlin.*
import rawhttp.core.RawHttp
import java.io.ByteArrayOutputStream
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME

class OfflineHttpClientTest {

    @Test
    fun givenMessageSent_whenReplyReceived_thenShouldReturn() {
        runBlocking(Dispatchers.Default) {
            val sender = mock<OfflineHttpClient.Sender> {

            }
            val offlineHttpClient = OfflineHttpClient(sender)
            val rawHttp = RawHttp()
            val request = rawHttp.parseRequest(
                "GET /hello HTTP/1.1\r\n" +
                        "Host: headers.jsontest.com\r\n" +
                        "User-Agent: RawHTTP\r\n" +
                        "Accept: application/json")
            println(request.uri)

            val asyncResponse = async {
                offlineHttpClient.send(request)
            }

            verify(sender, timeout(1000)).sendMessage(argWhere {
                val msgRequest = it.data.getRawHttpRequest(KEY_REQUEST, rawHttp)
                msgRequest.uri.toString() == "http://headers.jsontest.com/hello" &&
                        msgRequest.headers.get("accept").first() == "application/json"
            })

            val dateString = RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC))
            val body = "Hello Reply"
            val response = rawHttp.parseResponse(
                "HTTP/1.1 200 OK\r\n" +
                        "Content-Type: plain/text\r\n" +
                        "Content-Length: " + body.length + "\r\n" +
                        "Server: RawHTTP\r\n" +
                        "Date: " + dateString + "\r\n" +
                        "\r\n" +
                        body
            )

            offlineHttpClient.handler.handleMessage(
                Message.obtain(
                    offlineHttpClient.handler, HTTP_MSG
                ).apply {
                    data = Bundle().apply {
                        putByteArray(KEY_RESPONSE, ByteArrayOutputStream().also {
                            response.writeTo(it)
                        }.toByteArray())
                    }
                }
            )

            val rawResponse = asyncResponse.await()
            Assert.assertEquals(200, rawResponse.statusCode)
        }
    }

}