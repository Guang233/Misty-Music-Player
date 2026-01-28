package com.guang.misty.util

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * JVM (Desktop) 剪贴板实现
 */
actual fun copyToClipboard(text: String) {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    val selection = StringSelection(text)
    clipboard.setContents(selection, selection)
}
