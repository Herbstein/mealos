package com.mealos.ui

/*
THESIS: MealOS is an unfolded mise-en-place station, refusing the permanent dashboard rail and generic recipe-card grid.
OWN-WORLD: Deep green enamel surrounds warm cream prep trays; yellow marks active time and burnt orange marks decisive completion.
STORY: Plan familiar meals, consolidate the shop, then cook with the active step, ingredients, and timers held in equal view.
FIRST VIEWPORT: A hidden drawer preserves width; the cooking surface balances a current-step tray against ingredients and a tactile timer shelf across the hinge.
FORM: Mise en Place Board, first-ranked grounded direction, seed e36aa34b; approved comp 3 with previous-step preview and no bespoke recipe drawings.
FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
*/

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.view.WindowCompat
import androidx.window.layout.FoldingFeature
import com.mealos.MealOsViewModel
import com.mealos.data.GroceryAggregator
import com.mealos.data.loadChatGptRecipePrompt
import com.mealos.data.shareRecipe
import com.mealos.model.CookingTimer
import com.mealos.model.GroceryItem
import com.mealos.model.Ingredient
import com.mealos.model.IngredientCategory
import com.mealos.model.MealOsUserState
import com.mealos.model.MealOsAccountState
import com.mealos.model.CloudSyncStatus
import com.mealos.model.PlanEntry
import com.mealos.model.PortionMode
import com.mealos.model.Recipe
import com.mealos.model.RecipeAssistantModel
import com.mealos.model.scaledAmount
import com.mealos.ui.theme.ActionOrange
import com.mealos.ui.theme.Cream
import com.mealos.ui.theme.Enamel
import com.mealos.ui.theme.RaisedEnamel
import com.mealos.ui.theme.TimerYellow
import com.mealos.ui.theme.WarmLine
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private enum class Destination(val label: String, val icon: ImageVector) {
    TODAY("Today", Icons.Default.Home),
    PLAN("Plan", Icons.Default.CalendarMonth),
    RECIPES("Recipes", Icons.AutoMirrored.Filled.MenuBook),
    GROCERY("Grocery", Icons.Default.ShoppingCart),
    ASSISTANT("Recipe assistant", Icons.Default.AutoAwesome),
    PANTRY("Pantry staples", Icons.Default.Inventory2)
}

private sealed interface Route {
    data class Main(val destination: Destination) : Route
    data class Detail(val recipeId: String) : Route
    data class Cooking(val recipeId: String, val portion: PortionMode) : Route
    data object Editor : Route
}

