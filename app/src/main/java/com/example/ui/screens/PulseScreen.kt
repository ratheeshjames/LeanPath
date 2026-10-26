package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.MetabolicEngine
import com.example.model.DAILY_QUOTES
import com.example.model.LeanPathState
import com.example.ui.StudioTool
import com.example.ui.components.BmiSpectrumBar
import com.example.ui.components.RadialMetabolicGauge
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun PulseScreen(
    state: LeanPathState,
    nowMillis: Long,
    onLogWeight: (Double) -> Unit,
    onAddWaterMl: (Int) -> Unit,
    onStartFast: (Int) -> Unit,
    onEndFast: () -> Unit,
    onDismissReminder: (String) -> Unit,
    onOpenFoodTab: () -> Unit,
    onOpenStudioTool: (StudioTool) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = MetabolicEngine.dateOffset(0)
    val units = state.profile.units
    val metrics = remember(state) { MetabolicEngine.calculateMetrics(state) }
    val todayFoods = state.foodByDate[today].orEmpty()
    val eaten = remember(todayFoods) { MetabolicEngine.computeDailyFoodTotals(todayFoods) }
    val burnedToday = remember(state, today) { MetabolicEngine.computeBurnedKcal(state, today) }
    val waterMl = state.waterByDateMl[today] ?: 0
    val currentKg = MetabolicEngine.currentWeightKg(state)
    val waterGoalMl = (currentKg * 35.0).roundToInt().coerceAtLeast(1500)

    val todayWeighIn = state.weighIns.find { it.date == today }
    var weightInputText by remember(todayWeighIn, units) {
        mutableStateOf(
            todayWeighIn?.let { MetabolicEngine.formatWeightValueOnly(it.weightKg, units) }
                ?: MetabolicEngine.formatWeightValueOnly(currentKg, units)
        )
    }

    val totalToLose = (state.profile.startWeightKg - state.profile.goalWeightKg).coerceAtLeast(0.1)
    val lostSoFar = state.profile.startWeightKg - currentKg
    val goalProgressPct = ((lostSoFar / totalToLose) * 100.0).coerceIn(0.0, 100.0)
    val streak = remember(state.weighIns) { MetabolicEngine.computeStreak(state) }
    val xp = remember(state) { MetabolicEngine.computeXp(state) }
    val level = remember(xp) { MetabolicEngine.computeLevel(xp) }

    val quote = DAILY_QUOTES[LocalDate.now().dayOfMonth % DAILY_QUOTES.size]
    val nowHm = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
    val dismissedToday = state.dismissedRemindersByDate[today].orEmpty()
    val activeReminders = state.reminders.filter { it.timeHm <= nowHm && !dismissedToday.contains(it.id) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Reminders Banner
        activeReminders.forEach { reminder ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Alarm,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = "${reminder.timeHm} · ${reminder.label}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                    TextButton(
                        onClick = { onDismissReminder(reminder.id) },
                        modifier = Modifier.testTag("dismiss_reminder_${reminder.id}")
                    ) {
                        Text("Dismiss")
                    }
                }
            }
        }

        // Hero Motivation & Gamification Strip
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "$streak-day streak",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Text(
                            text = "Level $level · $xp XP",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                Text(
                    text = quote,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                if (state.profile.motivationWhy.isNotBlank()) {
                    Text(
                        text = "“${state.profile.motivationWhy}”",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Daily Weigh-In & Goal Trajectory Card
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.MonitorWeight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Today's Weigh-In", style = MaterialTheme.typography.titleLarge)
                    }
                    if (todayWeighIn != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "Logged ✓",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            val v = (weightInputText.toDoubleOrNull() ?: 75.0) - 0.1
                            weightInputText = String.format(Locale.US, "%.1f", v)
                        }
                    ) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrease weight by 0.1")
                    }

                    OutlinedTextField(
                        value = weightInputText,
                        onValueChange = { weightInputText = it },
                        label = { Text("Weight (${MetabolicEngine.weightUnit(units)})") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("today_weight_input")
                    )

                    FilledTonalIconButton(
                        onClick = {
                            val v = (weightInputText.toDoubleOrNull() ?: 75.0) + 0.1
                            weightInputText = String.format(Locale.US, "%.1f", v)
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Increase weight by 0.1")
                    }

                    Button(
                        onClick = {
                            weightInputText.toDoubleOrNull()?.let { onLogWeight(it) }
                        },
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("log_weight_button")
                    ) {
                        Text("Log")
                    }
                }

                HorizontalDivider()

                // Goal progress bar
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Goal Progress (${goalProgressPct.roundToInt()}%)",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Text(
                            text = "${MetabolicEngine.formatWeight(max(0.0, currentKg - state.profile.goalWeightKg), units)} to go",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    LinearProgressIndicator(
                        progress = { (goalProgressPct / 100.0).toFloat() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Start: ${MetabolicEngine.formatWeight(state.profile.startWeightKg, units)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Now: ${MetabolicEngine.formatWeight(currentKg, units)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Goal: ${MetabolicEngine.formatWeight(state.profile.goalWeightKg, units)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Calories, Macros & Hydration Core Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text("Metabolic Fuel & Hydration", style = MaterialTheme.typography.titleLarge)
                    }
                    FilledTonalButton(
                        onClick = onOpenFoodTab,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("pulse_add_food_button")
                    ) {
                        Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Log Food")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isOverBudget = eaten.kcal > (metrics.targetKcal + burnedToday)
                    RadialMetabolicGauge(
                        valueText = "${eaten.kcal.roundToInt()}",
                        subtitle = "of ${metrics.targetKcal} kcal",
                        progress = (eaten.kcal / metrics.targetKcal.coerceAtLeast(1)).toFloat(),
                        accentColor = if (isOverBudget) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )

                    RadialMetabolicGauge(
                        valueText = "$waterMl",
                        subtitle = "of $waterGoalMl ml",
                        progress = (waterMl.toFloat() / waterGoalMl.toFloat()),
                        accentColor = HydrationBlue
                    )
                }

                if (burnedToday > 0) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Exercise Burned: +$burnedToday kcal",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                text = "Net left: ${(metrics.targetKcal + burnedToday - eaten.kcal.roundToInt()).coerceAtLeast(0)} kcal",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // 3 Macro Bars
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MacroMiniStatCard(
                        label = "Protein",
                        current = eaten.proteinG.roundToInt(),
                        target = metrics.proteinG,
                        color = ProteinBlue,
                        modifier = Modifier.weight(1f)
                    )
                    MacroMiniStatCard(
                        label = "Carbs",
                        current = eaten.carbsG.roundToInt(),
                        target = metrics.carbsG,
                        color = CarbsEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    MacroMiniStatCard(
                        label = "Fat",
                        current = eaten.fatG.roundToInt(),
                        target = metrics.fatG,
                        color = FatAmber,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Hydration Quick Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onAddWaterMl(-250) },
                        enabled = waterMl > 0,
                        modifier = Modifier.weight(0.8f)
                    ) {
                        Text("-250 ml")
                    }
                    FilledTonalButton(
                        onClick = { onAddWaterMl(250) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("water_add_250_button")
                    ) {
                        Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+250 ml")
                    }
                    FilledTonalButton(
                        onClick = { onAddWaterMl(500) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("water_add_500_button")
                    ) {
                        Icon(Icons.Default.WaterDrop, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+500 ml")
                    }
                }
            }
        }

        // Micronutrients: Fiber, Sodium & Alcohol
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Fiber, Sodium & Alcohol", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${eaten.fiberG.roundToInt()}/${metrics.fiberTargetG}g",
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Fiber", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${eaten.sodiumMg.roundToInt()} mg",
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = if (eaten.sodiumMg > 2300) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            Text("Sodium (<2300)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.0f", eaten.alcoholDrinks),
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Alcohol drinks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        // Body Composition Snapshot Card
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
                    Text("Body Composition", style = MaterialTheme.typography.titleLarge)
                    TextButton(onClick = { onOpenStudioTool(StudioTool.MEASURES) }) {
                        Text("Measures")
                    }
                }

                BmiSpectrumBar(bmi = metrics.bmi)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1f", metrics.bmi),
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "BMI · ${metrics.bmiCategory}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${metrics.tdee.roundToInt()}",
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "TDEE kcal",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = metrics.bodyFatPct?.let { String.format(Locale.US, "%.1f%%", it) } ?: "–",
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Body Fat",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Intermittent Fasting Protocol Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.Timer,
                        contentDescription = null,
                        tint = FastingViolet
                    )
                    Text("Intermittent Fasting Timer", style = MaterialTheme.typography.titleLarge)
                }

                val fast = state.activeFast
                if (fast == null) {
                    Text(
                        text = "Select a fasting window to start tracking your metabolic switch:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(12, 14, 16, 18, 20).forEach { h ->
                            FilledTonalButton(
                                onClick = { onStartFast(h) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("start_fast_${h}h_button")
                            ) {
                                Text("${h}h")
                            }
                        }
                    }
                } else {
                    val elapsedHours = max(0.0, (nowMillis - fast.startTimeMillis) / 3_600_000.0)
                    val progress = min(1.0, elapsedHours / fast.targetHours.toDouble()).toFloat()
                    val reached = elapsedHours >= fast.targetHours

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                        color = FastingViolet,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${String.format(Locale.US, "%.1f", elapsedHours)}h of ${fast.targetHours}h fast",
                                style = MaterialTheme.typography.titleMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (reached) "Goal reached ✓ Autophagy & fat oxidation active" else "${String.format(Locale.US, "%.1f", fast.targetHours - elapsedHours)}h remaining",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (reached) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(
                            onClick = onEndFast,
                            modifier = Modifier.testTag("end_fast_button")
                        ) {
                            Text("End Fast")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroMiniStatCard(
    label: String,
    current: Int,
    target: Int,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    val ratio = (current.toFloat() / target.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$current/${target}g",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold
            )
            LinearProgressIndicator(
                progress = { ratio },
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            )
        }
    }
}
