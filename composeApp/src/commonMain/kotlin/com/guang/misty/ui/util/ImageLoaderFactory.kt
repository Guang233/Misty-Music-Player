package com.guang.misty.ui.util

import coil3.ImageLoader
import coil3.PlatformContext

/**
 * 创建平台特定的 ImageLoader
 */
expect fun createImageLoader(context: PlatformContext): ImageLoader
