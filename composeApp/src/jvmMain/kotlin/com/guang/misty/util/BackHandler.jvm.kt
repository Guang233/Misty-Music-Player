package com.guang.misty.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import java.awt.KeyEventDispatcher
import java.awt.KeyboardFocusManager
import java.awt.event.KeyEvent

/**
 * BackHandler 栈管理器
 * 
 * 确保 ESC 键只触发最内层（最后注册）的 BackHandler
 */
private object BackHandlerStack {
    private val handlers = mutableListOf<() -> Unit>()
    private var dispatcher: KeyEventDispatcher? = null
    
    @Synchronized
    fun push(handler: () -> Unit) {
        handlers.add(handler)
        ensureDispatcherRegistered()
    }
    
    @Synchronized
    fun remove(handler: () -> Unit) {
        handlers.remove(handler)
        if (handlers.isEmpty()) {
            unregisterDispatcher()
        }
    }
    
    private fun ensureDispatcherRegistered() {
        if (dispatcher == null) {
            dispatcher = KeyEventDispatcher { event: KeyEvent ->
                if (event.id == KeyEvent.KEY_PRESSED && event.keyCode == KeyEvent.VK_ESCAPE) {
                    synchronized(this) {
                        // 只触发栈顶（最内层）的 handler
                        handlers.lastOrNull()?.invoke()
                    }
                    true // 消费事件
                } else {
                    false
                }
            }
            KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(dispatcher)
        }
    }
    
    private fun unregisterDispatcher() {
        dispatcher?.let {
            KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(it)
        }
        dispatcher = null
    }
}

/**
 * Desktop/JVM 实现：监听 ESC 键作为返回
 * 
 * 使用栈管理确保只有最内层的 BackHandler 被触发
 */
@Composable
actual fun BackHandler(enabled: Boolean, onBack: () -> Unit) {
    // 使用 remember 确保 handler 引用稳定
    val handler = remember(onBack) { { onBack() } }
    
    DisposableEffect(enabled, handler) {
        if (enabled) {
            BackHandlerStack.push(handler)
        }
        
        onDispose {
            if (enabled) {
                BackHandlerStack.remove(handler)
            }
        }
    }
}
