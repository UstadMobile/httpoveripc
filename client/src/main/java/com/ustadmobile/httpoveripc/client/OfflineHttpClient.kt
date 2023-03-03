package com.ustadmobile.httpoveripc.client

import android.os.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import rawhttp.core.RawHttp
import rawhttp.core.RawHttpRequest
import rawhttp.core.RawHttpResponse
import java.util.concurrent.atomic.AtomicReference

class OfflineHttpClient(
    serverMessenger: Sender,
) {

    private val handlerThread = HandlerThread("background").also {
        if(!it.isAlive)
            it.start()
    }

    fun interface Sender {
        fun sendMessage(message: Message)
    }

    class DefaultSenderService(binder: Binder): Sender {
        private val messenger = Messenger(binder)

        override fun sendMessage(message: Message) {
            messenger.send(message)
        }
    }

    constructor(binder: Binder): this (DefaultSenderService(binder))

    data class PendingRequest(
        val request: RawHttpRequest,
        val completableDeferred: CompletableDeferred<RawHttpResponse<*>>,
    )

    private val coroutineScope = CoroutineScope(Dispatchers.Main + Job())

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

    @Suppress("SpellCheckingInspection")
    suspend fun send(request: RawHttpRequest): RawHttpResponse<*> {
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

    companion object {

        const val HTTP_MSG = 42

        const val KEY_REQUEST = "request"

        const val KEY_RESPONSE = "response"
    }
}