private enum class CookingPane { STEP, WORKSPACE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealOsApp(
    viewModel: MealOsViewModel,
    fold: FoldingFeature?,
    timerAlertsAvailable: Boolean,
    preciseTimersAvailable: Boolean,
    onEnableTimerAlerts: () -> Unit,
    onEnablePreciseTimers: () -> Unit,
    onScanRecipe: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    val userState by viewModel.userState.collectAsStateWithLifecycle()
    val recipes by viewModel.recipes.collectAsStateWithLifecycle()
    val editorState by viewModel.editorState.collectAsStateWithLifecycle()
    val importError by viewModel.importError.collectAsStateWithLifecycle()
    val timers by viewModel.timers.collectAsStateWithLifecycle()
    val assistantState by viewModel.assistantState.collectAsStateWithLifecycle()
    val accountState by viewModel.accountState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var route: Route by rememberSaveable(stateSaver = RouteSaver) { mutableStateOf(Route.Main(Destination.TODAY)) }
    var showDiscardRecipeDialog by rememberSaveable { mutableStateOf(false) }
    val view = LocalView.current
    val darkTheme = isSystemInDarkTheme()

    importError?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::clearImportError,
            title = { Text("Recipe could not be imported") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::clearImportError) { Text("Dismiss") } }
        )
    }

    if (showDiscardRecipeDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardRecipeDialog = false },
            title = { Text("Discard unsaved recipe?") },
            text = { Text("Your changes have not been saved to the recipe library.") },
            dismissButton = { TextButton(onClick = { showDiscardRecipeDialog = false }) { Text("Keep editing") } },
            confirmButton = {
                TextButton(onClick = {
                    showDiscardRecipeDialog = false
                    viewModel.closeEditor()
                    route = Route.Main(Destination.RECIPES)
                }) { Text("Discard") }
            }
        )
    }

    LaunchedEffect(editorState?.recipe?.id, editorState?.hasUnsavedChanges) {
        if (editorState?.hasUnsavedChanges == true) route = Route.Editor
    }

    SideEffect {
        view.context.findActivity()?.window?.let { window ->
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars =
                route is Route.Detail && !darkTheme
        }
    }

    BackHandler(route !is Route.Main || (route as? Route.Main)?.destination != Destination.TODAY) {
        route = when (val current = route) {
            is Route.Cooking -> Route.Detail(current.recipeId)
            is Route.Detail -> Route.Main(Destination.RECIPES)
            Route.Editor -> {
                if (editorState?.hasUnsavedChanges == true) {
                    showDiscardRecipeDialog = true
                    Route.Editor
                } else {
                    viewModel.closeEditor()
                    Route.Main(Destination.RECIPES)
                }
            }
            is Route.Main -> Route.Main(Destination.TODAY)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(312.dp)) {
                Text(
                    "MealOS",
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(start = 24.dp, top = 36.dp, bottom = 24.dp)
                )
                Destination.entries.forEach { destination ->
                    val selected = (route as? Route.Main)?.destination == destination
                    NavigationDrawerItem(
                        label = { Text(destination.label) },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        selected = selected,
                        onClick = {
                            route = Route.Main(destination)
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                AccountShelf(
                    state = accountState,
                    onSignIn = onSignIn,
                    onRetry = viewModel::retryCloudSync,
                    onSignOut = onSignOut,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    ) {
        AnimatedContent(
            targetState = route,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "MealOS destination"
        ) { currentRoute ->
            when (currentRoute) {
                is Route.Main -> MainDestination(
                    destination = currentRoute.destination,
                    state = userState,
                    recipes = recipes,
                    viewModel = viewModel,
                    onMenu = { scope.launch { drawerState.open() } },
                    onRecipe = { route = Route.Detail(it) },
                    onCook = { id, portion -> route = Route.Cooking(id, portion) },
                    onNewRecipe = {
                        viewModel.startNewRecipe()
                        route = Route.Editor
                    },
                    fold = fold,
                    assistantState = assistantState,
                    accountState = accountState,
                    onSignIn = onSignIn,
                    onScanRecipe = onScanRecipe
                )
                is Route.Detail -> RecipeDetail(
                    recipe = recipes.firstOrNull { it.id == currentRoute.recipeId }
                        ?: editorState?.recipe
                        ?: recipes.first(),
                    state = userState,
                    viewModel = viewModel,
                    onBack = { route = Route.Main(Destination.RECIPES) },
                    onMenu = { scope.launch { drawerState.open() } },
                    onCook = { portion -> route = Route.Cooking(currentRoute.recipeId, portion) },
                    onEdit = {
                        viewModel.startEditingRecipe(currentRoute.recipeId)
                        route = Route.Editor
                    }
                )
                is Route.Cooking -> CookingMode(
                    recipe = recipes.firstOrNull { it.id == currentRoute.recipeId } ?: recipes.first(),
                    portion = currentRoute.portion,
                    timers = timers.values.toList(),
                    pantry = userState.pantry,
                    viewModel = viewModel,
                    fold = fold,
                    timerAlertsAvailable = timerAlertsAvailable,
                    preciseTimersAvailable = preciseTimersAvailable,
                    onEnableTimerAlerts = onEnableTimerAlerts,
                    onEnablePreciseTimers = onEnablePreciseTimers,
                    onBack = { route = Route.Detail(currentRoute.recipeId) },
                    onMenu = { scope.launch { drawerState.open() } }
                )
                Route.Editor -> editorState?.let { state ->
                    RecipeEditorScreen(
                        state = state,
                        onRecipeChange = viewModel::updateEditorRecipe,
                        onSave = viewModel::saveEditorRecipe,
                        onClose = {
                            if (state.hasUnsavedChanges) {
                                showDiscardRecipeDialog = true
                            } else {
                                viewModel.closeEditor()
                                route = Route.Main(Destination.RECIPES)
                            }
                        },
                        onSaved = { recipeId -> route = Route.Detail(recipeId) }
                    )
                }
            }
        }
    }
}

private val RouteSaver = androidx.compose.runtime.saveable.Saver<Route, String>(
    save = { route ->
        when (route) {
            is Route.Main -> "main:${route.destination.name}"
            is Route.Detail -> "detail:${route.recipeId}"
            is Route.Cooking -> "cooking:${route.recipeId}:${route.portion.name}"
            Route.Editor -> "editor"
        }
    },
    restore = { raw ->
        val parts = raw.split(":")
        when (parts.firstOrNull()) {
            "detail" -> Route.Detail(parts.getOrElse(1) { "garlic-herb-chicken" })
            "cooking" -> Route.Cooking(parts.getOrElse(1) { "garlic-herb-chicken" }, runCatching { PortionMode.valueOf(parts[2]) }.getOrDefault(PortionMode.TWO))
            "editor" -> Route.Editor
            else -> Route.Main(runCatching { Destination.valueOf(parts.getOrElse(1) { Destination.TODAY.name }) }.getOrDefault(Destination.TODAY))
        }
    }
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainDestination(
    destination: Destination,
    state: MealOsUserState,
    recipes: List<Recipe>,
    viewModel: MealOsViewModel,
    onMenu: () -> Unit,
    onRecipe: (String) -> Unit,
    onCook: (String, PortionMode) -> Unit,
    onNewRecipe: () -> Unit,
    fold: FoldingFeature?,
    assistantState: com.mealos.model.RecipeAssistantState,
    accountState: MealOsAccountState,
    onSignIn: () -> Unit,
    onScanRecipe: () -> Unit
) {
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (destination == Destination.TODAY) "MealOS" else destination.label) },
                navigationIcon = { IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, contentDescription = "Open navigation") } },
                actions = {
                    if (destination == Destination.ASSISTANT) {
                        AssistantModelMenu(
                            selected = assistantState.model,
                            enabled = !assistantState.sending,
                            onSelect = viewModel::setAssistantModel
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (destination == Destination.ASSISTANT) Enamel else MaterialTheme.colorScheme.background,
                    titleContentColor = if (destination == Destination.ASSISTANT) Cream else MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = if (destination == Destination.ASSISTANT) Cream else MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        Surface(
            color = if (destination == Destination.ASSISTANT) Enamel else MaterialTheme.colorScheme.surface,
            shape = if (destination == Destination.ASSISTANT) RoundedCornerShape(0.dp) else RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            modifier = Modifier.padding(padding).fillMaxSize()
        ) {
            when (destination) {
                Destination.TODAY -> TodayScreen(state, recipes, onRecipe, onCook)
                Destination.PLAN -> PlannerScreen(state, recipes, viewModel, onRecipe)
                Destination.RECIPES -> RecipeLibrary(state, recipes, viewModel, onRecipe, onNewRecipe)
                Destination.GROCERY -> GroceryScreen(state, recipes, viewModel)
                Destination.ASSISTANT -> RecipeAssistantScreen(
                    state = assistantState,
                    fold = fold,
                    onScan = onScanRecipe,
                    onSend = viewModel::sendAssistantMessage,
                    onRetry = viewModel::retryAssistant,
                    onClear = viewModel::clearAssistantChat,
                    onReviewDraft = viewModel::reviewAssistantDraft,
                    signedIn = accountState.signedIn,
                    onSignIn = onSignIn
                )
                Destination.PANTRY -> PantryScreen(state, recipes, viewModel)
            }
        }
    }
}

@Composable
private fun AccountShelf(
    state: MealOsAccountState,
    onSignIn: () -> Unit,
    onRetry: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Enamel,
        contentColor = Cream,
        shape = RoundedCornerShape(22.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = if (state.syncStatus == CloudSyncStatus.SYNCED) Cream else RaisedEnamel,
                    shape = CircleShape,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        when {
                            state.signingIn || state.syncStatus in setOf(CloudSyncStatus.CONNECTING, CloudSyncStatus.SYNCING) ->
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = TimerYellow)
                            state.syncStatus == CloudSyncStatus.SYNCED ->
                                Icon(Icons.Default.CloudDone, contentDescription = null, tint = Enamel, modifier = Modifier.size(22.dp))
                            else -> Icon(Icons.Default.CloudOff, contentDescription = null, tint = Cream, modifier = Modifier.size(22.dp))
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        state.displayName?.takeIf(String::isNotBlank) ?: if (state.signedIn) "Google account" else "Cloud sync",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        accountStatusLabel(state),
                        style = MaterialTheme.typography.bodySmall,
                        color = Cream.copy(alpha = .74f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            state.message?.let { message ->
                Text(message, style = MaterialTheme.typography.bodySmall, color = TimerYellow, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            if (state.signedIn) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.syncStatus in setOf(CloudSyncStatus.OFFLINE, CloudSyncStatus.ERROR)) {
                        TextButton(onClick = onRetry, contentPadding = PaddingValues(horizontal = 0.dp)) {
                            Text("Retry sync", color = TimerYellow)
                        }
                    }
                    TextButton(onClick = onSignOut, contentPadding = PaddingValues(horizontal = 0.dp)) {
                        Text("Sign out", color = Cream)
                    }
                }
            } else {
                Button(
                    onClick = onSignIn,
                    enabled = !state.signingIn,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = ActionOrange),
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (state.signingIn) "Opening Google…" else "Sign in with Google") }
            }
        }
    }
}

