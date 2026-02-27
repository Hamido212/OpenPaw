package com.openpaw.app.data.remote

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.openpaw.app.data.remote.dto.*
import com.openpaw.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LlmProvider backed by Azure.
 *
 * Supports two endpoint formats – detected automatically from the endpoint URL:
 *
 * ① Classic Azure OpenAI  (*.openai.azure.com)
 *    URL : {endpoint}/openai/deployments/{deployment}/chat/completions?api-version=2024-02-15-preview
 *    Body: no "model" field (model is baked into the deployment URL)
 *
 * ② Azure AI Foundry  (*.services.ai.azure.com) ← modern, recommended
 *    URL : {endpoint}/models/chat/completions?api-version=2024-05-01-preview
 *    Body: "model" = deployment name
 *
 * Auth for both: "api-key" header (NOT "Authorization: Bearer …")
 * Tool arguments come back as a JSON **string** that must be deserialized.
 */
@Singleton
class AzureOpenAiLlmProvider @Inject constructor(
    private val apiService: AzureOpenAiApiService,
    private val settingsRepository: SettingsRepository,
    private val gson: Gson
) : LlmProvider {

    override val name = "Azure OpenAI"

    override suspend fun complete(
        messages: List<com.openpaw.app.data.remote.dto.ApiMessage>,
        systemPrompt: String,
        tools: List<com.openpaw.app.data.remote.dto.ApiTool>
    ): LlmResponse {

        val endpoint   = settingsRepository.azureEndpoint.first().trimEnd('/')
        val deployment = settingsRepository.azureDeploymentName.first().trim()
        val apiKey     = settingsRepository.azureApiKey.first().trim()

        require(endpoint.isNotBlank())   { "Azure-Endpoint nicht gesetzt. Bitte in den Einstellungen eintragen." }
        require(deployment.isNotBlank()) { "Azure Deployment-Name nicht gesetzt. Bitte in den Einstellungen eintragen." }
        require(apiKey.isNotBlank())     { "Azure API-Key nicht gesetzt. Bitte in den Einstellungen eintragen." }

        // ── Auto-detect endpoint type ─────────────────────────────────────────
        val isFoundry = endpoint.contains("services.ai.azure.com", ignoreCase = true)

        val url = if (isFoundry) {
            // Azure AI Foundry / AI Services — model in body, no deployment in URL
            "$endpoint/models/chat/completions?api-version=2024-05-01-preview"
        } else {
            // Classic Azure OpenAI — deployment in URL, no model in body
            "$endpoint/openai/deployments/$deployment/chat/completions?api-version=2024-02-15-preview"
        }

        // Build messages list: prepend system message
        val azureMessages = mutableListOf<AzureChatMessage>()
        if (systemPrompt.isNotBlank()) {
            azureMessages += AzureChatMessage(role = "system", content = systemPrompt)
        }
        azureMessages += messages.map { msg ->
            AzureChatMessage(
                role = msg.role,
                content = msg.content as? String ?: msg.content.toString()
            )
        }

        // Convert Anthropic-style tools → OpenAI function-calling format
        val azureTools = tools.map { tool ->
            AzureTool(
                function = AzureFunction(
                    name = tool.name,
                    description = tool.description,
                    parameters = AzureFunctionParameters(
                        properties = tool.inputSchema.properties.mapValues { (_, prop) ->
                            AzureFunctionProperty(type = prop.type, description = prop.description)
                        },
                        required = tool.inputSchema.required
                    )
                )
            )
        }

        val request = AzureChatRequest(
            // Foundry requires the model name in the body; Classic bakes it into the URL
            model    = if (isFoundry) deployment else null,
            messages = azureMessages,
            tools    = azureTools.ifEmpty { null }
        )

        val response = apiService.chatCompletion(
            url = url,
            apiKey = apiKey,
            request = request
        )

        val choice = response.choices.firstOrNull()
            ?: return LlmResponse(textContent = null, stopReason = "no_choices")

        val textContent = choice.message.content?.takeIf { it.isNotBlank() }

        // Parse tool calls – arguments is a JSON *string* that needs to be deserialized
        val toolCalls = choice.message.toolCalls?.map { azureCall ->
            val inputMap: Map<String, Any> = try {
                val type = object : TypeToken<Map<String, Any>>() {}.type
                gson.fromJson(azureCall.function.arguments, type) ?: emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }
            ToolCallRequest(
                id = azureCall.id,
                toolName = azureCall.function.name,
                input = inputMap
            )
        } ?: emptyList()

        return LlmResponse(
            textContent = textContent,
            toolCalls = toolCalls,
            stopReason = choice.finishReason
        )
    }
}
