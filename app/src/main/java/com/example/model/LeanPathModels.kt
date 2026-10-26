package com.example.model

data class UserProfile(
    val units: String = "met", // "met" or "imp"
    val sex: String = "m", // "m" or "f"
    val age: Int = 32,
    val heightCm: Double = 175.0,
    val startWeightKg: Double = 86.0,
    val goalWeightKg: Double = 74.0,
    val startDate: String = "",
    val targetDate: String = "",
    val activityLevel: Double = 1.375,
    val dietStyle: String = "bal", // "bal", "hp", "lf", "keto"
    val healthConsideration: String = "none", // "none", "preg", "dm", "ed"
    val motivationWhy: String = "Sustained daily energy, metabolic health, and long-term strength",
    val waistCm: Double? = null,
    val neckCm: Double? = null,
    val hipCm: Double? = null
)

data class WeighInEntry(
    val date: String, // YYYY-MM-DD
    val weightKg: Double
)

data class TrendPoint(
    val date: String,
    val weightKg: Double,
    val trendKg: Double
)

data class FoodEntry(
    val id: String,
    val name: String,
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double = 0.0,
    val sodiumMg: Double = 0.0,
    val alcoholDrinks: Double = 0.0,
    val mealType: String = "Breakfast" // Breakfast, Lunch, Dinner, Snack
)

data class SavedMeal(
    val id: String,
    val name: String,
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double = 0.0,
    val sodiumMg: Double = 0.0
)

data class FastingSession(
    val startTimeMillis: Long,
    val targetHours: Int
)

data class BodyMeasurementEntry(
    val id: String,
    val date: String,
    val siteKey: String, // waist, hips, chest, arm, thigh, neck, bf
    val valueCmOrPct: Double
)

data class ExerciseEntry(
    val id: String,
    val name: String,
    val minutes: Int,
    val kcalBurned: Int
)

data class WellnessEntry(
    val sleepHours: Double = 7.5,
    val restingHr: Int = 62,
    val mood: Int = 4, // 1..5
    val energy: Int = 4, // 1..5
    val stress: Int = 2 // 1..5 (low is good)
)

data class JournalEntry(
    val id: String,
    val date: String,
    val type: String, // "note" or "nsv" (Non-scale victory)
    val text: String
)

data class GroceryItem(
    val id: String,
    val text: String,
    val checked: Boolean = false
)

data class ProgressPhotoEntry(
    val id: String,
    val date: String,
    val uriString: String,
    val note: String = ""
)

data class ReminderItem(
    val id: String,
    val timeHm: String, // HH:MM
    val label: String
)

data class ProfileMeta(
    val key: String,
    val name: String
)

data class LeanPathState(
    val onboarded: Boolean = false,
    val profile: UserProfile = UserProfile(),
    val weighIns: List<WeighInEntry> = emptyList(),
    val foodByDate: Map<String, List<FoodEntry>> = emptyMap(),
    val waterByDateMl: Map<String, Int> = emptyMap(),
    val activeFast: FastingSession? = null,
    val themePreference: String = "", // "", "light", "dark"
    val measurements: List<BodyMeasurementEntry> = emptyList(),
    val exercisesByDate: Map<String, List<ExerciseEntry>> = emptyMap(),
    val stepsByDate: Map<String, Int> = emptyMap(),
    val wellnessByDate: Map<String, WellnessEntry> = emptyMap(),
    val habitsList: List<String> = listOf(
        "10,000 daily steps",
        "Hit daily protein target",
        "2.5L+ hydration",
        "No late snacks after 8pm"
    ),
    val habitsDoneByDate: Map<String, List<String>> = emptyMap(),
    val journalEntries: List<JournalEntry> = emptyList(),
    val weeklyMealPlan: Map<Int, String> = emptyMap(), // 0=Mon .. 6=Sun
    val groceryList: List<GroceryItem> = emptyList(),
    val savedMeals: List<SavedMeal> = emptyList(),
    val progressPhotos: List<ProgressPhotoEntry> = emptyList(),
    val reminders: List<ReminderItem> = listOf(
        ReminderItem(id = "rem_1", timeHm = "08:00", label = "Morning weigh-in & hydration")
    ),
    val dismissedRemindersByDate: Map<String, List<String>> = emptyMap(),
    val milestoneRewards: Map<Int, String> = mapOf(
        25 to "New insulated water bottle",
        50 to "New running shoes",
        75 to "Tailored athletic outfit",
        100 to "Weekend mountain getaway"
    )
)

