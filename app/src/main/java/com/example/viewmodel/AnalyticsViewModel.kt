package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CsvParser
import com.example.data.DataCleaner
import com.example.data.InsightsGenerator
import com.example.data.PdfReportGenerator
import com.example.data.SampleDatasets
import com.example.data.StreamGenerator
import com.example.model.AggType
import com.example.model.ChartConfig
import com.example.model.ChartDataPoint
import com.example.model.ChartType
import com.example.model.CleaningType
import com.example.model.ColumnType
import com.example.model.Dataset
import com.example.model.InsightItem
import com.example.model.StreamRecord
import com.example.model.StreamScenario
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File

class AnalyticsViewModel : ViewModel() {

    // Current Active Dataset
    private val _dataset = MutableStateFlow<Dataset>(
        CsvParser.parse(
            content = SampleDatasets.ecommerceSalesCsv,
            sourceName = "Global_Retail_Sales.csv",
            title = "Global Tech E-Commerce Sales"
        )
    )
    val dataset: StateFlow<Dataset> = _dataset.asStateFlow()

    // Chart Configuration
    private val _chartConfig = MutableStateFlow(
        ChartConfig(
            type = ChartType.BAR,
            xColumn = "Product_Category",
            yColumn = "Revenue_USD",
            aggregation = AggType.SUM,
            title = "Revenue by Product Category"
        )
    )
    val chartConfig: StateFlow<ChartConfig> = _chartConfig.asStateFlow()

    // Computed Chart Data Points
    private val _chartPoints = MutableStateFlow<List<ChartDataPoint>>(emptyList())
    val chartPoints: StateFlow<List<ChartDataPoint>> = _chartPoints.asStateFlow()

    // Auto-computed Insights
    private val _insights = MutableStateFlow<List<InsightItem>>(emptyList())
    val insights: StateFlow<List<InsightItem>> = _insights.asStateFlow()

    // Real-Time Streaming State
    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _streamScenario = MutableStateFlow(StreamScenario.ECOMMERCE_TRANSACTIONS)
    val streamScenario: StateFlow<StreamScenario> = _streamScenario.asStateFlow()

    private val _streamBuffer = MutableStateFlow<List<StreamRecord>>(emptyList())
    val streamBuffer: StateFlow<List<StreamRecord>> = _streamBuffer.asStateFlow()

    private val _streamSpeedMs = MutableStateFlow(1000L)
    val streamSpeedMs: StateFlow<Long> = _streamSpeedMs.asStateFlow()

    private var streamingJob: Job? = null

    // PDF Exporting State
    private val _isExportingPdf = MutableStateFlow(false)
    val isExportingPdf: StateFlow<Boolean> = _isExportingPdf.asStateFlow()

    private val _lastExportedFile = MutableStateFlow<File?>(null)
    val lastExportedFile: StateFlow<File?> = _lastExportedFile.asStateFlow()

    // Filter by Category/Dimension
    private val _selectedFilterValue = MutableStateFlow<String?>(null)
    val selectedFilterValue: StateFlow<String?> = _selectedFilterValue.asStateFlow()

    init {
        updateChartPoints()
        refreshInsights()
        preloadStreamBuffer()
    }

    private fun preloadStreamBuffer() {
        val initialPoints = mutableListOf<StreamRecord>()
        for (i in 0 until 12) {
            initialPoints.add(StreamGenerator.generateNextRecord(_streamScenario.value))
        }
        _streamBuffer.value = initialPoints
    }

    // ================= DATA IMPORT & MANAGEMENT =================

    fun loadCsvContent(csvString: String, fileName: String, datasetTitle: String) {
        val parsed = CsvParser.parse(
            content = csvString,
            sourceName = fileName,
            title = datasetTitle.ifBlank { fileName }
        )
        _dataset.value = parsed
        _selectedFilterValue.value = null

        // Auto-configure best chart columns
        val catCol = parsed.columns.firstOrNull { it.type == ColumnType.CATEGORICAL }?.name
            ?: parsed.columns.firstOrNull()?.name ?: ""
        val numCol = parsed.columns.firstOrNull { it.type == ColumnType.NUMERIC }?.name

        _chartConfig.value = ChartConfig(
            type = ChartType.BAR,
            xColumn = catCol,
            yColumn = numCol,
            aggregation = AggType.SUM,
            title = "${numCol ?: "Count"} by $catCol"
        )

        updateChartPoints()
        refreshInsights()
    }

