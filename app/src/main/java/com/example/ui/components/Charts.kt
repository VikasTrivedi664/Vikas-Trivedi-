package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ChartDataPoint
import com.example.ui.theme.ChartPalette
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun InteractiveBarChart(
    data: List<ChartDataPoint>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    onPointSelected: (ChartDataPoint?) -> Unit = {}
) {
    if (data.isEmpty()) {
        EmptyChartPlaceholder()
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var animProgress by remember { mutableStateOf(0f) }
    val animatedFraction by animateFloatAsState(
        targetValue = animProgress,
        animationSpec = tween(durationMillis = 650),
        label = "bar_anim"
    )

    LaunchedEffect(data) {
        animProgress = 0f
        animProgress = 1f
    }

    val maxVal = remember(data) { (data.maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(1.0) }
    val totalSum = remember(data) { data.sumOf { it.value }.coerceAtLeast(0.001) }

    Column(modifier = modifier) {
        // Tooltip Banner if selected
        if (selectedIndex != null && selectedIndex!! in data.indices) {
            val point = data[selectedIndex!!]
            val pct = (point.value / totalSum * 100).toInt()
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(barColor, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = point.label,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${formatNum(point.value)} ($pct%)",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            val activeColor = MaterialTheme.colorScheme.primary
            val highlightColor = MaterialTheme.colorScheme.tertiary

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .pointerInput(data) {
                        detectTapGestures { offset ->
                            val slotWidth = size.width / data.size
                            val clickedIdx = (offset.x / slotWidth).toInt().coerceIn(0, data.size - 1)
                            selectedIndex = if (selectedIndex == clickedIdx) null else clickedIdx
                            onPointSelected(selectedIndex?.let { data[it] })
                        }
                    }
            ) {
                val w = size.width
                val h = size.height - 24.dp.toPx() // Reserve bottom for labels
                val count = data.size
                val slotWidth = w / count
                val barWidth = slotWidth * 0.6f

                // Draw Horizontal Gridlines (4 lines)
                val gridLines = 3
                for (g in 0..gridLines) {
                    val gy = h * (g.toFloat() / gridLines)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, gy),
                        end = Offset(w, gy),
                        strokeWidth = 1f
                    )
                }

                // Draw Bars
                data.forEachIndexed { i, pt ->
                    val isSelected = selectedIndex == i
                    val barH = (pt.value / maxVal * h * animatedFraction).toFloat().coerceAtLeast(3.dp.toPx())
                    val barX = i * slotWidth + (slotWidth - barWidth) / 2f
                    val barY = h - barH

                    val brush = Brush.verticalGradient(
                        colors = if (isSelected) {
                            listOf(highlightColor, highlightColor.copy(alpha = 0.7f))
                        } else {
                            listOf(activeColor, activeColor.copy(alpha = 0.6f))
                        },
                        startY = barY,
                        endY = h
                    )

                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(barX, barY),
                        size = Size(barWidth, barH),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )

                    // Draw base line indicator
                    drawLine(
                        color = if (isSelected) highlightColor else gridColor,
                        start = Offset(i * slotWidth, h),
                        end = Offset((i + 1) * slotWidth, h),
                        strokeWidth = if (isSelected) 3f else 1f
                    )
                }
            }
        }

        // X-Axis Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val step = if (data.size > 7) 2 else 1
            data.forEachIndexed { index, point ->
                if (index % step == 0 || index == data.lastIndex) {
                    Text(
                        text = if (point.label.length > 8) point.label.take(7) + ".." else point.label,
                        fontSize = 10.sp,
                        color = if (selectedIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (selectedIndex == index) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun InteractiveLineAreaChart(
    data: List<ChartDataPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    onPointSelected: (ChartDataPoint?) -> Unit = {}
) {
    if (data.isEmpty()) {
        EmptyChartPlaceholder()
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var animProgress by remember { mutableStateOf(0f) }
    val animatedFraction by animateFloatAsState(
        targetValue = animProgress,
        animationSpec = tween(durationMillis = 700),
        label = "line_anim"
    )

    LaunchedEffect(data) {
        animProgress = 0f
        animProgress = 1f
    }

    val maxVal = remember(data) { (data.maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(1.0) }
    val minVal = remember(data) { (data.minOfOrNull { it.value } ?: 0.0).coerceAtLeast(0.0) }
    val range = (maxVal - minVal).coerceAtLeast(1.0)

    Column(modifier = modifier) {
        // Scrubber / Value banner
        if (selectedIndex != null && selectedIndex!! in data.indices) {
            val point = data[selectedIndex!!]
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = point.label,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatNum(point.value),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = lineColor
                    )
                }
            }
        }

        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .pointerInput(data) {
                        detectTapGestures { offset ->
                            val slotWidth = size.width / (data.size - 1).coerceAtLeast(1)
                            val clickedIdx = (offset.x / slotWidth + 0.5f).toInt().coerceIn(0, data.size - 1)
                            selectedIndex = if (selectedIndex == clickedIdx) null else clickedIdx
                            onPointSelected(selectedIndex?.let { data[it] })
                        }
                    }
            ) {
                val w = size.width
                val h = size.height - 24.dp.toPx()
                val count = data.size
                val stepX = if (count > 1) w / (count - 1) else w

                // Draw Grid
                for (g in 0..3) {
                    val gy = h * (g.toFloat() / 3)
                    drawLine(gridColor, Offset(0f, gy), Offset(w, gy), 1f)
                }

                val points = data.mapIndexed { idx, pt ->
                    val x = idx * stepX
                    val normY = (pt.value - minVal) / range
                    val y = h - (normY * h * animatedFraction).toFloat()
                    Offset(x, y)
                }

                // Area Fill Path
                val areaPath = Path().apply {
                    if (points.isNotEmpty()) {
                        moveTo(points.first().x, h)
                        points.forEach { lineTo(it.x, it.y) }
                        lineTo(points.last().x, h)
                        close()
                    }
                }

                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(lineColor.copy(alpha = 0.45f), lineColor.copy(alpha = 0.02f)),
                        startY = 0f,
                        endY = h
                    )
                )

                // Line Stroke Path
                val linePath = Path().apply {
                    if (points.isNotEmpty()) {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val prev = points[i - 1]
                            val curr = points[i]
                            val cx = (prev.x + curr.x) / 2f
                            cubicTo(cx, prev.y, cx, curr.y, curr.x, curr.y)
                        }
                    }
                }

                drawPath(
                    path = linePath,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Points & Selected Indicator
                points.forEachIndexed { i, pt ->
                    val isSelected = selectedIndex == i
                    if (isSelected) {
                        drawLine(
                            color = lineColor.copy(alpha = 0.6f),
                            start = Offset(pt.x, 0f),
                            end = Offset(pt.x, h),
                            strokeWidth = 2f
                        )
                        drawCircle(color = lineColor, radius = 7.dp.toPx(), center = pt)
                        drawCircle(color = Color.White, radius = 3.dp.toPx(), center = pt)
                    } else if (count <= 15) {
                        drawCircle(color = lineColor, radius = 3.5.dp.toPx(), center = pt)
                    }
                }
            }
        }

        // Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = data.firstOrNull()?.label ?: "",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (data.size > 2) {
                Text(
                    text = data[data.size / 2].label,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = data.lastOrNull()?.label ?: "",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InteractiveDonutChart(
    data: List<ChartDataPoint>,
    modifier: Modifier = Modifier,
    centerTitle: String = "Total",
    onSliceSelected: (ChartDataPoint?) -> Unit = {}
) {
    if (data.isEmpty()) {
        EmptyChartPlaceholder()
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val totalSum = remember(data) { data.sumOf { it.value }.coerceAtLeast(0.001) }

    val angles = remember(data) {
        var start = -90f
        data.map { pt ->
            val sweep = (pt.value / totalSum * 360f).toFloat()
            val slice = Triple(start, sweep, pt)
            start += sweep
            slice
        }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(184.dp)
                    .pointerInput(data) {
                        detectTapGestures { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            var angle = Math.toDegrees(kotlin.math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            if (angle < -90f) angle += 360f
                            // Angle is relative to -90f
                            val hit = angles.indexOfFirst { (start, sweep, _) ->
                                val end = start + sweep
                                angle in start..end || (angle + 360f in start..end)
                            }
                            selectedIndex = if (selectedIndex == hit) null else (if (hit >= 0) hit else null)
                            onSliceSelected(selectedIndex?.let { data[it] })
                        }
                    }
            ) {
                val strokeW = 28.dp.toPx()
                val radius = (size.minDimension - strokeW) / 2f
                val topLeft = Offset(strokeW / 2f, strokeW / 2f)
                val arcSize = Size(radius * 2, radius * 2)

                angles.forEachIndexed { i, (start, sweep, _) ->
                    val isSelected = selectedIndex == i
                    val color = ChartPalette[i % ChartPalette.size]

                    drawArc(
                        color = if (isSelected) color else color.copy(alpha = 0.85f),
                        startAngle = start + 1.5f,
                        sweepAngle = (sweep - 3f).coerceAtLeast(1f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(
                            width = if (isSelected) strokeW * 1.25f else strokeW,
                            cap = StrokeCap.Round
                        )
                    )
                }
            }

            // Central KPI Text inside Donut Hole
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (selectedIndex != null && selectedIndex!! in data.indices) {
                    val sel = data[selectedIndex!!]
                    val pct = (sel.value / totalSum * 100).toInt()
                    Text(
                        text = sel.label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$pct%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = ChartPalette[selectedIndex!! % ChartPalette.size]
                    )
                    Text(
                        text = formatNum(sel.value),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        text = centerTitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatNum(totalSum),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${data.size} Segments",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Legend Chips
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            data.forEachIndexed { i, point ->
                val isSelected = selectedIndex == i
                val color = ChartPalette[i % ChartPalette.size]
                Surface(
                    onClick = {
                        selectedIndex = if (isSelected) null else i
                        onSliceSelected(selectedIndex?.let { data[it] })
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) color.copy(alpha = 0.2f) else Color.Transparent,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(color, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = point.label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InteractiveScatterPlot(
    data: List<ChartDataPoint>,
    modifier: Modifier = Modifier,
    pointColor: Color = MaterialTheme.colorScheme.primary
) {
    if (data.isEmpty()) {
        EmptyChartPlaceholder()
        return
    }

    val maxVal = remember(data) { (data.maxOfOrNull { it.value } ?: 1.0).coerceAtLeast(1.0) }
    val maxSec = remember(data) { (data.maxOfOrNull { it.secondaryValue ?: it.value } ?: 1.0).coerceAtLeast(1.0) }

    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
            val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)

            Canvas(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                val w = size.width
                val h = size.height

                // Grid
                for (g in 0..3) {
                    val gy = h * (g.toFloat() / 3)
                    val gx = w * (g.toFloat() / 3)
                    drawLine(gridColor, Offset(0f, gy), Offset(w, gy), 1f)
                    drawLine(gridColor, Offset(gx, 0f), Offset(gx, h), 1f)
                }

                // Plot dots
                data.forEachIndexed { i, pt ->
                    val secVal = pt.secondaryValue ?: (i.toDouble() / data.size * maxSec)
                    val px = ((secVal / maxSec) * w).toFloat().coerceIn(10f, w - 10f)
                    val py = (h - (pt.value / maxVal) * h).toFloat().coerceIn(10f, h - 10f)

                    drawCircle(
                        color = pointColor.copy(alpha = 0.35f),
                        radius = 8.dp.toPx(),
                        center = Offset(px, py)
                    )
                    drawCircle(
                        color = pointColor,
                        radius = 4.dp.toPx(),
                        center = Offset(px, py)
                    )
                }

                // Trend line
                drawLine(
                    color = Color(0xFF10B981).copy(alpha = 0.7f),
                    start = Offset(10f, h * 0.85f),
                    end = Offset(w - 10f, h * 0.15f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
fun EmptyChartPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "No numeric data points available to visualize",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatNum(value: Double): String {
    return when {
        value >= 1_000_000 -> String.format(Locale.US, "%.2fM", value / 1_000_000)
        value >= 1_000 -> String.format(Locale.US, "%.1fK", value / 1_000)
        else -> String.format(Locale.US, "%.1f", value)
    }
}
