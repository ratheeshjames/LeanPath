package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.model.UserProfile
import com.example.ui.components.BmiSpectrumBar
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

private val ONBOARDING_STEPS = listOf(
    "Welcome",
    "About You",
    "Your Goal",
    "Lifestyle",
    "Body Measures",
    "Your Blueprint"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    initialProfile: UserProfile,
    onComplete: (UserProfile) -> Unit,
    onLoadDemo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var step by remember { mutableIntStateOf(0) }
    var units by remember { mutableStateOf(initialProfile.units) }
    var sex by remember { mutableStateOf(initialProfile.sex) }
    var ageText by remember { mutableStateOf(initialProfile.age.toString()) }
    var heightText by remember {
        mutableStateOf(
            if (initialProfile.units == "imp")
                String.format(Locale.US, "%.1f", MetabolicEngine.cmToIn(initialProfile.heightCm))
            else
                String.format(Locale.US, "%.1f", initialProfile.heightCm)
        )
    }
    var currentWeightText by remember {
        mutableStateOf(
            MetabolicEngine.formatWeightValueOnly(initialProfile.startWeightKg, initialProfile.units)
        )
    }
    var goalWeightText by remember {
        mutableStateOf(
            MetabolicEngine.formatWeightValueOnly(initialProfile.goalWeightKg, initialProfile.units)
        )
    }
    var targetDateIso by remember {
        mutableStateOf(initialProfile.targetDate.ifEmpty { MetabolicEngine.dateOffset(120) })
    }
    var activityLevel by remember { mutableDoubleStateOf(initialProfile.activityLevel) }
    var dietStyle by remember { mutableStateOf(initialProfile.dietStyle) }
    var healthConsideration by remember { mutableStateOf(initialProfile.healthConsideration) }
    var motivationWhy by remember { mutableStateOf(initialProfile.motivationWhy) }
    var waistText by remember { mutableStateOf(initialProfile.waistCm?.toString() ?: "") }
    var neckText by remember { mutableStateOf(initialProfile.neckCm?.toString() ?: "") }
    var hipText by remember { mutableStateOf(initialProfile.hipCm?.toString() ?: "") }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    BackHandler(enabled = step > 0) {
        validationError = null
        step--
    }

    fun buildDraftProfile(): UserProfile {
        val rawH = heightText.toDoubleOrNull() ?: if (units == "imp") 69.0 else 175.0
        val hCm = if (units == "imp") MetabolicEngine.inToCm(rawH) else rawH
        val w0Kg = MetabolicEngine.toKgInput(currentWeightText.toDoubleOrNull() ?: 85.0, units)
        val goalKg = MetabolicEngine.toKgInput(goalWeightText.toDoubleOrNull() ?: 75.0, units)
        val waistCm = waistText.toDoubleOrNull()?.let { if (units == "imp") MetabolicEngine.inToCm(it) else it }
        val neckCm = neckText.toDoubleOrNull()?.let { if (units == "imp") MetabolicEngine.inToCm(it) else it }
        val hipCm = hipText.toDoubleOrNull()?.let { if (units == "imp") MetabolicEngine.inToCm(it) else it }
        return UserProfile(
            units = units,
            sex = sex,
            age = (ageText.toIntOrNull() ?: 32).coerceIn(14, 100),
            heightCm = hCm,
            startWeightKg = w0Kg,
            goalWeightKg = goalKg,
            startDate = MetabolicEngine.dateOffset(0),
            targetDate = targetDateIso,
            activityLevel = activityLevel,
            dietStyle = dietStyle,
            healthConsideration = healthConsideration,
            motivationWhy = motivationWhy.trim(),
            waistCm = waistCm,
            neckCm = neckCm,
            hipCm = hipCm
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Step ${step + 1} of 6 · ${ONBOARDING_STEPS[step]}",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                },
                navigationIcon = {
                    if (step > 0) {
                        IconButton(
                            onClick = {
                                validationError = null
                                step--
                            },
                            modifier = Modifier.testTag("onboarding_back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LinearProgressIndicator(
                progress = { (step + 1) / 6f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
            )

            validationError?.let { err ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = err,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            AnimatedContent(targetState = step, label = "onboardingStep") { currentStep ->
                when (currentStep) {
                    0 -> {
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.VerifiedUser,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "100% On-Device Privacy",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                Text(
                                    text = "LeanPath",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "A clinical, evidence-based metabolic and weight-trend tracker. Your health records never leave this device.",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                HorizontalDivider()
                                listOf(
                                    "Exponentially Weighted Moving Average (EMA) trend smoothing to filter water noise",
                                    "Adaptive TDEE from your real intake & scale trajectory",
                                    "Mifflin-St Jeor, Harris-Benedict, Katch-McArdle & US Navy Body Fat equations",
                                    "Integrated Macros, Hydration, Intermittent Fasting, Recovery & 90-Day Heatmap"
                                ).forEach { point ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(point, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                                OutlinedButton(
                                    onClick = onLoadDemo,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_load_demo_button")
                                ) {
                                    Icon(Icons.Default.AutoGraph, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Explore with 30-Day Science Demo Data")
                                }
                            }
                        }
                    }

                    1 -> {
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text("Measurement Units", style = MaterialTheme.typography.titleMedium)
                                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                    SegmentedButton(
                                        selected = units == "met",
                                        onClick = { units = "met" },
                                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                                    ) {
                                        Text("Metric (kg, cm)")
                                    }
                                    SegmentedButton(
                                        selected = units == "imp",
                                        onClick = { units = "imp" },
                                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                                    ) {
                                        Text("Imperial (lb, in)")
                                    }
                                }

                                Text("Biological Sex (for BMR equations)", style = MaterialTheme.typography.titleMedium)
                                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                                    SegmentedButton(
                                        selected = sex == "m",
                                        onClick = { sex = "m" },
                                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                                    ) {
                                        Text("Male")
                                    }
                                    SegmentedButton(
                                        selected = sex == "f",
                                        onClick = { sex = "f" },
                                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                                    ) {
                                        Text("Female")
                                    }
                                }

                                OutlinedTextField(
                                    value = ageText,
                                    onValueChange = { ageText = it },
                                    label = { Text("Age (years)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_age_input")
                                )

                                OutlinedTextField(
                                    value = heightText,
                                    onValueChange = { heightText = it },
                                    label = { Text("Height (${if (units == "imp") "inches" else "cm"})") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_height_input")
                                )
                            }
                        }
                    }

                    2 -> {
                        val w0Kg = MetabolicEngine.toKgInput(currentWeightText.toDoubleOrNull() ?: 0.0, units)
                        val gKg = MetabolicEngine.toKgInput(goalWeightText.toDoubleOrNull() ?: 0.0, units)
                        val days = MetabolicEngine.daysBetween(MetabolicEngine.dateOffset(0), targetDateIso).coerceAtLeast(1L)
                        val weeklyLossKg = (w0Kg - gKg) / days.toDouble() * 7.0
                        val weeklyPct = if (w0Kg > 0) (weeklyLossKg / w0Kg * 100.0) else 0.0

                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                OutlinedTextField(
                                    value = currentWeightText,
                                    onValueChange = { currentWeightText = it },
                                    label = { Text("Current Weight (${MetabolicEngine.weightUnit(units)})") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_current_weight_input")
                                )
                                OutlinedTextField(
                                    value = goalWeightText,
                                    onValueChange = { goalWeightText = it },
                                    label = { Text("Goal Weight (${MetabolicEngine.weightUnit(units)})") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("onboarding_goal_weight_input")
                                )

                                OutlinedCard(
                                    onClick = { showDatePicker = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("Target Date", style = MaterialTheme.typography.labelMedium)
                                            Text(targetDateIso, style = MaterialTheme.typography.titleMedium)
                                        }
                                        Icon(Icons.Default.CalendarMonth, contentDescription = "Pick target date")
                                    }
                                }

                                if (w0Kg > 0 && gKg > 0) {
                                    val paceText = when {
                                        gKg >= w0Kg -> "Goal weight should be below your current weight."
                                        weeklyPct > 1.0 -> "${MetabolicEngine.formatWeight(weeklyLossKg, units, 2)}/wk (${String.format(Locale.US, "%.2f", weeklyPct)}%/wk). Faster than recommended 1% max — risks muscle loss."
                                        weeklyPct < 0.25 -> "${MetabolicEngine.formatWeight(weeklyLossKg, units, 2)}/wk (${String.format(Locale.US, "%.2f", weeklyPct)}%/wk). Very gentle pace."
                                        else -> "${MetabolicEngine.formatWeight(weeklyLossKg, units, 2)}/wk (${String.format(Locale.US, "%.2f", weeklyPct)}%/wk). Evidence-based sweet spot (0.25–1%/wk)."
                                    }
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (weeklyPct > 1.0 || gKg >= w0Kg)
                                                MaterialTheme.colorScheme.tertiaryContainer
                                            else
                                                MaterialTheme.colorScheme.primaryContainer
                                        )
                                    ) {
                                        Text(
                                            text = paceText,
                                            modifier = Modifier.padding(12.dp),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }

                    3 -> {
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text("Activity Level", style = MaterialTheme.typography.titleMedium)
                                listOf(
                                    1.2 to "Sedentary (Desk job, minimal planned exercise)",
                                    1.375 to "Light (Light exercise 1–3 days/week)",
                                    1.55 to "Moderate (Moderate training 3–5 days/week)",
                                    1.725 to "Very Active (Hard training 6–7 days/week)",
                                    1.9 to "Athlete (Intense daily training or physical job)"
                                ).forEach { (factor, label) ->
                                    FilterChip(
                                        selected = activityLevel == factor,
                                        onClick = { activityLevel = factor },
                                        label = { Text(label) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                Text("Macro & Diet Architecture", style = MaterialTheme.typography.titleMedium)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "bal" to "Balanced",
                                        "hp" to "High Protein",
                                        "lf" to "Low Fat",
                                        "keto" to "Keto"
                                    ).forEach { (key, label) ->
                                        FilterChip(
                                            selected = dietStyle == key,
                                            onClick = { dietStyle = key },
                                            label = { Text(label) }
                                        )
                                    }
                                }

                                Text("Health Considerations", style = MaterialTheme.typography.titleMedium)
                                listOf(
                                    "none" to "None",
                                    "preg" to "Pregnant or breastfeeding",
                                    "dm" to "Diabetes or on metabolic medication",
                                    "ed" to "History of disordered eating"
                                ).forEach { (key, label) ->
                                    FilterChip(
                                        selected = healthConsideration == key,
                                        onClick = { healthConsideration = key },
                                        label = { Text(label) },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                OutlinedTextField(
                                    value = motivationWhy,
                                    onValueChange = { motivationWhy = it },
                                    label = { Text("Why is this goal important to you?") },
                                    placeholder = { Text("e.g., More energy for my kids & running a 10K") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    4 -> {
                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Text(
                                    text = "Optional: US Navy Body Fat & Lean Mass",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Entering waist and neck unlocks US Navy Body-Fat %, Katch-McArdle BMR, and Waist-to-Height cardiometabolic ratio. You can also skip this step.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedTextField(
                                    value = waistText,
                                    onValueChange = { waistText = it },
                                    label = { Text("Waist (${MetabolicEngine.lengthUnit(units)})") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = neckText,
                                    onValueChange = { neckText = it },
                                    label = { Text("Neck (${MetabolicEngine.lengthUnit(units)})") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                if (sex == "f") {
                                    OutlinedTextField(
                                        value = hipText,
                                        onValueChange = { hipText = it },
                                        label = { Text("Hips (${MetabolicEngine.lengthUnit(units)})") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    5 -> {
                        val draft = buildDraftProfile()
                        val calc = MetabolicEngine.calculateMetrics(
                            state = LeanPathState(profile = draft),
                            weightKgOverride = draft.startWeightKg
                        )
                        val wkLossKg = calc.dailyDeficitKcal * 7.0 / 7700.0

                        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text("Your Metabolic Blueprint", style = MaterialTheme.typography.headlineSmall)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    BlueprintStatCard(
                                        value = "${calc.targetKcal}",
                                        label = "kcal / day",
                                        modifier = Modifier.weight(1f)
                                    )
                                    BlueprintStatCard(
                                        value = "${calc.proteinG}g",
                                        label = "protein / day",
                                        modifier = Modifier.weight(1f)
                                    )
                                    BlueprintStatCard(
                                        value = MetabolicEngine.formatWeight(wkLossKg, draft.units, 2),
                                        label = "per week",
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                BmiSpectrumBar(bmi = calc.bmi)

                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "BMI ${String.format(Locale.US, "%.1f", calc.bmi)} · ${calc.bmiCategory}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Estimated TDEE: ${calc.tdee.roundToInt()} kcal · Daily deficit: ${calc.dailyDeficitKcal.roundToInt()} kcal · Fat: ${calc.fatG}g · Carbs: ${calc.carbsG}g",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (draft.healthConsideration != "none") {
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.errorContainer
                                        )
                                    ) {
                                        Text(
                                            text = "Because of your health consideration, please obtain medical guidance before dieting. LeanPath caps your daily deficit at 500 kcal.",
                                            modifier = Modifier.padding(12.dp),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step > 0) {
                    OutlinedButton(
                        onClick = {
                            validationError = null
                            step--
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Back")
                    }
                }
                Button(
                    onClick = {
                        validationError = null
                        if (step == 1) {
                            val age = ageText.toIntOrNull()
                            val h = heightText.toDoubleOrNull()
                            if (age == null || age < 14 || h == null || h <= 30.0) {
                                validationError = "Please enter a valid age and height."
                                return@Button
                            }
                        }
                        if (step == 2) {
                            val w0 = currentWeightText.toDoubleOrNull()
                            val g = goalWeightText.toDoubleOrNull()
                            if (w0 == null || g == null || g >= w0) {
                                validationError = "Please enter a goal weight below your current weight."
                                return@Button
                            }
                        }
                        if (step < 5) {
                            step++
                        } else {
                            onComplete(buildDraftProfile())
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("onboarding_continue_button")
                ) {
                    Text(if (step == 5) "Start Tracking" else "Continue")
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selected = Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.of("UTC"))
                                .toLocalDate()
                            if (selected.isAfter(LocalDate.now())) {
                                targetDateIso = selected.toString()
                            }
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
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun BlueprintStatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
