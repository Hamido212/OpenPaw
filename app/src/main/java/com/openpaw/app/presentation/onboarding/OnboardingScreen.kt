package com.openpaw.app.presentation.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.openpaw.app.data.remote.LlmProviderType

// ─── Available emoji choices for the agent avatar ────────────────────────────
private val EMOJI_OPTIONS = listOf(
    "🐾", "🤖", "🦊", "🐺", "🦁", "🐉", "🦋", "🌟",
    "⚡", "🚀", "🎯", "💡", "🔥", "🌈", "👾", "🎪",
    "🐼", "🦅", "🦄", "🐬", "🧠", "🎭", "🌙", "☀️"
)

private val PERSONALITY_OPTIONS = listOf(
    Triple("freundlich",     "😊", "Freundlich\nWarm & hilfsbereit"),
    Triple("professionell",  "💼", "Professionell\nPräzise & sachlich"),
    Triple("witzig",         "😄", "Witzig\nLocker & humorvoll"),
    Triple("direkt",         "⚡", "Direkt\nKurz & klar")
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val step = viewModel.step

    // Determine slide direction
    var lastStep by remember { mutableStateOf(0) }
    val forward = step >= lastStep
    LaunchedEffect(step) { lastStep = step }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                )
            )
            .systemBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Progress indicator ────────────────────────────────────────────
            if (step > 0) {
                OnboardingProgressBar(
                    current = step,
                    total   = viewModel.totalSteps - 1  // step 0 is welcome, no bar
                )
            }

            // ── Animated step content ─────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        if (forward) {
                            (slideInHorizontally { it } + fadeIn()).togetherWith(
                                slideOutHorizontally { -it } + fadeOut()
                            )
                        } else {
                            (slideInHorizontally { -it } + fadeIn()).togetherWith(
                                slideOutHorizontally { it } + fadeOut()
                            )
                        }
                    },
                    label = "step-content"
                ) { currentStep ->
                    when (currentStep) {
                        0 -> StepWelcome()
                        1 -> StepProvider(viewModel)
                        2 -> StepUserProfile(viewModel)
                        3 -> StepAgentSetup(viewModel)
                        4 -> StepDone(viewModel)
                        else -> Unit
                    }
                }
            }

            // ── Navigation buttons ────────────────────────────────────────────
            OnboardingNavBar(
                step       = step,
                totalSteps = viewModel.totalSteps,
                canProceed = viewModel.canProceed(),
                onBack     = { viewModel.prevStep() },
                onNext     = {
                    if (step < viewModel.totalSteps - 1) viewModel.nextStep()
                    else viewModel.completeOnboarding(onComplete)
                }
            )
        }
    }
}

// ─── Progress bar ────────────────────────────────────────────────────────────
@Composable
private fun OnboardingProgressBar(current: Int, total: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        repeat(total) { index ->
            val filled = index < current
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        if (filled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
            )
        }
    }
}

