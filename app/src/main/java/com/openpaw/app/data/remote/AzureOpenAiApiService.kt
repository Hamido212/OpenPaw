package com.openpaw.app.data.remote

import com.openpaw.app.data.remote.dto.AzureChatRequest
import com.openpaw.app.data.remote.dto.AzureChatResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Retrofit service for Azure OpenAI.
 *
 * Uses @Url so the full endpoint URL is passed at call time:
 *   https://{resourceName}.openai.azure.com/openai/deployments/{deployment}/chat/completions?api-version=...
 *
 * Auth: "api-key" header (NOT "Authorization: Bearer ...")
 */
interface AzureOpenAiApiService {

    @POST
    suspend fun chatCompletion(
        @Url url: String,
        @Header("api-key") apiKey: String,
        @Body request: AzureChatRequest
    ): AzureChatResponse
}
