package com.ustadmobile.httpoveripc.client

import rawhttp.core.RawHttpRequest
import rawhttp.core.RawHttpResponse

interface IHttpOverIpcClient {

    suspend fun send(request: RawHttpRequest): RawHttpResponse<*>

}