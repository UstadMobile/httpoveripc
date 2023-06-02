package com.ustadmobile.httpoveripc.server

import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import com.ustadmobile.httpoveripc.core.HttpOverIpcConstants.HTTP_RESPONSE
import com.ustadmobile.httpoveripc.core.HttpOverIpcConstants.KEY_REQUEST
import com.ustadmobile.httpoveripc.core.HttpOverIpcConstants.KEY_RESPONSE
import com.ustadmobile.httpoveripc.core.ext.getRawHttpRequest
import com.ustadmobile.httpoveripc.core.ext.putRawHttpResponse
import rawhttp.core.RawHttp
import rawhttp.core.RawHttpRequest
import rawhttp.core.RawHttpResponse
import rawhttp.core.body.StringBody

/**
 * Abstract class for use implementing a server for HTTP requests coming in over IPC. These could be
 * passed to an embedded server or processed any other way.
 *
 * See
 * https://android.googlesource.com/platform/development/+/master/samples/ApiDemos/src/com/example/android/apis/app/MessengerService.java
 */
abstract class AbstractHttpOverIpcServer: Service() {

    private val rawHttp by lazy {  RawHttp() }

    private val handlerThread = HandlerThread("HttpServiceHandler").also {
        if(!it.isAlive)
            it.start()
    }

    private inner class IncomingHandler(
        looper: Looper,
    ) : Handler(looper) {
        override fun handleMessage(msg: Message) {
            var requestUri: String? = null
            val response = try {
                val incomingRequest = msg.data.getRawHttpRequest(KEY_REQUEST, rawHttp)
                requestUri = incomingRequest.uri.toString()
                handleRequest(incomingRequest)
            }catch(e: Exception) {
                rawHttp.parseResponse("HTTP/1.1 500 Internal Server Error\n" +
                        "Content-Type: text/plain\n" +
                        "\n"
                ).withBody(StringBody(e.stackTraceToString())).also {
                    Log.e("HttpOverIpcServer", "HttpOverIpc: uri=$requestUri exception: $e")
                }
            }
            val replyMessage = Message.obtain(this@IncomingHandler, HTTP_RESPONSE)
            replyMessage.data.putRawHttpResponse(KEY_RESPONSE, response)

            try {
                msg.replyTo.send(replyMessage)
            }catch(e: RemoteException) {
                Log.e("HttpOverIpcServer", "HttpOverIpc: could not send ReplyMessage to remote: $e")
                e.printStackTrace()
            }
        }
    }

    private val mMessenger = Messenger(IncomingHandler(handlerThread.looper))

    /**
     * Process an incoming http request and provide a response.
     */
    abstract fun handleRequest(request: RawHttpRequest): RawHttpResponse<*>

    override fun onBind(intent: Intent?): IBinder {
        return mMessenger.binder
    }

    override fun onDestroy() {
        super.onDestroy()

        handlerThread.quit()
    }
}