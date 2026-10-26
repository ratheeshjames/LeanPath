package com.example.ui.screens

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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.domain.MetabolicEngine
import com.example.model.LeanPathState
import com.example.ui.components.WeightProjectionConeChart
import com.example.ui.components.WeightTrendChart
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrendScreen(
    state: LeanPathState,
    rangeDays: Int,
    onSelectRangeDays: (Int) -> Unit,
    onAddBackdatedWeight: (Double, String) -> Unit,
    onDeleteWeight: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val units = state.profile.units
    val today = MetabolicEngine.dateOffset(0)
    val allEma = remember(state.weighIns) { MetabolicEngine.computeEmaTrend(state.weighIns) }
    val filteredEma = remember(allEma, rangeDays, today) {
        if (rangeDays == 0) allEma
        else allEma.filter { MetabolicEngine.daysBetween(it.date, today) <= rangeDays }
    }
    val latestTrend = allEma.lastOrNull()
    val weeklyRateKg = remember(state) { MetabolicEngine.computeWeeklyRateKg(state) }

    fun avgWeightDays(days: Int): String {
        val sub = state.weighIns.filter { MetabolicEngine.daysBetween(it.date, today) < days }
        return if (sub.isNotEmpty()) {
            MetabolicEngine.formatWeight(sub.map { it.weightKg }.average(), units)
        } else "–"
    }

    var backdateIso by remember { mutableStateOf(MetabolicEngine.dateOffset(-1)) }
    var backdateWeightText by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showWaterNoiseExplainer by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Range Segmented Button + EMA Trend Chart Card
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
                    Text("EMA Trend & Scale Noise", style = MaterialTheme.typography.titleLarge)
                }

                val ranges = listOf(7 to "1W", 30 to "1M", 90 to "3M", 0 to "All")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ranges.forEachIndexed { index, (days, label) ->
                        SegmentedButton(
                            selected = rangeDays == days,
                            onClick = { onSelectRangeDays(days) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = ranges.size),
                            modifier = Modifier.testTag("trend_range_$label")
                        ) {
                            Text(label)
                        }
                    }
                }

                WeightTrendChart(
                    points = filteredEma,
                    goalWeightKg = state.profile.goalWeightKg,
                    units = units
                )

                Text(
                    text = "Dots = daily scale weight · Solid curve = EMA trend (α = 0.1 smoothing filters glycogen & water noise)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 6-Cell Bento Grid of Trend Telemetry
        val trendKg = latestTrend?.trendKg ?: state.profile.startWeightKg
        val lostTrendKg = state.profile.startWeightKg - trendKg
        val daysToTarget = max(0L, MetabolicEngine.daysBetween(today, state.profile.targetDate))

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TrendStatTile(
                    value = MetabolicEngine.formatWeight(trendKg, units),
                    label = "Current Trend",
                    modifier = Modifier.weight(1f)
                )
                TrendStatTile(
                    value = weeklyRateKg?.let {
                        val disp = if (units == "imp") MetabolicEngine.kgToLb(it) else it
                        String.format(Locale.US, "%.2f %s", disp, MetabolicEngine.weightUnit(units))
                    } ?: "–",
                    label = "Per Week",
                    modifier = Modifier.weight(1f)
                )
                TrendStatTile(
                    value = MetabolicEngine.formatWeight(lostTrendKg, units),
                    label = "Trend Lost",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TrendStatTile(
                    value = avgWeightDays(7),
                    label = "7d Scale Avg",
                    modifier = Modifier.weight(1f)
                )
                TrendStatTile(
                    value = avgWeightDays(30),
                    label = "30d Scale Avg",
                    modifier = Modifier.weight(1f)
                )
                TrendStatTile(
                    value = "${daysToTarget}d",
                    label = "To Target Date",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Expandable Water Fluctuation Science Explainer
        OutlinedCard(
            onClick = { showWaterNoiseExplainer = !showWaterNoiseExplainer },
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
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
                            Icons.Default.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Why does my scale weight jump around?",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Icon(
                        if (showWaterNoiseExplainer) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                }
                AnimatedVisibility(visible = showWaterNoiseExplainer) {
                    Text(
                        text = "Daily scale weight can swing 0.5–2.0 kg without any change in body fat. Common causes: water retention from sodium or carbohydrate (muscle glycogen binds ~3g of water per gram), food mass still being digested, hormonal cycles, cortisol/sleep shifts, and intense workouts causing temporary muscle repair inflammation. LeanPath's EMA trend line smooths out this daily noise so you see your true metabolic direction.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }

        // Goal Projection Cone Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Trajectory Projection Cone", style = MaterialTheme.typography.titleLarge)
                if (weeklyRateKg == null || weeklyRateKg >= -0.02 || latestTrend == null) {
                    Text(
                        text = "Needs 5+ days of weigh-ins and a downward trend to render your confidence cone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val weeks = ceil((latestTrend.trendKg - state.profile.goalWeightKg) / -weeklyRateKg).toInt().coerceAtLeast(1)
                    val projectedDate = MetabolicEngine.dateOffset((weeks * 7).toLong())
                    WeightProjectionConeChart(
                        currentTrendKg = latestTrend.trendKg,
                        goalWeightKg = state.profile.goalWeightKg,
                        weeklyRateKg = weeklyRateKg,
                        units = units
                    )
                    Text(
                        text = "Projected goal arrival in ~$weeks weeks ($projectedDate). Shaded cone illustrates a ±40% faster or slower pace.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Before and Now Comparison Card
        val startMetrics = remember(state) {
            MetabolicEngine.calculateMetrics(state, weightKgOverride = state.profile.startWeightKg)
        }
        val currentMetrics = remember(state) { MetabolicEngine.calculateMetrics(state) }
        val firstWaist = state.measurements.firstOrNull { it.siteKey == "waist" }?.valueCmOrPct ?: state.profile.waistCm
        val latestWaist = state.measurements.lastOrNull { it.siteKey == "waist" }?.valueCmOrPct ?: state.profile.waistCm

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Before & Now", style = MaterialTheme.typography.titleLarge)
                BeforeNowRow(
                    label = "Scale Weight",
                    before = MetabolicEngine.formatWeight(state.profile.startWeightKg, units),
                    now = MetabolicEngine.formatWeight(MetabolicEngine.currentWeightKg(state), units)
                )
                BeforeNowRow(
                    label = "BMI",
                    before = String.format(Locale.US, "%.1f", startMetrics.bmi),
                    now = String.format(Locale.US, "%.1f", currentMetrics.bmi)
                )
                BeforeNowRow(
                    label = "Energy Needs (TDEE)",
                    before = "${startMetrics.tdee.roundToInt()} kcal",
                    now = "${currentMetrics.tdee.roundToInt()} kcal"
                )
                if (firstWaist != null && latestWaist != null) {
                    BeforeNowRow(
                        label = "Waist Circumference",
                        before = MetabolicEngine.formatMeasurement("waist", firstWaist, units),
                        now = MetabolicEngine.formatMeasurement("waist", latestWaist, units)
                    )
                }
            }
        }

        // Milestones Unlocked Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Weight Loss Milestones", style = MaterialTheme.typography.titleLarge)
                val stepKg = if (units == "imp") 1.0 / 2.20462 else 1.0
                val count = if (latestTrend != null) {
                    floor((state.profile.startWeightKg - latestTrend.trendKg) / stepKg).toInt().coerceAtLeast(0)
                } else 0

                if (count <= 0) {
                    Text(
                        text = "Your first 1 ${MetabolicEngine.weightUnit(units)} trend milestone is on the horizon!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    (1..count).toList().takeLast(8).reversed().forEach { k ->
                        val milestoneDate = allEma.firstOrNull {
                            it.trendKg <= state.profile.startWeightKg - k * stepKg
                        }?.date ?: today
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "🏁 $k ${MetabolicEngine.weightUnit(units)} lost",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = milestoneDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Add Past Weigh-In Entry Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Add Past Weigh-In", style = MaterialTheme.typography.titleLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(backdateIso)
                    }

                    OutlinedTextField(
                        value = backdateWeightText,
                        onValueChange = { backdateWeightText = it },
                        label = { Text(MetabolicEngine.weightUnit(units)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("backdate_weight_input")
                    )

                    Button(
                        onClick = {
                            backdateWeightText.toDoubleOrNull()?.let { w ->
                                onAddBackdatedWeight(w, backdateIso)
                                backdateWeightText = ""
                            }
                        },
                        modifier = Modifier.testTag("add_backdate_weight_button")
                    ) {
                        Text("Add")
                    }
                }
            }
        }

        // Weigh-In History List Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Recent Weigh-In History", style = MaterialTheme.typography.titleLarge)
                state.weighIns.reversed().take(25).forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = entry.date,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = MetabolicEngine.formatWeight(entry.weightKg, units),
                                style = MaterialTheme.typography.titleSmall,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.Bold
                            )
                            if (state.weighIns.size > 1) {
                                IconButton(
                                    onClick = { onDeleteWeight(entry.date) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Delete weigh-in ${entry.date}",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val d = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            backdateIso = d.toString()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun TrendStatTile(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BeforeNowRow(
    label: String,
    before: String,
    now: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = "$before → $now",
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold
        )
    }
}
