package com.ustadmobile.httpoveripc.server

import android.content.Intent
import android.os.*
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ServiceTestRule
import com.ustadmobile.httpoveripc.core.HttpOverIpcConstants.HTTP_REQUEST
import com.ustadmobile.httpoveripc.core.HttpOverIpcConstants.KEY_REQUEST
import com.ustadmobile.httpoveripc.core.HttpOverIpcConstants.KEY_RESPONSE
import com.ustadmobile.httpoveripc.core.ext.getRawHttpResponse
import com.ustadmobile.httpoveripc.core.ext.putRawHttpRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Rule
import rawhttp.core.RawHttp
import rawhttp.core.RawHttpResponse

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class AbstractHttpOverIpcServiceTest {

    @get:Rule
    val serviceRule = ServiceTestRule()

    @Test
    fun givenWorkingHandler_whenRawRequestMessageSent_thenShouldGetReply() {
        val rawHttp = RawHttp()
        val serviceIntent = Intent(
            ApplicationProvider.getApplicationContext(),
            EchoHttpOverIpcServer::class.java
        )

        val binder: IBinder = serviceRule.bindService(serviceIntent)
        val service = Messenger(binder)

        val completeableDeffered = CompletableDeferred<RawHttpResponse<*>>()
        val incomingHandler = object: Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                val reply = msg.data.getRawHttpResponse(KEY_RESPONSE, rawHttp)
                completeableDeffered.complete(reply)
            }
        }
        val incomingMessenger = Messenger(incomingHandler)

        service.send(Message.obtain(null, HTTP_REQUEST).also {
            it.replyTo = incomingMessenger

            val request = rawHttp.parseRequest(
            "GET /hello HTTP/1.1\r\n" +
                    "Host: headers.jsontest.com\r\n" +
                    "User-Agent: RawHTTP\r\n" +
                    "Accept: application/json")
            it.data.putRawHttpRequest(KEY_REQUEST, request)
        })

        val response = runBlocking {
            withTimeout(5000) {
                completeableDeffered.await()
            }
        }


        assertNotNull(response)


        // Context of the app under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("com.ustadmobile.httpoveripc.server.test", appContext.packageName)
    }

    @Test
    fun givenHandlerThrowsException_whenRawRequestMessageSent_thenShouldReplyHttp500() {
        val rawHttp = RawHttp()
        val serviceIntent = Intent(
            ApplicationProvider.getApplicationContext(),
            ThrowExceptionHttpOverIpcServer::class.java
        )

        val binder: IBinder = serviceRule.bindService(serviceIntent)
        val service = Messenger(binder)

        val completeableDeffered = CompletableDeferred<RawHttpResponse<*>>()
        val incomingHandler = object: Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                super.handleMessage(msg)
                val reply = msg.data.getRawHttpResponse(KEY_RESPONSE, rawHttp)
                completeableDeffered.complete(reply)
            }
        }

        val incomingMessenger = Messenger(incomingHandler)

        service.send(Message.obtain(null, HTTP_REQUEST).also {
            it.replyTo = incomingMessenger

            val request = rawHttp.parseRequest(
                "GET /hello HTTP/1.1\r\n" +
                        "Host: headers.jsontest.com\r\n" +
                        "User-Agent: RawHTTP\r\n" +
                        "Accept: application/json")
            it.data.putRawHttpRequest(KEY_REQUEST, request)
        })

        val response = runBlocking {
            withTimeout(5000) {
                completeableDeffered.await()
            }
        }

        assertNotNull(response)
        assertEquals(500, response.statusCode)
    }

}