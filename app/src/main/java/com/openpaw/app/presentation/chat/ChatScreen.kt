package com.openpaw.app.presentation.chat

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.openpaw.app.data.model.Message
import com.openpaw.app.data.model.MessageRole
import com.openpaw.app.presentation.voice.VoiceInputManager
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState          by viewModel.uiState.collectAsState()
    val sttState         by viewModel.voiceInputManager.sttState.collectAsState()
    val ttsEnabled       by viewModel.voiceInputManager.ttsEnabled.collectAsState()
    val sessionPreviews  by viewModel.sessionPreviews.collectAsState()
    val context          = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var inputText        by remember { mutableStateOf("") }
    var showSessionSheet by remember { mutableStateOf(false) }
    val listState        = rememberLazyListState()

    // Permission launcher for RECORD_AUDIO
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startVoiceInput()
    }

    // Auto-scroll to bottom on new messages
    LaunchedEffect(uiState.messages.size, uiState.isLoading) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size)
        }
    }

    // ── Session history bottom sheet ──────────────────────────────────────────
    if (showSessionSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSessionSheet = false }
        ) {
            SessionHistorySheet(
                sessions         = sessionPreviews,
                currentSessionId = uiState.sessionId,
                onLoadSession    = { sessionId ->
                    viewModel.loadSession(sessionId)
                    showSessionSheet = false
                },
                onDeleteSession  = { sessionId ->
                    viewModel.deleteSession(sessionId)
                }
            )
        }
    }

    // ── Main layout ───────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top App Bar ──────────────────────────────────────────────────
            ModernTopBar(
                isAccessibilityEnabled = uiState.isAccessibilityEnabled,
                ttsEnabled             = ttsEnabled,
                onToggleTts            = { viewModel.toggleTts() },
                onNewChat              = { viewModel.startNewSession() },
                onShowHistory          = { showSessionSheet = true },
                onSettings             = onNavigateToSettings
            )

            // ── Messages area ────────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                if (uiState.messages.isEmpty()) {
                    EmptyStateView(
                        onSuggestionClick = { suggestion ->
                            viewModel.sendMessage(suggestion)
                        }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 12.dp, end = 12.dp,
                            top = 8.dp, bottom = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(uiState.messages, key = { it.id }) { message ->
                            AnimatedMessageBubble(
                                message     = message,
                                onLongClick = {
                                    if (message.role != MessageRole.TOOL) {
                                        clipboardManager.setText(AnnotatedString(message.content))
                                    }
                                }
                            )
                        }
                        if (uiState.isLoading) {
                            item { TypingIndicator() }
                        }
                    }
                }
            }

            // ── Status bar: tool progress or voice state ─────────────────────
            StatusBar(
                sttState          = sttState,
                currentToolStatus = uiState.currentToolStatus,
                error             = uiState.error,
                onDismissError    = { viewModel.clearError() }
            )

            // ── Input bar ────────────────────────────────────────────────────
            ModernInputBar(
                inputText    = inputText,
                onInputChange = { inputText = it },
                onSend        = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText.trim())
                        inputText = ""
                    }
                },
                onMicClick    = {
                    val isListening = sttState == VoiceInputManager.SttState.LISTENING
                    if (isListening) {
                        viewModel.stopVoiceInput()
                    } else {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) viewModel.startVoiceInput()
                        else permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
                isLoading = uiState.isLoading,
                sttState  = sttState
            )
        }
    }
}

