package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.LeanPathRepository
import com.example.domain.MetabolicEngine
import com.example.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.roundToInt

enum class MainDestination {
    PULSE,
    TREND,
    FUEL,
    STUDIO,
    SCIENCE
}

enum class StudioTool(val label: String) {
    MEASURES("Measures"),
    ACTIVITY("Activity"),
    WELLNESS("Wellness"),
    HABITS("Habits"),
    JOURNAL("Journal"),
    CALENDAR("90d Map"),
    REVIEW("Weekly Review"),
    PLANNER("Meal Plan"),
    GROCERY("Grocery"),
    PHOTOS("Photos"),
    GOALS("Goals & Alerts")
}

data class LeanPathUiState(
    val isLoading: Boolean = true,
    val activeProfileKey: String = "leanpath.v1",
    val activeProfileName: String = "Me",
    val profiles: List<ProfileMeta> = listOf(ProfileMeta("leanpath.v1", "Me")),
    val state: LeanPathState = LeanPathState(),
    val currentDestination: MainDestination = MainDestination.PULSE,
    val selectedStudioTool: StudioTool = StudioTool.MEASURES,
    val weightChartRangeDays: Int = 30, // 7, 30, 90, 0 (All)
    val selectedMealType: String = "Breakfast",
    val foodSearchQuery: String = "",
    val nowMillis: Long = System.currentTimeMillis(),
    val statusBannerMessage: String? = null
)