private fun accountStatusLabel(state: MealOsAccountState): String = when (state.syncStatus) {
    CloudSyncStatus.SIGNED_OUT -> "Sync recipes, plans, pantry, and notes"
    CloudSyncStatus.CONNECTING -> "Connecting your kitchen…"
    CloudSyncStatus.SYNCING -> "Waiting to sync latest changes…"
    CloudSyncStatus.SYNCED -> state.email ?: "Recipes and plans synced"
    CloudSyncStatus.OFFLINE -> "Offline · changes stay on this device"
    CloudSyncStatus.ERROR -> "Cloud sync needs attention"
}

@Composable
private fun AssistantModelMenu(
    selected: RecipeAssistantModel,
    enabled: Boolean,
    onSelect: (RecipeAssistantModel) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(
            onClick = { expanded = true },
            enabled = enabled,
            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                contentColor = TimerYellow,
                disabledContentColor = Cream.copy(alpha = 0.52f)
            ),
            modifier = Modifier.semantics {
                contentDescription = "Recipe model, ${selected.label}"
            }
        ) {
            Text(selected.label, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Icon(Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            RecipeAssistantModel.entries.forEach { model ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(model.label, fontWeight = FontWeight.SemiBold)
                            Text(
                                model.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    leadingIcon = {
                        if (model == selected) Icon(Icons.Default.Check, contentDescription = "Selected")
                        else Spacer(Modifier.size(24.dp))
                    },
                    onClick = {
                        expanded = false
                        onSelect(model)
                    }
                )
            }
        }
    }
}

