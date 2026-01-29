package com.guang.misty.util

/**
 * 打开文件夹（仅桌面端有效）
 */
expect fun openFolder(path: String)

/**
 * 分享文件（仅移动端有效）
 * @param filePath 要分享的文件路径
 * @param mimeType 文件的 MIME 类型
 */
expect fun shareFile(filePath: String, mimeType: String)

/**
 * 是否是桌面平台
 */
expect val isDesktopPlatform: Boolean
