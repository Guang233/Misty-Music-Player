package com.guang.misty.util

import android.util.Log

actual fun mistyDebugLog(jsonLine: String) {
    Log.d("MistyDebug", jsonLine)
}