    fun loadSampleDataset(index: Int) {
        when (index) {
            0 -> loadCsvContent(
                SampleDatasets.ecommerceSalesCsv,
                "Global_Retail_Sales.csv",
                "Global Tech E-Commerce Sales"
            )
            1 -> loadCsvContent(
                SampleDatasets.iotFactorySensorsCsv,
                "Factory_Sensors.csv",
                "IoT Smart Factory Telemetry"
            )
            2 -> loadCsvContent(
                SampleDatasets.financialMarketPulseCsv,
                "Financial_Pulse.csv",
                "Crypto & Market Asset Dynamics"
            )
        }
    }

    // ================= DATA CLEANING OPERATIONS =================

    fun cleanRemoveDuplicates() {
        _dataset.update { DataCleaner.removeDuplicates(it) }
        onDataChanged()
    }

    fun cleanDropNulls(colIndex: Int? = null) {
        _dataset.update { DataCleaner.dropNullRows(it, colIndex) }
        onDataChanged()
    }

    fun cleanFillNullsMean(colIndex: Int) {
        _dataset.update { DataCleaner.fillNullsWithMean(it, colIndex) }
        onDataChanged()
    }

    fun cleanFillNullsMedian(colIndex: Int) {
        _dataset.update { DataCleaner.fillNullsWithMedian(it, colIndex) }
        onDataChanged()
    }

    fun cleanFillNullsMode(colIndex: Int) {
        _dataset.update { DataCleaner.fillNullsWithMode(it, colIndex) }
        onDataChanged()
    }

    fun cleanFillNullsCustom(colIndex: Int, customValue: String) {
        _dataset.update { DataCleaner.fillNullsCustom(it, colIndex, customValue) }
        onDataChanged()
    }

    fun cleanRemoveOutliers(colIndex: Int) {
        _dataset.update { DataCleaner.removeOutliersIqr(it, colIndex) }
        onDataChanged()
    }

    fun cleanTrimWhitespace() {
        _dataset.update { DataCleaner.trimWhitespace(it) }
        onDataChanged()
    }

    fun cleanConvertCase(colIndex: Int, type: CleaningType) {
        _dataset.update { DataCleaner.convertCase(it, colIndex, type) }
        onDataChanged()
    }

    fun autoCleanDataset() {
        _dataset.update { DataCleaner.autoClean(it) }
        onDataChanged()
    }

    fun resetDatasetToOriginal() {
        _dataset.update { DataCleaner.resetToOriginal(it) }
        onDataChanged()
    }

    private fun onDataChanged() {
        updateChartPoints()
        refreshInsights()
    }

    // ================= CHART & VISUALIZATION CONFIG =================

    fun setChartType(type: ChartType) {
        _chartConfig.update { it.copy(type = type) }
        updateChartPoints()
    }

    fun setXColumn(colName: String) {
        _chartConfig.update { it.copy(xColumn = colName) }
        updateChartPoints()
    }

    fun setYColumn(colName: String?) {
        _chartConfig.update { it.copy(yColumn = colName) }
        updateChartPoints()
    }

    fun setAggregation(agg: AggType) {
        _chartConfig.update { it.copy(aggregation = agg) }
        updateChartPoints()
    }

    fun setFilterValue(value: String?) {
        _selectedFilterValue.value = value
        updateChartPoints()
    }