@Composable
private fun TodayScreen(
    state: MealOsUserState,
    recipes: List<Recipe>,
    onRecipe: (String) -> Unit,
    onCook: (String, PortionMode) -> Unit
) {
    val planned = state.plan.sortedBy { it.dayIndex }
    val tonight = planned.firstOrNull()?.let { entry -> recipes.firstOrNull { it.id == entry.recipeId }?.let { it to entry } }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item {
            Text("Your kitchen, ready.", style = MaterialTheme.typography.displayMedium, color = Enamel)
            Text("Plan once. Shop once. Cook without hunting for the next step.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            if (tonight == null) {
                EmptyTray("Nothing planned yet", "Open Plan and add a familiar dinner to your week.")
            } else {
                val (recipe, entry) = tonight
                Card(colors = CardDefaults.cardColors(containerColor = Enamel, contentColor = Cream), shape = RoundedCornerShape(28.dp)) {
                    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Tonight", style = MaterialTheme.typography.titleMedium, color = TimerYellow)
                        Text(recipe.shortName, style = MaterialTheme.typography.headlineLarge)
                        Text(entry.portionMode.label, style = MaterialTheme.typography.bodyLarge)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(onClick = { onCook(recipe.id, entry.portionMode) }, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = ActionOrange)) {
                                Icon(Icons.Default.Restaurant, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Start cooking")
                            }
                            OutlinedButton(onClick = { onRecipe(recipe.id) }) { Text("Review recipe") }
                        }
                    }
                }
            }
        }
        item { Text("This week", style = MaterialTheme.typography.headlineMedium) }
        if (planned.isEmpty()) item { Text("Your week is open. Add meals from Plan.") }
        items(planned) { entry ->
            recipes.firstOrNull { it.id == entry.recipeId }?.let { recipe ->
                ListItem(
                    headlineContent = { Text(recipe.shortName, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text("${dayLabel(entry.dayIndex)} · ${entry.portionMode.label}") },
                    leadingContent = { IngredientMark(recipe) },
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                    modifier = Modifier.clip(RoundedCornerShape(18.dp)).clickable { onRecipe(recipe.id) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlannerScreen(state: MealOsUserState, recipes: List<Recipe>, viewModel: MealOsViewModel, onRecipe: (String) -> Unit) {
    var editingDay by remember { mutableStateOf<Int?>(null) }
    val planByDay = state.plan.associateBy { it.dayIndex }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Build the week", style = MaterialTheme.typography.displayMedium)
            Text("Choose familiar dinners; quantities roll directly into Grocery.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(7) { day ->
            val entry = planByDay[day]
            val recipe = entry?.let { planned -> recipes.firstOrNull { it.id == planned.recipeId } }
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = if (day == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(dayLabel(day), style = MaterialTheme.typography.titleMedium)
                            Text(recipe?.shortName ?: "Open night", style = MaterialTheme.typography.titleLarge)
                        }
                        if (entry != null) IconButton(onClick = { viewModel.removePlan(day) }) { Icon(Icons.Default.DeleteOutline, contentDescription = "Remove ${recipe?.shortName}") }
                    }
                    if (entry != null) {
                        PortionSelector(entry.portionMode) { viewModel.setPlan(entry.copy(portionMode = it)) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilledTonalButton(onClick = { editingDay = day }) { Text(if (entry == null) "Add meal" else "Swap meal") }
                        if (recipe != null) TextButton(onClick = { onRecipe(recipe.id) }) { Text("Recipe") }
                    }
                }
            }
        }
        if (state.plan.isNotEmpty()) item { TextButton(onClick = viewModel::clearPlan) { Text("Clear the week") } }
    }
    editingDay?.let { day ->
        androidx.compose.material3.ModalBottomSheet(onDismissRequest = { editingDay = null }) {
            Text("Choose ${dayLabel(day)}", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp))
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                items(recipes) { recipe ->
                    ListItem(
                        headlineContent = { Text(recipe.shortName, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { Text("${recipe.effortMinutes} min hands-on · ${recipe.equipment.joinToString()}") },
                        leadingContent = { IngredientMark(recipe) },
                        modifier = Modifier.clickable {
                            viewModel.setPlan(PlanEntry(day, recipe.id, PortionMode.TWO))
                            editingDay = null
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeLibrary(
    state: MealOsUserState,
    recipes: List<Recipe>,
    viewModel: MealOsViewModel,
    onRecipe: (String) -> Unit,
    onNewRecipe: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var query by rememberSaveable { mutableStateOf("") }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    val filtered = recipes.filter { recipe ->
        (!favoritesOnly || recipe.id in state.favorites) && (query.isBlank() || recipe.name.contains(query, true) || recipe.tags.any { it.contains(query, true) })
    }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Your recipe bench", style = MaterialTheme.typography.displayMedium)
            Text("Core favorites and your own recipes, ready for planning and cooking.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Button(onClick = onNewRecipe, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Create a recipe")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    clipboard.setText(AnnotatedString(loadChatGptRecipePrompt(context)))
                    Toast.makeText(context, "AI recipe prompt copied", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Copy AI recipe prompt")
            }
            Text(
                "Paste it into ChatGPT, then share the JSON response back to MealOS for review.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                placeholder = { Text("Search meals or equipment") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { favoritesOnly = !favoritesOnly },
                    label = { Text(if (favoritesOnly) "Favorites only" else "All favorites") },
                    leadingIcon = { Icon(if (favoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null) }
                )
            }
        }
        items(filtered) { recipe ->
            RecipeRow(recipe, recipe.id in state.favorites, state.ratings[recipe.id], { viewModel.toggleFavorite(recipe.id) }, { onRecipe(recipe.id) })
        }
        if (filtered.isEmpty()) item { EmptyTray("No meals found", "Try a shorter search or turn off the favorites filter.") }
    }
}

@Composable
private fun RecipeRow(recipe: Recipe, favorite: Boolean, rating: Int?, onFavorite: () -> Unit, onOpen: () -> Unit) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen)
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            IngredientMark(recipe)
            Column(Modifier.padding(horizontal = 14.dp).weight(1f)) {
                Text(recipe.shortName, style = MaterialTheme.typography.titleLarge)
                Text("${recipe.effortMinutes} min hands-on · ${recipe.totalMinutes} min total", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(recipe.equipment.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                if (rating != null) Text("$rating / 5", color = ActionOrange, fontWeight = FontWeight.SemiBold)
            }
            IconButton(onClick = onFavorite) {
                Icon(if (favorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = if (favorite) "Remove favorite" else "Add favorite", tint = if (favorite) ActionOrange else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun GroceryScreen(state: MealOsUserState, recipes: List<Recipe>, viewModel: MealOsViewModel) {
    val groceries = GroceryAggregator.aggregate(state.plan, recipes, state.pantry)
    val grouped = groceries.groupBy { it.category }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            Text("One clean shop", style = MaterialTheme.typography.displayMedium)
            Text("Combined from ${state.plan.size} planned meals. Pantry staples are already excluded.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (groceries.isEmpty()) item { EmptyTray("Your list is empty", "Add meals in Plan, or review Pantry staples if something is missing.") }
        grouped.forEach { (category, items) ->
            item { Text(category.label, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(top = 18.dp)) }
            items(items) { item -> GroceryRow(item, item.id in state.checkedGroceries) { viewModel.toggleGrocery(item.id) } }
        }
    }
}

@Composable
private fun GroceryRow(item: GroceryItem, checked: Boolean, onToggle: () -> Unit) {
    ListItem(
        headlineContent = { Text(item.name, fontWeight = FontWeight.SemiBold, textDecoration = if (checked) TextDecoration.LineThrough else null) },
        supportingContent = { Text(item.displayAmount) },
        leadingContent = { Checkbox(checked = checked, onCheckedChange = { onToggle() }) },
        modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable(onClick = onToggle)
    )
}

@Composable
private fun PantryScreen(state: MealOsUserState, recipes: List<Recipe>, viewModel: MealOsViewModel) {
    val ingredients = recipes.flatMap { it.ingredients }.distinctBy { it.id }.sortedWith(compareBy<Ingredient> { it.category.ordinal }.thenBy { it.name })
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        item {
            Text("Pantry staples", style = MaterialTheme.typography.displayMedium)
            Text("Anything switched on stays out of Grocery until you switch it off.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        items(ingredients) { ingredient ->
            ListItem(
                headlineContent = { Text(ingredient.name) },
                supportingContent = { Text(ingredient.category.label) },
                leadingContent = { CategoryIcon(ingredient.category) },
                trailingContent = { Switch(checked = ingredient.id in state.pantry, onCheckedChange = { viewModel.togglePantry(ingredient.id) }) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun RecipeDetail(
    recipe: Recipe,
    state: MealOsUserState,
    viewModel: MealOsViewModel,
    onBack: () -> Unit,
    onMenu: () -> Unit,
    onCook: (PortionMode) -> Unit,
    onEdit: () -> Unit
) {
    val context = LocalContext.current
    var portion by rememberSaveable { mutableStateOf(PortionMode.TWO) }
    var note by rememberSaveable(recipe.id) { mutableStateOf(state.notes[recipe.id].orEmpty()) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe.shortName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit or customize recipe") }
                    IconButton(onClick = { shareRecipe(context, recipe) }) { Icon(Icons.Default.Share, contentDescription = "Share recipe file") }
                    IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, contentDescription = "Open navigation") }
                    IconButton(onClick = { viewModel.toggleFavorite(recipe.id) }) { Icon(if (recipe.id in state.favorites) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = "Toggle favorite") }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 4.dp) {
                Button(
                    onClick = { onCook(portion) },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = ActionOrange),
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(56.dp)
                ) { Text("Start cooking") }
            }
        }
    ) { padding ->
        LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = padding.calculateTopPadding() + 10.dp, bottom = padding.calculateBottomPadding() + 18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            item {
                Text(recipe.name, style = MaterialTheme.typography.displayMedium)
                if (recipe.description.isNotBlank()) {
                    Text(recipe.description, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${recipe.effortMinutes} min hands-on · ${recipe.totalMinutes} min total", style = MaterialTheme.typography.bodyLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { recipe.tags.forEach { StaticChip(it) } }
            }
            item {
                Text("How much?", style = MaterialTheme.typography.headlineMedium)
                PortionSelector(portion) { portion = it }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer), shape = RoundedCornerShape(22.dp)) {
                    Row(Modifier.padding(18.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Inventory2, contentDescription = null)
                        Column(Modifier.padding(start = 12.dp)) {
                            Text("Leftovers built in", fontWeight = FontWeight.Bold)
                            Text(recipe.leftoverNote)
                        }
                    }
                }
            }
            item { Text("Ingredients", style = MaterialTheme.typography.headlineMedium) }
            items(recipe.ingredients) { ingredient ->
                ListItem(
                    headlineContent = { Text(ingredient.name) },
                    supportingContent = { if (ingredient.id in state.pantry) Text("In pantry", color = MaterialTheme.colorScheme.primary) },
                    leadingContent = { CategoryIcon(ingredient.category) },
                    trailingContent = { Text(scaledAmount(ingredient, recipe, portion), fontWeight = FontWeight.SemiBold) }
                )
            }
            item { Text("Steps", style = MaterialTheme.typography.headlineMedium) }
            itemsIndexed(recipe.steps) { index, step ->
                ListItem(
                    headlineContent = { Text(step.title, fontWeight = FontWeight.SemiBold) },
                    supportingContent = { Text(step.instruction) },
                    leadingContent = { StepNumber(index + 1) },
                    trailingContent = { step.timerSeconds?.let { Text("${it / 60} min") } }
                )
            }
            item {
                Text("Your rating", style = MaterialTheme.typography.headlineMedium)
                Row { (1..5).forEach { rating -> IconButton(onClick = { viewModel.setRating(recipe.id, rating) }) { Icon(if ((state.ratings[recipe.id] ?: 0) >= rating) Icons.Default.Star else Icons.Default.StarBorder, contentDescription = "$rating stars", tint = TimerYellow) } } }
            }
            item {
                Text("Notes", style = MaterialTheme.typography.headlineMedium)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    minLines = 3,
                    placeholder = { Text("What should future-you remember?") },
                    keyboardActions = KeyboardActions(onDone = { viewModel.setNote(recipe.id, note) }),
                    modifier = Modifier.fillMaxWidth()
                )
                TextButton(onClick = { viewModel.setNote(recipe.id, note) }) { Text("Save note") }
            }
        }
    }
}

@Composable
private fun CookingMode(
    recipe: Recipe,
    portion: PortionMode,
    timers: List<CookingTimer>,
    pantry: Set<String>,
    viewModel: MealOsViewModel,
    fold: FoldingFeature?,
    timerAlertsAvailable: Boolean,
    preciseTimersAvailable: Boolean,
    onEnableTimerAlerts: () -> Unit,
    onEnablePreciseTimers: () -> Unit,
    onBack: () -> Unit,
    onMenu: () -> Unit
) {
    var stepIndex by rememberSaveable(recipe.id) { mutableIntStateOf(0) }
    val step = recipe.steps[stepIndex]
    val density = LocalDensity.current
    val stepScroll = rememberScrollState()
    val workspaceScroll = rememberScrollState()
    val compactScroll = rememberScrollState()
    var activePane by rememberSaveable { mutableStateOf(CookingPane.STEP) }
    var compactStepHeightPx by rememberSaveable { mutableIntStateOf(0) }
    var previousExpanded by rememberSaveable { mutableStateOf<Boolean?>(null) }
    var pendingCompactRestore by remember { mutableStateOf(false) }
    val hingeWidth = with(density) { if (fold?.isSeparating == true) fold.bounds.width().toDp().coerceAtLeast(8.dp) else 8.dp }
    BoxWithConstraints(Modifier.fillMaxSize().background(Enamel).safeDrawingPadding()) {
        val expanded = maxWidth >= 720.dp

        LaunchedEffect(stepScroll.isScrollInProgress) {
            if (expanded && stepScroll.isScrollInProgress) activePane = CookingPane.STEP
        }
        LaunchedEffect(workspaceScroll.isScrollInProgress) {
            if (expanded && workspaceScroll.isScrollInProgress) activePane = CookingPane.WORKSPACE
        }
        LaunchedEffect(expanded) {
            val previous = previousExpanded
            if (previous != null && previous != expanded) {
                if (expanded) {
                    if (compactScroll.value < compactStepHeightPx) {
                        activePane = CookingPane.STEP
                        stepScroll.scrollTo(compactScroll.value)
                    } else {
                        activePane = CookingPane.WORKSPACE
                        workspaceScroll.scrollTo(compactScroll.value - compactStepHeightPx)
                    }
                } else {
                    pendingCompactRestore = true
                }
            }
            previousExpanded = expanded
        }
        LaunchedEffect(pendingCompactRestore, compactStepHeightPx) {
            if (pendingCompactRestore && compactStepHeightPx > 0) {
                val target = when (activePane) {
                    CookingPane.STEP -> stepScroll.value
                    CookingPane.WORKSPACE -> compactStepHeightPx + workspaceScroll.value
                }
                compactScroll.scrollTo(target)
                pendingCompactRestore = false
            }
        }

        if (expanded) {
            Row(Modifier.fillMaxSize()) {
                StepPane(recipe, portion, stepIndex, onStep = { stepIndex = it }, onBack, onMenu, onAddTimer = { seconds -> viewModel.addTimer(timerLabel(step.title), seconds) }, Modifier.weight(1f).verticalScroll(stepScroll))
                Spacer(Modifier.width(hingeWidth).fillMaxHeight().background(Color(0xFF41573B)))
                WorkspacePane(
                    recipe, portion, timers, pantry, viewModel,
                    timerAlertsAvailable, preciseTimersAvailable,
                    onEnableTimerAlerts, onEnablePreciseTimers,
                    Modifier.weight(1f).verticalScroll(workspaceScroll)
                )
            }
        } else {
            Column(Modifier.fillMaxSize().verticalScroll(compactScroll).padding(bottom = 20.dp)) {
                StepPane(
                    recipe,
                    portion,
                    stepIndex,
                    onStep = { stepIndex = it },
                    onBack,
                    onMenu,
                    onAddTimer = { seconds -> viewModel.addTimer(timerLabel(step.title), seconds) },
                    Modifier.fillMaxWidth().onSizeChanged { compactStepHeightPx = it.height }
                )
                WorkspacePane(
                    recipe, portion, timers, pantry, viewModel,
                    timerAlertsAvailable, preciseTimersAvailable,
                    onEnableTimerAlerts, onEnablePreciseTimers,
                    Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun StepPane(
    recipe: Recipe,
    portion: PortionMode,
    stepIndex: Int,
    onStep: (Int) -> Unit,
    onBack: () -> Unit,
    onMenu: () -> Unit,
    onAddTimer: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val current = recipe.steps[stepIndex]
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onMenu) { Icon(Icons.Default.Menu, contentDescription = "Open navigation", tint = Cream) }
            Column(Modifier.weight(1f)) {
                Text(recipe.shortName, style = MaterialTheme.typography.titleLarge, color = Cream, maxLines = 2)
                Text(portion.label, color = TimerYellow)
            }
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit cooking mode", tint = Cream) }
        }
        RecipeProgress(stepIndex, recipe.steps.size)
        if (stepIndex > 0) {
            val previous = recipe.steps[stepIndex - 1]
            StepPreview("Previous", previous.title, previous.instruction, Icons.AutoMirrored.Filled.ArrowBack) { onStep(stepIndex - 1) }
        }
        Surface(color = Cream, contentColor = Enamel, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Step ${stepIndex + 1} of ${recipe.steps.size}", style = MaterialTheme.typography.titleMedium)
                Text(current.title, style = MaterialTheme.typography.displayMedium)
                Text(current.instruction, style = MaterialTheme.typography.bodyLarge)
                StaticChip(current.equipment, Icons.Default.Kitchen)
                current.tip?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold) }
                current.timerSeconds?.let { seconds ->
                    FilledTonalButton(
                        onClick = { onAddTimer(seconds) },
                        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                            containerColor = TimerYellow.copy(alpha = 0.24f),
                            contentColor = Enamel
                        )
                    ) {
                        Icon(Icons.Default.AccessTime, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add ${seconds / 60} min timer")
                    }
                }
            }
        }
        recipe.steps.getOrNull(stepIndex + 1)?.let { StepPreview("Next · Step ${stepIndex + 2}", it.title, it.instruction, Icons.AutoMirrored.Filled.ArrowForward) { onStep(stepIndex + 1) } }
        recipe.steps.getOrNull(stepIndex + 2)?.let { StepPreview("Then · Step ${stepIndex + 3}", it.title, it.instruction, Icons.AutoMirrored.Filled.ArrowForward) { onStep(stepIndex + 2) } }
        Button(
            onClick = { if (stepIndex < recipe.steps.lastIndex) onStep(stepIndex + 1) },
            enabled = stepIndex < recipe.steps.lastIndex,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = ActionOrange,
                contentColor = Cream,
                disabledContainerColor = ActionOrange.copy(alpha = 0.45f),
                disabledContentColor = Cream.copy(alpha = 0.7f)
            ),
            modifier = Modifier.fillMaxWidth().height(60.dp)
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (stepIndex == recipe.steps.lastIndex) "Recipe complete" else "Done · next step")
        }
    }
}

@Composable
private fun WorkspacePane(
    recipe: Recipe,
    portion: PortionMode,
    timers: List<CookingTimer>,
    pantry: Set<String>,
    viewModel: MealOsViewModel,
    timerAlertsAvailable: Boolean,
    preciseTimersAvailable: Boolean,
    onEnableTimerAlerts: () -> Unit,
    onEnablePreciseTimers: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(color = Cream, contentColor = Enamel, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(vertical = 12.dp)) {
                Text("Ingredients", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                recipe.ingredients.forEach { ingredient ->
                    ListItem(
                        headlineContent = { Text(ingredient.name, color = Enamel, fontWeight = FontWeight.SemiBold) },
                        supportingContent = { if (ingredient.id in pantry) Text("Pantry staple", color = ActionOrange) },
                        leadingContent = { CategoryIcon(ingredient.category, tint = Enamel) },
                        trailingContent = { Text(scaledAmount(ingredient, recipe, portion), color = Enamel) },
                        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Cream)
                    )
                }
            }
        }
        TimerShelf(
            timers, viewModel, timerAlertsAvailable, preciseTimersAvailable,
            onEnableTimerAlerts, onEnablePreciseTimers
        )
    }
}

@Composable
private fun StaticChip(label: String, icon: ImageVector? = null) {
    Surface(
        color = Cream,
        contentColor = Enamel,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, WarmLine)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let { Icon(it, contentDescription = null, modifier = Modifier.size(18.dp)) }
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun TimerShelf(
    timers: List<CookingTimer>,
    viewModel: MealOsViewModel,
    timerAlertsAvailable: Boolean,
    preciseTimersAvailable: Boolean,
    onEnableTimerAlerts: () -> Unit,
    onEnablePreciseTimers: () -> Unit
) {
    Surface(color = RaisedEnamel, contentColor = Cream, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Timers", style = MaterialTheme.typography.headlineMedium)
            if (timers.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = TimerYellow)
                    Text(" Add a timer from a cooking step.", style = MaterialTheme.typography.bodyLarge)
                }
            }
            timers.take(2).forEachIndexed { index, timer ->
                TimerControl(timer, if (index == 0) TimerYellow else Color(0xFFFF7A32), { viewModel.toggleTimer(timer.id) }, { viewModel.addMinute(timer.id) })
            }
            if (timers.isNotEmpty() && (!timerAlertsAvailable || !preciseTimersAvailable)) {
                TimerAccessRecovery(
                    timerAlertsAvailable = timerAlertsAvailable,
                    preciseTimersAvailable = preciseTimersAvailable,
                    onEnableTimerAlerts = onEnableTimerAlerts,
                    onEnablePreciseTimers = onEnablePreciseTimers
                )
            }
        }
    }
}

@Composable
private fun TimerAccessRecovery(
    timerAlertsAvailable: Boolean,
    preciseTimersAvailable: Boolean,
    onEnableTimerAlerts: () -> Unit,
    onEnablePreciseTimers: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (!timerAlertsAvailable) {
            TimerAccessRow(
                icon = Icons.Default.NotificationsOff,
                title = "Timer alerts are off",
                description = "MealOS keeps counting, but Android cannot alert you when time is up.",
                action = "Turn on alerts",
                onAction = onEnableTimerAlerts
            )
        }
        if (!timerAlertsAvailable && !preciseTimersAvailable) {
            HorizontalDivider(color = Cream.copy(alpha = 0.18f))
        }
        if (!preciseTimersAvailable) {
            TimerAccessRow(
                icon = Icons.Default.AccessTime,
                title = "Precise timing is off",
                description = "Timers still run, but Android may deliver them late while the phone sleeps.",
                action = "Allow precise timing",
                onAction = onEnablePreciseTimers
            )
        }
    }
}

@Composable
private fun TimerAccessRow(
    icon: ImageVector,
    title: String,
    description: String,
    action: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(icon, contentDescription = null, tint = TimerYellow, modifier = Modifier.padding(top = 3.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodyMedium, color = Cream.copy(alpha = 0.82f))
            TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)) {
                Text(action, color = TimerYellow)
            }
        }
    }
}

