package com.example.domain

import com.example.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.UUID
import kotlin.math.*

object MetabolicEngine {

    private const val KG_TO_LB = 2.20462
    private const val CM_TO_IN = 2.54

    fun dateOffset(offsetDays: Long = 0L): String {
        return LocalDate.now().plusDays(offsetDays).toString()
    }

    fun daysBetween(startIso: String, endIso: String): Long {
        return try {
            val d1 = LocalDate.parse(startIso)
            val d2 = LocalDate.parse(endIso)
            ChronoUnit.DAYS.between(d1, d2)
        } catch (e: Exception) {
            1L
        }
    }

    fun kgToLb(kg: Double): Double = kg * KG_TO_LB
    fun lbToKg(lb: Double): Double = lb / KG_TO_LB
    fun cmToIn(cm: Double): Double = cm / CM_TO_IN
    fun inToCm(inches: Double): Double = inches * CM_TO_IN

    fun formatWeight(kg: Double, units: String, decimals: Int = 1): String {
        val value = if (units == "imp") kgToLb(kg) else kg
        val suffix = if (units == "imp") "lb" else "kg"
        return String.format(Locale.US, "%.${decimals}f %s", value, suffix)
    }

    fun formatWeightValueOnly(kg: Double, units: String, decimals: Int = 1): String {
        val value = if (units == "imp") kgToLb(kg) else kg
        return String.format(Locale.US, "%.${decimals}f", value)
    }

    fun weightUnit(units: String): String = if (units == "imp") "lb" else "kg"
    fun lengthUnit(units: String): String = if (units == "imp") "in" else "cm"

    fun toKgInput(raw: Double, units: String): Double {
        return if (units == "imp") lbToKg(raw) else raw
    }

    fun formatMeasurement(siteKey: String, valueCmOrPct: Double, units: String): String {
        return if (siteKey == "bf") {
            String.format(Locale.US, "%.1f%%", valueCmOrPct)
        } else if (units == "imp") {
            String.format(Locale.US, "%.1f in", cmToIn(valueCmOrPct))
        } else {
            String.format(Locale.US, "%.1f cm", valueCmOrPct)
        }
    }

    fun currentWeightKg(state: LeanPathState): Double {
        return state.weighIns.lastOrNull()?.weightKg ?: state.profile.startWeightKg
    }

    fun computeEmaTrend(weighIns: List<WeighInEntry>): List<TrendPoint> {
        val sorted = weighIns.sortedBy { it.date }
        val result = mutableListOf<TrendPoint>()
        var currentTrend: Double? = null
        for (entry in sorted) {
            val t = if (currentTrend == null) {
                entry.weightKg
            } else {
                currentTrend + 0.1 * (entry.weightKg - currentTrend)
            }
            currentTrend = t
            result.add(TrendPoint(date = entry.date, weightKg = entry.weightKg, trendKg = t))
        }
        return result
    }

    fun bmiCategory(bmi: Double): String = when {
        bmi < 18.5 -> "Underweight"
        bmi < 25.0 -> "Healthy"
        bmi < 30.0 -> "Overweight"
        else -> "Obese"
    }

