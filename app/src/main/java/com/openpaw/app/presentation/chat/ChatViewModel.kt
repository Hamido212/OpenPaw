package com.openpaw.app.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.openpaw.app.data.local.MessageDao
import com.openpaw.app.data.model.Message
import com.openpaw.app.data.repository.SettingsRepository
import com.openpaw.app.domain.usecase.AgentEvent
import com.openpaw.app.service.OpenPawAccessibilityService
import com.openpaw.app.domain.usecase.AgentUseCase
import com.openpaw.app.presentation.voice.VoiceInputManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val currentToolStatus: String? = null,
    val error: String? = null,
    val sessionId: String = UUID.randomUUID().toString(),
    val isAccessibilityEnabled: Boolean = false,
    val isAgentServiceRunning: Boolean = false
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val agentUseCase: AgentUseCase,
    private val messageDao: MessageDao,
    private val settingsRepository: SettingsRepository,
    val voiceInputManager: VoiceInputManager          // ← public so ChatScreen can observe
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init {
        // Observe messages from DB for current session
        viewModelScope.launch {
            _uiState.collect { state ->
                messageDao.getMessagesForSession(state.sessionId)
                    .collect { messages ->
                        _uiState.update { it.copy(messages = messages) }
                    }
            }
        }
        // Observe accessibility + foreground service state
        viewModelScope.launch {
            OpenPawAccessibilityService.instance.collect { service ->
                _uiState.update { it.copy(isAccessibilityEnabled = service != null) }
            }
        }
        viewModelScope.launch {
            com.openpaw.app.service.AgentForegroundService.isRunning.collect { running ->
                _uiState.update { it.copy(isAgentServiceRunning = running) }
            }
        }
        // Auto-start voice input when triggered from FloatingBubble / QsTile
        viewModelScope.launch {
            voiceInputManager.voiceTrigger.collect { triggeredAt ->
                // Only act on fresh triggers (within last 5 s) to avoid replaying on re-subscribe
                if (System.currentTimeMillis() - triggeredAt < 5_000L) {
                    startVoiceInput()
                }
            }
        }
    }

    // ── Text message ──────────────────────────────────────────────────────────

    fun sendMessage(userInput: String) {
        if (userInput.isBlank() || _uiState.value.isLoading) return

        val sessionId = _uiState.value.sessionId
        _uiState.update { it.copy(isLoading = true, error = null, currentToolStatus = null) }

        viewModelScope.launch {
            // Provider-aware key check: only block if Anthropic selected AND no key entered.
            // Azure / Local providers validate their own credentials internally.
            val selectedProvider = settingsRepository.selectedProvider.first()
            val anthropicKey = settingsRepository.apiKey.first()
            if (selectedProvider == com.openpaw.app.data.remote.LlmProviderType.ANTHROPIC.id
                && anthropicKey.isBlank()
            ) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Kein Anthropic API-Key gesetzt. Bitte in Einstellungen eintragen."
                    )
                }
                return@launch
            }

            agentUseCase.processMessage(userInput, sessionId)
                .collect { event ->
                    when (event) {
                        is AgentEvent.Thinking -> {
                            _uiState.update { it.copy(currentToolStatus = "Thinking...") }
                        }
                        is AgentEvent.ToolCall -> {
                            _uiState.update { it.copy(currentToolStatus = "Using tool: ${event.toolName}...") }
                        }
                        is AgentEvent.ToolResult -> {
                            val status = if (event.success) "✓ ${event.toolName}" else "✗ ${event.toolName}: ${event.output}"
                            _uiState.update { it.copy(currentToolStatus = status) }
                        }
                        is AgentEvent.FinalResponse -> {
                            _uiState.update { it.copy(isLoading = false, currentToolStatus = null) }
                            // Speak the response if TTS is enabled
                            voiceInputManager.speak(event.text)
                        }
                        is AgentEvent.Error -> {
                            _uiState.update {
                                it.copy(isLoading = false, currentToolStatus = null, error = event.message)
                            }
                        }
                    }
                }
        }
    }

    // ── Voice input ───────────────────────────────────────────────────────────

    /** Starts STT – must be called while RECORD_AUDIO permission is granted. */
    fun startVoiceInput() {
        voiceInputManager.startListening { spokenText ->
            sendMessage(spokenText)
        }
    }

    fun stopVoiceInput() {
        voiceInputManager.stopListening()
    }

    fun toggleTts() {
        voiceInputManager.setTtsEnabled(!voiceInputManager.ttsEnabled.value)
    }

    // ── Session ───────────────────────────────────────────────────────────────

    fun startNewSession() {
        _uiState.update {
            ChatUiState(sessionId = UUID.randomUUID().toString())
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
