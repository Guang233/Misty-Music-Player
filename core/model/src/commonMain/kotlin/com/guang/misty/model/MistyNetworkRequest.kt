package com.guang.misty.model

import kotlinx.serialization.Serializable

@Serializable
data class MistyNetworkRequest(
    val url: String,
    val method: String = "GET", // "GET" or "POST"
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
    val bodyEncoding: String = "string", // "string", "hex", or "base64"
    val responseEncoding: String = "string" // "string", "hex", or "base64"
)