@Composable
private fun TimerControl(timer: CookingTimer, accent: Color, onToggle: () -> Unit, onAddMinute: () -> Unit) {
    val totalSeconds = (timer.remainingMs / 1_000).coerceAtLeast(0)
    val text = if (totalSeconds == 0L) "Done" else "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Enamel).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(timer.label, style = MaterialTheme.typography.titleMedium, color = accent)
            Text(text, fontSize = 40.sp, fontWeight = FontWeight.Bold, color = accent, modifier = Modifier.semantics { contentDescription = "${timer.label} timer $text" })
        }
        IconButton(
            onClick = onToggle,
            enabled = timer.remainingMs > 0,
            modifier = Modifier.size(56.dp).background(RaisedEnamel, CircleShape)
        ) { Icon(if (timer.running) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = if (timer.running) "Pause ${timer.label}" else "Start ${timer.label}") }
        IconButton(onClick = onAddMinute, modifier = Modifier.size(56.dp).background(RaisedEnamel, CircleShape)) { Icon(Icons.Default.Add, contentDescription = "Add one minute to ${timer.label}") }
    }
}

@Composable
private fun RecipeProgress(stepIndex: Int, count: Int) {
    Canvas(Modifier.fillMaxWidth().height(18.dp).semantics { contentDescription = "Step ${stepIndex + 1} of $count" }) {
        val y = size.height / 2
        drawLine(color = Color(0xFF6F8477), start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 3.dp.toPx())
        repeat(count) { index ->
            val x = if (count == 1) 0f else size.width * index / (count - 1)
            drawCircle(color = if (index <= stepIndex) TimerYellow else Color(0xFF6F8477), radius = if (index == stepIndex) 7.dp.toPx() else 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x, y))
        }
    }
}

