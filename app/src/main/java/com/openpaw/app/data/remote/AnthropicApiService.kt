package com.openpaw.app.data.remote

import com.openpaw.app.data.remote.dto.AnthropicRequest
import com.openpaw.app.data.remote.dto.AnthropicResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface AnthropicApiService {

    @POST("v1/messages")
    suspend fun sendMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String = "2023-06-01",
        @Body request: AnthropicRequest
    ): AnthropicResponse
}