// ─── Nav buttons bar ─────────────────────────────────────────────────────────
@Composable
private fun OnboardingNavBar(
    step: Int,
    totalSteps: Int,
    canProceed: Boolean,
    onBack: () -> Unit,
    onNext: () -> Unit
) {
    val isLastStep = step == totalSteps - 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back button (hidden on step 0)
        if (step > 0 && !isLastStep) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.width(110.dp)
            ) {
                Text("← Zurück")
            }
        } else {
            Spacer(Modifier.width(110.dp))
        }

        Button(
            onClick = onNext,
            enabled = canProceed,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.width(140.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Text(
                text = when {
                    step == 0           -> "Starten →"
                    isLastStep          -> "🚀 Los geht's!"
                    step == totalSteps - 2 -> "Fertig ✓"
                    else                -> "Weiter →"
                },
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STEP 0 – Welcome
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepWelcome() {
    val bounce = rememberInfiniteTransition(label = "bounce")
    val offsetY by bounce.animateFloat(
        initialValue = 0f,
        targetValue  = -16f,
        animationSpec = infiniteRepeatable(
            tween(900, easing = EaseInOut),
            RepeatMode.Reverse
        ),
        label = "bounce-y"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Bouncing logo
        Text(
            "🐾",
            fontSize = 80.sp,
            modifier = Modifier.offset(y = offsetY.dp)
        )
        Spacer(Modifier.height(32.dp))
        Text(
            "Willkommen!",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Ich bin OpenPaw –\ndein intelligenter KI-Agent\ndirekt auf deinem Android.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 28.sp
        )
        Spacer(Modifier.height(32.dp))

        // Feature pills
        val features = listOf(
            "🎤 Spracheingabe",
            "📱 Apps steuern",
            "💬 WhatsApp & SMS",
            "🧠 Persönlichkeit"
        )
        features.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { feat ->
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Text(
                            feat,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Kurze Einrichtung • ca. 2 Minuten",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STEP 1 – AI Provider
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepProvider(vm: OnboardingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.Start
    ) {
        StepTitle("🤖", "KI-Anbieter", "Wähle deinen KI-Anbieter und trage deinen API-Key ein.")

        // Provider tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(LlmProviderType.ANTHROPIC, LlmProviderType.AZURE).forEach { provider ->
                val selected = vm.selectedProvider == provider.id
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { vm.selectedProvider = provider.id },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Column(
                        Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(if (provider == LlmProviderType.ANTHROPIC) "🧠" else "☁️", fontSize = 28.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            provider.displayName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Fields based on selected provider
        AnimatedContent(
            targetState = vm.selectedProvider,
            label = "provider-fields"
        ) { provider ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (provider) {
                    LlmProviderType.ANTHROPIC.id -> {
                        OnboardingInfoCard("API-Key bei console.anthropic.com erstellen → API Keys → Create Key")
                        PasswordField(
                            value = vm.anthropicKey,
                            onValueChange = { vm.anthropicKey = it },
                            label = "Anthropic API-Key",
                            placeholder = "sk-ant-api03-..."
                        )
                    }
                    LlmProviderType.AZURE.id -> {
                        OnboardingInfoCard("Endpoint + Key findest du im Azure Portal → deine AI Services Ressource → Keys und Endpunkt")
                        OnboardingTextField(
                            value = vm.azureEndpoint,
                            onValueChange = { vm.azureEndpoint = it },
                            label = "Azure Endpoint",
                            placeholder = "https://DEINE-RESSOURCE.services.ai.azure.com"
                        )
                        OnboardingTextField(
                            value = vm.azureDeployment,
                            onValueChange = { vm.azureDeployment = it },
                            label = "Deployment Name",
                            placeholder = "Kimi-K2.5 oder gpt-4o"
                        )
                        PasswordField(
                            value = vm.azureApiKey,
                            onValueChange = { vm.azureApiKey = it },
                            label = "Azure API-Key",
                            placeholder = "Dein Azure API-Key"
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STEP 2 – User profile
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepUserProfile(vm: OnboardingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StepTitle("👤", "Wer bist du?", "Damit ich dich besser kennenlernen kann – alles optional.")

        OnboardingTextField(
            value = vm.userName,
            onValueChange = { vm.userName = it },
            label = "Dein Name",
            placeholder = "z.B. Max"
        )

        Column {
            Text(
                "Erzähl mir etwas über dich",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = vm.userBio,
                onValueChange = { vm.userBio = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 120.dp),
                placeholder = {
                    Text(
                        "z.B. Student in Berlin, 25 Jahre, mag Sport und Technik.\nArbeite als Entwickler, nutze oft WhatsApp und Spotify.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                shape = RoundedCornerShape(14.dp),
                maxLines = 6,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            Text(
                "Diese Info hilft mir, personalisierter zu antworten.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STEP 3 – Agent setup (name + emoji + personality)
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepAgentSetup(vm: OnboardingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        StepTitle("✨", "Dein Agent", "Gib deinem Agenten eine Persönlichkeit.")

        // Agent name
        OnboardingTextField(
            value = vm.agentName,
            onValueChange = { vm.agentName = it },
            label = "Name des Agenten",
            placeholder = "OpenPaw"
        )

        // Emoji picker
        Column {
            Text(
                "Wähle ein Emoji",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            // 6 columns grid
            val rows = EMOJI_OPTIONS.chunked(6)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                rows.forEach { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        row.forEach { emoji ->
                            val selected = emoji == vm.agentEmoji
                            Surface(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable { vm.agentEmoji = emoji },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                else null
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(emoji, fontSize = 22.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Personality selection
        Column {
            Text(
                "Persönlichkeit",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PERSONALITY_OPTIONS.forEach { (id, emoji, label) ->
                    val selected = vm.agentPersonality == id
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { vm.agentPersonality = id },
                        shape = RoundedCornerShape(14.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceVariant,
                        border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        else null
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(emoji, fontSize = 22.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                label.substringBefore("\n"),
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                label.substringAfter("\n"),
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.outline,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// STEP 4 – Done / summary
// ─────────────────────────────────────────────────────────────────────────────
@Composable
private fun StepDone(vm: OnboardingViewModel) {
    val scale = remember { Animatable(0.5f) }
    LaunchedEffect(Unit) {
        scale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated agent avatar
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
                .size(100.dp)
                .scale(scale.value)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(vm.agentEmoji, fontSize = 52.sp)
            }
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "${vm.agentName} ist bereit!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Dein persönlicher KI-Agent wurde eingerichtet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(28.dp))

        // Summary card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SummaryRow("🤖", "Anbieter",
                    if (vm.selectedProvider == "azure") "Azure OpenAI • ${vm.azureDeployment}"
                    else "Anthropic Claude"
                )
                if (vm.userName.isNotBlank()) {
                    SummaryRow("👤", "Name", vm.userName)
                }
                SummaryRow(vm.agentEmoji, "Agent", vm.agentName)
                SummaryRow(
                    "✨", "Persönlichkeit",
                    PERSONALITY_OPTIONS.find { it.first == vm.agentPersonality }?.second + " " +
                    PERSONALITY_OPTIONS.find { it.first == vm.agentPersonality }?.third?.substringBefore("\n")
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Shared components
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun StepTitle(emoji: String, title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(emoji, fontSize = 40.sp)
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun OnboardingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, fontSize = 13.sp) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true
    )
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, fontSize = 13.sp) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        singleLine = true,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = if (visible) "Verbergen" else "Anzeigen"
                )
            }
        }
    )
}

@Composable
private fun OnboardingInfoCard(text: String) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text("ℹ️", fontSize = 14.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun SummaryRow(emoji: String, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 16.sp, modifier = Modifier.width(28.dp))
        Column {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}
