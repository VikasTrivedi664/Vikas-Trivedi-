package com.example.data

import com.example.model.ColumnType
import com.example.model.Dataset
import com.example.model.InsightItem
import com.example.model.InsightSeverity
import java.util.UUID
import kotlin.math.sqrt

object InsightsGenerator {

    fun generateInsights(dataset: Dataset): List<InsightItem> {
        val insights = mutableListOf<InsightItem>()
        val cols = dataset.columns
        val rows = dataset.rows

        if (rows.isEmpty() || cols.isEmpty()) {
            return listOf(
                InsightItem(
                    id = UUID.randomUUID().toString(),
                    title = "No Data Available",
                    description = "Import or load a dataset to view statistical insights.",
                    severity = InsightSeverity.INFO,
                    metricLabel = "Status",
                    metricValue = "Empty",
                    category = "Overview"
                )
            )
        }

        // 1. Data Health Insight
        val health = dataset.profile.qualityScore
        val healthSeverity = when {
            health >= 90 -> InsightSeverity.POSITIVE
            health >= 70 -> InsightSeverity.INFO
            else -> InsightSeverity.WARNING
        }
        val cleanStepsCount = dataset.cleaningHistory.size
        insights.add(
            InsightItem(
                id = UUID.randomUUID().toString(),
                title = if (cleanStepsCount > 0) "Data Cleansed & Optimized" else "Data Quality Health",
                description = "Dataset scored $health% completeness with ${dataset.rows.size} validated rows across ${cols.size} attributes. $cleanStepsCount transformation step(s) applied.",
                severity = healthSeverity,
                metricLabel = "Quality Score",
                metricValue = "$health%",
                category = "Data Health"
            )
        )

        // 2. Numeric Columns KPI Insights
        val numericCols = cols.filter { it.type == ColumnType.NUMERIC && it.meanValue != null }
        if (numericCols.isNotEmpty()) {
            val primaryNumCol = numericCols.maxByOrNull { it.meanValue ?: 0.0 } ?: numericCols.first()
            val totalSum = rows.sumOf {
                it.values.getOrElse(primaryNumCol.index) { "" }
                    .replace("$", "").replace(",", "").toDoubleOrNull() ?: 0.0
            }
            val formattedSum = formatCompactNumber(totalSum)
            val meanFormatted = formatCompactNumber(primaryNumCol.meanValue ?: 0.0)

            insights.add(
                InsightItem(
                    id = UUID.randomUUID().toString(),
                    title = "Primary Volume: ${primaryNumCol.name}",
                    description = "Total cumulative sum reached $formattedSum with an average of $meanFormatted per recorded observation (StdDev: ${String.format("%.1f", primaryNumCol.stdDev ?: 0.0)}).",
                    severity = InsightSeverity.POSITIVE,
                    metricLabel = "Total Sum",
                    metricValue = formattedSum,
                    category = "Distribution"
                )
            )
        }

        // 3. Categorical Concentration Insight (Pareto principle / Top category)
        val catCols = cols.filter { it.type == ColumnType.CATEGORICAL && it.uniqueValuesCount in 2..20 }
        if (catCols.isNotEmpty() && numericCols.isNotEmpty()) {
            val catCol = catCols.first()
            val numCol = numericCols.first()

            val grouped = rows.groupBy { it.values.getOrElse(catCol.index) { "Other" }.ifBlank { "Other" } }
                .mapValues { (_, rList) ->
                    rList.sumOf { r ->
                        r.values.getOrElse(numCol.index) { "" }
                            .replace("$", "").replace(",", "").toDoubleOrNull() ?: 0.0
                    }
                }

            val totalVolume = grouped.values.sum()
            if (totalVolume > 0) {
                val topEntry = grouped.maxByOrNull { it.value }
                if (topEntry != null) {
                    val sharePct = (topEntry.value / totalVolume * 100.0).toInt()
                    insights.add(
                        InsightItem(
                            id = UUID.randomUUID().toString(),
                            title = "Leader: ${topEntry.key}",
                            description = "'${topEntry.key}' accounts for $sharePct% of aggregate '${numCol.name}' ($${formatCompactNumber(topEntry.value)}), leading across all ${catCol.name} segments.",
                            severity = InsightSeverity.INFO,
                            metricLabel = "Segment Share",
                            metricValue = "$sharePct%",
                            category = "Categorical"
                        )
                    )
                }
            }
        }

        // 4. Trend & Peak Analysis
        if (numericCols.size >= 2) {
            val colA = numericCols[0]
            val colB = numericCols[1]
            val correlation = calculateCorrelation(rows, colA.index, colB.index)

            if (!correlation.isNaN() && kotlin.math.abs(correlation) > 0.3) {
                val strength = if (kotlin.math.abs(correlation) > 0.7) "strong" else "moderate"
                val direction = if (correlation > 0) "positive" else "inverse"
                insights.add(
                    InsightItem(
                        id = UUID.randomUUID().toString(),
                        title = "Correlation: ${colA.name} vs ${colB.name}",
                        description = "Detected $strength $direction statistical correlation (r = ${String.format("%.2f", correlation)}). Movements in '${colA.name}' closely track '${colB.name}'.",
                        severity = if (correlation > 0) InsightSeverity.POSITIVE else InsightSeverity.INFO,
                        metricLabel = "Pearson r",
                        metricValue = String.format("%.2f", correlation),
                        category = "Correlation"
                    )
                )
            }
        }

        // 5. Outliers / Anomalies detection
        for (numCol in numericCols.take(2)) {
            val values = rows.mapNotNull {
                it.values.getOrElse(numCol.index) { "" }.replace("$", "").replace(",", "").toDoubleOrNull()
            }
            if (values.size >= 10 && numCol.meanValue != null && numCol.stdDev != null && numCol.stdDev > 0) {
                val upper2Sigma = numCol.meanValue + 2 * numCol.stdDev
                val anomalies = values.count { it > upper2Sigma }
                if (anomalies > 0) {
                    insights.add(
                        InsightItem(
                            id = UUID.randomUUID().toString(),
                            title = "High Anomalies in ${numCol.name}",
                            description = "Found $anomalies data point(s) exceeding +2σ baseline ($${formatCompactNumber(upper2Sigma)}). Flagged for monitoring or threshold inspection.",
                            severity = InsightSeverity.WARNING,
                            metricLabel = "Anomalies",
                            metricValue = "$anomalies detected",
                            category = "Anomalies"
                        )
                    )
                    break
                }
            }
        }

        return insights
    }

