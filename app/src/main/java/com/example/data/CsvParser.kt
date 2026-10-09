package com.example.data

import com.example.model.ColumnType
import com.example.model.DataColumn
import com.example.model.DataProfile
import com.example.model.DataRow
import com.example.model.Dataset
import java.util.UUID
import kotlin.math.sqrt

object CsvParser {

    fun parse(
        content: String,
        sourceName: String = "Uploaded_Data.csv",
        title: String = "Dataset Analysis"
    ): Dataset {
        val lines = content.lineSequence()
            .map { it.trimEnd('\r', '\n') }
            .filter { it.isNotBlank() }
            .toList()

        if (lines.isEmpty()) {
            return emptyDataset(sourceName, title)
        }

        // Auto-detect delimiter (, or ; or \t)
        val delimiter = detectDelimiter(lines.take(5))

        val parsedLines = lines.map { parseCsvLine(it, delimiter) }
        val header = parsedLines.first().map { it.trim() }
        val dataRowsRaw = parsedLines.drop(1)

        val colCount = header.size
        val dataRows = dataRowsRaw.mapIndexed { idx, rowList ->
            // Pad or trim to header size
            val padded = if (rowList.size < colCount) {
                rowList + List(colCount - rowList.size) { "" }
            } else {
                rowList.take(colCount)
            }
            DataRow(id = idx, values = padded.map { it.trim() })
        }

        val columns = inferColumns(header, dataRows)
        val profile = calculateProfile(columns, dataRows)

        return Dataset(
            id = UUID.randomUUID().toString(),
            title = title,
            sourceName = sourceName,
            columns = columns,
            rawRows = dataRows,
            rows = dataRows,
            cleaningHistory = emptyList(),
            profile = profile
        )
    }

    private fun detectDelimiter(sampleLines: List<String>): Char {
        val delimiters = listOf(',', ';', '\t', '|')
        val counts = delimiters.associateWith { delim ->
            sampleLines.sumOf { line -> line.count { it == delim } }
        }
        return counts.maxByOrNull { it.value }?.key ?: ','
    }

    private fun parseCsvLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                    sb.append('"')
                    i++ // Skip escaped quote
                } else {
                    inQuotes = !inQuotes
                }
            } else if (c == delimiter && !inQuotes) {
                result.add(sb.toString())
                sb.clear()
            } else {
                sb.append(c)
            }
            i++
        }
        result.add(sb.toString())
        return result
    }

    fun inferColumns(headers: List<String>, rows: List<DataRow>): List<DataColumn> {
        return headers.mapIndexed { colIdx, colName ->
            val nonNullValues = rows.map { it.values.getOrElse(colIdx) { "" }.trim() }
                .filter { it.isNotEmpty() && !it.equals("null", ignoreCase = true) && !it.equals("nan", ignoreCase = true) }

            val totalNonNull = nonNullValues.size
            val nullCount = rows.size - totalNonNull

            // Check if numeric
            val numericValues = nonNullValues.mapNotNull { it.replace("$", "").replace(",", "").toDoubleOrNull() }
            val isNumeric = totalNonNull > 0 && (numericValues.size.toDouble() / totalNonNull) >= 0.85

            // Check if boolean
            val isBoolean = !isNumeric && totalNonNull > 0 && nonNullValues.all {
                it.equals("true", ignoreCase = true) || it.equals("false", ignoreCase = true) ||
                        it.equals("yes", ignoreCase = true) || it.equals("no", ignoreCase = true)
            }

            // Check if date (simple regex check for YYYY-MM-DD or MM/DD/YYYY)
            val dateRegex = Regex("""^\d{4}[-/.]\d{1,2}[-/.]\d{1,2}.*|^\d{1,2}[-/.]\d{1,2}[-/.]\d{2,4}.*""")
            val isDate = !isNumeric && !isBoolean && totalNonNull > 0 && nonNullValues.count { it.matches(dateRegex) } >= totalNonNull * 0.7

            val type = when {
                isNumeric -> ColumnType.NUMERIC
                isBoolean -> ColumnType.BOOLEAN
                isDate -> ColumnType.DATE
                else -> ColumnType.CATEGORICAL
            }

            val uniqueCount = nonNullValues.distinct().size
            val sampleValues = nonNullValues.distinct().take(4)

            val minVal = if (isNumeric && numericValues.isNotEmpty()) numericValues.minOrNull() else null
            val maxVal = if (isNumeric && numericValues.isNotEmpty()) numericValues.maxOrNull() else null
            val meanVal = if (isNumeric && numericValues.isNotEmpty()) numericValues.average() else null
            val medianVal = if (isNumeric && numericValues.isNotEmpty()) {
                val sorted = numericValues.sorted()
                if (sorted.size % 2 == 1) sorted[sorted.size / 2] else (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0
            } else null

            val stdDev = if (isNumeric && numericValues.size > 1 && meanVal != null) {
                val variance = numericValues.sumOf { (it - meanVal) * (it - meanVal) } / (numericValues.size - 1)
                sqrt(variance)
            } else null

            DataColumn(
                name = colName.ifBlank { "Column_${colIdx + 1}" },
                index = colIdx,
                type = type,
                uniqueValuesCount = uniqueCount,
                nullCount = nullCount,
                minValue = minVal,
                maxValue = maxVal,
                meanValue = meanVal,
                medianValue = medianVal,
                stdDev = stdDev,
                sampleValues = sampleValues
            )
        }
    }

    fun calculateProfile(columns: List<DataColumn>, rows: List<DataRow>): DataProfile {
        val totalRows = rows.size
        val totalCols = columns.size
        val totalCells = totalRows * totalCols

        // Duplicate rows check
        val distinctRowCount = rows.map { it.values.joinToString("|||") }.distinct().size
        val duplicateCount = totalRows - distinctRowCount

        // Missing cells count
        val missingCount = columns.sumOf { it.nullCount }

        // Quality score: deduction for missing values and duplicates
        val missingRate = if (totalCells > 0) missingCount.toDouble() / totalCells else 0.0
        val duplicateRate = if (totalRows > 0) duplicateCount.toDouble() / totalRows else 0.0

        val score = (100.0 - (missingRate * 60.0) - (duplicateRate * 40.0)).coerceIn(0.0, 100.0).toInt()

        val approxBytes = rows.sumOf { row -> row.values.sumOf { it.length } * 2 }
        val memoryKb = approxBytes / 1024.0

        val numCols = columns.count { it.type == ColumnType.NUMERIC }
        val catCols = columns.count { it.type != ColumnType.NUMERIC }

        return DataProfile(
            totalRows = totalRows,
            totalColumns = totalCols,
            duplicateRowCount = duplicateCount,
            missingCellsCount = missingCount,
            qualityScore = score,
            memoryFootprintKb = memoryKb,
            numericColumnsCount = numCols,
            categoricalColumnsCount = catCols
        )
    }

    private fun emptyDataset(sourceName: String, title: String): Dataset {
        return Dataset(
            id = UUID.randomUUID().toString(),
            title = title,
            sourceName = sourceName,
            columns = emptyList(),
            rawRows = emptyList(),
            rows = emptyList(),
            cleaningHistory = emptyList(),
            profile = DataProfile(0, 0, 0, 0, 100, 0.0, 0, 0)
        )
    }
}
