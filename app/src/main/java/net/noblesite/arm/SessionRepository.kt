package net.noblesite.arm

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SessionRepository(
    private val dao: ArmDao
) {
    suspend fun loadState(): SessionState = withContext(Dispatchers.IO) {
        dao.readState()
    }

    suspend fun startSession(nowMillis: Long, startSnapshot: ResourceSnapshot): SessionState {
        return withContext(Dispatchers.IO) {
            if (dao.getActiveSession() == null) {
                dao.insertSession(
                    SessionEntity(
                        id = nowMillis,
                        startedAtMillis = nowMillis,
                        endedAtMillis = null
                    )
                )
                dao.insertSnapshot(
                    startSnapshot.toEntity(
                        sessionId = nowMillis,
                        stage = SnapshotStage.START
                    )
                )
            }
            dao.readState()
        }
    }

    suspend fun stopSession(nowMillis: Long, stopSnapshot: ResourceSnapshot): SessionState {
        return withContext(Dispatchers.IO) {
            val activeSession = dao.getActiveSession()
            if (activeSession != null) {
                val endedAtMillis = maxOf(nowMillis, activeSession.session.startedAtMillis)
                dao.markSessionEnded(
                    sessionId = activeSession.session.id,
                    endedAtMillis = endedAtMillis
                )
                dao.insertSnapshot(
                    stopSnapshot.copy(capturedAtMillis = endedAtMillis).toEntity(
                        sessionId = activeSession.session.id,
                        stage = SnapshotStage.STOP
                    )
                )
            }
            dao.readState()
        }
    }

    private suspend fun ArmDao.readState(): SessionState {
        return SessionState(
            activeSession = getActiveSession()?.toActiveSession(),
            lastSummary = getLastCompletedSession()?.toSessionSummary()
        )
    }
}

fun ResourceSnapshot.toEntity(
    sessionId: Long,
    stage: SnapshotStage
): ResourceSnapshotEntity {
    return ResourceSnapshotEntity(
        sessionId = sessionId,
        stage = stage,
        capturedAtMillis = capturedAtMillis,
        availableMemoryBytes = availableMemoryBytes,
        totalMemoryBytes = totalMemoryBytes,
        lowMemory = lowMemory,
        batteryPercent = batteryPercent,
        batteryCharging = batteryCharging,
        totalRxBytes = totalRxBytes,
        totalTxBytes = totalTxBytes
    )
}

fun ResourceSnapshotEntity.toResourceSnapshot(): ResourceSnapshot {
    return ResourceSnapshot(
        capturedAtMillis = capturedAtMillis,
        availableMemoryBytes = availableMemoryBytes,
        totalMemoryBytes = totalMemoryBytes,
        lowMemory = lowMemory,
        batteryPercent = batteryPercent,
        batteryCharging = batteryCharging,
        totalRxBytes = totalRxBytes,
        totalTxBytes = totalTxBytes
    )
}

fun SessionWithSnapshots.toActiveSession(): ActiveSession? {
    val startSnapshot = snapshots.firstOrNull { it.stage == SnapshotStage.START }
        ?: return null
    return ActiveSession(
        id = session.id,
        startedAtMillis = session.startedAtMillis,
        startSnapshot = startSnapshot.toResourceSnapshot()
    )
}

fun SessionWithSnapshots.toSessionSummary(): SessionSummary? {
    val endedAtMillis = session.endedAtMillis ?: return null
    val startSnapshot = snapshots.firstOrNull { it.stage == SnapshotStage.START }
        ?: return null
    val stopSnapshot = snapshots.firstOrNull { it.stage == SnapshotStage.STOP }
        ?: return null

    return SessionSummary(
        id = session.id,
        startedAtMillis = session.startedAtMillis,
        endedAtMillis = endedAtMillis,
        startSnapshot = startSnapshot.toResourceSnapshot(),
        stopSnapshot = stopSnapshot.toResourceSnapshot()
    )
}
