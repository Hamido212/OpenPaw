package com.openpaw.app.data.remote

import com.openpaw.app.data.remote.dto.ApiMessage
import com.openpaw.app.data.remote.dto.ApiTool
import com.openpaw.app.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Runtime-switching LlmProvider.
 *
 * Reads the user's selected provider from [SettingsRepository] on every call,
 * so switching providers in Settings takes effect immediately without a restart.
 *
 * Provider IDs (stored in DataStore):
 *   "anthropic"  → AnthropicLlmProvider  (Claude)
 *   "azure"      → AzureOpenAiLlmProvider
 *   "local"      → LocalLlmProvider       (stub – future)
 */
@Singleton
class DelegatingLlmProvider @Inject constructor(
    private val anthropicProvider: AnthropicLlmProvider,
    private val azureProvider: AzureOpenAiLlmProvider,
    private val localProvider: LocalLlmProvider,
    private val settingsRepository: SettingsRepository
) : LlmProvider {

    override val name: String get() = "DelegatingLlmProvider"

    private suspend fun activeProvider(): LlmProvider =
        when (settingsRepository.selectedProvider.first()) {
            LlmProviderType.AZURE.id -> azureProvider
            LlmProviderType.LOCAL.id -> localProvider
            else -> anthropicProvider   // default: Anthropic
        }

    override suspend fun complete(
        messages: List<ApiMessage>,
        systemPrompt: String,
        tools: List<ApiTool>
    ): LlmResponse = activeProvider().complete(messages, systemPrompt, tools)

    override suspend fun buildContinuationMessages(
        response: LlmResponse,
        toolResults: List<ToolResultEntry>
    ): List<ApiMessage> = activeProvider().buildContinuationMessages(response, toolResults)
}

/** Canonical provider IDs used throughout the app. */
enum class LlmProviderType(val id: String, val displayName: String) {
    ANTHROPIC("anthropic", "Anthropic Claude"),
    AZURE("azure", "Azure OpenAI"),
    LOCAL("local", "Local LLM (coming soon)")
}
