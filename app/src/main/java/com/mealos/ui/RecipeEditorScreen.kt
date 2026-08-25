package com.mealos.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.mealos.data.RecipeDocument
import com.mealos.model.Ingredient
import com.mealos.model.IngredientCategory
import com.mealos.model.Recipe
import com.mealos.model.RecipeEditorState
import com.mealos.model.RecipeStep
import com.mealos.ui.theme.ActionOrange
import com.mealos.ui.theme.Cream
import com.mealos.ui.theme.Enamel
import com.mealos.ui.theme.RaisedEnamel
import com.mealos.ui.theme.TimerYellow
import com.mealos.ui.theme.WarmLine
import java.util.UUID

private enum class EditorSection(val label: String) {
    BASICS("Basics"),
    INGREDIENTS("Ingredients"),
    STEPS("Steps"),
    PREVIEW("Preview")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecipeEditorScreen(
    state: RecipeEditorState,
    onRecipeChange: ((Recipe) -> Recipe) -> Unit,
    onSave: () -> List<String>,
    onClose: () -> Unit,
    onSaved: (String) -> Unit
) {
    var section by remember(state.recipe.id) { mutableStateOf(EditorSection.BASICS) }
    var errors by remember(state.recipe.id) { mutableStateOf<List<String>>(emptyList()) }
    val recipe = state.recipe

    Scaffold(
        containerColor = Enamel,
        topBar = {
            TopAppBar(
                title = { Text(if (recipe.name.isBlank()) "New recipe" else recipe.shortName.ifBlank { recipe.name }) },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close recipe editor") } },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Enamel,
                    titleContentColor = Cream,
                    navigationIconContentColor = Cream
                )
            )
        },
        bottomBar = {
            Surface(color = RaisedEnamel) {
                Button(
                    onClick = {
                        errors = onSave()
                        if (errors.isEmpty()) onSaved(recipe.id)
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = ActionOrange, contentColor = Cream),
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(56.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Save recipe")
                }
            }
        }
    ) { padding ->
        BoxWithConstraints(Modifier.padding(padding).fillMaxSize()) {
            val expanded = maxWidth >= 720.dp
            if (expanded) {
                Row(Modifier.fillMaxSize()) {
                    EditorOutline(recipe, state, section, errors, { section = it }, Modifier.width(300.dp).fillMaxHeight())
                    Spacer(Modifier.width(8.dp).fillMaxHeight().background(Color(0xFF41573B)))
                    EditorSectionPane(recipe, section, onRecipeChange, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EditorSection.entries.forEach { item ->
                            AssistChip(
                                onClick = { section = item },
                                label = { Text(item.label) },
                                leadingIcon = if (item == section) ({ Icon(Icons.Default.Check, contentDescription = null) }) else null,
                                colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                                    containerColor = if (item == section) TimerYellow else RaisedEnamel,
                                    labelColor = if (item == section) Enamel else Cream,
                                    leadingIconContentColor = Enamel
                                )
                            )
                        }
                    }
                    if (state.message != null || errors.isNotEmpty()) {
                        EditorNotice(state.message, errors, Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    }
                    EditorSectionPane(recipe, section, onRecipeChange, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun EditorOutline(
    recipe: Recipe,
    state: RecipeEditorState,
    selected: EditorSection,
    errors: List<String>,
    onSelect: (EditorSection) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.background(Enamel).padding(20.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Build the recipe", style = MaterialTheme.typography.displayMedium, color = Cream)
        Text(
            "Work through each tray, then check the cooking preview before saving.",
            style = MaterialTheme.typography.bodyLarge,
            color = Cream.copy(alpha = 0.82f)
        )
        EditorSection.entries.forEach { section ->
            val complete = when (section) {
                EditorSection.BASICS -> recipe.name.isNotBlank() && recipe.totalMinutes >= recipe.effortMinutes
                EditorSection.INGREDIENTS -> recipe.ingredients.isNotEmpty() && recipe.ingredients.all { it.name.isNotBlank() && it.amount > 0 && it.unit.isNotBlank() }
                EditorSection.STEPS -> recipe.steps.isNotEmpty() && recipe.steps.all { it.title.isNotBlank() && it.instruction.isNotBlank() }
                EditorSection.PREVIEW -> RecipeDocument.validate(recipe).isEmpty()
            }
            Surface(
                color = if (selected == section) Cream else RaisedEnamel,
                contentColor = if (selected == section) Enamel else Cream,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().clickable { onSelect(section) }
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(section.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    if (complete) {
                        Surface(color = TimerYellow, contentColor = Enamel, shape = CircleShape, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Check, contentDescription = "Complete", modifier = Modifier.padding(5.dp))
                        }
                    }
                }
            }
        }
        if (state.message != null || errors.isNotEmpty()) EditorNotice(state.message, errors)
    }
}

@Composable
private fun EditorNotice(message: String?, errors: List<String>, modifier: Modifier = Modifier) {
    Surface(
        color = if (errors.isEmpty()) TimerYellow.copy(alpha = 0.2f) else MaterialTheme.colorScheme.errorContainer,
        contentColor = if (errors.isEmpty()) Cream else MaterialTheme.colorScheme.onErrorContainer,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            errors.forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
        }
    }
}

@Composable
private fun EditorSectionPane(
    recipe: Recipe,
    section: EditorSection,
    onRecipeChange: ((Recipe) -> Recipe) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    ) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            when (section) {
                EditorSection.BASICS -> BasicsEditor(recipe, onRecipeChange)
                EditorSection.INGREDIENTS -> IngredientsEditor(recipe, onRecipeChange)
                EditorSection.STEPS -> StepsEditor(recipe, onRecipeChange)
                EditorSection.PREVIEW -> RecipeEditorPreview(recipe)
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun BasicsEditor(recipe: Recipe, onRecipeChange: ((Recipe) -> Recipe) -> Unit) {
    EditorHeading("Name the meal", "Use the wording you want to see while planning and cooking.")
    OutlinedTextField(recipe.name, { value -> onRecipeChange { it.copy(name = value, shortName = it.shortName.ifBlank { value }) } }, label = { Text("Recipe name") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(recipe.shortName, { value -> onRecipeChange { it.copy(shortName = value) } }, label = { Text("Short name") }, supportingText = { Text("Used in compact planner rows") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(recipe.description, { value -> onRecipeChange { it.copy(description = value) } }, label = { Text("Description") }, minLines = 2, modifier = Modifier.fillMaxWidth())
    EditorHeading("Set the pace", "MealOS uses these values when deciding how the recipe feels in the library.")
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        NumberField("Servings", recipe.baseServings, Modifier.weight(1f)) { value -> onRecipeChange { it.copy(baseServings = value.coerceAtLeast(1)) } }
        NumberField("Hands-on min", recipe.effortMinutes, Modifier.weight(1f)) { value -> onRecipeChange { it.copy(effortMinutes = value.coerceAtLeast(0)) } }
        NumberField("Total min", recipe.totalMinutes, Modifier.weight(1f)) { value -> onRecipeChange { it.copy(totalMinutes = value.coerceAtLeast(1)) } }
    }
    OutlinedTextField(recipe.equipment.joinToString(", "), { value -> onRecipeChange { it.copy(equipment = splitList(value)) } }, label = { Text("Equipment") }, supportingText = { Text("Separate with commas") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(recipe.tags.joinToString(", "), { value -> onRecipeChange { it.copy(tags = splitList(value)) } }, label = { Text("Tags") }, supportingText = { Text("Separate with commas") }, modifier = Modifier.fillMaxWidth())
    OutlinedTextField(recipe.leftoverNote, { value -> onRecipeChange { it.copy(leftoverNote = value) } }, label = { Text("Leftover plan") }, minLines = 2, modifier = Modifier.fillMaxWidth())
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IngredientsEditor(recipe: Recipe, onRecipeChange: ((Recipe) -> Recipe) -> Unit) {
    EditorHeading("Stock the ingredient tray", "Amounts scale automatically and matching ingredient names combine in Grocery.")
    recipe.ingredients.forEachIndexed { index, ingredient ->
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Ingredient ${index + 1}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    ReorderControls(index, recipe.ingredients.lastIndex,
                        onUp = { onRecipeChange { it.copy(ingredients = it.ingredients.moved(index, index - 1)) } },
                        onDown = { onRecipeChange { it.copy(ingredients = it.ingredients.moved(index, index + 1)) } },
                        onDelete = { onRecipeChange { it.copy(ingredients = it.ingredients.filterIndexed { itemIndex, _ -> itemIndex != index }) } }
                    )
                }
                OutlinedTextField(ingredient.name, { name -> updateIngredient(recipe, index, ingredient.copy(name = name, id = RecipeDocument.ingredientId(name)), onRecipeChange) }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        ingredient.amount.toCleanString(),
                        { value -> value.toDoubleOrNull()?.let { updateIngredient(recipe, index, ingredient.copy(amount = it), onRecipeChange) } },
                        label = { Text("Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(ingredient.unit, { value -> updateIngredient(recipe, index, ingredient.copy(unit = value), onRecipeChange) }, label = { Text("Unit") }, modifier = Modifier.weight(1f))
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IngredientCategory.entries.forEach { category ->
                        AssistChip(
                            onClick = { updateIngredient(recipe, index, ingredient.copy(category = category), onRecipeChange) },
                            label = { Text(category.label) },
                            leadingIcon = if (ingredient.category == category) ({ Icon(Icons.Default.Check, contentDescription = null) }) else null
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(ingredient.pantryStaple, { checked -> updateIngredient(recipe, index, ingredient.copy(pantryStaple = checked), onRecipeChange) })
                    Text("Usually a pantry staple")
                }
            }
        }
    }
    FilledTonalButton(
        onClick = {
            onRecipeChange {
                it.copy(ingredients = it.ingredients + Ingredient(UUID.randomUUID().toString(), "", 1.0, "count", IngredientCategory.PANTRY))
            }
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Add ingredient")
    }
}

@Composable
private fun StepsEditor(recipe: Recipe, onRecipeChange: ((Recipe) -> Recipe) -> Unit) {
    EditorHeading("Lay out the cooking steps", "Each step should stand on its own when MealOS shows it at arm's length.")
    recipe.steps.forEachIndexed { index, step ->
        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Enamel, contentColor = Cream, shape = CircleShape, modifier = Modifier.size(40.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) { Text("${index + 1}", fontWeight = FontWeight.Bold) }
                    }
                    Text("Cooking step", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 12.dp).weight(1f))
                    ReorderControls(index, recipe.steps.lastIndex,
                        onUp = { onRecipeChange { it.copy(steps = it.steps.moved(index, index - 1)) } },
                        onDown = { onRecipeChange { it.copy(steps = it.steps.moved(index, index + 1)) } },
                        onDelete = { onRecipeChange { it.copy(steps = it.steps.filterIndexed { itemIndex, _ -> itemIndex != index }) } }
                    )
                }
                OutlinedTextField(step.title, { value -> updateStep(recipe, index, step.copy(title = value), onRecipeChange) }, label = { Text("Step title") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(step.instruction, { value -> updateStep(recipe, index, step.copy(instruction = value), onRecipeChange) }, label = { Text("Instruction") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(step.equipment, { value -> updateStep(recipe, index, step.copy(equipment = value), onRecipeChange) }, label = { Text("Equipment or surface") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        step.timerSeconds?.div(60)?.toString().orEmpty(),
                        { value -> updateStep(recipe, index, step.copy(timerSeconds = value.toIntOrNull()?.takeIf { it > 0 }?.times(60)), onRecipeChange) },
                        label = { Text("Timer minutes") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(step.tip.orEmpty(), { value -> updateStep(recipe, index, step.copy(tip = value.takeIf { it.isNotBlank() }), onRecipeChange) }, label = { Text("Optional tip") }, modifier = Modifier.weight(2f))
                }
            }
        }
    }
    FilledTonalButton(
        onClick = { onRecipeChange { it.copy(steps = it.steps + RecipeStep("", "", "Kitchen")) } },
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Add cooking step")
    }
}

@Composable
private fun RecipeEditorPreview(recipe: Recipe) {
    EditorHeading("Check the cooking view", "This is how the first step and ingredient tray will read before you save.")
    val step = recipe.steps.firstOrNull()
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth >= 600.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StepPreview(recipe, Modifier.weight(1f))
                IngredientPreview(recipe, Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                StepPreview(recipe, Modifier.fillMaxWidth())
                IngredientPreview(recipe, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun StepPreview(recipe: Recipe, modifier: Modifier = Modifier) {
    val step = recipe.steps.firstOrNull()
    Surface(color = Cream, contentColor = Enamel, shape = RoundedCornerShape(28.dp), modifier = modifier) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Step 1 of ${recipe.steps.size.coerceAtLeast(1)}", style = MaterialTheme.typography.titleMedium)
            Text(step?.title?.ifBlank { "Your first cooking action" } ?: "Add a cooking step", style = MaterialTheme.typography.displayMedium)
            Text(step?.instruction?.ifBlank { "The full instruction will appear here at an easy cooking distance." } ?: "Return to Steps to build the cooking flow.", style = MaterialTheme.typography.bodyLarge)
            Surface(color = Enamel, contentColor = TimerYellow, shape = RoundedCornerShape(10.dp)) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Kitchen, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text("  ${step?.equipment ?: "Kitchen"}")
                }
            }
        }
    }
}

@Composable
private fun IngredientPreview(recipe: Recipe, modifier: Modifier = Modifier) {
    Surface(color = RaisedEnamel, contentColor = Cream, shape = RoundedCornerShape(28.dp), modifier = modifier) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Ingredients", style = MaterialTheme.typography.headlineMedium)
            recipe.ingredients.take(5).forEach { ingredient ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (ingredient.category == IngredientCategory.PANTRY) Icons.Default.Inventory2 else Icons.Default.Restaurant, contentDescription = null, tint = TimerYellow)
                    Text(ingredient.name.ifBlank { "Unnamed ingredient" }, modifier = Modifier.padding(horizontal = 10.dp).weight(1f))
                    Text("${ingredient.amount.toCleanString()} ${ingredient.unit}", fontWeight = FontWeight.SemiBold)
                }
                HorizontalDivider(color = Cream.copy(alpha = 0.14f))
            }
        }
    }
}

@Composable
private fun EditorHeading(title: String, body: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.displayMedium)
        Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun NumberField(label: String, value: Int, modifier: Modifier = Modifier, onValue: (Int) -> Unit) {
    OutlinedTextField(
        value.toString(),
        { raw -> raw.toIntOrNull()?.let(onValue) },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

@Composable
private fun ReorderControls(index: Int, lastIndex: Int, onUp: () -> Unit, onDown: () -> Unit, onDelete: () -> Unit) {
    IconButton(onClick = onUp, enabled = index > 0) { Icon(Icons.Default.ArrowUpward, contentDescription = "Move up") }
    IconButton(onClick = onDown, enabled = index < lastIndex) { Icon(Icons.Default.ArrowDownward, contentDescription = "Move down") }
    IconButton(onClick = onDelete) { Icon(Icons.Default.DeleteOutline, contentDescription = "Delete") }
}

private fun updateIngredient(recipe: Recipe, index: Int, ingredient: Ingredient, onRecipeChange: ((Recipe) -> Recipe) -> Unit) {
    onRecipeChange { it.copy(ingredients = recipe.ingredients.toMutableList().apply { this[index] = ingredient }) }
}

private fun updateStep(recipe: Recipe, index: Int, step: RecipeStep, onRecipeChange: ((Recipe) -> Recipe) -> Unit) {
    onRecipeChange { it.copy(steps = recipe.steps.toMutableList().apply { this[index] = step }) }
}

private fun splitList(value: String): List<String> = value.split(',').map(String::trim).filter(String::isNotBlank)

private fun Double.toCleanString(): String = if (this % 1.0 == 0.0) toInt().toString() else toString()

private fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices) return this
    return toMutableList().apply { add(to, removeAt(from)) }
}
