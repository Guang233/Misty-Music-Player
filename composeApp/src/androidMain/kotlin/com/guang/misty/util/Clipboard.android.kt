package com.guang.misty.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import com.guang.misty.MistyApplication

/**
 * Android 剪贴板实现
 */
actual fun copyToClipboard(text: String) {
    val context = MistyApplication.instance
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Misty Log", text)
    clipboard.setPrimaryClip(clip)
}
