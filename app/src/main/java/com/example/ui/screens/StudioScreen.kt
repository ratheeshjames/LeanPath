package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.domain.MetabolicEngine
import com.example.model.*
import com.example.ui.StudioTool
import com.example.ui.components.ConsistencyHeatmap90Days
import com.example.ui.components.HabitRadarChart
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudioScreen(
    state: LeanPathState,
    selectedTool: StudioTool,
    onSelectTool: (StudioTool) -> Unit,
    onAddMeasurement: (String, Double) -> Unit,
    onAddExercise: (ExerciseMet, Int) -> Unit,
    onDeleteExercise: (String) -> Unit,
    onSaveStepsToday: (Int) -> Unit,
    onSaveWellnessToday: (Double, Int, Int, Int, Int) -> Unit,
    onAddHabit: (String) -> Unit,
    onToggleHabitToday: (String) -> Unit,
    onDeleteHabit: (String) -> Unit,
    onAddJournalEntry: (String, String) -> Unit,
    onDeleteJournalEntry: (String) -> Unit,
    onUpdateMealPlan: (Int, String) -> Unit,
    onSendMealPlanToGrocery: () -> Unit,
    onAddGroceryItem: (String) -> Unit,
    onToggleGroceryItem: (String) -> Unit,
    onRemoveGroceryItem: (String) -> Unit,
    onClearCheckedGroceries: () -> Unit,
    onAddProgressPhoto: (String, String) -> Unit,
    onDeleteProgressPhoto: (String) -> Unit,
    onUpdateMilestoneReward: (Int, String) -> Unit,
    onAddReminder: (String, String) -> Unit,
    onDeleteReminder: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val tools = StudioTool.entries

    Column(modifier = modifier.fillMaxSize()) {
        SecondaryScrollableTabRow(
            selectedTabIndex = tools.indexOf(selectedTool),
            edgePadding = 12.dp,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            tools.forEach { tool ->
                Tab(
                    selected = selectedTool == tool,
                    onClick = { onSelectTool(tool) },
                    text = { Text(tool.label) },
                    modifier = Modifier.testTag("studio_tab_${tool.name.lowercase()}")
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTool) {
                StudioTool.MEASURES -> MeasuresSubScreen(state, onAddMeasurement)
                StudioTool.ACTIVITY -> ActivitySubScreen(state, onAddExercise, onDeleteExercise, onSaveStepsToday)
                StudioTool.WELLNESS -> WellnessSubScreen(state, onSaveWellnessToday)
                StudioTool.HABITS -> HabitsSubScreen(state, onAddHabit, onToggleHabitToday, onDeleteHabit)
                StudioTool.JOURNAL -> JournalSubScreen(state, onAddJournalEntry, onDeleteJournalEntry)
                StudioTool.CALENDAR -> CalendarHeatmapSubScreen(state)
                StudioTool.REVIEW -> WeeklyReviewSubScreen(state)
                StudioTool.PLANNER -> MealPlannerSubScreen(state, onUpdateMealPlan, onSendMealPlanToGrocery)
                StudioTool.GROCERY -> GrocerySubScreen(state, onAddGroceryItem, onToggleGroceryItem, onRemoveGroceryItem, onClearCheckedGroceries)
                StudioTool.PHOTOS -> ProgressPhotosSubScreen(state, onAddProgressPhoto, onDeleteProgressPhoto)
                StudioTool.GOALS -> GoalsRemindersSubScreen(state, onUpdateMilestoneReward, onAddReminder, onDeleteReminder)
            }
        }
    }
}

@Composable
private fun MeasuresSubScreen(
    state: LeanPathState,
    onAddMeasurement: (String, Double) -> Unit
) {
    var selectedSite by remember { mutableStateOf("waist") }
    var valueText by remember { mutableStateOf("") }
    val units = state.profile.units

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Log Body Measurement", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Waist circumference is the gold-standard non-scale indicator of visceral fat reduction when scale weight stalls.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MEASUREMENT_SITES.take(4).forEach { (key, label) ->
                    FilterChip(
                        selected = selectedSite == key,
                        onClick = { selectedSite = key },
                        label = { Text(label) }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MEASUREMENT_SITES.drop(4).forEach { (key, label) ->
                    FilterChip(
                        selected = selectedSite == key,
                        onClick = { selectedSite = key },
                        label = { Text(label) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val unitLabel = if (selectedSite == "bf") "%" else MetabolicEngine.lengthUnit(units)
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { valueText = it },
                    label = { Text("Value ($unitLabel)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("measurement_value_input")
                )
                Button(
                    onClick = {
                        valueText.toDoubleOrNull()?.let { v ->
                            onAddMeasurement(selectedSite, v)
                            valueText = ""
                        }
                    },
                    modifier = Modifier.testTag("add_measurement_button")
                ) {
                    Text("Add")
                }
            }
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Change Since First Entry", style = MaterialTheme.typography.titleLarge)
            val sitesWithData = MEASUREMENT_SITES.mapNotNull { (key, label) ->
                val entries = state.measurements.filter { it.siteKey == key }
                if (entries.isEmpty()) null else Triple(key, label, entries)
            }
            if (sitesWithData.isEmpty()) {
                Text(
                    text = "No measurements logged yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                sitesWithData.forEach { (key, label, entries) ->
                    val first = entries.first().valueCmOrPct
                    val last = entries.last().valueCmOrPct
                    val delta = last - first
                    val deltaFormatted = if (key == "bf") {
                        String.format(Locale.US, "%+.1f%%", delta)
                    } else if (units == "imp") {
                        String.format(Locale.US, "%+.1f in", MetabolicEngine.cmToIn(delta))
                    } else {
                        String.format(Locale.US, "%+.1f cm", delta)
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${MetabolicEngine.formatMeasurement(key, last, units)} ($deltaFormatted)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            color = if (delta <= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }
}

@Composable
private fun ActivitySubScreen(
    state: LeanPathState,
    onAddExercise: (ExerciseMet, Int) -> Unit,
    onDeleteExercise: (String) -> Unit,
    onSaveStepsToday: (Int) -> Unit
) {
    val today = MetabolicEngine.dateOffset(0)
    var selectedMet by remember { mutableStateOf(EXERCISE_METS.first()) }
    var minutesText by remember { mutableStateOf("30") }
    var stepsText by remember(state.stepsByDate[today]) {
        mutableStateOf((state.stepsByDate[today] ?: 0).takeIf { it > 0 }?.toString() ?: "")
    }

    val burnedToday = MetabolicEngine.computeBurnedKcal(state, today)
    val metrics = remember(state) { MetabolicEngine.calculateMetrics(state) }
    val eaten = remember(state.foodByDate[today]) {
        MetabolicEngine.computeDailyFoodTotals(state.foodByDate[today].orEmpty())
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Log MET Exercise", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Formula: Calories Burned = MET × body weight (kg) × duration (hours)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EXERCISE_METS.take(4).forEach { ex ->
                    FilterChip(
                        selected = selectedMet == ex,
                        onClick = { selectedMet = ex },
                        label = { Text("${ex.name} (${ex.met})") }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EXERCISE_METS.drop(4).forEach { ex ->
                    FilterChip(
                        selected = selectedMet == ex,
                        onClick = { selectedMet = ex },
                        label = { Text("${ex.name} (${ex.met})") }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { minutesText = it },
                    label = { Text("Minutes") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("exercise_minutes_input")
                )
                Button(
                    onClick = {
                        minutesText.toIntOrNull()?.let { mins ->
                            onAddExercise(selectedMet, mins)
                        }
                    },
                    modifier = Modifier.testTag("add_exercise_button")
                ) {
                    Text("Log Workout")
                }
            }
        }
    }

    // 7-Day Burn Chart & Today's Workouts
    val weekBurns = (0..6).map { i ->
        val d = MetabolicEngine.dateOffset((i - 6).toLong())
        d.takeLast(2) to MetabolicEngine.computeBurnedKcal(state, d)
    }
    val maxBurn = max(300, weekBurns.maxOfOrNull { it.second } ?: 300)
    val barColor = MaterialTheme.colorScheme.primary

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Burned Today: $burnedToday kcal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    text = "Net budget: ${(metrics.targetKcal + burnedToday - eaten.kcal.roundToInt()).coerceAtLeast(0)} kcal",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            state.exercisesByDate[today].orEmpty().forEach { ex ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${ex.name} · ${ex.minutes} min", style = MaterialTheme.typography.bodyMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${ex.kcalBurned} kcal", style = MaterialTheme.typography.labelSmall)
                        IconButton(onClick = { onDeleteExercise(ex.id) }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Delete exercise", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(95.dp)
            ) {
                val slotW = size.width / 7f
                val maxBarH = size.height - 20.dp.toPx()
                weekBurns.forEachIndexed { idx, (_, kcal) ->
                    val h = (kcal.toFloat() / maxBurn.toFloat()) * maxBarH
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(idx * slotW + 8.dp.toPx(), maxBarH - h),
                        size = Size(slotW - 16.dp.toPx(), max(4.dp.toPx(), h)),
                        cornerRadius = CornerRadius(6.dp.toPx())
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                weekBurns.forEach { (dayNum, _) ->
                    Text(dayNum, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }

    // Steps Today Card
    val currentSteps = state.stepsByDate[today] ?: 0
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Daily Steps (NEAT)", style = MaterialTheme.typography.titleLarge)
                Text("$currentSteps / 10,000", style = MaterialTheme.typography.labelSmall)
            }
            LinearProgressIndicator(
                progress = { (currentSteps / 10000f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = stepsText,
                    onValueChange = { stepsText = it },
                    label = { Text("Steps today") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        stepsText.toIntOrNull()?.let { onSaveStepsToday(it) }
                    }
                ) {
                    Text("Save Steps")
                }
            }
        }
    }
}

@Composable
private fun WellnessSubScreen(
    state: LeanPathState,
    onSaveWellnessToday: (Double, Int, Int, Int, Int) -> Unit
) {
    val today = MetabolicEngine.dateOffset(0)
    val current = state.wellnessByDate[today] ?: WellnessEntry()
    var sleepText by remember(current) { mutableStateOf(current.sleepHours.toString()) }
    var hrText by remember(current) { mutableStateOf(current.restingHr.toString()) }
    var mood by remember(current) { mutableIntStateOf(current.mood) }
    var energy by remember(current) { mutableIntStateOf(current.energy) }
    var stress by remember(current) { mutableIntStateOf(current.stress) }

    val draftEntry = WellnessEntry(
        sleepHours = sleepText.toDoubleOrNull() ?: 7.5,
        restingHr = hrText.toIntOrNull() ?: 60,
        mood = mood,
        energy = energy,
        stress = stress
    )
    val recoveryScore = MetabolicEngine.computeRecoveryScore(draftEntry)

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sleep & Bio-Recovery", style = MaterialTheme.typography.titleLarge)
                Surface(
                    color = if (recoveryScore >= 70) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Recovery $recoveryScore/100",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = sleepText,
                    onValueChange = { sleepText = it },
                    label = { Text("Sleep (hours)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = hrText,
                    onValueChange = { hrText = it },
                    label = { Text("Resting HR (bpm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            RatingSelectorRow("Mood (1–5)", mood) { mood = it }
            RatingSelectorRow("Energy (1–5)", energy) { energy = it }
            RatingSelectorRow("Stress (1–5, low is calm)", stress) { stress = it }

            if (recoveryScore < 50) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                ) {
                    Text(
                        text = "Recovery is low ($recoveryScore/100). Prioritize 7.5h+ sleep tonight — sleep deprivation elevates ghrelin (hunger hormone) and cortisol.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = {
                    onSaveWellnessToday(
                        sleepText.toDoubleOrNull() ?: 7.5,
                        hrText.toIntOrNull() ?: 60,
                        mood,
                        energy,
                        stress
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_wellness_button")
            ) {
                Text("Save Today's Wellness")
            }
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Last 7 Recovery Logs", style = MaterialTheme.typography.titleLarge)
            state.wellnessByDate.keys.sorted().takeLast(7).reversed().forEach { d ->
                val w = state.wellnessByDate[d] ?: return@forEach
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(d, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        text = "${w.sleepHours}h sleep · RHR ${w.restingHr} · Score ${MetabolicEngine.computeRecoveryScore(w)}/100",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = JetBrainsMonoFontFamily
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun RatingSelectorRow(
    label: String,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (1..5).forEach { i ->
                FilterChip(
                    selected = selected == i,
                    onClick = { onSelect(i) },
                    label = { Text("$i") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HabitsSubScreen(
    state: LeanPathState,
    onAddHabit: (String) -> Unit,
    onToggleHabitToday: (String) -> Unit,
    onDeleteHabit: (String) -> Unit
) {
    val today = MetabolicEngine.dateOffset(0)
    val doneToday = state.habitsDoneByDate[today].orEmpty()
    var newHabitText by remember { mutableStateOf("") }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Daily Keystone Habits", style = MaterialTheme.typography.titleLarge)
            state.habitsList.forEach { habit ->
                val isDone = doneToday.contains(habit)
                val streak = MetabolicEngine.computeHabitStreak(state, habit)
                Surface(
                    color = if (isDone) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleHabitToday(habit) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = isDone,
                                onCheckedChange = { onToggleHabitToday(habit) }
                            )
                            Column {
                                Text(
                                    text = habit,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "🔥 $streak day streak",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { onDeleteHabit(habit) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete habit $habit")
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newHabitText,
                    onValueChange = { newHabitText = it },
                    label = { Text("New daily habit…") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("new_habit_input")
                )
                Button(
                    onClick = {
                        onAddHabit(newHabitText)
                        newHabitText = ""
                    },
                    modifier = Modifier.testTag("add_habit_button")
                ) {
                    Text("Add")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun JournalSubScreen(
    state: LeanPathState,
    onAddJournalEntry: (String, String) -> Unit,
    onDeleteJournalEntry: (String) -> Unit
) {
    var entryType by remember { mutableStateOf("nsv") }
    var text by remember { mutableStateOf("") }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Journal & Non-Scale Victories", style = MaterialTheme.typography.titleLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = entryType == "nsv",
                    onClick = { entryType = "nsv" },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("🏆 Non-Scale Victory")
                }
                SegmentedButton(
                    selected = entryType == "note",
                    onClick = { entryType = "note" },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Daily Reflection")
                }
            }

            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(if (entryType == "nsv") "e.g., Jeans fit better, climbed stairs effortlessly…" else "How did today go?") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("journal_text_input")
            )
            Button(
                onClick = {
                    onAddJournalEntry(entryType, text)
                    text = ""
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("save_journal_button")
            ) {
                Text("Save Entry")
            }
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Recent Entries", style = MaterialTheme.typography.titleLarge)
            state.journalEntries.reversed().forEach { j ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${if (j.type == "nsv") "🏆 " else "📝 "}${j.text}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (j.type == "nsv") FontWeight.SemiBold else FontWeight.Normal
                        )
                        Text(j.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { onDeleteJournalEntry(j.id) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete journal entry")
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun CalendarHeatmapSubScreen(state: LeanPathState) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("90-Day Consistency Matrix", style = MaterialTheme.typography.titleLarge)
            ConsistencyHeatmap90Days(state = state)
            Text(
                text = "Each cell represents one day across the last 91 days. Opacity reflects how many of the 4 core actions were logged: Weigh-In, Food, Hydration, and Exercise.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyReviewSubScreen(state: LeanPathState) {
    val units = state.profile.units
    val thisWeek = remember(state) { MetabolicEngine.computeWeeklyReview(state, 0) }
    val lastWeek = remember(state) { MetabolicEngine.computeWeeklyReview(state, 1) }
    val metrics = remember(state) { MetabolicEngine.calculateMetrics(state) }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    val weightDelta = if (thisWeek.avgWeightKg != null && lastWeek.avgWeightKg != null) {
        thisWeek.avgWeightKg - lastWeek.avgWeightKg
    } else null

    val radarValues = listOf(
        (thisWeek.weighInDays / 7f).coerceIn(0f, 1f),
        (thisWeek.foodLoggedDays / 7f).coerceIn(0f, 1f),
        min(1f, thisWeek.exerciseDays / 3f),
        (thisWeek.waterDays / 7f).coerceIn(0f, 1f),
        min(1f, ((thisWeek.avgSleepHours ?: 0.0) / 8.0).toFloat())
    )

    val weeklyBudget = metrics.targetKcal * 7
    val weekendHigh = metrics.targetKcal + 300
    val weekdayLow = ((weeklyBudget - 2 * weekendHigh) / 5.0).roundToInt()

    val lost = max(0.0, state.profile.startWeightKg - MetabolicEngine.currentWeightKg(state))
    val totalGoal = (state.profile.startWeightKg - state.profile.goalWeightKg).coerceAtLeast(0.1)
    val pct = ((lost / totalGoal) * 100.0).roundToInt().coerceIn(0, 100)
    val shareSummary = "LeanPath: ${MetabolicEngine.formatWeight(lost, units)} lost, $pct% to my goal, ${MetabolicEngine.computeStreak(state)}-day streak! 💪"

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("This Week's Telemetry", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ReviewTile(
                    value = thisWeek.avgWeightKg?.let { MetabolicEngine.formatWeight(it, units) } ?: "–",
                    subtitle = weightDelta?.let {
                        val v = if (units == "imp") MetabolicEngine.kgToLb(it) else it
                        String.format(Locale.US, "Avg (%+.2f)", v)
                    } ?: "Avg weight",
                    modifier = Modifier.weight(1f)
                )
                ReviewTile(
                    value = thisWeek.avgKcal?.roundToInt()?.toString() ?: "–",
                    subtitle = "Avg kcal",
                    modifier = Modifier.weight(1f)
                )
                ReviewTile(
                    value = "${thisWeek.activeMinutes}m",
                    subtitle = "Active min",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                ReviewTile("${thisWeek.foodLoggedDays}/7", "Days food", Modifier.weight(1f))
                ReviewTile("${thisWeek.waterDays}/7", "Days water", Modifier.weight(1f))
                ReviewTile(
                    thisWeek.avgSleepHours?.let { String.format(Locale.US, "%.1fh", it) } ?: "–",
                    "Avg sleep",
                    Modifier.weight(1f)
                )
            }
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("5-Axis Habit Radar", style = MaterialTheme.typography.titleLarge)
            HabitRadarChart(values = radarValues)
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Social Weekend Calorie Cycling", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Preserves your exact weekly budget ($weeklyBudget kcal) while allocating extra flexibility for Friday/Saturday social meals:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Monday – Friday", style = MaterialTheme.typography.bodyMedium)
                Text("$weekdayLow kcal / day", style = MaterialTheme.typography.titleSmall, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Saturday – Sunday", style = MaterialTheme.typography.bodyMedium)
                Text("$weekendHigh kcal / day", style = MaterialTheme.typography.titleSmall, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
            }
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Share Progress Summary", style = MaterialTheme.typography.titleLarge)
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = shareSummary,
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilledTonalButton(
                    onClick = { clipboard.setText(AnnotatedString(shareSummary)) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy")
                }
                Button(
                    onClick = {
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareSummary)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Share LeanPath Progress"))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }
            }
        }
    }
}

@Composable
private fun ReviewTile(value: String, subtitle: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontFamily = JetBrainsMonoFontFamily, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MealPlannerSubScreen(
    state: LeanPathState,
    onUpdateMealPlan: (Int, String) -> Unit,
    onSendMealPlanToGrocery: () -> Unit
) {
    val days = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Weekly Meal Architecture", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Separate meals or ingredients with commas to export directly to your Grocery List.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            days.forEachIndexed { idx, dayName ->
                var localText by remember(state.weeklyMealPlan[idx]) {
                    mutableStateOf(state.weeklyMealPlan[idx] ?: "")
                }
                OutlinedTextField(
                    value = localText,
                    onValueChange = {
                        localText = it
                        onUpdateMealPlan(idx, it)
                    },
                    label = { Text(dayName) },
                    placeholder = { Text("e.g., Oats, chicken salad, salmon & broccoli") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Button(
                onClick = onSendMealPlanToGrocery,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("send_meal_plan_to_grocery_button")
            ) {
                Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Ingredients to Grocery List")
            }
        }
    }
}

@Composable
private fun GrocerySubScreen(
    state: LeanPathState,
    onAddGroceryItem: (String) -> Unit,
    onToggleGroceryItem: (String) -> Unit,
    onRemoveGroceryItem: (String) -> Unit,
    onClearCheckedGroceries: () -> Unit
) {
    var newItemText by remember { mutableStateOf("") }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Smart Grocery List", style = MaterialTheme.typography.titleLarge)
                if (state.groceryList.any { it.checked }) {
                    TextButton(onClick = onClearCheckedGroceries) {
                        Text("Clear Checked")
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newItemText,
                    onValueChange = { newItemText = it },
                    label = { Text("Add grocery item…") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("grocery_item_input")
                )
                Button(
                    onClick = {
                        onAddGroceryItem(newItemText)
                        newItemText = ""
                    },
                    modifier = Modifier.testTag("add_grocery_button")
                ) {
                    Text("Add")
                }
            }

            if (state.groceryList.isEmpty()) {
                Text(
                    text = "Your grocery list is empty. Add items above or generate from your Weekly Meal Planner.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                state.groceryList.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleGroceryItem(item.id) }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = item.checked,
                                onCheckedChange = { onToggleGroceryItem(item.id) }
                            )
                            Text(
                                text = item.text,
                                style = MaterialTheme.typography.bodyLarge,
                                textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (item.checked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        IconButton(onClick = { onRemoveGroceryItem(item.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove ${item.text}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressPhotosSubScreen(
    state: LeanPathState,
    onAddProgressPhoto: (String, String) -> Unit,
    onDeleteProgressPhoto: (String) -> Unit
) {
    var noteText by remember { mutableStateOf("") }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onAddProgressPhoto(uri.toString(), noteText)
            noteText = ""
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("On-Device Progress Photos", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Uses Android's zero-permission Photo Picker. Photos remain strictly on your device.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Optional photo note (e.g. Morning fasted, 81.5 kg)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pick_progress_photo_button")
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Progress Photo")
            }

            if (state.progressPhotos.size >= 2) {
                val first = state.progressPhotos.first()
                val latest = state.progressPhotos.last()
                HorizontalDivider()
                Text("Before vs. Latest Comparison", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Before · ${first.date}", style = MaterialTheme.typography.labelSmall)
                        AsyncImage(
                            model = first.uriString,
                            contentDescription = "Before progress photo ${first.date}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Latest · ${latest.date}", style = MaterialTheme.typography.labelSmall)
                        AsyncImage(
                            model = latest.uriString,
                            contentDescription = "Latest progress photo ${latest.date}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }

            state.progressPhotos.reversed().forEach { photo ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        AsyncImage(
                            model = photo.uriString,
                            contentDescription = "Progress photo ${photo.date}",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Column {
                            Text(photo.date, style = MaterialTheme.typography.titleSmall)
                            if (photo.note.isNotBlank()) {
                                Text(photo.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    IconButton(onClick = { onDeleteProgressPhoto(photo.id) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete photo")
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalsRemindersSubScreen(
    state: LeanPathState,
    onUpdateMilestoneReward: (Int, String) -> Unit,
    onAddReminder: (String, String) -> Unit,
    onDeleteReminder: (String) -> Unit
) {
    val cur = MetabolicEngine.currentWeightKg(state)
    val totalGoal = (state.profile.startWeightKg - state.profile.goalWeightKg).coerceAtLeast(0.1)
    val progressPct = ((state.profile.startWeightKg - cur) / totalGoal * 100.0).coerceIn(0.0, 100.0)

    var reminderTime by remember { mutableStateOf("08:00") }
    var reminderLabel by remember { mutableStateOf("") }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Non-Food Milestone Rewards", style = MaterialTheme.typography.titleLarge)
            listOf(25, 50, 75, 100).forEach { m ->
                val unlocked = progressPct >= m
                var rewardText by remember(state.milestoneRewards[m]) {
                    mutableStateOf(state.milestoneRewards[m] ?: "")
                }
                OutlinedTextField(
                    value = rewardText,
                    onValueChange = {
                        rewardText = it
                        onUpdateMilestoneReward(m, it)
                    },
                    label = { Text("$m% Goal ${if (unlocked) "🎉 Unlocked!" else ""}") },
                    placeholder = { Text("A non-food reward") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Daily Check-In Reminders", style = MaterialTheme.typography.titleLarge)
            state.reminders.forEach { rem ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${rem.timeHm} · ${rem.label}", style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { onDeleteReminder(rem.id) }) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete reminder")
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = reminderTime,
                    onValueChange = { reminderTime = it },
                    label = { Text("Time (HH:MM)") },
                    singleLine = true,
                    modifier = Modifier.weight(0.9f)
                )
                OutlinedTextField(
                    value = reminderLabel,
                    onValueChange = { reminderLabel = it },
                    label = { Text("Reminder label") },
                    singleLine = true,
                    modifier = Modifier.weight(1.4f)
                )
                Button(
                    onClick = {
                        onAddReminder(reminderTime, reminderLabel)
                        reminderLabel = ""
                    }
                ) {
                    Text("Add")
                }
            }
        }
    }
}
