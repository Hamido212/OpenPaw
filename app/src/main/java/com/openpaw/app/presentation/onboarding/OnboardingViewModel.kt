package com.openpaw.app.presentation.onboarding

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openpaw.app.data.remote.LlmProviderType
import com.openpaw.app.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    // ── Navigation state ──────────────────────────────────────────────────────
    var step by mutableStateOf(0)
        private set
    val totalSteps = 5  // 0=Welcome, 1=Provider, 2=User, 3=Agent, 4=Done

    // ── Step 1: AI Provider ────────────────────────────────────────────────────
    var selectedProvider by mutableStateOf(LlmProviderType.ANTHROPIC.id)
    var anthropicKey     by mutableStateOf("")
    var azureEndpoint    by mutableStateOf("")
    var azureDeployment  by mutableStateOf("")
    var azureApiKey      by mutableStateOf("")

    // ── Step 2: User profile ──────────────────────────────────────────────────
    var userName by mutableStateOf("")
    var userBio  by mutableStateOf("")

    // ── Step 3: Agent personality ─────────────────────────────────────────────
    var agentName        by mutableStateOf("OpenPaw")
    var agentEmoji       by mutableStateOf("🐾")
    var agentPersonality by mutableStateOf("freundlich")

    // ── Navigation ─────────────────────────────────────────────────────────────
    fun nextStep() { if (step < totalSteps - 1) step++ }
    fun prevStep() { if (step > 0) step-- }

    /** Whether the "Weiter" button on the current step should be enabled. */
    fun canProceed(): Boolean = when (step) {
        1 -> when (selectedProvider) {
            LlmProviderType.ANTHROPIC.id -> anthropicKey.isNotBlank()
            LlmProviderType.AZURE.id     -> azureEndpoint.isNotBlank() &&
                                            azureDeployment.isNotBlank() &&
                                            azureApiKey.isNotBlank()
            else                         -> true
        }
        else -> true
    }

    // ── Finish ─────────────────────────────────────────────────────────────────
    fun completeOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.setSelectedProvider(selectedProvider)

            if (anthropicKey.isNotBlank())  settingsRepository.setApiKey(anthropicKey)
            if (azureEndpoint.isNotBlank()) settingsRepository.setAzureEndpoint(azureEndpoint)
            if (azureDeployment.isNotBlank()) settingsRepository.setAzureDeploymentName(azureDeployment)
            if (azureApiKey.isNotBlank())   settingsRepository.setAzureApiKey(azureApiKey)

            settingsRepository.setUserName(userName.trim())
            settingsRepository.setUserBio(userBio.trim())
            settingsRepository.setAgentName(agentName.trim().ifBlank { "OpenPaw" })
            settingsRepository.setAgentEmoji(agentEmoji)
            settingsRepository.setAgentPersonality(agentPersonality)

            settingsRepository.setOnboardingDone(true)
            onDone()
        }
    }
}
