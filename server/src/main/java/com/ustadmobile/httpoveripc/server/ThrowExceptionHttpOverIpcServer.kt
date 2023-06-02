package com.ustadmobile.httpoveripc.server

import rawhttp.core.RawHttpRequest
import rawhttp.core.RawHttpResponse
import java.io.IOException

/**
 * Used for testing purposes
 */
class ThrowExceptionHttpOverIpcServer: AbstractHttpOverIpcServer() {

    override fun handleRequest(request: RawHttpRequest): RawHttpResponse<*> {
        throw IOException("Fail!")
    }
}