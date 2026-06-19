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
