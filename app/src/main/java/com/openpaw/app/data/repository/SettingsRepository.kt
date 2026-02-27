package com.openpaw.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.openpaw.app.BuildConfig
import com.openpaw.app.data.remote.LlmProviderType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // ─── Preference keys ─────────────────────────────────────────────────────

    private val KEY_ANTHROPIC_API_KEY    = stringPreferencesKey("anthropic_api_key")
    private val KEY_LLM_MODEL            = stringPreferencesKey("llm_model")
    private val KEY_SELECTED_PROVIDER    = stringPreferencesKey("selected_provider")

    // Azure OpenAI
    private val KEY_AZURE_ENDPOINT       = stringPreferencesKey("azure_endpoint")
    private val KEY_AZURE_DEPLOYMENT     = stringPreferencesKey("azure_deployment_name")
    private val KEY_AZURE_API_KEY        = stringPreferencesKey("azure_api_key")

    // ── Onboarding ────────────────────────────────────────────────────────────
    private val KEY_ONBOARDING_DONE      = booleanPreferencesKey("onboarding_done")

    // ── User profile ──────────────────────────────────────────────────────────
    private val KEY_USER_NAME            = stringPreferencesKey("user_name")
    private val KEY_USER_BIO             = stringPreferencesKey("user_bio")

    // ── Agent personality ─────────────────────────────────────────────────────
    private val KEY_AGENT_NAME           = stringPreferencesKey("agent_name")
    private val KEY_AGENT_EMOJI          = stringPreferencesKey("agent_emoji")
    /** One of: "freundlich" | "professionell" | "witzig" | "direkt" */
    private val KEY_AGENT_PERSONALITY    = stringPreferencesKey("agent_personality")

    // ─── Flows ───────────────────────────────────────────────────────────────

    /** Which LLM provider is active: "anthropic" | "azure" | "local" */
    val selectedProvider: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_SELECTED_PROVIDER] ?: LlmProviderType.ANTHROPIC.id
    }

    // Anthropic
    val apiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_ANTHROPIC_API_KEY] ?: BuildConfig.ANTHROPIC_API_KEY
    }
    val llmModel: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_LLM_MODEL] ?: "claude-haiku-4-5-20251001"
    }

    // Azure OpenAI
    val azureEndpoint: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_AZURE_ENDPOINT] ?: ""
    }
    val azureDeploymentName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_AZURE_DEPLOYMENT] ?: ""
    }
    val azureApiKey: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_AZURE_API_KEY] ?: ""
    }

    // Onboarding
    val isOnboardingDone: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_DONE] ?: false
    }

    // User profile
    val userName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_NAME] ?: ""
    }
    val userBio: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_BIO] ?: ""
    }

    // Agent personality
    val agentName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_AGENT_NAME] ?: "OpenPaw"
    }
    val agentEmoji: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_AGENT_EMOJI] ?: "🐾"
    }
    val agentPersonality: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_AGENT_PERSONALITY] ?: "freundlich"
    }

    // ─── Setters ─────────────────────────────────────────────────────────────

    suspend fun setSelectedProvider(provider: String) {
        context.dataStore.edit { it[KEY_SELECTED_PROVIDER] = provider }
    }

    // Anthropic
    suspend fun setApiKey(key: String) {
        context.dataStore.edit { it[KEY_ANTHROPIC_API_KEY] = key }
    }
    suspend fun setLlmModel(model: String) {
        context.dataStore.edit { it[KEY_LLM_MODEL] = model }
    }

    // Azure
    suspend fun setAzureEndpoint(endpoint: String) {
        context.dataStore.edit { it[KEY_AZURE_ENDPOINT] = endpoint }
    }
    suspend fun setAzureDeploymentName(name: String) {
        context.dataStore.edit { it[KEY_AZURE_DEPLOYMENT] = name }
    }
    suspend fun setAzureApiKey(key: String) {
        context.dataStore.edit { it[KEY_AZURE_API_KEY] = key }
    }

    // Onboarding
    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = done }
    }

    // User profile
    suspend fun setUserName(name: String) {
        context.dataStore.edit { it[KEY_USER_NAME] = name }
    }
    suspend fun setUserBio(bio: String) {
        context.dataStore.edit { it[KEY_USER_BIO] = bio }
    }

    // Agent personality
    suspend fun setAgentName(name: String) {
        context.dataStore.edit { it[KEY_AGENT_NAME] = name }
    }
    suspend fun setAgentEmoji(emoji: String) {
        context.dataStore.edit { it[KEY_AGENT_EMOJI] = emoji }
    }
    suspend fun setAgentPersonality(personality: String) {
        context.dataStore.edit { it[KEY_AGENT_PERSONALITY] = personality }
    }
}