    private fun updateChartPoints() {
        val ds = _dataset.value
        val config = _chartConfig.value
        val xCol = ds.columns.firstOrNull { it.name == config.xColumn } ?: ds.columns.firstOrNull()
        val yCol = ds.columns.firstOrNull { it.name == config.yColumn }

        if (xCol == null || ds.rows.isEmpty()) {
            _chartPoints.value = emptyList()
            return
        }

        // Apply active filter if present
        val filteredRows = if (_selectedFilterValue.value != null && xCol.name.isNotEmpty()) {
            ds.rows.filter {
                it.values.getOrElse(xCol.index) { "" } == _selectedFilterValue.value
            }
        } else {
            ds.rows
        }

        val points = mutableListOf<ChartDataPoint>()

        if (yCol == null || config.aggregation == AggType.COUNT) {
            // Count distribution of X column
            val counts = filteredRows.groupingBy {
                it.values.getOrElse(xCol.index) { "Other" }.ifBlank { "N/A" }
            }.eachCount()

            val sorted = counts.toList().sortedByDescending { it.second }.take(config.maxBuckets)
            val total = sorted.sumOf { it.second }.toDouble().coerceAtLeast(1.0)

            sorted.forEach { (cat, count) ->
                points.add(
                    ChartDataPoint(
                        label = cat,
                        value = count.toDouble(),
                        percentage = (count / total).toFloat()
                    )
                )
            }
        } else {
            // Group by X and Aggregate Y
            val grouped = filteredRows.groupBy {
                it.values.getOrElse(xCol.index) { "Other" }.ifBlank { "N/A" }
            }

            val aggValues = grouped.mapValues { (_, rows) ->
                val numVals = rows.mapNotNull {
                    it.values.getOrElse(yCol.index) { "" }.replace("$", "").replace(",", "").toDoubleOrNull()
                }
                if (numVals.isEmpty()) 0.0 else when (config.aggregation) {
                    AggType.SUM -> numVals.sum()
                    AggType.AVG -> numVals.average()
                    AggType.MAX -> numVals.maxOrNull() ?: 0.0
                    AggType.MIN -> numVals.minOrNull() ?: 0.0
                    AggType.COUNT -> numVals.size.toDouble()
                }
            }

            val sorted = aggValues.toList().sortedByDescending { it.second }.take(config.maxBuckets)
            val total = sorted.sumOf { it.second }.coerceAtLeast(0.001)

            sorted.forEach { (cat, value) ->
                points.add(
                    ChartDataPoint(
                        label = cat,
                        value = value,
                        secondaryValue = null,
                        percentage = (value / total).toFloat()
                    )
                )
            }
        }

        _chartPoints.value = points
    }

    private fun refreshInsights() {
        _insights.value = InsightsGenerator.generateInsights(_dataset.value)
    }

    // ================= REAL-TIME STREAMING =================

    fun toggleStreaming() {
        if (_isStreaming.value) {
            stopStreaming()
        } else {
            startStreaming()
        }
    }

    fun startStreaming() {
        _isStreaming.value = true
        streamingJob?.cancel()
        streamingJob = viewModelScope.launch {
            while (isActive && _isStreaming.value) {
                delay(_streamSpeedMs.value)
                val newPoint = StreamGenerator.generateNextRecord(_streamScenario.value)
                _streamBuffer.update { list ->
                    (list + newPoint).takeLast(40)
                }
            }
        }
    }

    fun stopStreaming() {
        _isStreaming.value = false
        streamingJob?.cancel()
        streamingJob = null
    }

    fun setStreamScenario(scenario: StreamScenario) {
        _streamScenario.value = scenario
        clearStreamBuffer()
        preloadStreamBuffer()
    }

    fun setStreamSpeed(speedMs: Long) {
        _streamSpeedMs.value = speedMs
        if (_isStreaming.value) {
            // Restart with new speed
            startStreaming()
        }
    }

    fun clearStreamBuffer() {
        _streamBuffer.value = emptyList()
    }

    // Ingest stream buffer into main dataset as new batch!
    fun ingestStreamToDataset() {
        val buffer = _streamBuffer.value
        if (buffer.isEmpty()) return

        val scenario = _streamScenario.value
        val sb = StringBuilder()
        sb.append("Timestamp,Category,Metric1,Metric2,Status\n")
        buffer.forEach { rec ->
            sb.append("${rec.timestamp},${rec.category},${rec.value1},${rec.value2},${rec.status}\n")
        }

        loadCsvContent(
            csvString = sb.toString(),
            fileName = "LiveStream_${scenario.name}.csv",
            datasetTitle = "Live Stream: ${scenario.displayName}"
        )
    }

    // ================= PDF EXPORT =================

    fun exportPdf(context: Context, onComplete: (File) -> Unit) {
        viewModelScope.launch {
            _isExportingPdf.value = true
            try {
                val result = PdfReportGenerator.generateExecutiveReport(
                    context = context,
                    dataset = _dataset.value,
                    insights = _insights.value
                )
                _lastExportedFile.value = result.file
                onComplete(result.file)
            } finally {
                _isExportingPdf.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopStreaming()
    }
}