    fun calculateMetrics(
        state: LeanPathState,
        weightKgOverride: Double? = null
    ): MetabolicMetrics {
        val p = state.profile
        val w = (weightKgOverride ?: currentWeightKg(state)).coerceAtLeast(25.0)
        val h = p.heightCm.coerceAtLeast(100.0)
        val hm = h / 100.0
        val isMale = p.sex == "m"
        val age = p.age.coerceAtLeast(14)

        val bmrMifflin = 10.0 * w + 6.25 * h - 5.0 * age + (if (isMale) 5.0 else -161.0)
        val bmrHarris = if (isMale) {
            88.362 + 13.397 * w + 4.799 * h - 5.677 * age
        } else {
            447.593 + 9.247 * w + 3.098 * h - 4.330 * age
        }

        val latestWaist = state.measurements.lastOrNull { it.siteKey == "waist" }?.valueCmOrPct ?: p.waistCm
        val latestNeck = state.measurements.lastOrNull { it.siteKey == "neck" }?.valueCmOrPct ?: p.neckCm
        val latestHip = state.measurements.lastOrNull { it.siteKey == "hips" }?.valueCmOrPct ?: p.hipCm
        val loggedBf = state.measurements.lastOrNull { it.siteKey == "bf" }?.valueCmOrPct

        val navyBf = if (latestWaist != null && latestNeck != null && latestWaist > latestNeck + 2.0) {
            if (isMale) {
                val denom = 1.0324 - 0.19077 * log10(latestWaist - latestNeck) + 0.15456 * log10(h)
                if (denom > 0) (495.0 / denom - 450.0).coerceIn(3.0, 65.0) else null
            } else if (latestHip != null && (latestWaist + latestHip - latestNeck) > 5.0) {
                val denom = 1.29579 - 0.35004 * log10(latestWaist + latestHip - latestNeck) + 0.22100 * log10(h)
                if (denom > 0) (495.0 / denom - 450.0).coerceIn(5.0, 65.0) else null
            } else null
        } else null

        val bf = loggedBf ?: navyBf
        val bmrKatch = if (bf != null && bf > 2.0) {
            370.0 + 21.6 * (w * (1.0 - bf / 100.0))
        } else null

        val tdee = (bmrKatch ?: bmrMifflin) * p.activityLevel
        val inchesOver5Ft = cmToIn(h) - 60.0

        val devine = (if (isMale) 50.0 else 45.5) + 2.3 * inchesOver5Ft
        val robinson = (if (isMale) 52.0 else 49.0) + (if (isMale) 1.9 else 1.7) * inchesOver5Ft
        val miller = (if (isMale) 56.2 else 53.1) + (if (isMale) 1.41 else 1.36) * inchesOver5Ft
        val hamwi = (if (isMale) 48.0 else 45.4) + (if (isMale) 2.7 else 2.2) * inchesOver5Ft

        val floor = if (isMale) 1500 else 1200
        val targetDate = p.targetDate.ifEmpty { dateOffset(120) }
        val daysRemaining = max(1L, daysBetween(dateOffset(0), targetDate))

        var deficit = max(0.0, (p.startWeightKg - p.goalWeightKg) * 7700.0 / daysRemaining.toDouble())
        val maxAllowedDeficit = if (p.healthConsideration.isNotEmpty() && p.healthConsideration != "none") 500.0 else 1000.0
        deficit = min(deficit, min(maxAllowedDeficit, max(0.0, tdee - floor)))

        val kcal = (tdee - deficit).roundToInt().coerceAtLeast(floor)
        val proteinG = (w * (if (p.dietStyle == "hp") 2.2 else 1.8)).roundToInt()
        val fatFraction = when (p.dietStyle) {
            "keto" -> 0.65
            "lf" -> 0.20
            else -> 0.28
        }
        val fatG = (kcal * fatFraction / 9.0).roundToInt()
        val carbsG = max(0, ((kcal - proteinG * 4 - fatG * 9) / 4.0).roundToInt())
        val fiberTargetG = (kcal / 1000.0 * 14.0).roundToInt().coerceAtLeast(18)

        val bmi = w / (hm * hm)
        return MetabolicMetrics(
            bmi = bmi,
            bmiCategory = bmiCategory(bmi),
            bmiPrime = bmi / 25.0,
            ponderalIndex = w / (hm * hm * hm),
            bmrMifflin = bmrMifflin,
            bmrHarrisBenedict = bmrHarris,
            bmrKatchMcArdle = bmrKatch,
            tdee = tdee,
            bodyFatPct = bf,
            waistToHeightRatio = latestWaist?.let { it / h },
            waistToHipRatio = if (latestWaist != null && latestHip != null && latestHip > 0) latestWaist / latestHip else null,
            idealDevineKg = devine,
            idealRobinsonKg = robinson,
            idealMillerKg = miller,
            idealHamwiKg = hamwi,
            healthyMinKg = 18.5 * hm * hm,
            healthyMaxKg = 24.9 * hm * hm,
            dailyDeficitKcal = deficit,
            targetKcal = kcal,
            proteinG = proteinG,
            fatG = fatG,
            carbsG = carbsG,
            fiberTargetG = fiberTargetG,
            calorieFloor = floor,
            leanMassKg = bf?.let { w * (1.0 - it / 100.0) },
            fatMassKg = bf?.let { w * (it / 100.0) }
        )
    }

    fun computeDailyFoodTotals(entries: List<FoodEntry>): DailyMacroTotals {
        return entries.fold(DailyMacroTotals()) { acc, item ->
            DailyMacroTotals(
                kcal = acc.kcal + item.kcal,
                proteinG = acc.proteinG + item.proteinG,
                carbsG = acc.carbsG + item.carbsG,
                fatG = acc.fatG + item.fatG,
                fiberG = acc.fiberG + item.fiberG,
                sodiumMg = acc.sodiumMg + item.sodiumMg,
                alcoholDrinks = acc.alcoholDrinks + item.alcoholDrinks
            )
        }
    }

    fun computeBurnedKcal(state: LeanPathState, date: String = dateOffset(0)): Int {
        return state.exercisesByDate[date]?.sumOf { it.kcalBurned } ?: 0
    }

    fun computeAdaptiveTdee(state: LeanPathState): Int? {
        val today = dateOffset(0)
        val foodDates = state.foodByDate.keys.filter { d ->
            val list = state.foodByDate[d].orEmpty()
            list.isNotEmpty() && daysBetween(d, today) in 1..28
        }
        if (foodDates.size < 10) return null

        val ema = computeEmaTrend(state.weighIns).filter { daysBetween(it.date, today) <= 28 }
        if (ema.size < 8) return null

        val span = daysBetween(ema.first().date, ema.last().date)
        if (span < 14) return null

        val avgIntake = foodDates.sumOf { d ->
            state.foodByDate[d].orEmpty().sumOf { it.kcal }
        } / foodDates.size.toDouble()

        val trendDeltaKg = ema.last().trendKg - ema.first().trendKg
        return (avgIntake - trendDeltaKg * 7700.0 / span.toDouble()).roundToInt()
    }

    fun computeWeeklyRateKg(state: LeanPathState): Double? {
        val ema = computeEmaTrend(state.weighIns)
        if (ema.size < 4) return null
        val last = ema.last()
        val firstInWindow = ema.firstOrNull { daysBetween(it.date, last.date) <= 14 } ?: return null
        val span = daysBetween(firstInWindow.date, last.date)
        if (span < 5) return null
        return (last.trendKg - firstInWindow.trendKg) / span.toDouble() * 7.0
    }

