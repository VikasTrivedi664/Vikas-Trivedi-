package com.example.model

enum class ColumnType {
    NUMERIC,
    CATEGORICAL,
    DATE,
    BOOLEAN
}

data class DataColumn(
    val name: String,
    val index: Int,
    val type: ColumnType,
    val uniqueValuesCount: Int = 0,
    val nullCount: Int = 0,
    val minValue: Double? = null,
    val maxValue: Double? = null,
    val meanValue: Double? = null,
    val medianValue: Double? = null,
    val stdDev: Double? = null,
    val sampleValues: List<String> = emptyList()
)

data class DataRow(
    val id: Int,
    val values: List<String>
)

enum class CleaningType(val label: String) {
    REMOVE_DUPLICATES("Remove Duplicate Rows"),
    DROP_NULL_ROWS("Drop Rows with Missing Data"),
    FILL_NULLS_MEAN("Impute Missing with Mean"),
    FILL_NULLS_MEDIAN("Impute Missing with Median"),
    FILL_NULLS_MODE("Impute Missing with Mode (Most Frequent)"),
    FILL_NULLS_CUSTOM("Fill Missing with Custom Value"),
    REMOVE_OUTLIERS_IQR("Trim Statistical Outliers (1.5x IQR)"),
    TRIM_WHITESPACE("Trim Leading/Trailing Whitespace"),
    TO_LOWERCASE("Convert Text to Lowercase"),
    TO_UPPERCASE("Convert Text to Uppercase"),
    TO_TITLECASE("Convert Text to Title Case")
}

data class CleaningStep(
    val id: String,
    val type: CleaningType,
    val targetColumn: String? = null,
    val param: String? = null,
    val description: String,
    val rowsAffected: Int,
    val timestamp: Long = System.currentTimeMillis()
)

data class DataProfile(
    val totalRows: Int,
    val totalColumns: Int,
    val duplicateRowCount: Int,
    val missingCellsCount: Int,
    val qualityScore: Int, // 0 - 100%
    val memoryFootprintKb: Double,
    val numericColumnsCount: Int,
    val categoricalColumnsCount: Int
)

data class Dataset(
    val id: String,
    val title: String,
    val sourceName: String,
    val columns: List<DataColumn>,
    val rawRows: List<DataRow>,
    val rows: List<DataRow>,
    val cleaningHistory: List<CleaningStep> = emptyList(),
    val profile: DataProfile
)

enum class ChartType(val title: String) {
    BAR("Bar Chart"),
    LINE("Line Trend"),
    AREA("Area Chart"),
    PIE("Pie Breakdown"),
    DONUT("Donut Share"),
    SCATTER("Scatter Plot"),
    HISTOGRAM("Frequency Distribution")
}

enum class AggType(val label: String) {
    SUM("Sum"),
    AVG("Average"),
    COUNT("Count"),
    MIN("Min"),
    MAX("Max")
}

data class ChartConfig(
    val type: ChartType = ChartType.BAR,
    val xColumn: String = "",
    val yColumn: String? = null,
    val aggregation: AggType = AggType.SUM,
    val title: String = "",
    val maxBuckets: Int = 10
)

data class ChartDataPoint(
    val label: String,
    val value: Double,
    val secondaryValue: Double? = null,
    val percentage: Float = 0f
)

enum class InsightSeverity {
    INFO,
    POSITIVE,
    WARNING
}

data class InsightItem(
    val id: String,
    val title: String,
    val description: String,
    val severity: InsightSeverity,
    val metricLabel: String,
    val metricValue: String,
    val category: String
)

enum class StreamScenario(val displayName: String, val unit1: String, val unit2: String) {
    ECOMMERCE_TRANSACTIONS("E-Commerce Live Orders", "Revenue ($)", "Cart Items"),
    IOT_FACTORY_SENSORS("IoT Smart Factory Sensors", "Temp (°C)", "Vibration (Hz)"),
    FINANCIAL_MARKET_TICKER("Crypto/Stock Market Feed", "Price ($)", "Volume (K)"),
    SERVER_SYSTEM_HEALTH("Cloud Infrastructure Telemetry", "CPU Load (%)", "Latency (ms)")
}

data class StreamRecord(
    val id: Long,
    val timestamp: String,
    val label: String,
    val value1: Double,
    val value2: Double,
    val status: String,
    val category: String
)