@Composable
private fun StepPreview(label: String, title: String, description: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(color = Cream, contentColor = Enamel, shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = ActionOrange)
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF41573B),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(icon, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PortionSelector(selected: PortionMode, onSelect: (PortionMode) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PortionMode.entries.forEach { mode ->
            AssistChip(
                onClick = { onSelect(mode) },
                label = { Text(mode.label) },
                leadingIcon = { Icon(if (selected == mode) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, contentDescription = null) },
                colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                    containerColor = if (selected == mode) TimerYellow else Enamel,
                    labelColor = if (selected == mode) Enamel else Cream,
                    leadingIconContentColor = if (selected == mode) Enamel else Cream
                ),
                border = androidx.compose.material3.AssistChipDefaults.assistChipBorder(
                    enabled = true,
                    borderColor = WarmLine
                )
            )
        }
    }
}

@Composable
private fun IngredientMark(recipe: Recipe) {
    val category = recipe.ingredients.firstOrNull()?.category ?: IngredientCategory.PANTRY
    Surface(color = Enamel, contentColor = Cream, shape = CircleShape, modifier = Modifier.size(52.dp)) {
        Box(contentAlignment = Alignment.Center) { CategoryIcon(category, tint = TimerYellow) }
    }
}

@Composable
private fun CategoryIcon(category: IngredientCategory, tint: Color = MaterialTheme.colorScheme.primary) {
    val icon = when (category) {
        IngredientCategory.PRODUCE -> Icons.Default.Restaurant
        IngredientCategory.MEAT -> Icons.Default.Restaurant
        IngredientCategory.DAIRY -> Icons.Default.Kitchen
        IngredientCategory.BAKERY -> Icons.Default.Restaurant
        IngredientCategory.PANTRY -> Icons.Default.Inventory2
        IngredientCategory.FROZEN -> Icons.Default.Kitchen
    }
    Icon(icon, contentDescription = category.label, tint = tint)
}

@Composable
private fun StepNumber(number: Int) {
    Surface(color = Enamel, contentColor = Cream, shape = CircleShape, modifier = Modifier.size(40.dp)) {
        Box(contentAlignment = Alignment.Center) { Text(number.toString(), fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun EmptyTray(title: String, body: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

private fun dayLabel(dayIndex: Int): String {
    val day = LocalDate.now().plusDays(dayIndex.toLong())
    return if (dayIndex == 0) "Today · ${day.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())}"
    else day.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
}

private fun timerLabel(stepTitle: String): String = stepTitle
    .removePrefix("Start the ")
    .removePrefix("Cook the ")
    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