    fun isPlateau(state: LeanPathState): Boolean {
        val today = dateOffset(0)
        val ema = computeEmaTrend(state.weighIns).filter { daysBetween(it.date, today) <= 14 }
        return ema.size >= 8 && abs(ema.last().trendKg - ema.first().trendKg) < 0.2
    }

    fun computeStreak(state: LeanPathState): Int {
        val dates = state.weighIns.map { it.date }.toSet()
        val today = dateOffset(0)
        var i = if (dates.contains(today)) 0L else 1L
        var count = 0
        while (dates.contains(dateOffset(-i))) {
            count++
            i++
        }
        return count
    }

    fun computeHabitStreak(state: LeanPathState, habitName: String): Int {
        val today = dateOffset(0)
        val doneToday = state.habitsDoneByDate[today].orEmpty().contains(habitName)
        var i = if (doneToday) 0L else 1L
        var count = 0
        while (state.habitsDoneByDate[dateOffset(-i)].orEmpty().contains(habitName)) {
            count++
            i++
        }
        return count
    }

    fun computeXp(state: LeanPathState): Int {
        val weighInXp = state.weighIns.size * 10
        val foodXp = state.foodByDate.values.sumOf { it.size * 2 }
        val waterXp = state.waterByDateMl.count { it.value > 0 } * 3
        return weighInXp + foodXp + waterXp
    }

    fun computeLevel(xp: Int): Int {
        return floor(sqrt(xp / 50.0)).toInt() + 1
    }

    fun computeUnlockedBadges(state: LeanPathState): List<String> {
        val cur = currentWeightKg(state)
        val lost = state.profile.startWeightKg - cur
        val streak = computeStreak(state)
        val totalFoods = state.foodByDate.values.sumOf { it.size }
        val startW = state.profile.startWeightKg.coerceAtLeast(1.0)

        val checks = listOf(
            (state.weighIns.isNotEmpty()) to "First weigh-in",
            (streak >= 7) to "7-day streak",
            (streak >= 30) to "30-day streak",
            (lost >= 1.0) to "1 kg lost",
            (lost >= 5.0) to "5 kg lost",
            (lost / startW >= 0.05) to "5% body weight",
            (lost / startW >= 0.10) to "10% body weight",
            (totalFoods >= 30) to "30 foods logged",
            (cur <= state.profile.goalWeightKg) to "Goal reached"
        )
        return checks.filter { it.first }.map { it.second }
    }

    fun computeRecoveryScore(w: WellnessEntry): Int {
        val sleepScore = min(w.sleepHours, 8.0) / 8.0 * 50.0
        val energyScore = w.energy * 4.0
        val stressScore = (5 - w.stress) * 5.0
        val moodScore = w.mood * 2.0
        return (sleepScore + energyScore + stressScore + moodScore).roundToInt().coerceIn(0, 100)
    }

    fun computeWeeklyReview(state: LeanPathState, weeksAgo: Int): WeeklyReviewSummary {
        val dates = (0 until 7).map { i -> dateOffset(-(i + 7L * weeksAgo)) }
        val weekWeighIns = state.weighIns.filter { dates.contains(it.date) }
        val foodDays = dates.filter { state.foodByDate[it].orEmpty().isNotEmpty() }
        val dailyKcals = foodDays.map { d -> state.foodByDate[d].orEmpty().sumOf { it.kcal } }
        val sleepValues = dates.mapNotNull { state.wellnessByDate[it]?.sleepHours }

        return WeeklyReviewSummary(
            avgWeightKg = if (weekWeighIns.isNotEmpty()) weekWeighIns.map { it.weightKg }.average() else null,
            weighInDays = weekWeighIns.size,
            foodLoggedDays = foodDays.size,
            avgKcal = if (dailyKcals.isNotEmpty()) dailyKcals.average() else null,
            exerciseDays = dates.count { computeBurnedKcal(state, it) > 0 },
            activeMinutes = dates.sumOf { d -> state.exercisesByDate[d].orEmpty().sumOf { it.minutes } },
            waterDays = dates.count { (state.waterByDateMl[it] ?: 0) > 0 },
            avgSleepHours = if (sleepValues.isNotEmpty()) sleepValues.average() else null
        )
    }

    fun computePearsonCorrelation(points: List<Pair<Double, Double>>): Double? {
        if (points.size < 6) return null
        val n = points.size.toDouble()
        val meanX = points.sumOf { it.first } / n
        val meanY = points.sumOf { it.second } / n
        val num = points.sumOf { (it.first - meanX) * (it.second - meanY) }
        val denX = points.sumOf { (it.first - meanX).pow(2) }
        val denY = points.sumOf { (it.second - meanY).pow(2) }
        val denom = sqrt(denX * denY)
        return if (denom > 1e-9) (num / denom).coerceIn(-1.0, 1.0) else 0.0
    }

