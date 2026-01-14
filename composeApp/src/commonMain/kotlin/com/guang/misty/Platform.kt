package com.guang.misty

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform