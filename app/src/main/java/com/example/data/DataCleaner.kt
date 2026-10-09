package com.example.data

import com.example.model.CleaningStep
import com.example.model.CleaningType
import com.example.model.ColumnType
import com.example.model.DataRow
import com.example.model.Dataset
import java.util.UUID

object DataCleaner {

    fun removeDuplicates(dataset: Dataset): Dataset {
        val seen = mutableSetOf<String>()
        val distinctRows = mutableListOf<DataRow>()

        for (row in dataset.rows) {
            val key = row.values.joinToString("|||")
            if (seen.add(key)) {
                distinctRows.add(row)
            }
        }

        val rowsRemoved = dataset.rows.size - distinctRows.size
        val step = CleaningStep(
            id = UUID.randomUUID().toString(),
            type = CleaningType.REMOVE_DUPLICATES,
            description = "Removed $rowsRemoved duplicate row(s)",
            rowsAffected = rowsRemoved
        )

        return refreshDataset(dataset, distinctRows, dataset.cleaningHistory + step)
    }

    fun dropNullRows(dataset: Dataset, colIndex: Int? = null): Dataset {
        val filteredRows = dataset.rows.filter { row ->
            if (colIndex != null) {
                val value = row.values.getOrElse(colIndex) { "" }.trim()
                value.isNotEmpty() && !value.equals("null", true) && !value.equals("nan", true)
            } else {
                row.values.none { it.isBlank() || it.equals("null", true) || it.equals("nan", true) }
            }
        }

        val rowsAffected = dataset.rows.size - filteredRows.size
        val colName = colIndex?.let { dataset.columns.getOrNull(it)?.name }
        val desc = if (colName != null) {
            "Dropped $rowsAffected row(s) with missing '$colName'"
        } else {
            "Dropped $rowsAffected row(s) containing any missing values"
        }

        val step = CleaningStep(
            id = UUID.randomUUID().toString(),
            type = CleaningType.DROP_NULL_ROWS,
            targetColumn = colName,
            description = desc,
            rowsAffected = rowsAffected
        )

        return refreshDataset(dataset, filteredRows, dataset.cleaningHistory + step)
    }

    fun fillNullsWithMean(dataset: Dataset, colIndex: Int): Dataset {
        val column = dataset.columns.getOrNull(colIndex) ?: return dataset
        val meanVal = column.meanValue ?: return dataset
        val formattedMean = String.format("%.2f", meanVal)

        var affected = 0
        val updatedRows = dataset.rows.map { row ->
            val v = row.values.getOrElse(colIndex) { "" }.trim()
            if (v.isEmpty() || v.equals("null", true) || v.equals("nan", true)) {
                affected++
                val newVals = row.values.toMutableList()
                while (newVals.size <= colIndex) newVals.add("")
                newVals[colIndex] = formattedMean
                row.copy(values = newVals)
            } else row
        }

        val step = CleaningStep(
            id = UUID.randomUUID().toString(),
            type = CleaningType.FILL_NULLS_MEAN,
            targetColumn = column.name,
            param = formattedMean,
            description = "Imputed $affected missing value(s) in '${column.name}' with mean ($formattedMean)",
            rowsAffected = affected
        )

        return refreshDataset(dataset, updatedRows, dataset.cleaningHistory + step)
    }

    fun fillNullsWithMedian(dataset: Dataset, colIndex: Int): Dataset {
        val column = dataset.columns.getOrNull(colIndex) ?: return dataset
        val medianVal = column.medianValue ?: return dataset
        val formatted = String.format("%.2f", medianVal)

        var affected = 0
        val updatedRows = dataset.rows.map { row ->
            val v = row.values.getOrElse(colIndex) { "" }.trim()
            if (v.isEmpty() || v.equals("null", true) || v.equals("nan", true)) {
                affected++
                val newVals = row.values.toMutableList()
                while (newVals.size <= colIndex) newVals.add("")
                newVals[colIndex] = formatted
                row.copy(values = newVals)
            } else row
        }

        val step = CleaningStep(
            id = UUID.randomUUID().toString(),
            type = CleaningType.FILL_NULLS_MEDIAN,
            targetColumn = column.name,
            param = formatted,
            description = "Imputed $affected missing value(s) in '${column.name}' with median ($formatted)",
            rowsAffected = affected
        )

        return refreshDataset(dataset, updatedRows, dataset.cleaningHistory + step)
    }

    fun fillNullsWithMode(dataset: Dataset, colIndex: Int): Dataset {
        val column = dataset.columns.getOrNull(colIndex) ?: return dataset
        val nonNullVals = dataset.rows.map { it.values.getOrElse(colIndex) { "" }.trim() }
            .filter { it.isNotEmpty() && !it.equals("null", true) && !it.equals("nan", true) }
        val mode = nonNullVals.groupingBy { it }.eachCount().maxByOrNull { it.value }?.key ?: return dataset

        var affected = 0
        val updatedRows = dataset.rows.map { row ->
            val v = row.values.getOrElse(colIndex) { "" }.trim()
            if (v.isEmpty() || v.equals("null", true) || v.equals("nan", true)) {
                affected++
                val newVals = row.values.toMutableList()
                while (newVals.size <= colIndex) newVals.add("")
                newVals[colIndex] = mode
                row.copy(values = newVals)
            } else row
        }

        val step = CleaningStep(
            id = UUID.randomUUID().toString(),
            type = CleaningType.FILL_NULLS_MODE,
            targetColumn = column.name,
            param = mode,
            description = "Imputed $affected missing value(s) in '${column.name}' with mode ('$mode')",
            rowsAffected = affected
        )

        return refreshDataset(dataset, updatedRows, dataset.cleaningHistory + step)
    }

    fun fillNullsCustom(dataset: Dataset, colIndex: Int, customValue: String): Dataset {
        val column = dataset.columns.getOrNull(colIndex) ?: return dataset
        var affected = 0
        val updatedRows = dataset.rows.map { row ->
            val v = row.values.getOrElse(colIndex) { "" }.trim()
            if (v.isEmpty() || v.equals("null", true) || v.equals("nan", true)) {
                affected++
                val newVals = row.values.toMutableList()
                while (newVals.size <= colIndex) newVals.add("")
                newVals[colIndex] = customValue
                row.copy(values = newVals)
            } else row
        }

        val step = CleaningStep(
            id = UUID.randomUUID().toString(),
            type = CleaningType.FILL_NULLS_CUSTOM,
            targetColumn = column.name,
            param = customValue,
            description = "Filled $affected missing value(s) in '${column.name}' with '$customValue'",
            rowsAffected = affected
        )

        return refreshDataset(dataset, updatedRows, dataset.cleaningHistory + step)
    }

    fun removeOutliersIqr(dataset: Dataset, colIndex: Int): Dataset {
        val column = dataset.columns.getOrNull(colIndex) ?: return dataset
        if (column.type != ColumnType.NUMERIC) return dataset

        val valuesWithIdx = dataset.rows.mapIndexedNotNull { idx, r ->
            val d = r.values.getOrElse(colIndex) { "" }.replace("$", "").replace(",", "").toDoubleOrNull()
            if (d != null) idx to d else null
        }

        if (valuesWithIdx.size < 4) return dataset

        val sorted = valuesWithIdx.map { it.second }.sorted()
        val q1 = sorted[(sorted.size * 0.25).toInt()]
        val q3 = sorted[(sorted.size * 0.75).toInt()]
        val iqr = q3 - q1
        val lowerBound = q1 - 1.5 * iqr
        val upperBound = q3 + 1.5 * iqr

        val keptRows = dataset.rows.filter { row ->
            val d = row.values.getOrElse(colIndex) { "" }.replace("$", "").replace(",", "").toDoubleOrNull()
            d == null || (d in lowerBound..upperBound)
        }

        val removedCount = dataset.rows.size - keptRows.size
        val step = CleaningStep(
            id = UUID.randomUUID().toString(),
            type = CleaningType.REMOVE_OUTLIERS_IQR,
            targetColumn = column.name,
            description = "Trimmed $removedCount outlier row(s) in '${column.name}' [Range: ${String.format("%.1f", lowerBound)} to ${String.format("%.1f", upperBound)}]",
            rowsAffected = removedCount
        )

        return refreshDataset(dataset, keptRows, dataset.cleaningHistory + step)
    }

    fun trimWhitespace(dataset: Dataset): Dataset {
        var affectedCount = 0
        val updatedRows = dataset.rows.map { row ->
            var rowModified = false
            val newVals = row.values.map { v ->
                val trimmed = v.trim()
                if (trimmed != v) rowModified = true
                trimmed
            }
            if (rowModified) affectedCount++
            row.copy(values = newVals)
        }

        val step = CleaningStep(
            id = UUID.randomUUID().toString(),
            type = CleaningType.TRIM_WHITESPACE,
            description = "Trimmed whitespace across $affectedCount row(s)",
            rowsAffected = affectedCount
        )

        return refreshDataset(dataset, updatedRows, dataset.cleaningHistory + step)
    }

    fun convertCase(dataset: Dataset, colIndex: Int, type: CleaningType): Dataset {
        val column = dataset.columns.getOrNull(colIndex) ?: return dataset
        var affected = 0

        val updatedRows = dataset.rows.map { row ->
            val v = row.values.getOrElse(colIndex) { "" }
            val newV = when (type) {
                CleaningType.TO_LOWERCASE -> v.lowercase()
                CleaningType.TO_UPPERCASE -> v.uppercase()
                CleaningType.TO_TITLECASE -> v.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
                else -> v
            }
            if (newV != v) affected++
            val newVals = row.values.toMutableList()
            while (newVals.size <= colIndex) newVals.add("")
            newVals[colIndex] = newV
            row.copy(values = newVals)
        }

        val step = CleaningStep(
            id = UUID.randomUUID().toString(),
            type = type,
            targetColumn = column.name,
            description = "Transformed text case in '${column.name}' for $affected row(s)",
            rowsAffected = affected
        )

        return refreshDataset(dataset, updatedRows, dataset.cleaningHistory + step)
    }

    fun autoClean(dataset: Dataset): Dataset {
        // 1. Trim whitespace
        var current = trimWhitespace(dataset)

        // 2. Remove duplicates
        current = removeDuplicates(current)

        // 3. For columns with missing values: impute numeric with median, categorical with mode
        for ((idx, col) in current.columns.withIndex()) {
            if (col.nullCount > 0) {
                current = if (col.type == ColumnType.NUMERIC) {
                    fillNullsWithMedian(current, idx)
                } else {
                    fillNullsWithMode(current, idx)
                }
            }
        }

        return current
    }

    fun resetToOriginal(dataset: Dataset): Dataset {
        val headers = dataset.columns.map { it.name }
        val inferredCols = CsvParser.inferColumns(headers, dataset.rawRows)
        val profile = CsvParser.calculateProfile(inferredCols, dataset.rawRows)

        return dataset.copy(
            rows = dataset.rawRows,
            columns = inferredCols,
            cleaningHistory = emptyList(),
            profile = profile
        )
    }

    private fun refreshDataset(
        dataset: Dataset,
        newRows: List<DataRow>,
        newHistory: List<CleaningStep>
    ): Dataset {
        val headers = dataset.columns.map { it.name }
        val newCols = CsvParser.inferColumns(headers, newRows)
        val newProfile = CsvParser.calculateProfile(newCols, newRows)

        return dataset.copy(
            rows = newRows,
            columns = newCols,
            cleaningHistory = newHistory,
            profile = newProfile
        )
    }
}
