package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.MetabolicEngine
import com.example.model.EVIDENCE_NOTES
import com.example.model.LeanPathState
import com.example.model.ProfileMeta
import com.example.ui.components.CorrelationScatterChart
import com.example.ui.components.DeficitWaterfallChart
import com.example.ui.theme.JetBrainsMonoFontFamily
import java.time.LocalTime
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ScienceScreen(
    state: LeanPathState,
    activeProfileKey: String,
    profiles: List<ProfileMeta>,
    onUpdateUnits: (String) -> Unit,
    onUpdateTheme: (String) -> Unit,
    onUpdateActivityLevel: (Double) -> Unit,
    onSwitchProfile: (String) -> Unit,
    onCreateNewProfile: (String) -> Unit,
    onLoadDemoData: () -> Unit,
    onReopenOnboarding: () -> Unit,
    onImportJsonBackup: (String) -> Boolean,
    onResetAllData: () -> Unit,
    modifier: Modifier = Modifier
) {
    val units = state.profile.units
    val today = MetabolicEngine.dateOffset(0)
    val metrics = remember(state) { MetabolicEngine.calculateMetrics(state) }
    val weeklyRateKg = remember(state) { MetabolicEngine.computeWeeklyRateKg(state) }
    val adaptiveTdee = remember(state) { MetabolicEngine.computeAdaptiveTdee(state) }
    val isPlateau = remember(state) { MetabolicEngine.isPlateau(state) }
    val badges = remember(state) { MetabolicEngine.computeUnlockedBadges(state) }
    val curKg = MetabolicEngine.currentWeightKg(state)
    val todayEaten = remember(state.foodByDate[today]) {
        MetabolicEngine.computeDailyFoodTotals(state.foodByDate[today].orEmpty())
    }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var newProfileName by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }
    var showResetConfirm by remember { mutableStateOf(false) }
    var expandedEvidenceIndex by remember { mutableIntStateOf(-1) }

    // Build dynamic clinical advisories
    val advisories = remember(state, metrics, weeklyRateKg, adaptiveTdee, isPlateau, todayEaten) {
        buildList {
            if (state.weighIns.size < 7) {
                add("info" to "Log your weight for 7+ days to unlock full EMA trend analysis. Daily weight swings of 1–2 kg are normal water shifts.")
            }
            if (weeklyRateKg != null) {
                val pct = -weeklyRateKg / curKg.coerceAtLeast(1.0) * 100.0
                val msg = when {
                    pct > 1.0 -> "You are losing ${MetabolicEngine.formatWeight(-weeklyRateKg, units, 2)}/week (${String.format(Locale.US, "%.2f", pct)}% body weight). This exceeds the 1% ceiling and increases lean muscle catabolism. Consider adding 150–250 kcal/day."
                    pct < 0.25 -> "You are losing ${MetabolicEngine.formatWeight(-weeklyRateKg, units, 2)}/week (${String.format(Locale.US, "%.2f", pct)}% body weight). Steady, gentle progress."
                    else -> "You are losing ${MetabolicEngine.formatWeight(-weeklyRateKg, units, 2)}/week (${String.format(Locale.US, "%.2f", pct)}% body weight) — right inside the optimal 0.25%–1.0% clinical zone."
                }
                val severity = if (pct > 1.0) "error" else if (pct < 0.25) "warn" else "info"
                add(severity to msg)
            }
            if (isPlateau) {
                add("warn" to "Plateau detected: your EMA trend has moved <0.2 kg over 14 days. Consider a 1–2 week maintenance diet break, +2,000 daily steps, or tighter portion weighing.")
            }
            if (adaptiveTdee != null) {
                add("info" to "Adaptive TDEE from your real logs: $adaptiveTdee kcal/day (Formula estimate: ${metrics.tdee.roundToInt()} kcal/day). Computed from average intake minus EMA trend delta × 7,700 kcal/kg.")
            } else {
                add("info" to "Adaptive TDEE unlocks automatically after 10+ food-logged days across 14+ days of weigh-ins.")
            }
            if (todayEaten.kcal > 0 && todayEaten.proteinG < metrics.proteinG * 0.6 && LocalTime.now().hour >= 15) {
                add("warn" to "Protein is behind target today (${todayEaten.proteinG.roundToInt()}g / ${metrics.proteinG}g). Prioritizing protein at dinner preserves lean mass and satiety.")
            }
            if (weeklyRateKg != null && weeklyRateKg < -0.02) {
                val wks = (curKg - state.profile.goalWeightKg) / -weeklyRateKg
                if (wks > 0) {
                    val eta = MetabolicEngine.dateOffset((wks * 7.0).roundToInt().toLong())
                    add("info" to "At your current EMA trend velocity, you will reach your goal weight around $eta.")
                }
            }
        }
    }

    // Correlation pairs: Calories vs next-day weight change & Sleep vs next-day weight change
    val (calorieNextDayPairs, sleepNextDayPairs) = remember(state) {
        val calPairs = mutableListOf<Pair<Double, Double>>()
        val slpPairs = mutableListOf<Pair<Double, Double>>()
        val sortedW = state.weighIns.sortedBy { it.date }
        for (i in 0 until sortedW.size - 1) {
            val wToday = sortedW[i]
            val wNext = sortedW[i + 1]
            if (MetabolicEngine.daysBetween(wToday.date, wNext.date) == 1L) {
                val dw = wNext.weightKg - wToday.weightKg
                val foods = state.foodByDate[wToday.date].orEmpty()
                if (foods.isNotEmpty()) {
                    calPairs.add(foods.sumOf { it.kcal } to dw)
                }
                state.wellnessByDate[wToday.date]?.let { well ->
                    slpPairs.add(well.sleepHours to dw)
                }
            }
        }
        calPairs to slpPairs
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Coaching Advisories
        advisories.forEach { (severity, text) ->
            val containerColor = when (severity) {
                "error" -> MaterialTheme.colorScheme.errorContainer
                "warn" -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.secondaryContainer
            }
            val contentColor = when (severity) {
                "error" -> MaterialTheme.colorScheme.onErrorContainer
                "warn" -> MaterialTheme.colorScheme.onTertiaryContainer
                else -> MaterialTheme.colorScheme.onSecondaryContainer
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = containerColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = when (severity) {
                            "error" -> Icons.Default.WarningAmber
                            "warn" -> Icons.Default.TipsAndUpdates
                            else -> Icons.Default.Science
                        },
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor
                    )
                }
            }
        }

        // Weekly Deficit Waterfall Chart Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Weekly Deficit Waterfall", style = MaterialTheme.typography.titleLarge)
                DeficitWaterfallChart(
                    state = state,
                    tdee = adaptiveTdee?.toDouble() ?: metrics.tdee
                )
            }
        }

        // Correlation Scatter Plots Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Bio-Telemetry Correlations", style = MaterialTheme.typography.titleLarge)
                CorrelationScatterChart(
                    title = "Calories vs. Next-Day Scale Delta",
                    xAxisLabel = "calories eaten",
                    points = calorieNextDayPairs
                )
                HorizontalDivider()
                CorrelationScatterChart(
                    title = "Sleep Duration vs. Next-Day Scale Delta",
                    xAxisLabel = "sleep hours",
                    points = sleepNextDayPairs
                )
            }
        }

        // What-If Calorie Simulator Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("What-If Calorie Simulator", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Simulated weekly loss rate and time to goal (${MetabolicEngine.formatWeight(state.profile.goalWeightKg, units)}) around your ${metrics.targetKcal} kcal/day target:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(-200, -100, 0, 100, 200).forEach { delta ->
                    val simKcal = metrics.targetKcal + delta
                    val wkLossKg = (metrics.tdee - simKcal) * 7.0 / 7700.0
                    val wksToGoal = if (wkLossKg > 0.01) {
                        ceil((curKg - state.profile.goalWeightKg).coerceAtLeast(0.0) / wkLossKg).toInt()
                    } else null

                    Surface(
                        color = if (delta == 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$simKcal kcal/day ${if (delta == 0) "(Current)" else ""}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (delta == 0) FontWeight.Bold else FontWeight.Normal
                            )
                            Text(
                                text = "${MetabolicEngine.formatWeight(wkLossKg, units, 2)}/wk · ${wksToGoal?.let { "$it wks" } ?: "no loss"}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Full 15-Metric Anthropometric & Metabolic Science Panel
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Metabolic & Anthropometric Lab", style = MaterialTheme.typography.titleLarge)
                val rows = listOf(
                    "BMI" to "${String.format(Locale.US, "%.1f", metrics.bmi)} (${metrics.bmiCategory})",
                    "BMI Prime" to String.format(Locale.US, "%.2f", metrics.bmiPrime),
                    "Ponderal Index" to "${String.format(Locale.US, "%.1f", metrics.ponderalIndex)} kg/m³",
                    "BMR (Mifflin-St Jeor)" to "${metrics.bmrMifflin.roundToInt()} kcal",
                    "BMR (Harris-Benedict)" to "${metrics.bmrHarrisBenedict.roundToInt()} kcal",
                    "BMR (Katch-McArdle)" to (metrics.bmrKatchMcArdle?.let { "${it.roundToInt()} kcal" } ?: "Needs body-fat %"),
                    "TDEE (Activity × BMR)" to "${metrics.tdee.roundToInt()} kcal",
                    "Adaptive TDEE (Real Data)" to (adaptiveTdee?.let { "$it kcal" } ?: "Collecting logs"),
                    "Body Fat (US Navy)" to (metrics.bodyFatPct?.let { String.format(Locale.US, "%.1f%%", it) } ?: "Add waist & neck"),
                    "Lean / Fat Mass" to if (metrics.leanMassKg != null && metrics.fatMassKg != null) {
                        "${MetabolicEngine.formatWeight(metrics.leanMassKg, units)} / ${MetabolicEngine.formatWeight(metrics.fatMassKg, units)}"
                    } else "–",
                    "Waist-to-Height Ratio" to (metrics.waistToHeightRatio?.let {
                        String.format(Locale.US, "%.2f %s", it, if (it < 0.5) "✓ (<0.50)" else "(aim <0.50)")
                    } ?: "–"),
                    "Waist-to-Hip Ratio" to (metrics.waistToHipRatio?.let { String.format(Locale.US, "%.2f", it) } ?: "–"),
                    "Healthy BMI Weight Range" to "${MetabolicEngine.formatWeight(metrics.healthyMinKg, units)} – ${MetabolicEngine.formatWeight(metrics.healthyMaxKg, units)}",
                    "Ideal Weight (Devine / Robinson)" to "${MetabolicEngine.formatWeight(metrics.idealDevineKg, units)} / ${MetabolicEngine.formatWeight(metrics.idealRobinsonKg, units)}",
                    "Ideal Weight (Miller / Hamwi)" to "${MetabolicEngine.formatWeight(metrics.idealMillerKg, units)} / ${MetabolicEngine.formatWeight(metrics.idealHamwiKg, units)}"
                )
                rows.forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                }
            }
        }

        // Unlocked Badges Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Earned Achievements (${badges.size}/9)", style = MaterialTheme.typography.titleLarge)
                if (badges.isEmpty()) {
                    Text("Log your first weigh-in to earn badges.", style = MaterialTheme.typography.bodySmall)
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        badges.forEach { badge ->
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Text(
                                    text = "🏅 $badge",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 15 Peer-Reviewed Evidence Notes Accordion Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Clinical & Scientific Evidence Notes", style = MaterialTheme.typography.titleLarge)
                EVIDENCE_NOTES.forEachIndexed { idx, note ->
                    val isExpanded = expandedEvidenceIndex == idx
                    Surface(
                        onClick = { expandedEvidenceIndex = if (isExpanded) -1 else idx },
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(note.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Icon(
                                    if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }
                            AnimatedVisibility(visible = isExpanded) {
                                Text(
                                    text = note.body,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Profiles, Settings & Data Portability Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Profiles, Preferences & Data Backup", style = MaterialTheme.typography.titleLarge)

                Text("Units", style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = units == "met",
                        onClick = { onUpdateUnits("met") },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                    ) {
                        Text("Metric (kg, cm)")
                    }
                    SegmentedButton(
                        selected = units == "imp",
                        onClick = { onUpdateUnits("imp") },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                    ) {
                        Text("Imperial (lb, in)")
                    }
                }

                Text("Appearance Theme", style = MaterialTheme.typography.labelLarge)
                val themeOptions = listOf("" to "System", "light" to "Light", "dark" to "Dark")
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    themeOptions.forEachIndexed { idx, (key, label) ->
                        SegmentedButton(
                            selected = state.themePreference == key,
                            onClick = { onUpdateTheme(key) },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = 3)
                        ) {
                            Text(label)
                        }
                    }
                }

                Text("Activity Multiplier", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        1.2 to "Sedentary",
                        1.375 to "Light",
                        1.55 to "Moderate",
                        1.725 to "Active",
                        1.9 to "Athlete"
                    ).forEach { (factor, label) ->
                        FilterChip(
                            selected = state.profile.activityLevel == factor,
                            onClick = { onUpdateActivityLevel(factor) },
                            label = { Text(label) }
                        )
                    }
                }

                HorizontalDivider()

                // Multi-profile management
                Text("Local Profiles", style = MaterialTheme.typography.titleMedium)
                profiles.forEach { prof ->
                    val isCurrent = prof.key == activeProfileKey
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${prof.name}${if (isCurrent) " ✓ (Active)" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
                        )
                        if (!isCurrent) {
                            FilledTonalButton(onClick = { onSwitchProfile(prof.key) }) {
                                Text("Switch")
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newProfileName,
                        onValueChange = { newProfileName = it },
                        label = { Text("New profile name") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            onCreateNewProfile(newProfileName)
                            newProfileName = ""
                        }
                    ) {
                        Text("Create")
                    }
                }

                HorizontalDivider()

                // Export / Import / Demo / Onboarding / Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            val json = MetabolicEngine.serializeStateToJson(state)
                            clipboard.setText(AnnotatedString(json))
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/json"
                                putExtra(Intent.EXTRA_TEXT, json)
                            }
                            context.startActivity(Intent.createChooser(intent, "Export LeanPath JSON Backup"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_json_button")
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export JSON")
                    }

                    FilledTonalButton(
                        onClick = {
                            val csv = MetabolicEngine.exportCsv(state)
                            clipboard.setText(AnnotatedString(csv))
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_TEXT, csv)
                            }
                            context.startActivity(Intent.createChooser(intent, "Export LeanPath CSV"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("export_csv_button")
                    ) {
                        Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export CSV")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Import JSON")
                    }
                    OutlinedButton(
                        onClick = onReopenOnboarding,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reconfigure_plan_button")
                    ) {
                        Text("Edit Blueprint")
                    }
                }

                OutlinedButton(
                    onClick = onLoadDemoData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reload_demo_button")
                ) {
                    Icon(Icons.Default.AutoGraph, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Load 30-Day Science Demo Dataset")
                }

                TextButton(
                    onClick = { showResetConfirm = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reset_all_data_button")
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset All Profile Data")
                }

                Text(
                    text = "LeanPath provides general educational information, not medical advice. Consult a qualified physician before changing your nutrition or exercise program.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Import JSON Backup") },
            text = {
                OutlinedTextField(
                    value = importJsonText,
                    onValueChange = { importJsonText = it },
                    label = { Text("Paste LeanPath JSON backup") },
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (onImportJsonBackup(importJsonText)) {
                            showImportDialog = false
                            importJsonText = ""
                        }
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset All Data?") },
            text = { Text("This will clear the current profile's logs and reopen the 6-step Metabolic Blueprint Wizard.") },
            confirmButton = {
                Button(
                    onClick = {
                        showResetConfirm = false
                        onResetAllData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete & Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