// ─── Modern Top Bar ──────────────────────────────────────────────────────────
@Composable
private fun ModernTopBar(
    isAccessibilityEnabled: Boolean,
    ttsEnabled: Boolean,
    onToggleTts: () -> Unit,
    onNewChat: () -> Unit,
    onShowHistory: () -> Unit,
    onSettings: () -> Unit
) {
    Surface(
        modifier      = Modifier.fillMaxWidth(),
        color         = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Logo + title
            Surface(
                shape  = CircleShape,
                color  = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("🐾", fontSize = 20.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "OpenPaw",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(
                                if (isAccessibilityEnabled) Color(0xFF4CAF50)
                                else MaterialTheme.colorScheme.outline
                            )
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (isAccessibilityEnabled) "Screen Control aktiv" else "AI Agent",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isAccessibilityEnabled) Color(0xFF4CAF50)
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            // Action buttons
            IconButton(onClick = onToggleTts) {
                Icon(
                    if (ttsEnabled) Icons.Outlined.VolumeUp else Icons.Outlined.VolumeOff,
                    contentDescription = if (ttsEnabled) "TTS an" else "TTS aus",
                    tint = if (ttsEnabled) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = onShowHistory) {
                Icon(
                    Icons.Outlined.History,
                    contentDescription = "Chat-Verlauf",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = onNewChat) {
                Icon(
                    Icons.Outlined.AddComment,
                    contentDescription = "Neuer Chat",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = onSettings) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "Einstellungen",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// ─── Session History Bottom Sheet ────────────────────────────────────────────

@Composable
private fun SessionHistorySheet(
    sessions: List<Message>,
    currentSessionId: String,
    onLoadSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit
) {
    val dateFmt = remember { SimpleDateFormat("dd.MM. HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text     = "Chat-Verlauf",
            style    = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp)
        )

        if (sessions.isEmpty()) {
            Box(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment    = Alignment.Center
            ) {
                Text(
                    "Noch keine gespeicherten Chats",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            LazyColumn(
                modifier       = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(sessions, key = { it.sessionId }) { session ->
                    SessionItem(
                        preview   = session.content.take(70) +
                                    if (session.content.length > 70) "…" else "",
                        isActive  = session.sessionId == currentSessionId,
                        dateStr   = dateFmt.format(Date(session.timestamp)),
                        onLoad    = { onLoadSession(session.sessionId) },
                        onDelete  = { onDeleteSession(session.sessionId) }
                    )
                    HorizontalDivider(
                        modifier  = Modifier.padding(horizontal = 16.dp),
                        thickness = 0.5.dp,
                        color     = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionItem(
    preview: String,
    isActive: Boolean,
    dateStr: String,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onLoad)
            .background(
                if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                else Color.Transparent
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text  = preview,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text  = dateStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isActive) {
            Spacer(Modifier.width(8.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text     = "aktiv",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
        Spacer(Modifier.width(4.dp))
        IconButton(
            onClick  = onDelete,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Löschen",
                tint     = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ─── Status Bar ──────────────────────────────────────────────────────────────
@Composable
private fun StatusBar(
    sttState: VoiceInputManager.SttState,
    currentToolStatus: String?,
    error: String?,
    onDismissError: () -> Unit
) {
    val statusText = when (sttState) {
        VoiceInputManager.SttState.LISTENING  -> "🎤 Höre zu…"
        VoiceInputManager.SttState.PROCESSING -> "🧠 Verarbeite…"
        VoiceInputManager.SttState.ERROR      -> "⚠️ Sprache nicht erkannt"
        VoiceInputManager.SttState.IDLE       -> currentToolStatus
    }

    AnimatedVisibility(
        visible = statusText != null,
        enter   = expandVertically() + fadeIn(),
        exit    = shrinkVertically() + fadeOut()
    ) {
        statusText?.let { status ->
            Surface(
                color = when (sttState) {
                    VoiceInputManager.SttState.LISTENING  -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    VoiceInputManager.SttState.ERROR      -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                    else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (sttState == VoiceInputManager.SttState.IDLE) {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(12.dp),
                            strokeWidth = 1.5.dp,
                            color       = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        status,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }

    // Error bar
    AnimatedVisibility(
        visible = error != null,
        enter   = expandVertically() + fadeIn(),
        exit    = shrinkVertically() + fadeOut()
    ) {
        error?.let { err ->
            Surface(
                color    = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        err,
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick  = onDismissError,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Schließen",
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Modern Input Bar ────────────────────────────────────────────────────────
@Composable
private fun ModernInputBar(
    inputText: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onMicClick: () -> Unit,
    isLoading: Boolean,
    sttState: VoiceInputManager.SttState
) {
    val isListening = sttState == VoiceInputManager.SttState.LISTENING
    val pulseScale by rememberInfiniteTransition(label = "pulse")
        .animateFloat(
            initialValue = 1f,
            targetValue  = 1.15f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
            label        = "pulse-scale"
        )

    Surface(
        modifier        = Modifier.fillMaxWidth(),
        color           = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Mic button
            Surface(
                shape  = CircleShape,
                color  = if (isListening) MaterialTheme.colorScheme.error
                         else MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier
                    .size(44.dp)
                    .then(if (isListening) Modifier.scale(pulseScale) else Modifier)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication        = null,
                        onClick           = onMicClick
                    )
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isListening) "Stopp" else "Spracheingabe",
                        tint     = if (isListening) MaterialTheme.colorScheme.onError
                                   else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Text input field
            Surface(
                shape  = RoundedCornerShape(24.dp),
                color  = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
            ) {
                Box(
                    modifier         = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (inputText.isEmpty()) {
                        Text(
                            text  = "OpenPaw fragen…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontSize = 15.sp
                        )
                    }
                    BasicTextField(
                        value         = inputText,
                        onValueChange = onInputChange,
                        textStyle     = TextStyle(
                            color    = MaterialTheme.colorScheme.onSurface,
                            fontSize = 15.sp
                        ),
                        modifier   = Modifier.fillMaxWidth(),
                        maxLines   = 5,
                        enabled    = !isLoading && sttState == VoiceInputManager.SttState.IDLE
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            // Send button
            val canSend = inputText.isNotBlank() && !isLoading
            Surface(
                shape  = CircleShape,
                color  = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .size(44.dp)
                    .clickable(enabled = canSend, onClick = onSend)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        Icons.Default.Send,
                        contentDescription = "Senden",
                        tint     = if (canSend) MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ─── Empty state ─────────────────────────────────────────────────────────────

@Composable
private fun EmptyStateView(
    modifier: Modifier = Modifier,
    onSuggestionClick: (String) -> Unit
) {
    Column(
        modifier              = modifier.fillMaxSize(),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.Center
    ) {
        Text("🐾", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text("OpenPaw", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Dein KI-Agent auf Android",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "🎤 Tippe auf das Mikrofon oder schreib etwas",
            fontSize = 13.sp,
            color    = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(24.dp))

        val suggestions = listOf(
            "Lies den Bildschirm vor",
            "WhatsApp an Mama",
            "Alarm 7 Uhr",
            "Spotify öffnen"
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            suggestions.chunked(2).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { suggestion ->
                        SuggestionChip(
                            onClick = { onSuggestionClick(suggestion) },
                            label   = { Text(suggestion, fontSize = 12.sp) }
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

// ─── Animated Message bubble ─────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AnimatedMessageBubble(
    message: Message,
    onLongClick: () -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter   = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(initialAlpha = 0f)
    ) {
        val isUser  = message.role == MessageRole.USER
        val isTool  = message.role == MessageRole.TOOL
        val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
        val timeStr = timeFmt.format(Date(message.timestamp))

        if (isTool) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Surface(
                    shape    = RoundedCornerShape(12.dp),
                    color    = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Build, contentDescription = null,
                            modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            message.content, fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
            return@AnimatedVisibility
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
        ) {
            if (!isUser) {
                Box(
                    modifier         = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) { Text("🐾", fontSize = 16.sp) }
                Spacer(Modifier.width(8.dp))
            }
            Column(
                horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
                modifier            = Modifier.widthIn(max = 280.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(
                        topStart    = if (isUser) 18.dp else 4.dp,
                        topEnd      = if (isUser) 4.dp else 18.dp,
                        bottomStart = 18.dp, bottomEnd = 18.dp
                    ),
                    color    = if (isUser) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.combinedClickable(
                        onClick     = {},
                        onLongClick = onLongClick
                    )
                ) {
                    Text(
                        text     = message.content,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        color    = if (isUser) MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 15.sp
                    )
                }
                Text(
                    timeStr, fontSize = 10.sp, color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}

// ─── Typing Indicator ────────────────────────────────────────────────────────

@Composable
private fun TypingIndicator() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Box(
            modifier         = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) { Text("🐾", fontSize = 16.sp) }
        Spacer(Modifier.width(8.dp))

        Surface(
            shape = RoundedCornerShape(
                topStart = 4.dp, topEnd = 18.dp,
                bottomStart = 18.dp, bottomEnd = 18.dp
            ),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                val transition = rememberInfiniteTransition(label = "dots")
                val alpha by transition.animateFloat(
                    initialValue  = 0.2f,
                    targetValue   = 1f,
                    animationSpec = infiniteRepeatable(
                        animation  = tween(600, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot-alpha"
                )
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha * 0.7f)))
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha * 0.4f)))
            }
        }
    }
}
