package net.noblesite.arm

data class ActiveSession(
    val id: Long,
    val startedAtMillis: Long,
    val startSnapshot: ResourceSnapshot
)

data class SessionSummary(
    val id: Long,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val startSnapshot: ResourceSnapshot,
    val stopSnapshot: ResourceSnapshot
) {
    val durationMillis: Long = endedAtMillis - startedAtMillis
}

data class SessionState(
    val activeSession: ActiveSession? = null,
    val lastSummary: SessionSummary? = null
)

class SessionController(
    private val initialState: SessionState = SessionState()
) {
    var state: SessionState = initialState
        private set

    fun startSession(nowMillis: Long, startSnapshot: ResourceSnapshot) {
        if (state.activeSession != null) return

        state = state.copy(
            activeSession = ActiveSession(
                id = nowMillis,
                startedAtMillis = nowMillis,
                startSnapshot = startSnapshot
            )
        )
    }

    fun stopSession(nowMillis: Long, stopSnapshot: ResourceSnapshot) {
        val activeSession = state.activeSession ?: return
        val endedAtMillis = maxOf(nowMillis, activeSession.startedAtMillis)

        state = state.copy(
            activeSession = null,
            lastSummary = SessionSummary(
                id = activeSession.id,
                startedAtMillis = activeSession.startedAtMillis,
                endedAtMillis = endedAtMillis,
                startSnapshot = activeSession.startSnapshot,
                stopSnapshot = stopSnapshot
            )
        )
    }
}

fun formatDuration(durationMillis: Long): String {
    val totalSeconds = durationMillis.coerceAtLeast(0L) / 1_000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "${minutes}m ${seconds}s"
}

fun memoryDeltaBytes(summary: SessionSummary): Long {
    return summary.stopSnapshot.availableMemoryBytes - summary.startSnapshot.availableMemoryBytes
}

fun batteryDeltaPercent(summary: SessionSummary): Int? {
    val startPercent = summary.startSnapshot.batteryPercent ?: return null
    val stopPercent = summary.stopSnapshot.batteryPercent ?: return null
    return stopPercent - startPercent
}

fun networkDeltaBytes(startBytes: Long?, stopBytes: Long?): Long? {
    val knownStartBytes = startBytes ?: return null
    val knownStopBytes = stopBytes ?: return null
    return (knownStopBytes - knownStartBytes).coerceAtLeast(0L)
}
