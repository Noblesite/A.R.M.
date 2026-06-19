package net.noblesite.arm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.noblesite.arm.ui.theme.ARMTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ARMTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    SessionScreen(
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun SessionScreen(
    modifier: Modifier = Modifier,
    currentTimeMillis: () -> Long = System::currentTimeMillis
) {
    val context = LocalContext.current
    val repository = remember(context) {
        SessionRepository(
            dao = AppDatabase.getInstance(context.applicationContext).armDao()
        )
    }
    val snapshotCollector = remember(context) {
        ResourceSnapshotCollector(
            context = context.applicationContext,
            currentTimeMillis = currentTimeMillis
        )
    }
    val coroutineScope = rememberCoroutineScope()
    var sessionState by remember { mutableStateOf(SessionState()) }
    var isBusy by remember { mutableStateOf(false) }

    LaunchedEffect(repository) {
        sessionState = repository.loadState()
    }

    ProjectIdentity(
        state = sessionState,
        isBusy = isBusy,
        onStart = {
            val nowMillis = currentTimeMillis()
            val startSnapshot = snapshotCollector.collect(capturedAtMillis = nowMillis)
            coroutineScope.launch {
                isBusy = true
                try {
                    sessionState = repository.startSession(
                        nowMillis = nowMillis,
                        startSnapshot = startSnapshot
                    )
                } finally {
                    isBusy = false
                }
            }
        },
        onStop = {
            val nowMillis = currentTimeMillis()
            val stopSnapshot = snapshotCollector.collect(capturedAtMillis = nowMillis)
            coroutineScope.launch {
                isBusy = true
                try {
                    sessionState = repository.stopSession(
                        nowMillis = nowMillis,
                        stopSnapshot = stopSnapshot
                    )
                } finally {
                    isBusy = false
                }
            }
        },
        modifier = modifier
    )
}

@Composable
fun ProjectIdentity(
    state: SessionState,
    isBusy: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSession = state.activeSession
    val lastSummary = state.lastSummary

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "A.R.M.",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Android Resource Monitor",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = if (activeSession == null) "No session running" else "Session running",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = if (activeSession == null) {
                        "Start a baseline session before installing or changing software."
                    } else {
                        "Session ${activeSession.id} captured its start resource snapshot."
                    },
                    style = MaterialTheme.typography.bodyMedium
                )

                activeSession?.startSnapshot?.let { snapshot ->
                    SnapshotDetails(
                        title = "Start snapshot",
                        snapshot = snapshot
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onStart,
                enabled = activeSession == null && !isBusy
            ) {
                Text(text = "Start")
            }
            OutlinedButton(
                onClick = onStop,
                enabled = activeSession != null && !isBusy
            ) {
                Text(text = "Stop")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (lastSummary != null) {
            Text(
                text = "Last session",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Session ${lastSummary.id} ran for ${formatDuration(lastSummary.durationMillis)}.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            SnapshotDetails(
                title = "Start snapshot",
                snapshot = lastSummary.startSnapshot
            )
            Spacer(modifier = Modifier.height(12.dp))
            SnapshotDetails(
                title = "Stop snapshot",
                snapshot = lastSummary.stopSnapshot
            )
            Spacer(modifier = Modifier.height(12.dp))
            SessionDeltaDetails(summary = lastSummary)
        } else {
            Text(
                text = "No completed session yet.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun SnapshotDetails(
    title: String,
    snapshot: ResourceSnapshot,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = "Memory available: ${formatBytes(snapshot.availableMemoryBytes)} of ${formatBytes(snapshot.totalMemoryBytes)}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Battery: ${formatBatteryPercent(snapshot.batteryPercent)}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Network RX/TX: ${formatOptionalBytes(snapshot.totalRxBytes)} / ${formatOptionalBytes(snapshot.totalTxBytes)}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Low memory: ${if (snapshot.lowMemory) "Yes" else "No"}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun SessionDeltaDetails(
    summary: SessionSummary,
    modifier: Modifier = Modifier
) {
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

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Session change",
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = "Available memory: ${formatSignedBytes(memoryDelta)}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Battery: ${batteryDelta?.let { "$it%" } ?: "Unavailable"}",
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Network RX/TX: ${formatOptionalBytes(rxDelta)} / ${formatOptionalBytes(txDelta)}",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

fun formatSignedBytes(bytes: Long): String {
    val prefix = if (bytes > 0L) "+" else ""
    return "$prefix${formatBytes(bytes)}"
}

@Preview(showBackground = true)
@Composable
fun ProjectIdentityPreview() {
    ARMTheme {
        ProjectIdentity(
            state = SessionState(
                lastSummary = SessionSummary(
                    id = 1_000L,
                    startedAtMillis = 1_000L,
                    endedAtMillis = 61_000L,
                    startSnapshot = previewSnapshot(1_000L),
                    stopSnapshot = previewSnapshot(61_000L)
                )
            ),
            isBusy = false,
            onStart = {},
            onStop = {}
        )
    }
}

fun previewSnapshot(capturedAtMillis: Long): ResourceSnapshot {
    return ResourceSnapshot(
        capturedAtMillis = capturedAtMillis,
        availableMemoryBytes = 3_200L * 1024L * 1024L,
        totalMemoryBytes = 4_096L * 1024L * 1024L,
        lowMemory = false,
        batteryPercent = 88,
        batteryCharging = false,
        totalRxBytes = 512L * 1024L * 1024L,
        totalTxBytes = 128L * 1024L * 1024L
    )
}
