package com.guang.misty.model

import kotlinx.serialization.Serializable

@Serializable
data class MistyNetworkResponse(
    val statusCode: Int,
    val headers: Map<String, String> = emptyMap(),
    val body: String,
    val error: String? = null
)