    fun generate30DayDemoState(): LeanPathState {
        val startWeight = 86.4
        val goalWeight = 76.0
        val weighIns = mutableListOf<WeighInEntry>()
        val foodByDate = mutableMapOf<String, List<FoodEntry>>()
        val waterByDate = mutableMapOf<String, Int>()
        val exercisesByDate = mutableMapOf<String, List<ExerciseEntry>>()
        val stepsByDate = mutableMapOf<String, Int>()
        val wellnessByDate = mutableMapOf<String, WellnessEntry>()
        val habitsDoneByDate = mutableMapOf<String, List<String>>()

        val habits = listOf(
            "10,000 daily steps",
            "Hit daily protein target",
            "2.5L+ hydration",
            "No late snacks after 8pm"
        )

        // Generate 28 days of realistic weight loss with water noise, food intake, sleep, and exercise
        for (dayOffset in -27L..0L) {
            val d = dateOffset(dayOffset)
            val progressDays = (dayOffset + 27).toInt()
            val noise = sin(progressDays * 1.3) * 0.45 + cos(progressDays * 0.7) * 0.25
            val weight = (startWeight - progressDays * 0.088 + noise).let { (it * 10.0).roundToInt() / 10.0 }
            weighIns.add(WeighInEntry(date = d, weightKg = weight))

            val breakfast = FoodEntry(
                id = UUID.randomUUID().toString(),
                name = "Oats 40g & Greek yoghurt 150g",
                kcal = 250.0,
                proteinG = 22.0,
                carbsG = 33.0,
                fatG = 3.7,
                fiberG = 4.5,
                sodiumMg = 95.0,
                mealType = "Breakfast"
            )
            val lunch = FoodEntry(
                id = UUID.randomUUID().toString(),
                name = "Chicken breast 150g & Brown rice",
                kcal = 415.0 + (progressDays % 3) * 35.0,
                proteinG = 50.0,
                carbsG = 38.0,
                fatG = 7.0,
                fiberG = 4.0,
                sodiumMg = 480.0,
                mealType = "Lunch"
            )
            val dinner = FoodEntry(
                id = UUID.randomUUID().toString(),
                name = "Wild Salmon 150g, Broccoli & Sweet potato",
                kcal = 520.0 + (progressDays % 4) * 45.0,
                proteinG = 38.0,
                carbsG = 42.0,
                fatG = 21.0,
                fiberG = 7.5,
                sodiumMg = 540.0,
                mealType = "Dinner"
            )
            val snack = FoodEntry(
                id = UUID.randomUUID().toString(),
                name = "Protein shake & Almonds 28g",
                kcal = 284.0,
                proteinG = 30.0,
                carbsG = 9.0,
                fatG = 15.5,
                fiberG = 4.0,
                sodiumMg = 160.0,
                mealType = "Snack"
            )
            foodByDate[d] = if (dayOffset == 0L) {
                listOf(breakfast, lunch, snack)
            } else {
                listOf(breakfast, lunch, dinner, snack)
            }

            waterByDate[d] = if (dayOffset == 0L) 2000 else 2500 + (progressDays % 3) * 250
            stepsByDate[d] = 8400 + (progressDays % 5) * 950

            if (progressDays % 2 == 0) {
                val ex = EXERCISE_METS[progressDays % EXERCISE_METS.size]
                val mins = 35 + (progressDays % 3) * 10
                val burned = (ex.met * weight * mins / 60.0).roundToInt()
                exercisesByDate[d] = listOf(
                    ExerciseEntry(
                        id = UUID.randomUUID().toString(),
                        name = ex.name,
                        minutes = mins,
                        kcalBurned = burned
                    )
                )
            }

            wellnessByDate[d] = WellnessEntry(
                sleepHours = 7.0 + (progressDays % 3) * 0.5,
                restingHr = 61 - (progressDays / 8),
                mood = 4 + (progressDays % 2),
                energy = 4,
                stress = 2
            )

            habitsDoneByDate[d] = if (progressDays % 4 == 0) habits.take(3) else habits
        }

        val measurements = listOf(
            BodyMeasurementEntry(UUID.randomUUID().toString(), dateOffset(-27), "waist", 92.0),
            BodyMeasurementEntry(UUID.randomUUID().toString(), dateOffset(-14), "waist", 90.2),
            BodyMeasurementEntry(UUID.randomUUID().toString(), dateOffset(0), "waist", 88.4),
            BodyMeasurementEntry(UUID.randomUUID().toString(), dateOffset(-27), "neck", 39.5),
            BodyMeasurementEntry(UUID.randomUUID().toString(), dateOffset(0), "neck", 39.0),
            BodyMeasurementEntry(UUID.randomUUID().toString(), dateOffset(-27), "chest", 104.0),
            BodyMeasurementEntry(UUID.randomUUID().toString(), dateOffset(0), "chest", 102.5)
        )

        val journal = listOf(
            JournalEntry(UUID.randomUUID().toString(), dateOffset(-18), "nsv", "Dropped one belt notch comfortably this morning!"),
            JournalEntry(UUID.randomUUID().toString(), dateOffset(-9), "note", "Higher protein at breakfast eliminated my 4pm energy crash."),
            JournalEntry(UUID.randomUUID().toString(), dateOffset(-2), "nsv", "Ran 5km without stopping and resting heart rate hit 58 bpm.")
        )

        val savedMeals = listOf(
            SavedMeal(
                id = UUID.randomUUID().toString(),
                name = "High-Protein Power Oats",
                kcal = 370.0,
                proteinG = 35.0,
                carbsG = 36.0,
                fatG = 6.5,
                fiberG = 5.0
            ),
            SavedMeal(
                id = UUID.randomUUID().toString(),
                name = "Mediterranean Salmon Bowl",
                kcal = 540.0,
                proteinG = 42.0,
                carbsG = 45.0,
                fatG = 21.0,
                fiberG = 8.0
            )
        )

        val mealPlan = mapOf(
            0 to "High-Protein Oats, Chicken Brown Rice Bowl, Grilled Salmon & Broccoli",
            1 to "Greek Yoghurt & Berries, Turkey Quinoa Salad, Lean Beef Stir-Fry",
            2 to "Eggs & Avocado Toast, Lentil Soup & Chicken, Baked Cod & Sweet Potato",
            3 to "Protein Shake & Oats, Tuna Salad Wrap, Tofu & Vegetable Curry",
            4 to "Greek Yoghurt Parfait, Chicken Burrito Bowl, Homemade Thin-Crust Pizza & Salad",
            5 to "Weekend Omelette, Grilled Salmon Salad, Steak & Roasted Vegetables",
            6 to "Oats & Banana, Meal Prep Chicken & Rice, Roast Chicken & Greens"
        )

        val groceries = listOf(
            GroceryItem(UUID.randomUUID().toString(), "Greek yoghurt (1 kg tub)", true),
            GroceryItem(UUID.randomUUID().toString(), "Wild salmon fillets (600g)", false),
            GroceryItem(UUID.randomUUID().toString(), "Chicken breast (1 kg)", true),
            GroceryItem(UUID.randomUUID().toString(), "Rolled oats & almonds", false),
            GroceryItem(UUID.randomUUID().toString(), "Broccoli, spinach & sweet potatoes", false),
            GroceryItem(UUID.randomUUID().toString(), "Avocados (4 pack)", false)
        )

        return LeanPathState(
            onboarded = true,
            profile = UserProfile(
                units = "met",
                sex = "m",
                age = 32,
                heightCm = 178.0,
                startWeightKg = startWeight,
                goalWeightKg = goalWeight,
                startDate = dateOffset(-27),
                targetDate = dateOffset(90),
                activityLevel = 1.55,
                dietStyle = "hp",
                healthConsideration = "none",
                motivationWhy = "Sustained daily energy, metabolic health, and effortless 10K runs",
                waistCm = 88.4,
                neckCm = 39.0
            ),
            weighIns = weighIns,
            foodByDate = foodByDate,
            waterByDateMl = waterByDate,
            activeFast = FastingSession(
                startTimeMillis = System.currentTimeMillis() - (11L * 3600_000L + 20L * 60_000L),
                targetHours = 16
            ),
            measurements = measurements,
            exercisesByDate = exercisesByDate,
            stepsByDate = stepsByDate,
            wellnessByDate = wellnessByDate,
            habitsList = habits,
            habitsDoneByDate = habitsDoneByDate,
            journalEntries = journal,
            weeklyMealPlan = mealPlan,
            groceryList = groceries,
            savedMeals = savedMeals
        )
    }

