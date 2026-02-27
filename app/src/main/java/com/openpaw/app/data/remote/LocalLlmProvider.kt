package com.openpaw.app.data.remote

import com.openpaw.app.data.remote.dto.ApiMessage
import com.openpaw.app.data.remote.dto.ApiTool
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Stub for a future on-device LLM (Gemini Nano, llama.cpp, etc.).
 *
 * To implement:
 *  - Load the model from assets or internal storage
 *  - Run inference via llama.cpp JNI or MediaPipe LLM Inference API
 *  - Map the output to LlmResponse
 *
 * The interface is identical to AnthropicLlmProvider – just swap the
 * Hilt binding in di/LlmModule.kt when ready.
 */
@Singleton
class LocalLlmProvider @Inject constructor() : LlmProvider {

    override val name = "Local LLM (not yet implemented)"

    override suspend fun complete(
        messages: List<ApiMessage>,
        systemPrompt: String,
        tools: List<ApiTool>
    ): LlmResponse {
        // TODO: Load model, run inference, parse result
        // Suggested libraries:
        //   - Ollama Android: run Ollama server on device via Termux API
        //   - llama.cpp Android bindings: https://github.com/ggerganov/llama.cpp
        //   - MediaPipe LLM Inference API (Gemini Nano on Pixel 8+)
        throw NotImplementedError(
            "Local LLM not yet implemented. Use AnthropicLlmProvider. " +
            "See LocalLlmProvider.kt for implementation hints."
        )
    }
}
