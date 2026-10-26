package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.LeanPathDatabase
import com.example.data.LeanPathRepository
import com.example.ui.LeanPathUiState
import com.example.ui.LeanPathViewModel
import com.example.ui.MainDestination
import com.example.ui.screens.*
import com.example.ui.theme.LeanPathTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = LeanPathDatabase.getInstance(applicationContext)
        val repository = LeanPathRepository(db.profileStateDao())

        setContent {
            val viewModel: LeanPathViewModel = viewModel(
                factory = LeanPathViewModel.Factory(repository)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LeanPathTheme(themePreference = uiState.state.themePreference) {
                LeanPathApp(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }
}

private data class NavDestinationSpec(
    val destination: MainDestination,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val NAV_ITEMS = listOf(
    NavDestinationSpec(
        destination = MainDestination.PULSE,
        label = "Pulse",
        selectedIcon = Icons.Filled.Dashboard,
        unselectedIcon = Icons.Outlined.Dashboard,
        testTag = "nav_pulse"
    ),
    NavDestinationSpec(
        destination = MainDestination.TREND,
        label = "Trend",
        selectedIcon = Icons.Filled.ShowChart,
        unselectedIcon = Icons.Outlined.ShowChart,
        testTag = "nav_trend"
    ),
    NavDestinationSpec(
        destination = MainDestination.FUEL,
        label = "Fuel",
        selectedIcon = Icons.Filled.Restaurant,
        unselectedIcon = Icons.Outlined.Restaurant,
        testTag = "nav_fuel"
    ),
    NavDestinationSpec(
        destination = MainDestination.STUDIO,
        label = "Studio",
        selectedIcon = Icons.Filled.GridView,
        unselectedIcon = Icons.Outlined.GridView,
        testTag = "nav_studio"
    ),
    NavDestinationSpec(
        destination = MainDestination.SCIENCE,
        label = "Science",
        selectedIcon = Icons.Filled.Science,
        unselectedIcon = Icons.Outlined.Science,
        testTag = "nav_science"
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeanPathApp(
    uiState: LeanPathUiState,
    viewModel: LeanPathViewModel
) {
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    if (!uiState.state.onboarded) {
        OnboardingScreen(
            initialProfile = uiState.state.profile,
            onComplete = { viewModel.completeOnboarding(it) },
            onLoadDemo = { viewModel.loadDemoData() }
        )
        return
    }

    // Back handler on secondary tabs returns to Pulse home screen
    BackHandler(enabled = uiState.currentDestination != MainDestination.PULSE) {
        viewModel.selectDestination(MainDestination.PULSE)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.statusBannerMessage) {
        uiState.statusBannerMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.showMessage(null)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Eco,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "LeanPath",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Profile: ${uiState.activeProfileName} · On-Device",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { viewModel.startOnboardingWizard() },
                            modifier = Modifier.testTag("top_bar_blueprint_button")
                        ) {
                            Icon(
                                Icons.Default.Tune,
                                contentDescription = "Edit Blueprint",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Blueprint")
                        }
                        IconButton(
                            onClick = {
                                val nextTheme = when (uiState.state.themePreference) {
                                    "light" -> "dark"
                                    "dark" -> ""
                                    else -> "light"
                                }
                                viewModel.updateThemePreference(nextTheme)
                            },
                            modifier = Modifier.testTag("top_bar_theme_button")
                        ) {
                            Icon(
                                imageVector = when (uiState.state.themePreference) {
                                    "dark" -> Icons.Default.DarkMode
                                    "light" -> Icons.Default.LightMode
                                    else -> Icons.Default.BrightnessAuto
                                },
                                contentDescription = "Toggle theme"
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar {
                        NAV_ITEMS.forEach { item ->
                            val selected = uiState.currentDestination == item.destination
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectDestination(item.destination) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail {
                        Spacer(modifier = Modifier.height(12.dp))
                        NAV_ITEMS.forEach { item ->
                            val selected = uiState.currentDestination == item.destination
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectDestination(item.destination) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = { Text(item.label) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    val contentModifier = Modifier.widthIn(max = 700.dp)
                    when (uiState.currentDestination) {
                        MainDestination.PULSE -> {
                            PulseScreen(
                                state = uiState.state,
                                nowMillis = uiState.nowMillis,
                                onLogWeight = { viewModel.logWeight(it) },
                                onAddWaterMl = { viewModel.addWaterMl(it) },
                                onStartFast = { viewModel.startFast(it) },
                                onEndFast = { viewModel.endFast() },
                                onDismissReminder = { viewModel.dismissReminderToday(it) },
                                onOpenFoodTab = { viewModel.selectDestination(MainDestination.FUEL) },
                                onOpenStudioTool = { viewModel.selectStudioTool(it) },
                                modifier = contentModifier
                            )
                        }

                        MainDestination.TREND -> {
                            TrendScreen(
                                state = uiState.state,
                                rangeDays = uiState.weightChartRangeDays,
                                onSelectRangeDays = { viewModel.setWeightChartRange(it) },
                                onAddBackdatedWeight = { w, d -> viewModel.logWeight(w, d) },
                                onDeleteWeight = { viewModel.deleteWeighIn(it) },
                                modifier = contentModifier
                            )
                        }

                        MainDestination.FUEL -> {
                            FuelScreen(
                                state = uiState.state,
                                selectedMealType = uiState.selectedMealType,
                                searchQuery = uiState.foodSearchQuery,
                                onSelectMealType = { viewModel.setSelectedMealType(it) },
                                onSearchQueryChange = { viewModel.setFoodSearchQuery(it) },
                                onAddPresetFood = { preset, meal -> viewModel.addPresetFood(preset, meal) },
                                onAddCustomFood = { n, k, p, c, f, fi, na, al, m ->
                                    viewModel.addCustomFood(n, k, p, c, f, fi, na, al, m)
                                },
                                onDeleteFoodEntry = { viewModel.deleteFoodEntry(it) },
                                onSaveMealTemplate = { viewModel.saveMealGroupAsTemplate(it) },
                                onLogSavedMeal = { sm, meal -> viewModel.logSavedMeal(sm, meal) },
                                onDeleteSavedMeal = { viewModel.deleteSavedMeal(it) },
                                modifier = contentModifier
                            )
                        }

                        MainDestination.STUDIO -> {
                            StudioScreen(
                                state = uiState.state,
                                selectedTool = uiState.selectedStudioTool,
                                onSelectTool = { viewModel.selectStudioTool(it) },
                                onAddMeasurement = { k, v -> viewModel.addMeasurement(k, v) },
                                onAddExercise = { ex, m -> viewModel.addExercise(ex, m) },
                                onDeleteExercise = { viewModel.deleteExercise(it) },
                                onSaveStepsToday = { viewModel.saveStepsToday(it) },
                                onSaveWellnessToday = { s, r, m, e, st ->
                                    viewModel.saveWellnessToday(s, r, m, e, st)
                                },
                                onAddHabit = { viewModel.addHabit(it) },
                                onToggleHabitToday = { viewModel.toggleHabitToday(it) },
                                onDeleteHabit = { viewModel.deleteHabit(it) },
                                onAddJournalEntry = { t, x -> viewModel.addJournalEntry(t, x) },
                                onDeleteJournalEntry = { viewModel.deleteJournalEntry(it) },
                                onUpdateMealPlan = { idx, txt -> viewModel.updateDayMealPlan(idx, txt) },
                                onSendMealPlanToGrocery = { viewModel.sendMealPlanToGroceryList() },
                                onAddGroceryItem = { viewModel.addGroceryItem(it) },
                                onToggleGroceryItem = { viewModel.toggleGroceryItem(it) },
                                onRemoveGroceryItem = { viewModel.removeGroceryItem(it) },
                                onClearCheckedGroceries = { viewModel.clearCheckedGroceries() },
                                onAddProgressPhoto = { u, n -> viewModel.addProgressPhoto(u, n) },
                                onDeleteProgressPhoto = { viewModel.deleteProgressPhoto(it) },
                                onUpdateMilestoneReward = { p, r -> viewModel.updateMilestoneReward(p, r) },
                                onAddReminder = { t, l -> viewModel.addReminder(t, l) },
                                onDeleteReminder = { viewModel.deleteReminder(it) },
                                modifier = contentModifier
                            )
                        }

                        MainDestination.SCIENCE -> {
                            ScienceScreen(
                                state = uiState.state,
                                activeProfileKey = uiState.activeProfileKey,
                                profiles = uiState.profiles,
                                onUpdateUnits = { viewModel.updateUnits(it) },
                                onUpdateTheme = { viewModel.updateThemePreference(it) },
                                onUpdateActivityLevel = { viewModel.updateActivityLevel(it) },
                                onSwitchProfile = { viewModel.switchProfile(it) },
                                onCreateNewProfile = { viewModel.createNewProfile(it) },
                                onLoadDemoData = { viewModel.loadDemoData() },
                                onReopenOnboarding = { viewModel.startOnboardingWizard() },
                                onImportJsonBackup = { viewModel.importJsonBackup(it) },
                                onResetAllData = { viewModel.resetAllData() },
                                modifier = contentModifier
                            )
                        }
                    }
                }
            }
        }
    }
}