    fun serializeStateToJson(state: LeanPathState): String {
        val root = JSONObject()
        root.put("onboarded", state.onboarded)
        root.put("themePreference", state.themePreference)

        val pObj = JSONObject().apply {
            put("units", state.profile.units)
            put("sex", state.profile.sex)
            put("age", state.profile.age)
            put("heightCm", state.profile.heightCm)
            put("startWeightKg", state.profile.startWeightKg)
            put("goalWeightKg", state.profile.goalWeightKg)
            put("startDate", state.profile.startDate)
            put("targetDate", state.profile.targetDate)
            put("activityLevel", state.profile.activityLevel)
            put("dietStyle", state.profile.dietStyle)
            put("healthConsideration", state.profile.healthConsideration)
            put("motivationWhy", state.profile.motivationWhy)
            state.profile.waistCm?.let { put("waistCm", it) }
            state.profile.neckCm?.let { put("neckCm", it) }
            state.profile.hipCm?.let { put("hipCm", it) }
        }
        root.put("profile", pObj)

        val wArr = JSONArray()
        state.weighIns.forEach { w ->
            wArr.put(JSONObject().apply {
                put("d", w.date)
                put("w", w.weightKg)
            })
        }
        root.put("weighIns", wArr)

        val fObj = JSONObject()
        state.foodByDate.forEach { (date, list) ->
            val arr = JSONArray()
            list.forEach { f ->
                arr.put(JSONObject().apply {
                    put("id", f.id)
                    put("n", f.name)
                    put("k", f.kcal)
                    put("p", f.proteinG)
                    put("c", f.carbsG)
                    put("f", f.fatG)
                    put("fi", f.fiberG)
                    put("na", f.sodiumMg)
                    put("al", f.alcoholDrinks)
                    put("m", f.mealType)
                })
            }
            fObj.put(date, arr)
        }
        root.put("foodByDate", fObj)

        val h2oObj = JSONObject()
        state.waterByDateMl.forEach { (d, ml) -> h2oObj.put(d, ml) }
        root.put("waterByDateMl", h2oObj)

        state.activeFast?.let { fast ->
            root.put("activeFast", JSONObject().apply {
                put("s", fast.startTimeMillis)
                put("h", fast.targetHours)
            })
        }

        val mArr = JSONArray()
        state.measurements.forEach { m ->
            mArr.put(JSONObject().apply {
                put("id", m.id)
                put("d", m.date)
                put("k", m.siteKey)
                put("v", m.valueCmOrPct)
            })
        }
        root.put("measurements", mArr)

        val xrObj = JSONObject()
        state.exercisesByDate.forEach { (date, list) ->
            val arr = JSONArray()
            list.forEach { x ->
                arr.put(JSONObject().apply {
                    put("id", x.id)
                    put("n", x.name)
                    put("min", x.minutes)
                    put("k", x.kcalBurned)
                })
            }
            xrObj.put(date, arr)
        }
        root.put("exercisesByDate", xrObj)

        val spObj = JSONObject()
        state.stepsByDate.forEach { (d, s) -> spObj.put(d, s) }
        root.put("stepsByDate", spObj)

        val slObj = JSONObject()
        state.wellnessByDate.forEach { (d, w) ->
            slObj.put(d, JSONObject().apply {
                put("h", w.sleepHours)
                put("r", w.restingHr)
                put("m", w.mood)
                put("e", w.energy)
                put("s", w.stress)
            })
        }
        root.put("wellnessByDate", slObj)

        val habArr = JSONArray()
        state.habitsList.forEach { habArr.put(it) }
        root.put("habitsList", habArr)

        val habDoneObj = JSONObject()
        state.habitsDoneByDate.forEach { (d, list) ->
            val arr = JSONArray()
            list.forEach { arr.put(it) }
            habDoneObj.put(d, arr)
        }
        root.put("habitsDoneByDate", habDoneObj)

        val jrArr = JSONArray()
        state.journalEntries.forEach { j ->
            jrArr.put(JSONObject().apply {
                put("id", j.id)
                put("d", j.date)
                put("t", j.type)
                put("x", j.text)
            })
        }
        root.put("journalEntries", jrArr)

        val plObj = JSONObject()
        state.weeklyMealPlan.forEach { (dayIdx, plan) -> plObj.put(dayIdx.toString(), plan) }
        root.put("weeklyMealPlan", plObj)

        val grArr = JSONArray()
        state.groceryList.forEach { g ->
            grArr.put(JSONObject().apply {
                put("id", g.id)
                put("x", g.text)
                put("d", g.checked)
            })
        }
        root.put("groceryList", grArr)

        val smArr = JSONArray()
        state.savedMeals.forEach { s ->
            smArr.put(JSONObject().apply {
                put("id", s.id)
                put("n", s.name)
                put("k", s.kcal)
                put("p", s.proteinG)
                put("c", s.carbsG)
                put("f", s.fatG)
            })
        }
        root.put("savedMeals", smArr)

        val phArr = JSONArray()
        state.progressPhotos.forEach { ph ->
            phArr.put(JSONObject().apply {
                put("id", ph.id)
                put("d", ph.date)
                put("u", ph.uriString)
                put("n", ph.note)
            })
        }
        root.put("progressPhotos", phArr)

        val rmArr = JSONArray()
        state.reminders.forEach { rm ->
            rmArr.put(JSONObject().apply {
                put("id", rm.id)
                put("t", rm.timeHm)
                put("x", rm.label)
            })
        }
        root.put("reminders", rmArr)

        val rwObj = JSONObject()
        state.milestoneRewards.forEach { (pct, reward) -> rwObj.put(pct.toString(), reward) }
        root.put("milestoneRewards", rwObj)

        return root.toString()
    }

