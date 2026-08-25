/*
THESIS: Recipe creation is a proofing conversation, refusing the generic full-screen chatbot and hidden generated output.
OWN-WORLD: Deep enamel chat, cream recipe proof, yellow verification marks, and decisive orange review controls extend MealOS's mise-en-place station.
STORY: Describe or scan a recipe, resolve uncertainties in conversation, then inspect a structured draft before it can enter the editor.
FIRST VIEWPORT: Unfolded, equal chat and proof panes meet at the hinge; compact screens stay conversation-first and surface a draft review card inline.
FORM: Proofing Table, selected grounded direction from seed e63896b6, revised with a high-contrast material boundary between panes.
FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
*/
package com.mealos.ui

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import com.mealos.model.ChatRole
import com.mealos.model.Recipe
import com.mealos.model.RecipeAssistantState
import com.mealos.model.RecipeChatMessage
import com.mealos.ui.theme.ActionOrange
import com.mealos.ui.theme.Cream
import com.mealos.ui.theme.Enamel
import com.mealos.ui.theme.RaisedEnamel
import com.mealos.ui.theme.TimerYellow
import com.mealos.ui.theme.WarmLine

@Composable
fun RecipeAssistantScreen(
    state: RecipeAssistantState,
    fold: FoldingFeature?,
    onScan: () -> Unit,
    onSend: (String) -> Unit,
    onRetry: () -> Unit,
    onClear: () -> Unit,
    onReviewDraft: () -> Unit,
    signedIn: Boolean,
    onSignIn: () -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val twoPane = maxWidth >= 720.dp
        if (twoPane) {
            Row(Modifier.fillMaxSize().background(Enamel)) {
                ChatPane(
                    state = state,
                    onScan = onScan,
                    onSend = onSend,
                    onRetry = onRetry,
                    onClear = onClear,
                    compactDraft = false,
                    onReviewDraft = onReviewDraft,
                    signedIn = signedIn,
                    onSignIn = onSignIn,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
                val hinge = if (fold?.isSeparating == true) 8.dp else 2.dp
                Spacer(Modifier.width(hinge).fillMaxHeight().background(Color.Black.copy(alpha = .38f)))
                RecipeProof(
                    state = state,
                    onReviewDraft = onReviewDraft,
                    modifier = Modifier.weight(1f).fillMaxHeight()
                )
            }
        } else {
            ChatPane(
                state = state,
                onScan = onScan,
                onSend = onSend,
                onRetry = onRetry,
                onClear = onClear,
                compactDraft = true,
                onReviewDraft = onReviewDraft,
                signedIn = signedIn,
                onSignIn = onSignIn,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun ChatPane(
    state: RecipeAssistantState,
    onScan: () -> Unit,
    onSend: (String) -> Unit,
    onRetry: () -> Unit,
    onClear: () -> Unit,
    compactDraft: Boolean,
    onReviewDraft: () -> Unit,
    signedIn: Boolean,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    val canSend = input.isNotBlank() && !state.sending && signedIn
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size, state.sending, state.draft?.id) {
        val extras = if (compactDraft && state.draft != null) 1 else 0
        if (state.messages.isNotEmpty()) listState.animateScrollToItem((state.messages.size + extras - 1).coerceAtLeast(0))
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Start a new recipe chat?") },
            text = { Text("This clears the conversation and its current draft from this device.") },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Keep chat") } },
            confirmButton = { TextButton(onClick = { confirmClear = false; onClear() }) { Text("Clear and start over") } }
        )
    }
    Column(modifier.background(Enamel).navigationBarsPadding().imePadding()) {
        if (state.messages.isEmpty()) {
            Column(
                Modifier.weight(1f).fillMaxWidth().padding(28.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text("Build from a conversation.", style = MaterialTheme.typography.displaySmall, color = Cream)
                Spacer(Modifier.height(10.dp))
                Text(
                    "Describe a meal, paste a recipe, or scan printed pages. MealOS will keep the draft separate until you review it.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Cream.copy(alpha = .78f)
                )
                Spacer(Modifier.height(24.dp))
                OutlinedButton(onClick = onScan, enabled = !state.scanning, border = BorderStroke(1.dp, TimerYellow)) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = TimerYellow)
                    Spacer(Modifier.width(8.dp))
                    Text(if (state.scanning) "Opening scanner…" else "Scan or add photos", color = Cream)
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                items(state.messages, key = { it.id }) { message -> ChatTurn(message) }
                if (state.sending) item { WorkingTurn() }
                if (compactDraft && state.draft != null) item { CompactDraft(state.draft, state.questions.size, onReviewDraft) }
            }
        }

        state.error?.let { error ->
            Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(error, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer)
                    TextButton(onClick = onRetry) { Icon(Icons.Default.Refresh, null); Text("Retry") }
                }
            }
        }
        if (!signedIn) {
            Surface(color = RaisedEnamel, contentColor = Cream, modifier = Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CloudOff, contentDescription = null, tint = TimerYellow)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Sign in to ask MealOS. Planning and saved recipes still work offline.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onSignIn) { Text("Sign in", color = TimerYellow) }
                }
            }
        }
        ProofStrip(state)
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            IconButton(onClick = onScan, enabled = !state.scanning && !state.sending) {
                if (state.scanning) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = TimerYellow)
                else Icon(Icons.Default.CameraAlt, contentDescription = "Scan recipe", tint = Cream)
            }
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = { Text("Message MealOS…") },
                enabled = !state.sending,
                maxLines = 4,
                modifier = Modifier.weight(1f),
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Cream,
                    unfocusedContainerColor = Cream,
                    focusedTextColor = Enamel,
                    unfocusedTextColor = Enamel,
                    focusedBorderColor = TimerYellow,
                    unfocusedBorderColor = WarmLine
                )
            )
            IconButton(
                onClick = { onSend(input); input = "" },
                enabled = canSend,
                modifier = Modifier.size(52.dp).background(if (canSend) TimerYellow else RaisedEnamel, CircleShape)
            ) { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Send", tint = if (canSend) Enamel else Cream.copy(alpha = .42f)) }
        }
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Always review before saving.", style = MaterialTheme.typography.bodySmall, color = Cream.copy(alpha = .7f), modifier = Modifier.weight(1f))
            if (state.messages.isNotEmpty()) TextButton(onClick = { confirmClear = true }) { Text("New chat", color = Cream) }
        }
    }
}