class LeanPathViewModel(
    private val repository: LeanPathRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LeanPathUiState())
    val uiState: StateFlow<LeanPathUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.snapshotFlow.collect { snapshot ->
                if (snapshot == null) {
                    // Seed default profile with rich 30-day science demo so user can immediately explore or re-run onboarding anytime
                    val demoState = MetabolicEngine.generate30DayDemoState()
                    repository.saveProfileState(
                        profileKey = "leanpath.v1",
                        displayName = "Me",
                        state = demoState,
                        isActive = true
                    )
                } else {
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            activeProfileKey = snapshot.activeKey,
                            activeProfileName = snapshot.activeName,
                            profiles = snapshot.profiles,
                            state = snapshot.state
                        )
                    }
                }
            }
        }

        // Minute ticker for live fasting progress and reminder evaluation
        viewModelScope.launch {
            while (true) {
                delay(15_000L)
                _uiState.update { it.copy(nowMillis = System.currentTimeMillis()) }
            }
        }
    }

    private fun persistState(transform: (LeanPathState) -> LeanPathState) {
        val current = _uiState.value
        val updated = transform(current.state)
        _uiState.update { it.copy(state = updated) }
        viewModelScope.launch {
            repository.saveProfileState(
                profileKey = current.activeProfileKey,
                displayName = current.activeProfileName,
                state = updated,
                isActive = true
            )
        }
    }

    fun showMessage(msg: String?) {
        _uiState.update { it.copy(statusBannerMessage = msg) }
    }

    fun selectDestination(destination: MainDestination) {
        _uiState.update { it.copy(currentDestination = destination) }
    }

    fun selectStudioTool(tool: StudioTool) {
        _uiState.update {
            it.copy(
                currentDestination = MainDestination.STUDIO,
                selectedStudioTool = tool
            )
        }
    }

    fun setWeightChartRange(days: Int) {
        _uiState.update { it.copy(weightChartRangeDays = days) }
    }

    fun setSelectedMealType(mealType: String) {
        _uiState.update { it.copy(selectedMealType = mealType) }
    }

    fun setFoodSearchQuery(query: String) {
        _uiState.update { it.copy(foodSearchQuery = query) }
    }

    fun completeOnboarding(profile: UserProfile) {
        val today = MetabolicEngine.dateOffset(0)
        val initialMeasurements = buildList {
            profile.waistCm?.let {
                add(BodyMeasurementEntry(UUID.randomUUID().toString(), today, "waist", it))
            }
            profile.neckCm?.let {
                add(BodyMeasurementEntry(UUID.randomUUID().toString(), today, "neck", it))
            }
            profile.hipCm?.let {
                add(BodyMeasurementEntry(UUID.randomUUID().toString(), today, "hips", it))
            }
        }
        persistState { old ->
            old.copy(
                onboarded = true,
                profile = profile.copy(startDate = today),
                weighIns = listOf(WeighInEntry(date = today, weightKg = profile.startWeightKg)),
                measurements = initialMeasurements
            )
        }
        showMessage("Personalized metabolic blueprint activated!")
    }

    fun startOnboardingWizard() {
        persistState { it.copy(onboarded = false) }
    }

    fun loadDemoData() {
        val demo = MetabolicEngine.generate30DayDemoState()
        persistState { demo.copy(themePreference = it.themePreference) }
        showMessage("Loaded 30-day metabolic & nutrition dataset!")
    }

    fun logWeight(rawWeightInUserUnits: Double, dateIso: String = MetabolicEngine.dateOffset(0)) {
        val units = _uiState.value.state.profile.units
        val kg = MetabolicEngine.toKgInput(rawWeightInUserUnits, units)
        if (kg < 20.0 || kg > 400.0) {
            showMessage("Please enter a valid weight.")
            return
        }
        persistState { state ->
            val mutable = state.weighIns.toMutableList()
            val idx = mutable.indexOfFirst { it.date == dateIso }
            if (idx >= 0) {
                mutable[idx] = WeighInEntry(dateIso, kg)
            } else {
                mutable.add(WeighInEntry(dateIso, kg))
            }
            mutable.sortBy { it.date }
            state.copy(weighIns = mutable)
        }
        showMessage("Weight logged for $dateIso")
    }

    fun deleteWeighIn(dateIso: String) {
        persistState { state ->
            if (state.weighIns.size <= 1) state
            else state.copy(weighIns = state.weighIns.filterNot { it.date == dateIso })
        }
    }

    fun addWaterMl(deltaMl: Int) {
        val today = MetabolicEngine.dateOffset(0)
        persistState { state ->
            val current = state.waterByDateMl[today] ?: 0
            val updated = (current + deltaMl).coerceAtLeast(0)
            state.copy(waterByDateMl = state.waterByDateMl + (today to updated))
        }
    }

    fun startFast(hours: Int) {
        val now = System.currentTimeMillis()
        persistState { state ->
            state.copy(activeFast = FastingSession(startTimeMillis = now, targetHours = hours))
        }
        _uiState.update { it.copy(nowMillis = now) }
    }

    fun endFast() {
        persistState { state -> state.copy(activeFast = null) }
    }

    fun addPresetFood(preset: PresetFood, mealType: String = _uiState.value.selectedMealType) {
        val today = MetabolicEngine.dateOffset(0)
        val entry = FoodEntry(
            id = UUID.randomUUID().toString(),
            name = preset.name,
            kcal = preset.kcal,
            proteinG = preset.proteinG,
            carbsG = preset.carbsG,
            fatG = preset.fatG,
            fiberG = preset.fiberG,
            sodiumMg = 0.0,
            alcoholDrinks = if (preset.isAlcohol) 1.0 else 0.0,
            mealType = mealType
        )
        persistState { state ->
            val list = state.foodByDate[today].orEmpty() + entry
            state.copy(foodByDate = state.foodByDate + (today to list))
        }
        showMessage("Added ${preset.name} to $mealType")
    }

    fun addCustomFood(
        name: String,
        kcal: Double,
        proteinG: Double,
        carbsG: Double,
        fatG: Double,
        fiberG: Double,
        sodiumMg: Double,
        alcoholDrinks: Double,
        mealType: String
    ) {
        if (kcal <= 0.0) return
        val today = MetabolicEngine.dateOffset(0)
        val entry = FoodEntry(
            id = UUID.randomUUID().toString(),
            name = name.ifBlank { "Quick add" },
            kcal = kcal,
            proteinG = proteinG,
            carbsG = carbsG,
            fatG = fatG,
            fiberG = fiberG,
            sodiumMg = sodiumMg,
            alcoholDrinks = alcoholDrinks,
            mealType = mealType
        )
        persistState { state ->
            val list = state.foodByDate[today].orEmpty() + entry
            state.copy(foodByDate = state.foodByDate + (today to list))
        }
        showMessage("Added ${entry.name}")
    }

    fun deleteFoodEntry(entryId: String, dateIso: String = MetabolicEngine.dateOffset(0)) {
        persistState { state ->
            val updated = state.foodByDate[dateIso].orEmpty().filterNot { it.id == entryId }
            state.copy(foodByDate = state.foodByDate + (dateIso to updated))
        }
    }

    fun saveMealGroupAsTemplate(mealType: String) {
        val today = MetabolicEngine.dateOffset(0)
        val items = _uiState.value.state.foodByDate[today].orEmpty().filter { it.mealType == mealType }
        if (items.isEmpty()) return
        val saved = SavedMeal(
            id = UUID.randomUUID().toString(),
            name = "$mealType ($today)",
            kcal = items.sumOf { it.kcal },
            proteinG = items.sumOf { it.proteinG },
            carbsG = items.sumOf { it.carbsG },
            fatG = items.sumOf { it.fatG },
            fiberG = items.sumOf { it.fiberG },
            sodiumMg = items.sumOf { it.sodiumMg }
        )
        persistState { state ->
            state.copy(savedMeals = state.savedMeals + saved)
        }
        showMessage("Saved '$mealType' to Saved Meals!")
    }

    fun logSavedMeal(savedMeal: SavedMeal, mealType: String = _uiState.value.selectedMealType) {
        val today = MetabolicEngine.dateOffset(0)
        val entry = FoodEntry(
            id = UUID.randomUUID().toString(),
            name = savedMeal.name,
            kcal = savedMeal.kcal,
            proteinG = savedMeal.proteinG,
            carbsG = savedMeal.carbsG,
            fatG = savedMeal.fatG,
            fiberG = savedMeal.fiberG,
            sodiumMg = savedMeal.sodiumMg,
            mealType = mealType
        )
        persistState { state ->
            val list = state.foodByDate[today].orEmpty() + entry
            state.copy(foodByDate = state.foodByDate + (today to list))
        }
        showMessage("Logged ${savedMeal.name}")
    }

    fun deleteSavedMeal(id: String) {
        persistState { state ->
            state.copy(savedMeals = state.savedMeals.filterNot { it.id == id })
        }
    }

    fun addMeasurement(siteKey: String, rawValue: Double) {
        if (rawValue <= 0.0) return
        val units = _uiState.value.state.profile.units
        val normalized = if (siteKey == "bf" || units != "imp") {
            rawValue
        } else {
            MetabolicEngine.inToCm(rawValue)
        }
        val entry = BodyMeasurementEntry(
            id = UUID.randomUUID().toString(),
            date = MetabolicEngine.dateOffset(0),
            siteKey = siteKey,
            valueCmOrPct = normalized
        )
        persistState { state ->
            val updatedProfile = when (siteKey) {
                "waist" -> state.profile.copy(waistCm = normalized)
                "neck" -> state.profile.copy(neckCm = normalized)
                "hips" -> state.profile.copy(hipCm = normalized)
                else -> state.profile
            }
            state.copy(
                profile = updatedProfile,
                measurements = state.measurements + entry
            )
        }
        showMessage("Measurement logged!")
    }

    fun addExercise(exerciseMet: ExerciseMet, minutes: Int) {
        if (minutes <= 0) return
        val today = MetabolicEngine.dateOffset(0)
        val weightKg = MetabolicEngine.currentWeightKg(_uiState.value.state)
        val kcal = (exerciseMet.met * weightKg * minutes / 60.0).roundToInt()
        val entry = ExerciseEntry(
            id = UUID.randomUUID().toString(),
            name = exerciseMet.name,
            minutes = minutes,
            kcalBurned = kcal
        )
        persistState { state ->
            val list = state.exercisesByDate[today].orEmpty() + entry
            state.copy(exercisesByDate = state.exercisesByDate + (today to list))
        }
        showMessage("Logged ${exerciseMet.name} ($kcal kcal burned)")
    }

    fun deleteExercise(id: String) {
        val today = MetabolicEngine.dateOffset(0)
        persistState { state ->
            val list = state.exercisesByDate[today].orEmpty().filterNot { it.id == id }
            state.copy(exercisesByDate = state.exercisesByDate + (today to list))
        }
    }

    fun saveStepsToday(steps: Int) {
        val today = MetabolicEngine.dateOffset(0)
        persistState { state ->
            state.copy(stepsByDate = state.stepsByDate + (today to steps.coerceAtLeast(0)))
        }
        showMessage("Daily steps updated!")
    }

    fun saveWellnessToday(
        sleepHours: Double,
        restingHr: Int,
        mood: Int,
        energy: Int,
        stress: Int
    ) {
        val today = MetabolicEngine.dateOffset(0)
        val entry = WellnessEntry(
            sleepHours = sleepHours.coerceIn(0.0, 16.0),
            restingHr = restingHr.coerceIn(30, 200),
            mood = mood.coerceIn(1, 5),
            energy = energy.coerceIn(1, 5),
            stress = stress.coerceIn(1, 5)
        )
        persistState { state ->
            state.copy(wellnessByDate = state.wellnessByDate + (today to entry))
        }
        showMessage("Wellness & recovery score updated!")
    }

    fun addHabit(habitName: String) {
        val trimmed = habitName.trim()
        if (trimmed.isEmpty()) return
        persistState { state ->
            if (state.habitsList.contains(trimmed)) state
            else state.copy(habitsList = state.habitsList + trimmed)
        }
    }

    fun toggleHabitToday(habitName: String) {
        val today = MetabolicEngine.dateOffset(0)
        persistState { state ->
            val current = state.habitsDoneByDate[today].orEmpty().toMutableList()
            if (current.contains(habitName)) current.remove(habitName) else current.add(habitName)
            state.copy(habitsDoneByDate = state.habitsDoneByDate + (today to current))
        }
    }

    fun deleteHabit(habitName: String) {
        persistState { state ->
            state.copy(habitsList = state.habitsList.filterNot { it == habitName })
        }
    }

    fun addJournalEntry(type: String, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val entry = JournalEntry(
            id = UUID.randomUUID().toString(),
            date = MetabolicEngine.dateOffset(0),
            type = type,
            text = trimmed
        )
        persistState { state ->
            state.copy(journalEntries = state.journalEntries + entry)
        }
        showMessage(if (type == "nsv") "Non-scale victory recorded! 🏆" else "Journal note saved!")
    }

    fun deleteJournalEntry(id: String) {
        persistState { state ->
            state.copy(journalEntries = state.journalEntries.filterNot { it.id == id })
        }
    }

    fun updateDayMealPlan(dayIndex: Int, planText: String) {
        persistState { state ->
            state.copy(weeklyMealPlan = state.weeklyMealPlan + (dayIndex to planText))
        }
    }

    fun sendMealPlanToGroceryList() {
        persistState { state ->
            val existingLower = state.groceryList.map { it.text.lowercase() }.toMutableSet()
            val newItems = mutableListOf<GroceryItem>()
            state.weeklyMealPlan.values
                .flatMap { it.split(",", "\n", ";") }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .forEach { itemText ->
                    if (existingLower.add(itemText.lowercase())) {
                        newItems.add(GroceryItem(id = UUID.randomUUID().toString(), text = itemText, checked = false))
                    }
                }
            state.copy(groceryList = state.groceryList + newItems)
        }
        _uiState.update { it.copy(selectedStudioTool = StudioTool.GROCERY) }
        showMessage("Meal plan ingredients sent to Grocery List!")
    }

    fun addGroceryItem(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        persistState { state ->
            state.copy(
                groceryList = state.groceryList + GroceryItem(
                    id = UUID.randomUUID().toString(),
                    text = trimmed,
                    checked = false
                )
            )
        }
    }

    fun toggleGroceryItem(id: String) {
        persistState { state ->
            state.copy(
                groceryList = state.groceryList.map {
                    if (it.id == id) it.copy(checked = !it.checked) else it
                }
            )
        }
    }

    fun removeGroceryItem(id: String) {
        persistState { state ->
            state.copy(groceryList = state.groceryList.filterNot { it.id == id })
        }
    }

    fun clearCheckedGroceries() {
        persistState { state ->
            state.copy(groceryList = state.groceryList.filterNot { it.checked })
        }
    }

    fun addProgressPhoto(uriString: String, note: String = "") {
        val entry = ProgressPhotoEntry(
            id = UUID.randomUUID().toString(),
            date = MetabolicEngine.dateOffset(0),
            uriString = uriString,
            note = note
        )
        persistState { state ->
            state.copy(progressPhotos = state.progressPhotos + entry)
        }
        showMessage("Progress photo added (stored locally on device)!")
    }

    fun deleteProgressPhoto(id: String) {
        persistState { state ->
            state.copy(progressPhotos = state.progressPhotos.filterNot { it.id == id })
        }
    }

    fun updateMilestoneReward(percentage: Int, rewardText: String) {
        persistState { state ->
            state.copy(milestoneRewards = state.milestoneRewards + (percentage to rewardText))
        }
    }

    fun addReminder(timeHm: String, label: String) {
        val trimmed = label.trim()
        if (trimmed.isEmpty()) return
        persistState { state ->
            state.copy(
                reminders = state.reminders + ReminderItem(
                    id = UUID.randomUUID().toString(),
                    timeHm = timeHm,
                    label = trimmed
                )
            )
        }
    }

    fun deleteReminder(id: String) {
        persistState { state ->
            state.copy(reminders = state.reminders.filterNot { it.id == id })
        }
    }

    fun dismissReminderToday(id: String) {
        val today = MetabolicEngine.dateOffset(0)
        persistState { state ->
            val dismissed = state.dismissedRemindersByDate[today].orEmpty() + id
            state.copy(dismissedRemindersByDate = state.dismissedRemindersByDate + (today to dismissed))
        }
    }

    fun updateUnits(units: String) {
        persistState { state ->
            state.copy(profile = state.profile.copy(units = units))
        }
    }

    fun updateThemePreference(theme: String) {
        persistState { state ->
            state.copy(themePreference = theme)
        }
    }

    fun updateActivityLevel(activityLevel: Double) {
        persistState { state ->
            state.copy(profile = state.profile.copy(activityLevel = activityLevel))
        }
    }

    fun updateDietStyle(dietStyle: String) {
        persistState { state ->
            state.copy(profile = state.profile.copy(dietStyle = dietStyle))
        }
    }

    fun switchProfile(profileKey: String) {
        viewModelScope.launch {
            repository.switchProfile(profileKey)
            showMessage("Switched active profile")
        }
    }

    fun createNewProfile(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            repository.createNewProfile(trimmed)
            showMessage("Created profile '$trimmed'")
        }
    }

    fun importJsonBackup(jsonString: String): Boolean {
        return try {
            val imported = MetabolicEngine.deserializeStateFromJson(jsonString)
            if (imported.weighIns.isEmpty() && !imported.onboarded) {
                showMessage("Invalid backup format.")
                false
            } else {
                persistState { imported }
                showMessage("Backup imported successfully!")
                true
            }
        } catch (e: Exception) {
            showMessage("Failed to import JSON backup.")
            false
        }
    }

    fun resetAllData() {
        persistState {
            LeanPathState(onboarded = false)
        }
        showMessage("All profile data reset.")
    }

    class Factory(private val repository: LeanPathRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return LeanPathViewModel(repository) as T
        }
    }
}