    fun deserializeStateFromJson(jsonString: String): LeanPathState {
        return try {
            val root = JSONObject(jsonString)
            // Also support importing legacy web LeanPath JSON format ("p", "w", "f", "h2o")
            if (root.has("p") && root.has("w") && !root.has("profile")) {
                return parseWebLegacyJson(root)
            }

            val pObj = root.optJSONObject("profile") ?: JSONObject()
            val profile = UserProfile(
                units = pObj.optString("units", "met"),
                sex = pObj.optString("sex", "m"),
                age = pObj.optInt("age", 32),
                heightCm = pObj.optDouble("heightCm", 175.0),
                startWeightKg = pObj.optDouble("startWeightKg", 85.0),
                goalWeightKg = pObj.optDouble("goalWeightKg", 75.0),
                startDate = pObj.optString("startDate", dateOffset(-14)),
                targetDate = pObj.optString("targetDate", dateOffset(90)),
                activityLevel = pObj.optDouble("activityLevel", 1.375),
                dietStyle = pObj.optString("dietStyle", "bal"),
                healthConsideration = pObj.optString("healthConsideration", "none"),
                motivationWhy = pObj.optString("motivationWhy", ""),
                waistCm = if (pObj.has("waistCm")) pObj.optDouble("waistCm") else null,
                neckCm = if (pObj.has("neckCm")) pObj.optDouble("neckCm") else null,
                hipCm = if (pObj.has("hipCm")) pObj.optDouble("hipCm") else null
            )

            val weighIns = mutableListOf<WeighInEntry>()
            val wArr = root.optJSONArray("weighIns") ?: JSONArray()
            for (i in 0 until wArr.length()) {
                val item = wArr.getJSONObject(i)
                weighIns.add(WeighInEntry(item.optString("d"), item.optDouble("w")))
            }

            val foodByDate = mutableMapOf<String, List<FoodEntry>>()
            val fObj = root.optJSONObject("foodByDate") ?: JSONObject()
            fObj.keys().forEach { date ->
                val arr = fObj.optJSONArray(date) ?: JSONArray()
                val list = mutableListOf<FoodEntry>()
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    list.add(
                        FoodEntry(
                            id = item.optString("id", UUID.randomUUID().toString()),
                            name = item.optString("n", "Food"),
                            kcal = item.optDouble("k", 0.0),
                            proteinG = item.optDouble("p", 0.0),
                            carbsG = item.optDouble("c", 0.0),
                            fatG = item.optDouble("f", 0.0),
                            fiberG = item.optDouble("fi", 0.0),
                            sodiumMg = item.optDouble("na", 0.0),
                            alcoholDrinks = item.optDouble("al", 0.0),
                            mealType = item.optString("m", "Breakfast")
                        )
                    )
                }
                foodByDate[date] = list
            }

            val waterByDate = mutableMapOf<String, Int>()
            val h2oObj = root.optJSONObject("waterByDateMl") ?: JSONObject()
            h2oObj.keys().forEach { d -> waterByDate[d] = h2oObj.optInt(d, 0) }

            val activeFast = root.optJSONObject("activeFast")?.let {
                FastingSession(
                    startTimeMillis = it.optLong("s", System.currentTimeMillis()),
                    targetHours = it.optInt("h", 16)
                )
            }

            val measurements = mutableListOf<BodyMeasurementEntry>()
            val mArr = root.optJSONArray("measurements") ?: JSONArray()
            for (i in 0 until mArr.length()) {
                val item = mArr.getJSONObject(i)
                measurements.add(
                    BodyMeasurementEntry(
                        id = item.optString("id", UUID.randomUUID().toString()),
                        date = item.optString("d"),
                        siteKey = item.optString("k"),
                        valueCmOrPct = item.optDouble("v")
                    )
                )
            }

            val exercisesByDate = mutableMapOf<String, List<ExerciseEntry>>()
            val xrObj = root.optJSONObject("exercisesByDate") ?: JSONObject()
            xrObj.keys().forEach { date ->
                val arr = xrObj.optJSONArray(date) ?: JSONArray()
                val list = mutableListOf<ExerciseEntry>()
                for (i in 0 until arr.length()) {
                    val item = arr.getJSONObject(i)
                    list.add(
                        ExerciseEntry(
                            id = item.optString("id", UUID.randomUUID().toString()),
                            name = item.optString("n"),
                            minutes = item.optInt("min"),
                            kcalBurned = item.optInt("k")
                        )
                    )
                }
                exercisesByDate[date] = list
            }

            val stepsByDate = mutableMapOf<String, Int>()
            val spObj = root.optJSONObject("stepsByDate") ?: JSONObject()
            spObj.keys().forEach { d -> stepsByDate[d] = spObj.optInt(d, 0) }

            val wellnessByDate = mutableMapOf<String, WellnessEntry>()
            val slObj = root.optJSONObject("wellnessByDate") ?: JSONObject()
            slObj.keys().forEach { d ->
                val item = slObj.getJSONObject(d)
                wellnessByDate[d] = WellnessEntry(
                    sleepHours = item.optDouble("h", 7.0),
                    restingHr = item.optInt("r", 60),
                    mood = item.optInt("m", 4),
                    energy = item.optInt("e", 4),
                    stress = item.optInt("s", 2)
                )
            }

            val habitsList = mutableListOf<String>()
            val habArr = root.optJSONArray("habitsList")
            if (habArr != null) {
                for (i in 0 until habArr.length()) habitsList.add(habArr.getString(i))
            } else {
                habitsList.addAll(listOf("10,000 daily steps", "Hit daily protein target", "2.5L+ hydration"))
            }

            val habitsDoneByDate = mutableMapOf<String, List<String>>()
            val habDoneObj = root.optJSONObject("habitsDoneByDate") ?: JSONObject()
            habDoneObj.keys().forEach { d ->
                val arr = habDoneObj.optJSONArray(d) ?: JSONArray()
                val l = mutableListOf<String>()
                for (i in 0 until arr.length()) l.add(arr.getString(i))
                habitsDoneByDate[d] = l
            }

            val journalEntries = mutableListOf<JournalEntry>()
            val jrArr = root.optJSONArray("journalEntries") ?: JSONArray()
            for (i in 0 until jrArr.length()) {
                val item = jrArr.getJSONObject(i)
                journalEntries.add(
                    JournalEntry(
                        id = item.optString("id", UUID.randomUUID().toString()),
                        date = item.optString("d"),
                        type = item.optString("t", "note"),
                        text = item.optString("x")
                    )
                )
            }

            val weeklyMealPlan = mutableMapOf<Int, String>()
            val plObj = root.optJSONObject("weeklyMealPlan") ?: JSONObject()
            plObj.keys().forEach { k -> k.toIntOrNull()?.let { weeklyMealPlan[it] = plObj.optString(k) } }

            val groceryList = mutableListOf<GroceryItem>()
            val grArr = root.optJSONArray("groceryList") ?: JSONArray()
            for (i in 0 until grArr.length()) {
                val item = grArr.getJSONObject(i)
                groceryList.add(
                    GroceryItem(
                        id = item.optString("id", UUID.randomUUID().toString()),
                        text = item.optString("x"),
                        checked = item.optBoolean("d", false)
                    )
                )
            }

            val savedMeals = mutableListOf<SavedMeal>()
            val smArr = root.optJSONArray("savedMeals") ?: JSONArray()
            for (i in 0 until smArr.length()) {
                val item = smArr.getJSONObject(i)
                savedMeals.add(
                    SavedMeal(
                        id = item.optString("id", UUID.randomUUID().toString()),
                        name = item.optString("n"),
                        kcal = item.optDouble("k"),
                        proteinG = item.optDouble("p"),
                        carbsG = item.optDouble("c"),
                        fatG = item.optDouble("f")
                    )
                )
            }

            val progressPhotos = mutableListOf<ProgressPhotoEntry>()
            val phArr = root.optJSONArray("progressPhotos") ?: JSONArray()
            for (i in 0 until phArr.length()) {
                val item = phArr.getJSONObject(i)
                progressPhotos.add(
                    ProgressPhotoEntry(
                        id = item.optString("id", UUID.randomUUID().toString()),
                        date = item.optString("d"),
                        uriString = item.optString("u"),
                        note = item.optString("n", "")
                    )
                )
            }

            val reminders = mutableListOf<ReminderItem>()
            val rmArr = root.optJSONArray("reminders")
            if (rmArr != null) {
                for (i in 0 until rmArr.length()) {
                    val item = rmArr.getJSONObject(i)
                    reminders.add(
                        ReminderItem(
                            id = item.optString("id", UUID.randomUUID().toString()),
                            timeHm = item.optString("t", "08:00"),
                            label = item.optString("x", "Weigh in")
                        )
                    )
                }
            }

            val milestoneRewards = mutableMapOf<Int, String>()
            val rwObj = root.optJSONObject("milestoneRewards")
            if (rwObj != null) {
                rwObj.keys().forEach { k -> k.toIntOrNull()?.let { milestoneRewards[it] = rwObj.optString(k) } }
            }

            LeanPathState(
                onboarded = root.optBoolean("onboarded", true),
                profile = profile,
                weighIns = weighIns.sortedBy { it.date },
                foodByDate = foodByDate,
                waterByDateMl = waterByDate,
                activeFast = activeFast,
                themePreference = root.optString("themePreference", ""),
                measurements = measurements,
                exercisesByDate = exercisesByDate,
                stepsByDate = stepsByDate,
                wellnessByDate = wellnessByDate,
                habitsList = habitsList,
                habitsDoneByDate = habitsDoneByDate,
                journalEntries = journalEntries,
                weeklyMealPlan = weeklyMealPlan,
                groceryList = groceryList,
                savedMeals = savedMeals,
                progressPhotos = progressPhotos,
                reminders = reminders,
                milestoneRewards = milestoneRewards
            )
        } catch (e: Exception) {
            LeanPathState()
        }
    }

    private fun parseWebLegacyJson(root: JSONObject): LeanPathState {
        val p = root.optJSONObject("p") ?: JSONObject()
        val profile = UserProfile(
            units = p.optString("u", "met"),
            sex = p.optString("sex", "m"),
            age = p.optInt("age", 30),
            heightCm = p.optDouble("h", 175.0),
            startWeightKg = p.optDouble("w0", 85.0),
            goalWeightKg = p.optDouble("goal", 75.0),
            startDate = p.optString("start", dateOffset(-14)),
            targetDate = p.optString("date", dateOffset(90)),
            activityLevel = p.optDouble("act", 1.375),
            dietStyle = p.optString("diet", "bal"),
            healthConsideration = p.optString("hl", "none"),
            motivationWhy = p.optString("why", ""),
            waistCm = if (p.has("wc") && p.optDouble("wc") > 0) p.optDouble("wc") else null,
            neckCm = if (p.has("nc") && p.optDouble("nc") > 0) p.optDouble("nc") else null,
            hipCm = if (p.has("hc") && p.optDouble("hc") > 0) p.optDouble("hc") else null
        )
        val weighIns = mutableListOf<WeighInEntry>()
        val wArr = root.optJSONArray("w") ?: JSONArray()
        for (i in 0 until wArr.length()) {
            val item = wArr.getJSONObject(i)
            weighIns.add(WeighInEntry(item.optString("d"), item.optDouble("w")))
        }
        return LeanPathState(
            onboarded = true,
            profile = profile,
            weighIns = weighIns.sortedBy { it.date }
        )
    }

    fun exportCsv(state: LeanPathState): String {
        val sb = StringBuilder("date,weight_kg,trend_kg,calories_eaten,water_ml,steps\n")
        val emaMap = computeEmaTrend(state.weighIns).associateBy { it.date }
        for (w in state.weighIns) {
            val trend = emaMap[w.date]?.trendKg ?: w.weightKg
            val kcal = state.foodByDate[w.date].orEmpty().sumOf { it.kcal }.roundToInt()
            val water = state.waterByDateMl[w.date] ?: 0
            val steps = state.stepsByDate[w.date] ?: 0
            sb.append("${w.date},${String.format(Locale.US, "%.2f", w.weightKg)},${String.format(Locale.US, "%.2f", trend)},$kcal,$water,$steps\n")
        }
        return sb.toString()
    }
}
