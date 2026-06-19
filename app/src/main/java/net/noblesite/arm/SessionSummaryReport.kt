package net.noblesite.arm

data class GeneratedSessionSummary(
    val title: String,
    val lines: List<String>
)

fun generateSessionSummary(summary: SessionSummary): GeneratedSessionSummary {
    val memoryDelta = memoryDeltaBytes(summary)
    val batteryDelta = batteryDeltaPercent(summary)
    val rxDelta = networkDeltaBytes(
        startBytes = summary.startSnapshot.totalRxBytes,
        stopBytes = summary.stopSnapshot.totalRxBytes
    )
    val txDelta = networkDeltaBytes(
        startBytes = summary.startSnapshot.totalTxBytes,
        stopBytes = summary.stopSnapshot.totalTxBytes
    )

    return GeneratedSessionSummary(
        title = "Latest Session Summary",
        lines = listOf(
            "Session ${summary.id}",
            "Duration: ${formatDuration(summary.durationMillis)}",
            "Available memory changed by ${formatSignedBytes(memoryDelta)}.",
            "Battery changed by ${formatSignedPercent(batteryDelta)}.",
            "Network received ${formatOptionalBytes(rxDelta)} and sent ${formatOptionalBytes(txDelta)}.",
            "Low memory observed: ${formatLowMemoryObserved(summary)}."
        )
    )
}

fun buildSessionTextReport(summary: SessionSummary): String {
    val generatedSummary = generateSessionSummary(summary)
    return buildString {
        appendLine("A.R.M.")
        appendLine("Android Resource Monitor")
        appendLine()
        appendLine(generatedSummary.title)
        generatedSummary.lines.forEach { line ->
            appendLine(line)
        }
        appendLine()
        appendLine("Start Snapshot")
        appendSnapshot(summary.startSnapshot)
        appendLine()
        appendLine("Stop Snapshot")
        appendSnapshot(summary.stopSnapshot)
        appendLine()
        appendLine("Session Change")
        appendLine("Available memory: ${formatSignedBytes(memoryDeltaBytes(summary))}")
        appendLine("Battery: ${formatSignedPercent(batteryDeltaPercent(summary))}")
        appendLine(
            "Network RX/TX: ${
                formatOptionalBytes(
                    networkDeltaBytes(
                        summary.startSnapshot.totalRxBytes,
                        summary.stopSnapshot.totalRxBytes
                    )
                )
            } / ${
                formatOptionalBytes(
                    networkDeltaBytes(
                        summary.startSnapshot.totalTxBytes,
                        summary.stopSnapshot.totalTxBytes
                    )
                )
            }"
        )
    }
}

fun reportFileName(summary: SessionSummary): String {
    return "arm-session-${summary.id}.txt"
}

fun formatSignedPercent(percent: Int?): String {
    val knownPercent = percent ?: return "Unavailable"
    val prefix = if (knownPercent > 0) "+" else ""
    return "$prefix$knownPercent%"
}

private fun formatLowMemoryObserved(summary: SessionSummary): String {
    return if (summary.startSnapshot.lowMemory || summary.stopSnapshot.lowMemory) {
        "Yes"
    } else {
        "No"
    }
}

private fun StringBuilder.appendSnapshot(snapshot: ResourceSnapshot) {
    appendLine("Captured at: ${snapshot.capturedAtMillis}")
    appendLine("Available memory: ${formatBytes(snapshot.availableMemoryBytes)}")
    appendLine("Total memory: ${formatBytes(snapshot.totalMemoryBytes)}")
    appendLine("Low memory: ${if (snapshot.lowMemory) "Yes" else "No"}")
    appendLine("Battery: ${formatBatteryPercent(snapshot.batteryPercent)}")
    appendLine("Charging: ${formatCharging(snapshot.batteryCharging)}")
    appendLine("Network RX: ${formatOptionalBytes(snapshot.totalRxBytes)}")
    appendLine("Network TX: ${formatOptionalBytes(snapshot.totalTxBytes)}")
}

private fun formatCharging(charging: Boolean?): String {
    return when (charging) {
        true -> "Yes"
        false -> "No"
        null -> "Unavailable"
    }
}