@Composable
private fun ChatTurn(message: RecipeChatMessage) {
    val user = message.role == ChatRole.USER
    Box(Modifier.fillMaxWidth(), contentAlignment = if (user) Alignment.CenterEnd else Alignment.CenterStart) {
        Surface(
            color = if (user) RaisedEnamel else Cream,
            contentColor = if (user) Cream else Enamel,
            shape = RoundedCornerShape(22.dp),
            border = if (user) BorderStroke(1.dp, Cream.copy(alpha = .16f)) else BorderStroke(1.dp, WarmLine),
            modifier = Modifier.fillMaxWidth(if (user) .72f else .94f)
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (user) "You" else "MealOS", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                if (message.attachmentLabel != null) {
                    Surface(color = if (user) Enamel.copy(alpha = .35f) else Color.White.copy(alpha = .36f), shape = RoundedCornerShape(14.dp)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Description, null, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(message.attachmentLabel, fontWeight = FontWeight.Bold)
                                message.attachmentDetail?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                    }
                } else Text(message.content, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun WorkingTurn() {
    Surface(color = Cream, contentColor = Enamel, shape = RoundedCornerShape(22.dp), modifier = Modifier.padding(end = 40.dp)) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Text("Checking the recipe…")
        }
    }
}

@Composable
private fun ProofStrip(state: RecipeAssistantState) {
    val scanAttached = state.messages.any { it.attachmentLabel != null }
    val verification = when {
        state.sources.isNotEmpty() && scanAttached -> "Scan attached · ${state.sources.size} sources"
        state.sources.isNotEmpty() -> "${state.sources.size} sources checked"
        scanAttached -> "Scan attached"
        else -> "Ready to scan"
    }
    Row(
        Modifier.fillMaxWidth().background(RaisedEnamel).padding(horizontal = 18.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatusMark(
            if (state.questions.isEmpty()) "Questions clear" else "${state.questions.size} ${if (state.questions.size == 1) "question" else "questions"}",
            state.questions.isEmpty(),
            Modifier.weight(1f)
        )
        StatusMark(verification, scanAttached || state.sources.isNotEmpty(), Modifier.weight(1f))
    }
}

@Composable
private fun StatusMark(label: String, checked: Boolean, modifier: Modifier = Modifier) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.widthIn(min = 0.dp)) {
        Surface(color = if (checked) Cream else TimerYellow, shape = CircleShape, modifier = Modifier.size(24.dp)) {
            Box(contentAlignment = Alignment.Center) {
                if (checked) Icon(Icons.Default.Check, null, tint = Enamel, modifier = Modifier.size(16.dp))
                else Text("?", color = Enamel, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(7.dp))
        Text(label, color = Cream, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun RecipeProof(state: RecipeAssistantState, onReviewDraft: () -> Unit, modifier: Modifier = Modifier) {
    Surface(color = Cream, modifier = modifier) {
        if (state.draft == null) {
            Column(Modifier.fillMaxSize().padding(36.dp), verticalArrangement = Arrangement.Center) {
                Text("Recipe proof", style = MaterialTheme.typography.displaySmall, color = Enamel)
                Spacer(Modifier.height(12.dp))
                Text("A structured draft will appear here as you talk. Nothing is saved until you review it.", style = MaterialTheme.typography.bodyLarge, color = RaisedEnamel)
                if (state.questions.isNotEmpty()) {
                    Spacer(Modifier.height(24.dp))
                    QuestionTray(state.questions)
                }
            }
        } else {
            val recipe = state.draft
            Column(Modifier.fillMaxSize()) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(30.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    item {
                        Text(recipe.name, style = MaterialTheme.typography.displaySmall, color = Enamel)
                        Text("Draft · not saved", style = MaterialTheme.typography.bodyLarge, color = RaisedEnamel)
                    }
                    if (state.questions.isNotEmpty()) item { QuestionTray(state.questions) }
                    item {
                        Text("Set the pace", style = MaterialTheme.typography.headlineMedium, color = Enamel)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ProofStat("Servings", recipe.baseServings.toString(), Modifier.weight(1f))
                            ProofStat("Hands-on", "${recipe.effortMinutes} min", Modifier.weight(1f))
                            ProofStat("Total", "${recipe.totalMinutes} min", Modifier.weight(1f))
                        }
                    }
                    item { ProofIngredients(recipe) }
                    item { ProofSteps(recipe) }
                    if (state.sources.isNotEmpty()) item { SourceList(state) }
                }
                Button(
                    onClick = onReviewDraft,
                    colors = ButtonDefaults.buttonColors(containerColor = ActionOrange, contentColor = Cream),
                    shape = RoundedCornerShape(28.dp),
                    modifier = Modifier.fillMaxWidth().padding(20.dp).height(62.dp)
                ) { Icon(Icons.Default.Check, null); Spacer(Modifier.width(8.dp)); Text("Review draft") }
            }
        }
    }
}

@Composable
private fun QuestionTray(questions: List<String>) {
    Card(colors = CardDefaults.cardColors(containerColor = TimerYellow.copy(alpha = .16f)), border = BorderStroke(1.dp, TimerYellow)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Needs your answer", style = MaterialTheme.typography.titleMedium, color = Enamel)
            questions.forEach { Text("• $it", color = Enamel) }
        }
    }
}

@Composable
private fun ProofStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(color = Color.Transparent, border = BorderStroke(1.dp, WarmLine), shape = RoundedCornerShape(10.dp), modifier = modifier) {
        Column(Modifier.padding(12.dp)) { Text(label, style = MaterialTheme.typography.bodySmall, color = RaisedEnamel); Text(value, fontWeight = FontWeight.Bold, color = Enamel) }
    }
}

@Composable
private fun ProofIngredients(recipe: Recipe) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Ingredients", style = MaterialTheme.typography.headlineMedium, color = Enamel)
        Surface(border = BorderStroke(1.dp, WarmLine), shape = RoundedCornerShape(10.dp), color = Color.Transparent) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                recipe.ingredients.forEach { Text("• ${it.amount.toCompactString()} ${it.unit} ${it.name}".replace("  ", " "), color = Enamel) }
            }
        }
    }
}

