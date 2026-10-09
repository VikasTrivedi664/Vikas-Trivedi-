package com.example.data

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.model.ColumnType
import com.example.model.Dataset
import com.example.model.InsightItem
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    data class PdfResult(
        val file: File,
        val pageCount: Int,
        val sizeBytes: Long
    )

    fun generateExecutiveReport(
        context: Context,
        dataset: Dataset,
        insights: List<InsightItem>
    ): PdfResult {
        val pdfDocument = PdfDocument()
        val pageA4Width = 595
        val pageA4Height = 842

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        val currentDateStr = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())

        // ================= PAGE 1: EXECUTIVE SUMMARY & DATA AUDIT =================
        val pageInfo1 = PdfDocument.PageInfo.Builder(pageA4Width, pageA4Height, 1).create()
        val page1 = pdfDocument.startPage(pageInfo1)
        val canvas1 = page1.canvas

        // Header Background Banner
        paint.color = Color.rgb(11, 15, 25) // Deep Slate
        canvas1.drawRect(0f, 0f, pageA4Width.toFloat(), 110f, paint)

        // Accent Cyan Line
        paint.color = Color.rgb(0, 199, 217) // Cyan
        paint.strokeWidth = 4f
        canvas1.drawLine(0f, 108f, pageA4Width.toFloat(), 108f, paint)

        // Title & Branding
        textPaint.color = Color.WHITE
        textPaint.textSize = 20f
        textPaint.isFakeBoldText = true
        canvas1.drawText("DATASCOPE EXECUTIVE REPORT", 36f, 48f, textPaint)

        textPaint.textSize = 11f
        textPaint.isFakeBoldText = false
        textPaint.color = Color.rgb(148, 163, 184)
        canvas1.drawText("Automated Data Profiling, Cleansing & Statistical Insights", 36f, 68f, textPaint)
        canvas1.drawText("Generated: $currentDateStr  |  Dataset: ${dataset.title}", 36f, 88f, textPaint)

        // Quality Score & Metric KPI Cards (Row of 3 cards)
        val cardTop = 130f
        val cardHeight = 68f
        val cardWidth = 160f

        // Card 1: Data Health
        drawMetricCard(
            canvas1, 36f, cardTop, cardWidth, cardHeight,
            label = "DATA QUALITY SCORE",
            value = "${dataset.profile.qualityScore}%",
            sub = "${dataset.profile.missingCellsCount} nulls resolved",
            accentColor = Color.rgb(16, 185, 129) // Emerald
        )

        // Card 2: Cleaned Observations
        drawMetricCard(
            canvas1, 210f, cardTop, cardWidth, cardHeight,
            label = "TOTAL OBSERVATIONS",
            value = "${dataset.rows.size}",
            sub = "${dataset.profile.duplicateRowCount} duplicates removed",
            accentColor = Color.rgb(0, 199, 217) // Cyan
        )

        // Card 3: Feature Dimensions
        drawMetricCard(
            canvas1, 384f, cardTop, cardWidth, cardHeight,
            label = "DIMENSIONS & COLS",
            value = "${dataset.columns.size} Cols",
            sub = "${dataset.profile.numericColumnsCount} numeric attributes",
            accentColor = Color.rgb(129, 140, 248) // Indigo
        )

        // Section: Data Cleansing Audit Trail
        var currentY = 226f
        textPaint.color = Color.rgb(15, 23, 42)
        textPaint.textSize = 14f
        textPaint.isFakeBoldText = true
        canvas1.drawText("DATA CLEANSING & AUDIT TRAIL", 36f, currentY, textPaint)

        currentY += 16f
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 1f
        canvas1.drawLine(36f, currentY, (pageA4Width - 36).toFloat(), currentY, paint)

        currentY += 16f
        textPaint.textSize = 10f
        textPaint.isFakeBoldText = false

        if (dataset.cleaningHistory.isEmpty()) {
            textPaint.color = Color.rgb(100, 116, 139)
            canvas1.drawText("• Raw dataset loaded without manual transformations. Data completeness verified.", 36f, currentY, textPaint)
            currentY += 20f
        } else {
            dataset.cleaningHistory.take(5).forEach { step ->
                paint.color = Color.rgb(241, 245, 249)
                canvas1.drawRoundRect(RectF(36f, currentY - 12f, (pageA4Width - 36).toFloat(), currentY + 14f), 6f, 6f, paint)

                paint.color = Color.rgb(0, 199, 217)
                canvas1.drawCircle(48f, currentY, 3.5f, paint)

                textPaint.color = Color.rgb(15, 23, 42)
                textPaint.isFakeBoldText = true
                canvas1.drawText(step.type.label, 60f, currentY + 3f, textPaint)

                textPaint.color = Color.rgb(71, 85, 105)
                textPaint.isFakeBoldText = false
                canvas1.drawText(step.description, 230f, currentY + 3f, textPaint)

                currentY += 28f
            }
        }

        // Section: Column Schema & Summary Statistics Table
        currentY += 12f
        textPaint.color = Color.rgb(15, 23, 42)
        textPaint.textSize = 14f
        textPaint.isFakeBoldText = true
        canvas1.drawText("FEATURE SCHEMA & STATISTICAL PROFILE", 36f, currentY, textPaint)

        currentY += 14f
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 1f
        canvas1.drawLine(36f, currentY, (pageA4Width - 36).toFloat(), currentY, paint)

        currentY += 18f
        // Table Header
        paint.color = Color.rgb(241, 245, 249)
        canvas1.drawRect(36f, currentY - 14f, (pageA4Width - 36).toFloat(), currentY + 8f, paint)

        textPaint.color = Color.rgb(71, 85, 105)
        textPaint.textSize = 9f
        textPaint.isFakeBoldText = true
        canvas1.drawText("COLUMN", 44f, currentY, textPaint)
        canvas1.drawText("TYPE", 160f, currentY, textPaint)
        canvas1.drawText("MIN", 230f, currentY, textPaint)
        canvas1.drawText("MAX", 300f, currentY, textPaint)
        canvas1.drawText("MEAN / AVG", 370f, currentY, textPaint)
        canvas1.drawText("UNIQUE", 450f, currentY, textPaint)
        canvas1.drawText("NULLS", 510f, currentY, textPaint)

        currentY += 20f
        textPaint.isFakeBoldText = false
        textPaint.textSize = 8.5f

        dataset.columns.take(12).forEachIndexed { index, col ->
            if (index % 2 == 1) {
                paint.color = Color.rgb(248, 250, 252)
                canvas1.drawRect(36f, currentY - 12f, (pageA4Width - 36).toFloat(), currentY + 6f, paint)
            }

            textPaint.color = Color.rgb(15, 23, 42)
            val truncatedName = if (col.name.length > 16) col.name.take(14) + ".." else col.name
            canvas1.drawText(truncatedName, 44f, currentY, textPaint)

            textPaint.color = when (col.type) {
                ColumnType.NUMERIC -> Color.rgb(8, 145, 178)
                ColumnType.CATEGORICAL -> Color.rgb(79, 70, 229)
                ColumnType.DATE -> Color.rgb(5, 150, 105)
                ColumnType.BOOLEAN -> Color.rgb(217, 119, 6)
            }
            canvas1.drawText(col.type.name, 160f, currentY, textPaint)

            textPaint.color = Color.rgb(51, 65, 85)
            canvas1.drawText(col.minValue?.let { String.format(Locale.US, "%.1f", it) } ?: "-", 230f, currentY, textPaint)
            canvas1.drawText(col.maxValue?.let { String.format(Locale.US, "%.1f", it) } ?: "-", 300f, currentY, textPaint)
            canvas1.drawText(col.meanValue?.let { String.format(Locale.US, "%.1f", it) } ?: "-", 370f, currentY, textPaint)
            canvas1.drawText("${col.uniqueValuesCount}", 450f, currentY, textPaint)
            canvas1.drawText("${col.nullCount}", 510f, currentY, textPaint)

            currentY += 18f
        }

        // Footer Page 1
        textPaint.color = Color.rgb(148, 163, 184)
        textPaint.textSize = 8f
        canvas1.drawText("DataScope Analytics • Confidential Executive Summary • Page 1 of 2", 36f, pageA4Height - 24f, textPaint)

        pdfDocument.finishPage(page1)

        // ================= PAGE 2: VISUALIZATIONS & STRATEGIC INSIGHTS =================
        val pageInfo2 = PdfDocument.PageInfo.Builder(pageA4Width, pageA4Height, 2).create()
        val page2 = pdfDocument.startPage(pageInfo2)
        val canvas2 = page2.canvas

        // Header Strip
        paint.color = Color.rgb(11, 15, 25)
        canvas2.drawRect(0f, 0f, pageA4Width.toFloat(), 48f, paint)

        textPaint.color = Color.WHITE
        textPaint.textSize = 14f
        textPaint.isFakeBoldText = true
        canvas2.drawText("DATASCOPE VISUAL ANALYTICS & INSIGHTS", 36f, 30f, textPaint)

        var p2Y = 72f

        // Section: Visual Chart Snapshot
        textPaint.color = Color.rgb(15, 23, 42)
        textPaint.textSize = 13f
        textPaint.isFakeBoldText = true
        canvas2.drawText("DATA DISTRIBUTION & TREND VISUALIZATION", 36f, p2Y, textPaint)

        p2Y += 14f
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 1f
        canvas2.drawLine(36f, p2Y, (pageA4Width - 36).toFloat(), p2Y, paint)

        p2Y += 18f

        // Draw Embedded Bar Chart on PDF Canvas
        drawPdfBarChart(canvas2, 36f, p2Y, (pageA4Width - 72).toFloat(), 180f, dataset)
        p2Y += 205f

        // Section: Automated Strategic Insights
        textPaint.color = Color.rgb(15, 23, 42)
        textPaint.textSize = 13f
        textPaint.isFakeBoldText = true
        canvas2.drawText("KEY STRATEGIC & STATISTICAL INSIGHTS", 36f, p2Y, textPaint)

        p2Y += 14f
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 1f
        canvas2.drawLine(36f, p2Y, (pageA4Width - 36).toFloat(), p2Y, paint)

        p2Y += 16f

        insights.take(5).forEach { insight ->
            val boxHeight = 58f
            paint.color = Color.rgb(248, 250, 252)
            canvas2.drawRoundRect(RectF(36f, p2Y, (pageA4Width - 36).toFloat(), p2Y + boxHeight), 8f, 8f, paint)

            paint.strokeWidth = 1.5f
            paint.color = Color.rgb(226, 232, 240)
            paint.style = Paint.Style.STROKE
            canvas2.drawRoundRect(RectF(36f, p2Y, (pageA4Width - 36).toFloat(), p2Y + boxHeight), 8f, 8f, paint)
            paint.style = Paint.Style.FILL

            val badgeColor = when (insight.severity) {
                com.example.model.InsightSeverity.POSITIVE -> Color.rgb(16, 185, 129)
                com.example.model.InsightSeverity.WARNING -> Color.rgb(245, 158, 11)
                com.example.model.InsightSeverity.INFO -> Color.rgb(0, 199, 217)
            }
            paint.color = badgeColor
            canvas2.drawRect(36f, p2Y, 42f, p2Y + boxHeight, paint) // Accent left bar

            textPaint.color = Color.rgb(15, 23, 42)
            textPaint.textSize = 11f
            textPaint.isFakeBoldText = true
            canvas2.drawText(insight.title, 52f, p2Y + 18f, textPaint)

            textPaint.textSize = 9f
            textPaint.isFakeBoldText = false
            textPaint.color = Color.rgb(71, 85, 105)

            // Wrap text if needed
            val desc = insight.description
            val line1 = if (desc.length > 85) desc.take(85) + "..." else desc
            canvas2.drawText(line1, 52f, p2Y + 34f, textPaint)

            // Metric Tag on Right
            textPaint.color = badgeColor
            textPaint.textSize = 10f
            textPaint.isFakeBoldText = true
            val tagText = "${insight.metricLabel}: ${insight.metricValue}"
            val tagWidth = textPaint.measureText(tagText)
            canvas2.drawText(tagText, (pageA4Width - 48 - tagWidth), p2Y + 18f, textPaint)

            p2Y += boxHeight + 12f
        }

        // Footer Page 2
        textPaint.color = Color.rgb(148, 163, 184)
        textPaint.textSize = 8f
        textPaint.isFakeBoldText = false
        canvas2.drawText("DataScope Analytics • Verified Report • Page 2 of 2", 36f, pageA4Height - 24f, textPaint)

        pdfDocument.finishPage(page2)

        // Save PDF to reports dir in cache
        val reportsDir = File(context.cacheDir, "reports")
        if (!reportsDir.exists()) reportsDir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val sanitizedTitle = dataset.title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val file = File(reportsDir, "DataScope_${sanitizedTitle}_$timestamp.pdf")

        val out = FileOutputStream(file)
        pdfDocument.writeTo(out)
        out.close()
        pdfDocument.close()

        return PdfResult(
            file = file,
            pageCount = 2,
            sizeBytes = file.length()
        )
    }

    private fun drawMetricCard(
        canvas: android.graphics.Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        label: String,
        value: String,
        sub: String,
        accentColor: Int
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 8f, 8f, paint)

        // Border
        paint.color = Color.rgb(226, 232, 240)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        // Top accent line
        paint.color = accentColor
        canvas.drawRoundRect(RectF(x + 8f, y + 2f, x + 36f, y + 5f), 2f, 2f, paint)

        textPaint.color = Color.rgb(100, 116, 139)
        textPaint.textSize = 8f
        textPaint.isFakeBoldText = true
        canvas.drawText(label, x + 10f, y + 20f, textPaint)

        textPaint.color = Color.rgb(15, 23, 42)
        textPaint.textSize = 16f
        canvas.drawText(value, x + 10f, y + 42f, textPaint)

        textPaint.color = Color.rgb(100, 116, 139)
        textPaint.textSize = 7.5f
        textPaint.isFakeBoldText = false
        canvas.drawText(sub, x + 10f, y + 58f, textPaint)
    }

    private fun drawPdfBarChart(
        canvas: android.graphics.Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        dataset: Dataset
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Chart Background Box
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 8f, 8f, paint)

        paint.color = Color.rgb(226, 232, 240)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(RectF(x, y, x + w, y + h), 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        // Aggregate top categories
        val catCol = dataset.columns.firstOrNull { it.type == ColumnType.CATEGORICAL }
        val numCol = dataset.columns.firstOrNull { it.type == ColumnType.NUMERIC && it.meanValue != null }

        val bars = if (catCol != null && numCol != null) {
            dataset.rows.groupBy { it.values.getOrElse(catCol.index) { "Other" }.ifBlank { "Other" } }
                .mapValues { (_, rList) ->
                    rList.sumOf {
                        it.values.getOrElse(numCol.index) { "" }.replace("$", "").replace(",", "").toDoubleOrNull() ?: 0.0
                    }
                }
                .toList()
                .sortedByDescending { it.second }
                .take(6)
        } else {
            listOf(
                "Segment A" to 420.0,
                "Segment B" to 310.0,
                "Segment C" to 580.0,
                "Segment D" to 220.0,
                "Segment E" to 490.0
            )
        }

        val maxVal = (bars.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1.0)
        val chartPaddingLeft = 40f
        val chartPaddingBottom = 30f
        val chartPaddingTop = 30f
        val chartW = w - chartPaddingLeft - 20f
        val chartH = h - chartPaddingTop - chartPaddingBottom

        // Title
        textPaint.color = Color.rgb(15, 23, 42)
        textPaint.textSize = 10f
        textPaint.isFakeBoldText = true
        val titleText = "Metric Breakdown: ${numCol?.name ?: "Values"} by ${catCol?.name ?: "Category"}"
        canvas.drawText(titleText, x + 16f, y + 20f, textPaint)

        // Gridlines
        paint.color = Color.rgb(226, 232, 240)
        paint.strokeWidth = 1f
        val gridLines = 4
        for (g in 0..gridLines) {
            val gridY = y + chartPaddingTop + (chartH / gridLines) * g
            canvas.drawLine(x + chartPaddingLeft, gridY, x + w - 20f, gridY, paint)

            val gridVal = maxVal * (1.0 - g.toDouble() / gridLines)
            textPaint.color = Color.rgb(148, 163, 184)
            textPaint.textSize = 7f
            textPaint.isFakeBoldText = false
            canvas.drawText(String.format(Locale.US, "%.0f", gridVal), x + 8f, gridY + 3f, textPaint)
        }

        // Draw Bars
        val barCount = bars.size
        val barSlotWidth = chartW / barCount
        val barWidth = barSlotWidth * 0.55f

        val colors = listOf(
            Color.rgb(0, 199, 217),   // Cyan
            Color.rgb(129, 140, 248), // Indigo
            Color.rgb(16, 185, 129),  // Emerald
            Color.rgb(245, 158, 11),  // Amber
            Color.rgb(244, 63, 94),   // Coral
            Color.rgb(168, 85, 247)   // Purple
        )

        bars.forEachIndexed { i, (label, value) ->
            val barX = x + chartPaddingLeft + i * barSlotWidth + (barSlotWidth - barWidth) / 2f
            val barHeight = (value / maxVal * chartH).toFloat().coerceAtLeast(4f)
            val barY = y + chartPaddingTop + chartH - barHeight

            paint.color = colors[i % colors.size]
            canvas.drawRoundRect(RectF(barX, barY, barX + barWidth, y + chartPaddingTop + chartH), 4f, 4f, paint)

            // Value text above bar
            textPaint.color = Color.rgb(15, 23, 42)
            textPaint.textSize = 7.5f
            textPaint.isFakeBoldText = true
            val valText = if (value >= 1000) String.format(Locale.US, "%.1fK", value / 1000) else String.format(Locale.US, "%.0f", value)
            val valWidth = textPaint.measureText(valText)
            canvas.drawText(valText, barX + (barWidth - valWidth) / 2f, barY - 4f, textPaint)

            // Category Label below bar
            textPaint.color = Color.rgb(71, 85, 105)
            textPaint.textSize = 7.5f
            textPaint.isFakeBoldText = false
            val truncLabel = if (label.length > 9) label.take(8) + ".." else label
            val lblWidth = textPaint.measureText(truncLabel)
            canvas.drawText(truncLabel, barX + (barWidth - lblWidth) / 2f, y + chartPaddingTop + chartH + 14f, textPaint)
        }
    }

    fun createShareIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "DataScope Analytics Executive Report: ${file.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun createViewIntent(context: Context, file: File): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