data class MetabolicMetrics(
    val bmi: Double,
    val bmiCategory: String,
    val bmiPrime: Double,
    val ponderalIndex: Double,
    val bmrMifflin: Double,
    val bmrHarrisBenedict: Double,
    val bmrKatchMcArdle: Double?,
    val tdee: Double,
    val bodyFatPct: Double?,
    val waistToHeightRatio: Double?,
    val waistToHipRatio: Double?,
    val idealDevineKg: Double,
    val idealRobinsonKg: Double,
    val idealMillerKg: Double,
    val idealHamwiKg: Double,
    val healthyMinKg: Double,
    val healthyMaxKg: Double,
    val dailyDeficitKcal: Double,
    val targetKcal: Int,
    val proteinG: Int,
    val fatG: Int,
    val carbsG: Int,
    val fiberTargetG: Int,
    val calorieFloor: Int,
    val leanMassKg: Double?,
    val fatMassKg: Double?
)

data class DailyMacroTotals(
    val kcal: Double = 0.0,
    val proteinG: Double = 0.0,
    val carbsG: Double = 0.0,
    val fatG: Double = 0.0,
    val fiberG: Double = 0.0,
    val sodiumMg: Double = 0.0,
    val alcoholDrinks: Double = 0.0
)

data class WeeklyReviewSummary(
    val avgWeightKg: Double?,
    val weighInDays: Int,
    val foodLoggedDays: Int,
    val avgKcal: Double?,
    val exerciseDays: Int,
    val activeMinutes: Int,
    val waterDays: Int,
    val avgSleepHours: Double?
)

data class PresetFood(
    val name: String,
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val fiberG: Double = 0.0,
    val isAlcohol: Boolean = false
)

data class ExerciseMet(
    val name: String,
    val met: Double
)

data class EvidenceNote(
    val title: String,
    val body: String
)

val PRESET_FOODS = listOf(
    PresetFood("Egg (large)", 72.0, 6.0, 0.4, 5.0, 0.0),
    PresetFood("Chicken breast 100g", 165.0, 31.0, 0.0, 3.6, 0.0),
    PresetFood("Salmon 100g", 208.0, 20.0, 0.0, 13.0, 0.0),
    PresetFood("Greek yoghurt 150g", 100.0, 17.0, 6.0, 0.7, 0.0),
    PresetFood("Oats 40g", 150.0, 5.0, 27.0, 3.0, 4.0),
    PresetFood("Banana", 105.0, 1.3, 27.0, 0.4, 3.1),
    PresetFood("Apple", 95.0, 0.5, 25.0, 0.3, 4.4),
    PresetFood("Brown rice 150g", 165.0, 3.5, 35.0, 1.3, 2.4),
    PresetFood("White rice 150g", 195.0, 4.0, 43.0, 0.4, 0.6),
    PresetFood("Pasta 100g cooked", 160.0, 6.0, 31.0, 1.0, 1.8),
    PresetFood("Bread slice", 80.0, 3.0, 15.0, 1.0, 1.9),
    PresetFood("Avocado ½", 120.0, 1.5, 6.0, 11.0, 5.0),
    PresetFood("Almonds 28g", 164.0, 6.0, 6.0, 14.0, 3.5),
    PresetFood("Olive oil 1 tbsp", 119.0, 0.0, 0.0, 13.5, 0.0),
    PresetFood("Broccoli 100g", 34.0, 2.8, 7.0, 0.4, 2.6),
    PresetFood("Sweet potato 150g", 130.0, 2.4, 30.0, 0.2, 4.5),
    PresetFood("Lean beef 100g", 217.0, 26.0, 0.0, 12.0, 0.0),
    PresetFood("Tofu 100g", 76.0, 8.0, 2.0, 4.8, 0.3),
    PresetFood("Lentils 150g", 175.0, 13.0, 30.0, 0.6, 11.5),
    PresetFood("Milk 250ml", 122.0, 8.0, 12.0, 5.0, 0.0),
    PresetFood("Cheddar 30g", 120.0, 7.0, 0.4, 10.0, 0.0),
    PresetFood("Protein shake", 120.0, 24.0, 3.0, 1.5, 0.5),
    PresetFood("Pizza slice", 285.0, 12.0, 36.0, 10.0, 2.5),
    PresetFood("Beer 330ml", 150.0, 1.6, 13.0, 0.0, 0.0, isAlcohol = true),
    PresetFood("Wine glass", 125.0, 0.1, 4.0, 0.0, 0.0, isAlcohol = true),
    PresetFood("Chocolate 30g", 160.0, 2.0, 17.0, 9.0, 2.0)
)