@Composable
private fun ProofSteps(recipe: Recipe) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text("Steps", style = MaterialTheme.typography.headlineMedium, color = Enamel)
        recipe.steps.forEachIndexed { index, step ->
            Row(
                Modifier.fillMaxWidth().background(Color.Transparent, RoundedCornerShape(10.dp)).padding(vertical = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Surface(color = Enamel, shape = CircleShape, modifier = Modifier.size(30.dp)) { Box(contentAlignment = Alignment.Center) { Text("${index + 1}", color = Cream, fontWeight = FontWeight.Bold) } }
                Spacer(Modifier.width(12.dp))
                Column { Text(step.title, fontWeight = FontWeight.Bold, color = Enamel); Text(step.instruction, color = Enamel) }
            }
            if (index < recipe.steps.lastIndex) HorizontalDivider(color = WarmLine)
        }
    }
}

@Composable
private fun SourceList(state: RecipeAssistantState) {
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("Sources checked", style = MaterialTheme.typography.titleMedium, color = Enamel)
        state.sources.forEach { source ->
            TextButton(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(source.url))) },
                colors = ButtonDefaults.textButtonColors(contentColor = Enamel)
            ) { Text(source.title) }
        }
    }
}

@Composable
private fun CompactDraft(recipe: Recipe, questionCount: Int, onReviewDraft: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Cream, contentColor = Enamel), shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Draft ready", style = MaterialTheme.typography.titleMedium, color = TimerYellow)
            Text(recipe.name, style = MaterialTheme.typography.headlineMedium)
            Text("${recipe.ingredients.size} ingredients · ${recipe.steps.size} steps${if (questionCount > 0) " · $questionCount open question" else ""}")
            Button(onClick = onReviewDraft, colors = ButtonDefaults.buttonColors(containerColor = ActionOrange, contentColor = Cream), modifier = Modifier.fillMaxWidth()) { Text("Review draft") }
        }
    }
}

private fun Double.toCompactString(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()
