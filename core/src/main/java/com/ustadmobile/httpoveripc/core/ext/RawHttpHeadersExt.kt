package com.ustadmobile.httpoveripc.core.ext

import rawhttp.core.RawHttpHeaders

fun RawHttpHeaders.toSimpleMap(): Map<String, String>{
    return uniqueHeaderNames.associateWith { headerName ->
        get(headerName).first()
    }
}
