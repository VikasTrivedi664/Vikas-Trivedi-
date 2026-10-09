package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CsvParser
import com.example.data.DataCleaner
import com.example.data.InsightsGenerator
import com.example.data.SampleDatasets
import com.example.model.CleaningType
import com.example.model.ColumnType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("DataScope", appName)
  }

  @Test
  fun `test csv parser parses ecommerce dataset`() {
    val dataset = CsvParser.parse(
      content = SampleDatasets.ecommerceSalesCsv,
      sourceName = "test.csv",
      title = "Test E-Commerce"
    )

    assertTrue(dataset.columns.isNotEmpty())
    assertTrue(dataset.rows.isNotEmpty())

    val revCol = dataset.columns.find { it.name == "Revenue_USD" }
    assertNotNull(revCol)
    assertEquals(ColumnType.NUMERIC, revCol?.type)
  }

  @Test
  fun `test data cleaner auto clean removes duplicates and improves quality score`() {
    val dataset = CsvParser.parse(
      content = SampleDatasets.ecommerceSalesCsv,
      sourceName = "test.csv",
      title = "Test E-Commerce"
    )

    val initialQuality = dataset.profile.qualityScore
    val cleaned = DataCleaner.autoClean(dataset)

    assertTrue(cleaned.cleaningHistory.isNotEmpty())
    assertTrue(cleaned.profile.qualityScore >= initialQuality)
    assertEquals(0, cleaned.profile.duplicateRowCount)
  }

  @Test
  fun `test insights generator outputs insights`() {
    val dataset = CsvParser.parse(
      content = SampleDatasets.ecommerceSalesCsv,
      sourceName = "test.csv",
      title = "Test E-Commerce"
    )
    val insights = InsightsGenerator.generateInsights(dataset)
    assertTrue(insights.isNotEmpty())
  }
}