val EXERCISE_METS = listOf(
    ExerciseMet("Walking", 3.5),
    ExerciseMet("Running", 8.3),
    ExerciseMet("Cycling", 7.5),
    ExerciseMet("Swimming", 6.0),
    ExerciseMet("Strength", 5.0),
    ExerciseMet("HIIT", 8.0),
    ExerciseMet("Yoga", 2.5),
    ExerciseMet("Hiking", 6.0)
)

val MEASUREMENT_SITES = listOf(
    "waist" to "Waist",
    "hips" to "Hips",
    "chest" to "Chest",
    "arm" to "Arm",
    "thigh" to "Thigh",
    "neck" to "Neck",
    "bf" to "Body fat %"
)

val DAILY_QUOTES = listOf(
    "Small steps every day add up.",
    "Consistency beats intensity.",
    "Progress, not perfection.",
    "You are building a new normal.",
    "The trend matters, not the day.",
    "Fuel your body, do not fight it.",
    "Every healthy choice counts.",
    "Be patient: the scale lags the work."
)

val EVIDENCE_NOTES = listOf(
    EvidenceNote(
        "BMI (Body Mass Index)",
        "Weight divided by height squared, with WHO category cut-offs. It cannot distinguish muscle from adipose tissue, so treat it as a population screening tool."
    ),
    EvidenceNote(
        "BMR: Mifflin-St Jeor (1990)",
        "In validation studies this has generally been among the most accurate resting metabolic equations for non-athletes. Typical error is roughly ±10%."
    ),
    EvidenceNote(
        "BMR: Harris-Benedict (revised 1984)",
        "An older predictive equation, often slightly higher than Mifflin-St Jeor. Shown for clinical comparison."
    ),
    EvidenceNote(
        "BMR: Katch-McArdle",
        "Uses lean body mass directly, so it can suit lean or muscular individuals better. Requires a body-fat percentage estimate."
    ),
    EvidenceNote(
        "TDEE (Total Daily Energy Expenditure)",
        "BMR multiplied by a physical activity factor (1.2 to 1.9). Formula TDEE is a starting estimate; your adaptive TDEE from real intake and trend weight is more personal."
    ),
    EvidenceNote(
        "7,700 kcal per kg Rule",
        "Approximate metabolizable energy in 1 kg of human adipose tissue. A useful simplification, though real weight change adapts over time (Hall et al., 2011)."
    ),
    EvidenceNote(
        "Adaptive TDEE",
        "Average logged calorie intake minus the energy delta implied by your EMA trend weight change. Accuracy increases with consistent daily logging."
    ),
    EvidenceNote(
        "US Navy Body Fat Method",
        "Estimated from neck, waist, height and (for women) hip circumference. Accuracy is typically within 3–4 percentage points of DEXA."
    ),
    EvidenceNote(
        "Waist-to-Height Ratio (WHtR)",
        "Keeping waist circumference below half your height (<0.50) is strongly associated with lower cardiometabolic risk across populations."
    ),
    EvidenceNote(
        "Ideal Weight Formulas",
        "Devine, Robinson, Miller and Hamwi were originally designed for clinical drug dosing and population estimates, not rigid personal targets."
    ),
    EvidenceNote(
        "EMA Trend Weight",
        "An exponentially weighted moving average (smoothing factor α = 0.1), popularized by The Hacker's Diet. It filters out daily glycogen and water noise."
    ),
    EvidenceNote(
        "Safe Pace of Weight Loss",
        "Roughly 0.25%–1.0% of total body weight per week is the evidence-based sweet spot. Faster loss increases lean mass catabolism."
    ),
    EvidenceNote(
        "Protein 1.6–2.2 g/kg",
        "Systematic reviews show higher dietary protein preserves lean skeletal muscle during an energy deficit and improves satiety."
    ),
    EvidenceNote(
        "Calorie Safety Floors",
        "1,200 kcal (women) and 1,500 kcal (men) are common clinical minimums for unsupervised nutrition plans to ensure micronutrient adequacy."
    ),
    EvidenceNote(
        "Planned Diet Breaks",
        "Controlled trials (e.g., MATADOR, 2018) suggest intermittent maintenance phases after 8+ weeks of dieting can reduce adaptive thermogenesis and diet fatigue."
    )
)
