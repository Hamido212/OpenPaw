package com.openpaw.app.presentation.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openpaw.app.data.remote.LlmProviderType
import com.openpaw.app.service.AgentForegroundService
import com.openpaw.app.service.FloatingBubbleService
import com.openpaw.app.service.OpenPawAccessibilityService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val a11yActive     by OpenPawAccessibilityService.instance.collectAsState()
    val serviceRunning by AgentForegroundService.isRunning.collectAsState()
    val bubbleRunning  by FloatingBubbleService.isRunning.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Einstellungen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── 1. LLM Provider Selector ─────────────────────────────────────
            SettingsSection(title = "KI-Anbieter", icon = Icons.Default.SmartToy) {
                Text(
                    "Wähle welchen LLM-Anbieter OpenPaw verwenden soll:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                LlmProviderType.entries.forEach { provider ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = uiState.selectedProvider == provider.id,
                            onClick = { viewModel.setProvider(provider.id) },
                            enabled = provider != LlmProviderType.LOCAL
                        )
                        Column {
                            Text(
                                provider.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (provider == LlmProviderType.LOCAL)
                                    MaterialTheme.colorScheme.outline
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            if (provider == LlmProviderType.LOCAL) {
                                Text(
                                    "Noch nicht verfügbar (bald: Gemini Nano / llama.cpp)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            }

            // ── 2. Anthropic Config (sichtbar wenn Anthropic gewählt) ─────────
            AnimatedVisibility(visible = uiState.selectedProvider == LlmProviderType.ANTHROPIC.id) {
                SettingsSection(title = "Anthropic Claude", icon = Icons.Default.VpnKey) {
                    PasswordField(
                        label = "API Key",
                        placeholder = "sk-ant-api03-...",
                        value = uiState.anthropicApiKey,
                        onValueChange = { viewModel.setAnthropicKey(it) },
                        supportingText = "console.anthropic.com → API Keys"
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("Modell", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    listOf(
                        "claude-haiku-4-5-20251001" to "Haiku 4.5 — schnell & günstig",
                        "claude-sonnet-4-6"         to "Sonnet 4.6 — ausgewogen",
                        "claude-opus-4-6"           to "Opus 4.6 — leistungsstärkst"
                    ).forEach { (id, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = uiState.anthropicModel == id,
                                onClick = { viewModel.setAnthropicModel(id) }
                            )
                            Column {
                                Text(label, style = MaterialTheme.typography.bodyMedium)
                                Text(id, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                    }
                }
            }

            // ── 3. Azure OpenAI Config ────────────────────────────────────────
            AnimatedVisibility(visible = uiState.selectedProvider == LlmProviderType.AZURE.id) {
                SettingsSection(title = "Azure OpenAI Service", icon = Icons.Default.Cloud) {

                    // Visual hint of the resulting URL
                    if (uiState.azureEndpoint.isNotBlank() && uiState.azureDeploymentName.isNotBlank()) {
                        val previewUrl = "${uiState.azureEndpoint.trimEnd('/')}/openai/deployments/${uiState.azureDeploymentName}/chat/completions"
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text(
                                previewUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(8.dp),
                                fontSize = 10.sp
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = uiState.azureEndpoint,
                        onValueChange = { viewModel.setAzureEndpoint(it) },
                        label = { Text("Azure Endpoint") },
                        placeholder = { Text("https://moltiboy.services.ai.azure.com") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = { Text("AI Foundry: *.services.ai.azure.com  |  Classic: *.openai.azure.com") }
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.azureDeploymentName,
                        onValueChange = { viewModel.setAzureDeployment(it) },
                        label = { Text("Deployment / Model Name") },
                        placeholder = { Text("Kimi-K2.5  /  gpt-4o  /  gpt-4") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = { Text("Azure Portal → Modellbereitstellungen → Name der Bereitstellung") }
                    )

                    Spacer(Modifier.height(8.dp))

                    PasswordField(
                        label = "Azure API Key",
                        placeholder = "abc123... (32+ Zeichen)",
                        value = uiState.azureApiKey,
                        onValueChange = { viewModel.setAzureApiKey(it) },
                        supportingText = "Azure Portal → deine Ressource → Schlüssel und Endpunkt"
                    )

                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Auth: api-key Header (kein Bearer Token)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            // ── 4. Save button ────────────────────────────────────────────────
            Button(
                onClick = { viewModel.saveSettings() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isSaving
            ) {
                if (uiState.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Einstellungen speichern")
                }
            }
            uiState.saveMessage?.let { msg ->
                Text(
                    msg,
                    color = if (msg.startsWith("✓"))
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            // ── 5. Accessibility Service ──────────────────────────────────────
            SettingsSection(title = "Bildschirm-Steuerung", icon = Icons.Default.Accessibility) {
                StatusRow(
                    active = a11yActive != null,
                    activeText = "✓ Accessibility Service aktiv",
                    inactiveText = "Nicht aktiviert"
                )
                Text(
                    "Erlaubt OpenPaw den Bildschirm zu lesen, Buttons zu klicken und Text einzugeben.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = if (a11yActive != null)
                        ButtonDefaults.outlinedButtonColors()
                    else
                        ButtonDefaults.buttonColors()
                ) {
                    Icon(Icons.Default.OpenInNew, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (a11yActive != null) "In Systemeinstellungen verwalten" else "In Systemeinstellungen aktivieren")
                }
                if (a11yActive == null) {
                    Text(
                        "Tippe → 'OpenPaw Agent' suchen → einschalten",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // ── 6. Background Service ─────────────────────────────────────────
            SettingsSection(title = "Hintergrund-Agent", icon = Icons.Default.NotificationsActive) {
                StatusRow(
                    active = serviceRunning,
                    activeText = "✓ Agent läuft im Hintergrund",
                    inactiveText = "Agent gestoppt"
                )
                Text(
                    "Hält OpenPaw aktiv auch wenn die App minimiert ist.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                if (!serviceRunning) {
                    Button(
                        onClick = { AgentForegroundService.start(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Agent starten")
                    }
                } else {
                    OutlinedButton(
                        onClick = { AgentForegroundService.stop(context) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Stop, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Agent stoppen")
                    }
                }
            }

            // ── 7. Floating Bubble ────────────────────────────────────────────
            SettingsSection(title = "Floating Bubble", icon = Icons.Default.Lens) {
                val canOverlay = remember { Settings.canDrawOverlays(context) }
                if (!canOverlay) {
                    StatusRow(active = false, activeText = "",
                        inactiveText = "Berechtigung fehlt: 'Über anderen Apps anzeigen'")
                    Text(
                        "Nötig damit die 🐾-Blase über anderen Apps schwebt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            context.startActivity(Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            ))
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Berechtigung erteilen")
                    }
                } else {
                    StatusRow(active = bubbleRunning,
                        activeText = "✓ Floating Bubble aktiv",
                        inactiveText = "Floating Bubble gestoppt")
                    Text(
                        "Eine schwebende 🐾-Schaltfläche über allen Apps. Antippen startet sofort die Spracheingabe – auch wenn eine andere App offen ist.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    if (!bubbleRunning) {
                        Button(onClick = { FloatingBubbleService.start(context) },
                            modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Bubble starten")
                        }
                    } else {
                        OutlinedButton(onClick = { FloatingBubbleService.stop(context) },
                            modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Bubble stoppen")
                        }
                    }
                }
            }

            // ── 8. Quick Settings Tile ────────────────────────────────────────
            SettingsSection(title = "Schnelleinstellungen-Tile", icon = Icons.Default.Notifications) {
                Text(
                    "Das 🐾 OpenPaw-Tile im Benachrichtigungsmenü – einmal tippen startet sofort die Spracheingabe.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Surface(color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small) {
                    Text(
                        "So hinzufügen:\n1. Benachrichtigungsmenü runterziehen\n2. Stift-Symbol (Bearbeiten) tippen\n3. 'OpenPaw' in aktive Tiles ziehen",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            // ── 9. About ──────────────────────────────────────────────────────
            SettingsSection(title = "Über OpenPaw", icon = Icons.Default.Info) {
                Text("OpenPaw v1.0.0", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    "KI-Agent für Android. Unterstützt Anthropic Claude und Azure OpenAI.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ─── Reusable composables ────────────────────────────────────────────────────

@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun StatusRow(active: Boolean, activeText: String, inactiveText: String) {
    Text(
        if (active) activeText else inactiveText,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium,
        color = if (active) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
    )
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun PasswordField(
    label: String,
    placeholder: String,
    value: String,
    onValueChange: (String) -> Unit,
    supportingText: String
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Verstecken" else "Anzeigen"
                )
            }
        },
        supportingText = { Text(supportingText, fontSize = 11.sp) }
    )
}