    private fun calculateCorrelation(rows: List<com.example.model.DataRow>, idxA: Int, idxB: Int): Double {
        val pairs = rows.mapNotNull { r ->
            val vA = r.values.getOrElse(idxA) { "" }.replace("$", "").replace(",", "").toDoubleOrNull()
            val vB = r.values.getOrElse(idxB) { "" }.replace("$", "").replace(",", "").toDoubleOrNull()
            if (vA != null && vB != null) vA to vB else null
        }
        if (pairs.size < 3) return Double.NaN

        val meanA = pairs.map { it.first }.average()
        val meanB = pairs.map { it.second }.average()

        var num = 0.0
        var denA = 0.0
        var denB = 0.0

        for ((a, b) in pairs) {
            val diffA = a - meanA
            val diffB = b - meanB
            num += diffA * diffB
            denA += diffA * diffA
            denB += diffB * diffB
        }

        val denom = sqrt(denA * denB)
        return if (denom == 0.0) Double.NaN else num / denom
    }

    private fun formatCompactNumber(value: Double): String {
        return when {
            value >= 1_000_000 -> String.format("%.2fM", value / 1_000_000)
            value >= 1_000 -> String.format("%.1fK", value / 1_000)
            else -> String.format("%.2f", value)
        }
    }
}
