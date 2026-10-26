package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.MetabolicEngine
import com.example.model.LeanPathState
import com.example.model.TrendPoint
import com.example.ui.theme.*
import java.util.Locale
import kotlin.math.*

@Composable
fun RadialMetabolicGauge(
    valueText: String,
    subtitle: String,
    progress: Float,
    accentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 128.dp,
    strokeWidth: Dp = 12.dp
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "gaugeProgress"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = "$valueText $subtitle" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val arcDiameter = this.size.minDimension - strokePx
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcDiameter, arcDiameter),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )

            drawArc(
                color = accentColor,
                startAngle = 135f,
                sweepAngle = 270f * animatedProgress,
                useCenter = false,
                topLeft = topLeft,
                size = Size(arcDiameter, arcDiameter),
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 12.dp)
        ) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun BmiSpectrumBar(
    bmi: Double,
    modifier: Modifier = Modifier
) {
    val norm = ((bmi - 15.0) / 25.0).toFloat().coerceIn(0f, 1f)
    val animatedNorm by animateFloatAsState(
        targetValue = norm,
        animationSpec = tween(500),
        label = "bmiNorm"
    )
    val markerColor = MaterialTheme.colorScheme.onSurface
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(34.dp)
                .semantics { contentDescription = "BMI Spectrum Gauge ${String.format(Locale.US, "%.1f", bmi)}" }
        ) {
            val w = size.width
            val barY = 14.dp.toPx()
            val barH = 10.dp.toPx()

            val w1 = w * (3.5f / 25f) // 15..18.5
            val w2 = w * (6.5f / 25f) // 18.5..25
            val w3 = w * (5.0f / 25f) // 25..30
            val w4 = w * (10.0f / 25f) // 30..40

            drawRoundRect(
                color = HydrationBlue,
                topLeft = Offset(0f, barY),
                size = Size(w1 - 2.dp.toPx(), barH),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            drawRoundRect(
                color = primaryColor,
                topLeft = Offset(w1, barY),
                size = Size(w2 - 2.dp.toPx(), barH),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            drawRoundRect(
                color = FatAmber,
                topLeft = Offset(w1 + w2, barY),
                size = Size(w3 - 2.dp.toPx(), barH),
                cornerRadius = CornerRadius(4.dp.toPx())
            )
            drawRoundRect(
                color = errorColor,
                topLeft = Offset(w1 + w2 + w3, barY),
                size = Size(w4, barH),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            // Pointer triangle
            val px = (w * animatedNorm).coerceIn(6.dp.toPx(), w - 6.dp.toPx())
            val path = Path().apply {
                moveTo(px, barY - 2.dp.toPx())
                lineTo(px - 6.dp.toPx(), 1.dp.toPx())
                lineTo(px + 6.dp.toPx(), 1.dp.toPx())
                close()
            }
            drawPath(path = path, color = markerColor)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("15", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("18.5 Healthy", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text("25", style = MaterialTheme.typography.labelSmall, color = FatAmber)
            Text("30+", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
fun WeightTrendChart(
    points: List<TrendPoint>,
    goalWeightKg: Double,
    units: String,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Log at least 2 weigh-ins to unlock your EMA trend curve and water-noise filter.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        return
    }

    val allValues = points.flatMap { listOf(it.weightKg, it.trendKg) } + goalWeightKg
    val minKg = (allValues.minOrNull() ?: 60.0) - 0.6
    val maxKg = (allValues.maxOrNull() ?: 90.0) + 0.6
    val firstDate = points.first().date
    val lastDate = points.last().date
    val spanDays = max(1L, MetabolicEngine.daysBetween(firstDate, lastDate)).toFloat()

    val trendColor = MaterialTheme.colorScheme.primary
    val dotColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    val goalColor = FatAmber
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Max: ${MetabolicEngine.formatWeight(maxKg - 0.6, units)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Goal: ${MetabolicEngine.formatWeight(goalWeightKg, units)}",
                style = MaterialTheme.typography.labelSmall,
                color = goalColor
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .semantics { contentDescription = "Weight and EMA Trend Chart" }
        ) {
            val padLeft = 8.dp.toPx()
            val padRight = 8.dp.toPx()
            val padTop = 12.dp.toPx()
            val padBottom = 12.dp.toPx()
            val plotW = size.width - padLeft - padRight
            val plotH = size.height - padTop - padBottom

            fun xForDate(dateIso: String): Float {
                val d = MetabolicEngine.daysBetween(firstDate, dateIso).toFloat()
                return padLeft + (d / spanDays) * plotW
            }

            fun yForKg(kg: Double): Float {
                val ratio = ((kg - minKg) / (maxKg - minKg).coerceAtLeast(0.1)).toFloat()
                return padTop + plotH * (1f - ratio)
            }

            // Horizontal reference lines
            listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
                val y = padTop + plotH * frac
                drawLine(
                    color = gridColor,
                    start = Offset(padLeft, y),
                    end = Offset(size.width - padRight, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Goal dashed line
            val goalY = yForKg(goalWeightKg)
            drawLine(
                color = goalColor,
                start = Offset(padLeft, goalY),
                end = Offset(size.width - padRight, goalY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
            )

            // Area fill under EMA trend line
            val trendPath = Path()
            val fillPath = Path()
            points.forEachIndexed { idx, pt ->
                val x = xForDate(pt.date)
                val y = yForKg(pt.trendKg)
                if (idx == 0) {
                    trendPath.moveTo(x, y)
                    fillPath.moveTo(x, size.height - padBottom)
                    fillPath.lineTo(x, y)
                } else {
                    trendPath.lineTo(x, y)
                    fillPath.lineTo(x, y)
                }
                if (idx == points.lastIndex) {
                    fillPath.lineTo(x, size.height - padBottom)
                    fillPath.close()
                }
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        trendColor.copy(alpha = 0.26f),
                        trendColor.copy(alpha = 0.0f)
                    )
                )
            )

            // Daily raw scale weight dots
            points.forEach { pt ->
                val x = xForDate(pt.date)
                val y = yForKg(pt.weightKg)
                drawCircle(
                    color = dotColor,
                    radius = 3.5.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            // Smooth EMA trend curve
            drawPath(
                path = trendPath,
                color = trendColor,
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(firstDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "Min: ${MetabolicEngine.formatWeight(minKg + 0.6, units)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(lastDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun WeightProjectionConeChart(
    currentTrendKg: Double,
    goalWeightKg: Double,
    weeklyRateKg: Double,
    units: String,
    modifier: Modifier = Modifier
) {
    val weeksToGoal = ((currentTrendKg - goalWeightKg) / -weeklyRateKg).coerceAtLeast(1.0)
    val maxWeeks = max(weeksToGoal * 1.4, 2.0)
    val minKg = goalWeightKg - 0.6
    val maxKg = currentTrendKg + 0.6
    val primaryColor = MaterialTheme.colorScheme.primary
    val goalColor = FatAmber

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .semantics { contentDescription = "Weight Goal Projection Cone Chart" }
        ) {
            val pad = 12.dp.toPx()
            val plotW = size.width - pad * 2
            val plotH = size.height - pad * 2

            fun xForWeek(w: Double): Float = (pad + (w / maxWeeks) * plotW).toFloat()
            fun yForKg(kg: Double): Float {
                val clamped = kg.coerceIn(minKg, maxKg)
                val ratio = ((clamped - minKg) / (maxKg - minKg).coerceAtLeast(0.1)).toFloat()
                return pad + plotH * (1f - ratio)
            }
            fun weightAt(rate: Double, wk: Double): Double = max(minKg, currentTrendKg + rate * wk)

            // Confidence cone polygon (0.6x to 1.4x rate)
            val conePath = Path().apply {
                moveTo(xForWeek(0.0), yForKg(currentTrendKg))
                lineTo(xForWeek(maxWeeks), yForKg(weightAt(weeklyRateKg * 0.6, maxWeeks)))
                lineTo(xForWeek(maxWeeks), yForKg(weightAt(weeklyRateKg * 1.4, maxWeeks)))
                close()
            }
            drawPath(path = conePath, color = primaryColor.copy(alpha = 0.22f))

            // Goal line
            val goalY = yForKg(goalWeightKg)
            drawLine(
                color = goalColor,
                start = Offset(pad, goalY),
                end = Offset(size.width - pad, goalY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
            )

            // Central trajectory line
            drawLine(
                color = primaryColor,
                start = Offset(xForWeek(0.0), yForKg(currentTrendKg)),
                end = Offset(xForWeek(maxWeeks), yForKg(weightAt(weeklyRateKg, maxWeeks))),
                strokeWidth = 3.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 8f), 0f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Today (${MetabolicEngine.formatWeight(currentTrendKg, units)})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("+${maxWeeks.roundToInt()} wks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun MacroSplitDonutChart(
    proteinG: Double,
    carbsG: Double,
    fatG: Double,
    modifier: Modifier = Modifier
) {
    val pKcal = (proteinG * 4.0).toFloat()
    val cKcal = (carbsG * 4.0).toFloat()
    val fKcal = (fatG * 9.0).toFloat()
    val total = (pKcal + cKcal + fKcal).coerceAtLeast(1f)

    val segments = listOf(
        Triple("Protein", pKcal / total, ProteinBlue),
        Triple("Carbs", cKcal / total, CarbsEmerald),
        Triple("Fat", fKcal / total, FatAmber)
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { contentDescription = "Macro Split Donut Chart" }
            ) {
                val stroke = 16.dp.toPx()
                val d = size.minDimension - stroke
                val topLeft = Offset(stroke / 2f, stroke / 2f)
                var startAngle = -90f
                segments.forEach { (_, ratio, color) ->
                    val sweep = ratio * 360f
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = Size(d, d),
                        style = Stroke(width = stroke, cap = StrokeCap.Butt)
                    )
                    startAngle += sweep
                }
            }
            Text(
                text = "${total.roundToInt()}\nkcal",
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            segments.forEachIndexed { idx, (label, ratio, color) ->
                val grams = when (idx) {
                    0 -> proteinG.roundToInt()
                    1 -> carbsG.roundToInt()
                    else -> fatG.roundToInt()
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Text(
                        text = "$label ${(ratio * 100).roundToInt()}% (${grams}g)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun HabitRadarChart(
    values: List<Float>, // 5 values in 0..1: Weigh-in, Food, Active, Water, Sleep
    modifier: Modifier = Modifier
) {
    val labels = listOf("Weigh-in", "Food", "Active", "Water", "Sleep")
    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .size(190.dp)
                .semantics { contentDescription = "5-Axis Weekly Habit Radar Chart" }
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val maxR = size.minDimension * 0.42f

            fun pointAt(radiusRatio: Float, index: Int): Offset {
                val angleRad = Math.toRadians((index * 72.0 - 90.0))
                return Offset(
                    x = cx + (cos(angleRad) * maxR * radiusRatio).toFloat(),
                    y = cy + (sin(angleRad) * maxR * radiusRatio).toFloat()
                )
            }

            // Concentric pentagons
            listOf(0.5f, 1.0f).forEach { r ->
                val webPath = Path()
                for (i in 0..4) {
                    val pt = pointAt(r, i)
                    if (i == 0) webPath.moveTo(pt.x, pt.y) else webPath.lineTo(pt.x, pt.y)
                }
                webPath.close()
                drawPath(path = webPath, color = gridColor, style = Stroke(width = 1.dp.toPx()))
            }

            // Axis spokes
            for (i in 0..4) {
                val end = pointAt(1.0f, i)
                drawLine(color = gridColor.copy(alpha = 0.5f), start = Offset(cx, cy), end = end, strokeWidth = 1.dp.toPx())
            }

            // Data polygon
            val dataPath = Path()
            for (i in 0..4) {
                val v = values.getOrElse(i) { 0f }.coerceIn(0.05f, 1f)
                val pt = pointAt(v, i)
                if (i == 0) dataPath.moveTo(pt.x, pt.y) else dataPath.lineTo(pt.x, pt.y)
            }
            dataPath.close()
            drawPath(path = dataPath, color = primaryColor.copy(alpha = 0.32f))
            drawPath(path = dataPath, color = primaryColor, style = Stroke(width = 2.5.dp.toPx()))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            labels.forEachIndexed { idx, name ->
                val pct = (values.getOrElse(idx) { 0f } * 100).roundToInt()
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = "$name $pct%",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ConsistencyHeatmap90Days(
    state: LeanPathState,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val weighDates = state.weighIns.map { it.date }.toSet()

    val scores = (0..90).map { n ->
        val d = MetabolicEngine.dateOffset((n - 90).toLong())
        val s1 = if (weighDates.contains(d)) 1 else 0
        val s2 = if (state.foodByDate[d].orEmpty().isNotEmpty()) 1 else 0
        val s3 = if ((state.waterByDateMl[d] ?: 0) > 0) 1 else 0
        val s4 = if (MetabolicEngine.computeBurnedKcal(state, d) > 0) 1 else 0
        s1 + s2 + s3 + s4
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(125.dp)
            .semantics { contentDescription = "90-Day Consistency Heatmap" }
    ) {
        val cols = 13
        val rows = 7
        val gap = 4.dp.toPx()
        val cellW = (size.width - gap * (cols - 1)) / cols
        val cellH = (size.height - gap * (rows - 1)) / rows
        val cellSize = min(cellW, cellH)
        val totalGridW = cols * cellSize + (cols - 1) * gap
        val startX = (size.width - totalGridW) / 2f

        scores.forEachIndexed { idx, score ->
            val col = idx / 7
            val row = idx % 7
            val x = startX + col * (cellSize + gap)
            val y = row * (cellSize + gap)
            val color = if (score == 0) {
                emptyColor
            } else {
                primaryColor.copy(alpha = (0.25f + score * 0.185f).coerceIn(0f, 1f))
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(x, y),
                size = Size(cellSize, cellSize),
                cornerRadius = CornerRadius(3.dp.toPx())
            )
        }
    }
}

@Composable
fun DeficitWaterfallChart(
    state: LeanPathState,
    tdee: Double,
    modifier: Modifier = Modifier
) {
    val dates = (0..6).map { i -> MetabolicEngine.dateOffset((i - 6).toLong()) }
        .filter { state.foodByDate[it].orEmpty().isNotEmpty() }

    if (dates.size < 2) {
        Text(
            text = "Log food on 2+ days this week to unlock your cumulative deficit waterfall.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    data class WaterfallBar(val date: String, val delta: Double, val start: Double, val end: Double)
    var cumulative = 0.0
    val bars = dates.map { d ->
        val eaten = state.foodByDate[d].orEmpty().sumOf { it.kcal }
        val delta = tdee - eaten
        val s = cumulative
        cumulative += delta
        WaterfallBar(d, delta, s, cumulative)
    }

    val allVals = bars.flatMap { listOf(it.start, it.end) } + 0.0
    val minV = allVals.minOrNull() ?: 0.0
    val maxV = allVals.maxOrNull() ?: 1000.0
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val zeroLineColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .semantics { contentDescription = "Weekly Deficit Waterfall Chart" }
        ) {
            val pad = 10.dp.toPx()
            val plotH = size.height - pad * 2
            val span = (maxV - minV).coerceAtLeast(1.0)
            fun yFor(v: Double): Float = (pad + plotH * (1.0 - (v - minV) / span)).toFloat()

            val zeroY = yFor(0.0)
            drawLine(
                color = zeroLineColor,
                start = Offset(pad, zeroY),
                end = Offset(size.width - pad, zeroY),
                strokeWidth = 1.dp.toPx()
            )

            val slotW = (size.width - pad * 2) / bars.size
            bars.forEachIndexed { idx, bar ->
                val topY = yFor(max(bar.start, bar.end))
                val botY = yFor(min(bar.start, bar.end))
                val barH = max(3.dp.toPx(), abs(botY - topY))
                drawRoundRect(
                    color = if (bar.delta >= 0) primaryColor else errorColor,
                    topLeft = Offset(pad + idx * slotW + 4.dp.toPx(), topY),
                    size = Size(slotW - 8.dp.toPx(), barH),
                    cornerRadius = CornerRadius(4.dp.toPx())
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Cumulative 7d deficit: ${cumulative.roundToInt()} kcal ≈ ${String.format(Locale.US, "%.2f", cumulative / 7700.0)} kg theoretical fat loss.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CorrelationScatterChart(
    title: String,
    xAxisLabel: String,
    points: List<Pair<Double, Double>>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val zeroColor = MaterialTheme.colorScheme.outlineVariant

    Column(modifier = modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(6.dp))
        if (points.size < 6) {
            Text(
                text = "Needs 6+ matching consecutive days of logs to compute correlation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            return
        }

        val r = MetabolicEngine.computePearsonCorrelation(points) ?: 0.0
        val xs = points.map { it.first }
        val ys = points.map { it.second }
        val minX = xs.minOrNull() ?: 0.0
        val maxX = xs.maxOrNull() ?: 1.0
        val minY = ys.minOrNull() ?: -1.0
        val maxY = ys.maxOrNull() ?: 1.0

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(115.dp)
                .semantics { contentDescription = "$title Scatter Plot" }
        ) {
            val pad = 12.dp.toPx()
            val plotW = size.width - pad * 2
            val plotH = size.height - pad * 2

            fun xFor(v: Double): Float = (pad + (v - minX) / (maxX - minX).coerceAtLeast(1e-5) * plotW).toFloat()
            fun yFor(v: Double): Float = (pad + (1.0 - (v - minY) / (maxY - minY).coerceAtLeast(1e-5)) * plotH).toFloat()

            if (minY < 0.0 && maxY > 0.0) {
                val y0 = yFor(0.0)
                drawLine(color = zeroColor, start = Offset(pad, y0), end = Offset(size.width - pad, y0), strokeWidth = 1.dp.toPx())
            }

            points.forEach { (x, y) ->
                drawCircle(
                    color = primaryColor.copy(alpha = 0.78f),
                    radius = 4.dp.toPx(),
                    center = Offset(xFor(x), yFor(y))
                )
            }
        }
        val interpretation = when {
            abs(r) < 0.2 -> "no strong linear link"
            r > 0 -> "higher $xAxisLabel tends to precede weight up"
            else -> "higher $xAxisLabel tends to precede weight down"
        }
        Text(
            text = "Pearson r = ${String.format(Locale.US, "%.2f", r)} ($interpretation).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
