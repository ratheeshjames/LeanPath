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
import com.example.model.*
import com.example.ui.components.MacroSplitDonutChart
import com.example.ui.theme.JetBrainsMonoFontFamily
import kotlin.math.max
import kotlin.math.roundToInt

private val MEAL_SLOTS = listOf("Breakfast", "Lunch", "Dinner", "Snack")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelScreen(
    state: LeanPathState,
    selectedMealType: String,
    searchQuery: String,
    onSelectMealType: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onAddPresetFood: (PresetFood, String) -> Unit,
    onAddCustomFood: (String, Double, Double, Double, Double, Double, Double, Double, String) -> Unit,
    onDeleteFoodEntry: (String) -> Unit,
    onSaveMealTemplate: (String) -> Unit,
    onLogSavedMeal: (SavedMeal, String) -> Unit,
    onDeleteSavedMeal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = MetabolicEngine.dateOffset(0)
    val todayEntries = state.foodByDate[today].orEmpty()
    val eaten = remember(todayEntries) { MetabolicEngine.computeDailyFoodTotals(todayEntries) }
    val metrics = remember(state) { MetabolicEngine.calculateMetrics(state) }

    val matchingPresets = remember(searchQuery) {
        val q = searchQuery.trim().lowercase()
        PRESET_FOODS.filter { it.name.lowercase().contains(q) }.take(8)
    }

    var showCustomAdd by remember { mutableStateOf(false) }
    var customName by remember { mutableStateOf("") }
    var customKcal by remember { mutableStateOf("") }
    var customProtein by remember { mutableStateOf("") }
    var customCarbs by remember { mutableStateOf("") }
    var customFat by remember { mutableStateOf("") }
    var customFiber by remember { mutableStateOf("") }
    var customSodium by remember { mutableStateOf("") }
    var customAlcohol by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Daily Calorie Budget & Macro Split Card
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
                    Column {
                        Text(
                            text = "${eaten.kcal.roundToInt()} / ${metrics.targetKcal} kcal",
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${max(0, metrics.targetKcal - eaten.kcal.roundToInt())} kcal remaining today",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text(
                            text = "${((eaten.kcal / metrics.targetKcal.coerceAtLeast(1)) * 100).roundToInt()}%",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { (eaten.kcal / metrics.targetKcal.coerceAtLeast(1)).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )

                if (eaten.kcal > 0) {
                    HorizontalDivider()
                    Text("Macro Split", style = MaterialTheme.typography.titleMedium)
                    MacroSplitDonutChart(
                        proteinG = eaten.proteinG,
                        carbsG = eaten.carbsG,
                        fatG = eaten.fatG
                    )
                }
            }
        }

        // Meal Selector + Search & Quick Add Card
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Add Food to Meal", style = MaterialTheme.typography.titleLarge)

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    MEAL_SLOTS.forEachIndexed { idx, slot ->
                        SegmentedButton(
                            selected = selectedMealType == slot,
                            onClick = { onSelectMealType(slot) },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = MEAL_SLOTS.size),
                            modifier = Modifier.testTag("meal_slot_$slot")
                        ) {
                            Text(slot, maxLines = 1)
                        }
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text("Search 26 staple foods (e.g. salmon, oats, egg)…") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("food_search_input")
                )

                // Preset food search results
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    matchingPresets.forEach { preset ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${preset.kcal.roundToInt()} kcal · P ${preset.proteinG}g · C ${preset.carbsG}g · F ${preset.fatG}g",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                FilledTonalIconButton(
                                    onClick = { onAddPresetFood(preset, selectedMealType) },
                                    modifier = Modifier.testTag("add_preset_${preset.name}")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Add ${preset.name}")
                                }
                            }
                        }
                    }
                }

                // Custom / Quick Add Accordion
                OutlinedCard(
                    onClick = { showCustomAdd = !showCustomAdd },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("toggle_custom_food_card")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                Icon(Icons.Default.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text("Custom / Quick Add Macros & Micros", style = MaterialTheme.typography.titleMedium)
                            }
                            Icon(
                                if (showCustomAdd) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }

                        AnimatedVisibility(visible = showCustomAdd) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = customName,
                                    onValueChange = { customName = it },
                                    label = { Text("Food or Meal Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = customKcal,
                                        onValueChange = { customKcal = it },
                                        label = { Text("kcal *") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = customProtein,
                                        onValueChange = { customProtein = it },
                                        label = { Text("Protein g") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = customCarbs,
                                        onValueChange = { customCarbs = it },
                                        label = { Text("Carbs g") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = customFat,
                                        onValueChange = { customFat = it },
                                        label = { Text("Fat g") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = customFiber,
                                        onValueChange = { customFiber = it },
                                        label = { Text("Fiber g") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = customSodium,
                                        onValueChange = { customSodium = it },
                                        label = { Text("Sodium mg") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = customAlcohol,
                                        onValueChange = { customAlcohol = it },
                                        label = { Text("Alcohol") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Button(
                                    onClick = {
                                        val k = customKcal.toDoubleOrNull() ?: return@Button
                                        onAddCustomFood(
                                            customName,
                                            k,
                                            customProtein.toDoubleOrNull() ?: 0.0,
                                            customCarbs.toDoubleOrNull() ?: 0.0,
                                            customFat.toDoubleOrNull() ?: 0.0,
                                            customFiber.toDoubleOrNull() ?: 0.0,
                                            customSodium.toDoubleOrNull() ?: 0.0,
                                            customAlcohol.toDoubleOrNull() ?: 0.0,
                                            selectedMealType
                                        )
                                        customName = ""
                                        customKcal = ""
                                        customProtein = ""
                                        customCarbs = ""
                                        customFat = ""
                                        customFiber = ""
                                        customSodium = ""
                                        customAlcohol = ""
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("submit_custom_food_button")
                                ) {
                                    Text("Add to $selectedMealType")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Saved Meals Card
        if (state.savedMeals.isNotEmpty()) {
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Saved Meal Templates", style = MaterialTheme.typography.titleLarge)
                    state.savedMeals.forEach { sm ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(sm.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "${sm.kcal.roundToInt()} kcal · P ${sm.proteinG.roundToInt()}g · C ${sm.carbsG.roundToInt()}g · F ${sm.fatG.roundToInt()}g",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilledTonalButton(
                                    onClick = { onLogSavedMeal(sm, selectedMealType) },
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text("+ Log")
                                }
                                IconButton(onClick = { onDeleteSavedMeal(sm.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete saved meal ${sm.name}")
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    }
                }
            }
        }

        // Meal-by-Meal Logged Foods Today
        MEAL_SLOTS.forEach { mealSlot ->
            val items = todayEntries.filter { it.mealType == mealSlot }
            if (items.isNotEmpty()) {
                val mealKcal = items.sumOf { it.kcal }.roundToInt()
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(mealSlot, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = "$mealKcal kcal total",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            TextButton(onClick = { onSaveMealTemplate(mealSlot) }) {
                                Icon(Icons.Default.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Save as Meal")
                            }
                        }

                        items.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(item.name, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "P ${item.proteinG.roundToInt()}g · C ${item.carbsG.roundToInt()}g · F ${item.fatG.roundToInt()}g",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "${item.kcal.roundToInt()} kcal",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    IconButton(
                                        onClick = { onDeleteFoodEntry(item.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Delete ${item.name}",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                        }
                    }
                }
            }
        }
    }
}
