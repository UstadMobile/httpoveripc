package com.ustadmobile.httpoveripc.client

import android.os.*
import com.ustadmobile.httpoveripc.core.HttpOverIpcConstants.HTTP_MSG
import com.ustadmobile.httpoveripc.core.HttpOverIpcConstants.KEY_REQUEST
import com.ustadmobile.httpoveripc.core.HttpOverIpcConstants.KEY_RESPONSE
import com.ustadmobile.httpoveripc.core.ext.getRawHttpResponse
import com.ustadmobile.httpoveripc.core.ext.putRawHttpRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import rawhttp.core.RawHttp
import rawhttp.core.RawHttpRequest
import rawhttp.core.RawHttpResponse
import java.util.concurrent.atomic.AtomicReference

/**
 * Client to send HTTP requests and receive a response over an IPC service.
 *
 * Messenger Services does not allow concurrent processing. The client will use a first in first out
 * approach to sending requests.
 */
class HttpOverIpcClient internal constructor(
    serverMessenger: Sender,
) : IHttpOverIpcClient {

    private val handlerThread = HandlerThread("background").also {
        if(!it.isAlive)
            it.start()
    }

    fun interface Sender {
        fun sendMessage(message: Message)
    }

    private class DefaultSenderService(binder: IBinder): Sender {
        private val messenger = Messenger(binder)

        override fun sendMessage(message: Message) {
            messenger.send(message)
        }
    }

    /**
     * Create a new HttpOverIpcClient that will use a service binding.
     *
     * @param binder IBinder that is received from binding to a service (e.g. a service that implements AbstractHttpOverIpcServer)
     */
    constructor(binder: IBinder): this (DefaultSenderService(binder))

    data class PendingRequest(
        val request: RawHttpRequest,
        val completableDeferred: CompletableDeferred<RawHttpResponse<*>>,
    )

    private val coroutineScope = CoroutineScope(Dispatchers.Default + Job())

    private val rawHttp = RawHttp()

    private val currentRequest = AtomicReference<PendingRequest>()

    class IncomingHandler(
        private val rawHttp: RawHttp,
        private val currentRequestAtomic: AtomicReference<PendingRequest>,
        looper: Looper,
    ): Handler(looper) {
        override fun handleMessage(msg: Message) {
            val response = msg.data.getRawHttpResponse(KEY_RESPONSE, rawHttp)
            currentRequestAtomic.get().completableDeferred.complete(response)
        }
    }

    private val pendingRequestList = mutableListOf<PendingRequest>()

    internal val handler = IncomingHandler(rawHttp, currentRequest, handlerThread.looper)

    private val clientMessenger = Messenger(handler)

    private val channel = Channel<PendingRequest>(capacity = Channel.UNLIMITED)

    init {
        coroutineScope.launch {
            for(pendingRequest in channel) {
                val message = Message.obtain(null, HTTP_MSG)

                message.data = Bundle().apply {
                    putRawHttpRequest(KEY_REQUEST, pendingRequest.request)
                }

                message.replyTo = clientMessenger

                currentRequest.set(pendingRequest)
                serverMessenger.sendMessage(message)
                pendingRequest.completableDeferred.await()
            }
        }
    }

    /**
     * Send an http request to the server
     *
     * @param request http request to send
     */
    @Suppress("SpellCheckingInspection")
    override suspend fun send(request: RawHttpRequest): RawHttpResponse<*> {
        val completeable = CompletableDeferred<RawHttpResponse<*>>()
        val pendingRequest = PendingRequest(request, completeable)
        channel.send(pendingRequest)
        val response = completeable.await()
        pendingRequestList.remove(pendingRequest)
        return response
    }

    fun close() {
        channel.close()
        handlerThread.quit()
    }

}