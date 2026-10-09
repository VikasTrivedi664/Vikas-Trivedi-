package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DataThresholding
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ScatterPlot
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.AggType
import com.example.model.ChartType
import com.example.model.ColumnType
import com.example.ui.components.DataHealthScoreCard
import com.example.ui.components.InteractiveBarChart
import com.example.ui.components.InteractiveDonutChart
import com.example.ui.components.InteractiveLineAreaChart
import com.example.ui.components.InteractiveScatterPlot
import com.example.ui.components.KpiMetricCard
import com.example.viewmodel.AnalyticsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: AnalyticsViewModel,
    onNavigateToCleaning: () -> Unit,
    onNavigateToImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dataset by viewModel.dataset.collectAsStateWithLifecycle()
    val chartConfig by viewModel.chartConfig.collectAsStateWithLifecycle()
    val chartPoints by viewModel.chartPoints.collectAsStateWithLifecycle()
    val activeFilter by viewModel.selectedFilterValue.collectAsStateWithLifecycle()

    var showControls by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("dashboard_screen")
    ) {
        // Data Health & Auto-clean Banner
        DataHealthScoreCard(
            profile = dataset.profile,
            onAutoCleanClick = { viewModel.autoCleanDataset() },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("data_health_banner")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // KPI Summary Cards Grid
        val totalMetric = remember(chartPoints) { chartPoints.sumOf { it.value } }
        val avgMetric = remember(chartPoints) {
            if (chartPoints.isNotEmpty()) totalMetric / chartPoints.size else 0.0
        }
        val peakPoint = remember(chartPoints) { chartPoints.maxByOrNull { it.value } }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KpiMetricCard(
                title = "Total Volume",
                value = formatKpi(totalMetric),
                subtitle = "${chartPoints.size} Categories",
                icon = Icons.AutoMirrored.Filled.TrendingUp,
                accentColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_card_total")
            )

            KpiMetricCard(
                title = "Segment Avg",
                value = formatKpi(avgMetric),
                subtitle = "Mean / observation",
                icon = Icons.Default.DataThresholding,
                accentColor = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier
                    .weight(1f)
                    .testTag("kpi_card_avg")
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Main Chart Visualizer Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("chart_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Chart Card Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = chartConfig.title.ifBlank { "Visual Analytics" },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${chartConfig.aggregation.label} of ${chartConfig.yColumn ?: "Records"} by ${chartConfig.xColumn}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = { showControls = !showControls },
                        modifier = Modifier.testTag("toggle_chart_controls_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Chart Settings",
                            tint = if (showControls) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Chart Type Selector Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChartType.entries.forEach { type ->
                        val isSelected = chartConfig.type == type
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.setChartType(type) },
                            label = { Text(type.title, fontSize = 12.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (type) {
                                        ChartType.BAR -> Icons.Default.BarChart
                                        ChartType.LINE -> Icons.Default.ShowChart
                                        ChartType.AREA -> Icons.Default.ShowChart
                                        ChartType.PIE, ChartType.DONUT -> Icons.Default.PieChart
                                        ChartType.SCATTER, ChartType.HISTOGRAM -> Icons.Default.ScatterPlot
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                    }
                }

                // Collapsible Axis / Aggregation Controls
                AnimatedVisibility(visible = showControls) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "DIMENSION & METRIC CONTROLS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // X Axis Dropdown
                            var xExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = xExpanded,
                                onExpandedChange = { xExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = chartConfig.xColumn,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Dimension (X-Axis)") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = xExpanded) },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = xExpanded,
                                    onDismissRequest = { xExpanded = false }
                                ) {
                                    dataset.columns.forEach { col ->
                                        DropdownMenuItem(
                                            text = { Text("${col.name} (${col.type.name})") },
                                            onClick = {
                                                viewModel.setXColumn(col.name)
                                                xExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Y Axis Dropdown
                            var yExpanded by remember { mutableStateOf(false) }
                            ExposedDropdownMenuBox(
                                expanded = yExpanded,
                                onExpandedChange = { yExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = chartConfig.yColumn ?: "Count of Records",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Metric (Y-Axis)") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yExpanded) },
                                    modifier = Modifier
                                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = yExpanded,
                                    onDismissRequest = { yExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Count of Records (Row Frequency)") },
                                        onClick = {
                                            viewModel.setYColumn(null)
                                            yExpanded = false
                                        }
                                    )
                                    dataset.columns.filter { it.type == ColumnType.NUMERIC }.forEach { col ->
                                        DropdownMenuItem(
                                            text = { Text(col.name) },
                                            onClick = {
                                                viewModel.setYColumn(col.name)
                                                yExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Aggregation Pills
                            Text(
                                text = "Aggregation Mode",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AggType.entries.forEach { agg ->
                                    FilterChip(
                                        selected = chartConfig.aggregation == agg,
                                        onClick = { viewModel.setAggregation(agg) },
                                        label = { Text(agg.label, fontSize = 11.sp) }
                                    )
                                }
                            }
                        }
                    }
                }

                // Render Selected Chart
                when (chartConfig.type) {
                    ChartType.BAR -> InteractiveBarChart(
                        data = chartPoints,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ChartType.LINE, ChartType.AREA -> InteractiveLineAreaChart(
                        data = chartPoints,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ChartType.PIE, ChartType.DONUT -> InteractiveDonutChart(
                        data = chartPoints,
                        centerTitle = chartConfig.yColumn ?: "Total",
                        modifier = Modifier.fillMaxWidth()
                    )
                    ChartType.SCATTER, ChartType.HISTOGRAM -> InteractiveScatterPlot(
                        data = chartPoints,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Interactive Category Filter Chips
        if (chartPoints.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("filter_chips_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Slice by Dimension",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (activeFilter != null) {
                            Surface(
                                onClick = { viewModel.setFilterValue(null) },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "Clear Filter",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        chartPoints.take(8).forEach { pt ->
                            val isSelected = activeFilter == pt.label
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    viewModel.setFilterValue(if (isSelected) null else pt.label)
                                },
                                label = { Text(pt.label, fontSize = 11.sp) }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom bar
    }
}

private fun formatKpi(value: Double): String {
    return when {
        value >= 1_000_000 -> String.format(Locale.US, "$%.2fM", value / 1_000_000)
        value >= 1_000 -> String.format(Locale.US, "$%.1fK", value / 1_000)
        else -> String.format(Locale.US, "$%.1f", value)
    }
}
