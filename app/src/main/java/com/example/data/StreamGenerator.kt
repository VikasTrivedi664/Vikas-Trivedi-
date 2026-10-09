package com.example.data

import com.example.model.StreamRecord
import com.example.model.StreamScenario
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

object StreamGenerator {

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private var sequenceId = 1000L

    // State keepers for random walks
    private var btcPrice = 69200.0
    private var machineTemp = 24.5
    private var cpuLoad = 42.0

    fun generateNextRecord(scenario: StreamScenario): StreamRecord {
        sequenceId++
        val now = timeFormat.format(Date())

        return when (scenario) {
            StreamScenario.ECOMMERCE_TRANSACTIONS -> {
                val categories = listOf("Electronics", "Apparel", "Home", "Office", "Beauty")
                val category = categories.random()
                val price = when (category) {
                    "Electronics" -> Random.nextDouble(180.0, 950.0)
                    "Apparel" -> Random.nextDouble(35.0, 140.0)
                    else -> Random.nextDouble(20.0, 220.0)
                }
                val units = Random.nextInt(1, 8)
                val totalRevenue = price * units
                val status = if (totalRevenue > 1200.0) "HIGH_VALUE" else "NORMAL"

                StreamRecord(
                    id = sequenceId,
                    timestamp = now,
                    label = "Order #$sequenceId ($category)",
                    value1 = totalRevenue,
                    value2 = units.toDouble(),
                    status = status,
                    category = category
                )
            }

            StreamScenario.IOT_FACTORY_SENSORS -> {
                val machines = listOf("CNC-01", "CNC-02", "PRESS-01", "ROBOT-A", "MILL-03")
                val machine = machines.random()
                val tempDelta = (Random.nextDouble() - 0.48) * 1.5
                machineTemp = (machineTemp + tempDelta).coerceIn(18.0, 48.0)
                val vibration = (Random.nextDouble(25.0, 75.0) + (machineTemp - 20) * 1.2).coerceIn(20.0, 95.0)
                val status = if (machineTemp > 38.0 || vibration > 75.0) "ALERT" else "NORMAL"

                StreamRecord(
                    id = sequenceId,
                    timestamp = now,
                    label = "$machine Telemetry",
                    value1 = machineTemp,
                    value2 = vibration,
                    status = status,
                    category = machine
                )
            }

            StreamScenario.FINANCIAL_MARKET_TICKER -> {
                val tickers = listOf("BTC", "ETH", "SOL", "NVDA", "AAPL")
                val ticker = tickers.random()
                val walkPct = (Random.nextDouble() - 0.495) * 0.015
                btcPrice = (btcPrice * (1.0 + walkPct)).coerceAtLeast(1000.0)
                val volume = Random.nextDouble(5.0, 85.0)
                val status = if (walkPct > 0.008) "BULLISH" else if (walkPct < -0.008) "BEARISH" else "STEADY"

                StreamRecord(
                    id = sequenceId,
                    timestamp = now,
                    label = "$ticker @ ${String.format(Locale.US, "%.1f", btcPrice)}",
                    value1 = btcPrice,
                    value2 = volume,
                    status = status,
                    category = ticker
                )
            }

            StreamScenario.SERVER_SYSTEM_HEALTH -> {
                val nodes = listOf("Node-US-East", "Node-EU-Central", "Node-AP-South", "DB-Primary")
                val node = nodes.random()
                val cpuDelta = (Random.nextDouble() - 0.48) * 4.0
                cpuLoad = (cpuLoad + cpuDelta).coerceIn(10.0, 99.0)
                val latency = (cpuLoad * 1.8 + Random.nextDouble(5.0, 25.0)).coerceIn(15.0, 250.0)
                val status = if (cpuLoad > 85.0) "HIGH_LOAD" else "HEALTHY"

                StreamRecord(
                    id = sequenceId,
                    timestamp = now,
                    label = "$node Metrics",
                    value1 = cpuLoad,
                    value2 = latency,
                    status = status,
                    category = node
                )
            }
        }
    }
}
