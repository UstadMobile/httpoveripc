package com.ustadmobile.httpoveripc.core.ext

import java.net.URI

val URI.rawPathAndQuery: String
    get() = if(rawQuery != null) {
        "$rawPath?$rawQuery"
    }else {
        rawPath
    }
