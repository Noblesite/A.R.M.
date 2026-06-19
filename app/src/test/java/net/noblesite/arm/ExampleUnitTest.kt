package net.noblesite.arm

import org.junit.Test

import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class SessionControllerTest {
    @Test
    fun startSession_createsActiveSession() {
        val controller = SessionController()

        controller.startSession(
            nowMillis = 1_000L,
            startSnapshot = testSnapshot(capturedAtMillis = 1_000L)
        )

        assertEquals(1_000L, controller.state.activeSession?.id)
        assertEquals(1_000L, controller.state.activeSession?.startedAtMillis)
        assertEquals(1_000L, controller.state.activeSession?.startSnapshot?.capturedAtMillis)
        assertNull(controller.state.lastSummary)
    }

    @Test
    fun startSession_whenAlreadyActive_keepsOriginalSession() {
        val controller = SessionController()

        controller.startSession(
            nowMillis = 1_000L,
            startSnapshot = testSnapshot(capturedAtMillis = 1_000L)
        )
        controller.startSession(
            nowMillis = 2_000L,
            startSnapshot = testSnapshot(capturedAtMillis = 2_000L)
        )

        assertEquals(1_000L, controller.state.activeSession?.id)
        assertEquals(1_000L, controller.state.activeSession?.startedAtMillis)
    }

    @Test
    fun stopSession_createsSummaryAndClearsActiveSession() {
        val controller = SessionController()

        controller.startSession(
            nowMillis = 1_000L,
            startSnapshot = testSnapshot(
                capturedAtMillis = 1_000L,
                availableMemoryBytes = 4_000L,
                batteryPercent = 80,
                totalRxBytes = 1_000L,
                totalTxBytes = 2_000L
            )
        )
        controller.stopSession(
            nowMillis = 91_000L,
            stopSnapshot = testSnapshot(
                capturedAtMillis = 91_000L,
                availableMemoryBytes = 3_500L,
                batteryPercent = 78,
                totalRxBytes = 1_500L,
                totalTxBytes = 2_750L
            )
        )

        assertNull(controller.state.activeSession)
        assertEquals(1_000L, controller.state.lastSummary?.id)
        assertEquals(90_000L, controller.state.lastSummary?.durationMillis)
        assertEquals(1_000L, controller.state.lastSummary?.startSnapshot?.capturedAtMillis)
        assertEquals(91_000L, controller.state.lastSummary?.stopSnapshot?.capturedAtMillis)
        assertEquals(-500L, memoryDeltaBytes(controller.state.lastSummary!!))
        assertEquals(-2, batteryDeltaPercent(controller.state.lastSummary!!))
        assertEquals(500L, networkDeltaBytes(1_000L, 1_500L))
        assertEquals(750L, networkDeltaBytes(2_000L, 2_750L))
    }

    @Test
    fun formatDuration_usesMinutesAndSeconds() {
        assertEquals("2m 5s", formatDuration(125_000L))
    }

    @Test
    fun formatBytes_usesSmallestReadableUnit() {
        assertEquals("500 B", formatBytes(500L))
        assertEquals("2 KB", formatBytes(2_048L))
        assertEquals("3 MB", formatBytes(3L * 1024L * 1024L))
    }

    @Test
    fun resourceSnapshot_roundTripsThroughEntity() {
        val snapshot = testSnapshot(
            capturedAtMillis = 5_000L,
            availableMemoryBytes = 3_000L,
            totalMemoryBytes = 8_000L,
            lowMemory = true,
            batteryPercent = 77,
            batteryCharging = true,
            totalRxBytes = 12_000L,
            totalTxBytes = 13_000L
        )

        val entity = snapshot.toEntity(
            sessionId = 1_000L,
            stage = SnapshotStage.START
        )

        assertEquals(1_000L, entity.sessionId)
        assertEquals(SnapshotStage.START, entity.stage)
        assertEquals(snapshot, entity.toResourceSnapshot())
    }

    @Test
    fun sessionWithSnapshots_mapsToSessionSummary() {
        val sessionWithSnapshots = SessionWithSnapshots(
            session = SessionEntity(
                id = 1_000L,
                startedAtMillis = 1_000L,
                endedAtMillis = 61_000L
            ),
            snapshots = listOf(
                testSnapshot(capturedAtMillis = 1_000L).toEntity(
                    sessionId = 1_000L,
                    stage = SnapshotStage.START
                ),
                testSnapshot(capturedAtMillis = 61_000L).toEntity(
                    sessionId = 1_000L,
                    stage = SnapshotStage.STOP
                )
            )
        )

        val summary = sessionWithSnapshots.toSessionSummary()

        assertEquals(1_000L, summary?.id)
        assertEquals(60_000L, summary?.durationMillis)
        assertEquals(1_000L, summary?.startSnapshot?.capturedAtMillis)
        assertEquals(61_000L, summary?.stopSnapshot?.capturedAtMillis)
    }
}

private fun testSnapshot(
    capturedAtMillis: Long,
    availableMemoryBytes: Long = 4_000L,
    totalMemoryBytes: Long = 8_000L,
    lowMemory: Boolean = false,
    batteryPercent: Int? = 90,
    batteryCharging: Boolean? = false,
    totalRxBytes: Long? = 1_000L,
    totalTxBytes: Long? = 2_000L
): ResourceSnapshot {
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
