package com.openpaw.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openpaw.app.data.remote.LlmProviderType
import com.openpaw.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    // Provider selection
    val selectedProvider: String = LlmProviderType.ANTHROPIC.id,

    // Anthropic
    val anthropicApiKey: String = "",
    val anthropicModel: String = "claude-haiku-4-5-20251001",

    // Azure OpenAI
    val azureEndpoint: String = "",
    val azureDeploymentName: String = "",
    val azureApiKey: String = "",

    // Save state
    val isSaving: Boolean = false,
    val saveMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                settingsRepository.selectedProvider,
                settingsRepository.apiKey,
                settingsRepository.llmModel,
                settingsRepository.azureEndpoint,
                settingsRepository.azureDeploymentName,
                settingsRepository.azureApiKey
            ) { values ->
                SettingsUiState(
                    selectedProvider    = values[0] as String,
                    anthropicApiKey     = values[1] as String,
                    anthropicModel      = values[2] as String,
                    azureEndpoint       = values[3] as String,
                    azureDeploymentName = values[4] as String,
                    azureApiKey         = values[5] as String
                )
            }.collect { loaded ->
                if (!_uiState.value.isSaving) {
                    _uiState.update { loaded }
                }
            }
        }
    }

    fun setProvider(id: String)          = _uiState.update { it.copy(selectedProvider = id, saveMessage = null) }
    fun setAnthropicKey(v: String)       = _uiState.update { it.copy(anthropicApiKey = v, saveMessage = null) }
    fun setAnthropicModel(v: String)     = _uiState.update { it.copy(anthropicModel = v, saveMessage = null) }
    fun setAzureEndpoint(v: String)      = _uiState.update { it.copy(azureEndpoint = v, saveMessage = null) }
    fun setAzureDeployment(v: String)    = _uiState.update { it.copy(azureDeploymentName = v, saveMessage = null) }
    fun setAzureApiKey(v: String)        = _uiState.update { it.copy(azureApiKey = v, saveMessage = null) }

    fun saveSettings() {
        val s = _uiState.value
        _uiState.update { it.copy(isSaving = true, saveMessage = null) }
        viewModelScope.launch {
            try {
                settingsRepository.setSelectedProvider(s.selectedProvider)
                settingsRepository.setApiKey(s.anthropicApiKey.trim())
                settingsRepository.setLlmModel(s.anthropicModel)
                settingsRepository.setAzureEndpoint(s.azureEndpoint.trim())
                settingsRepository.setAzureDeploymentName(s.azureDeploymentName.trim())
                settingsRepository.setAzureApiKey(s.azureApiKey.trim())
                _uiState.update { it.copy(isSaving = false, saveMessage = "✓ Gespeichert!") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSaving = false, saveMessage = "Fehler: ${e.message}") }
            }
        }
    }
}
