package net.noblesite.arm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import net.noblesite.arm.ui.theme.CriticalRed
import net.noblesite.arm.ui.theme.DeepSlate
import net.noblesite.arm.ui.theme.MutedSlateText
import net.noblesite.arm.ui.theme.PanelSlate
import net.noblesite.arm.ui.theme.PanelSlateElevated
import net.noblesite.arm.ui.theme.PrimaryText
import net.noblesite.arm.ui.theme.SuccessGreen
import net.noblesite.arm.ui.theme.ARMTheme
import net.noblesite.arm.ui.theme.TelemetryCyan
import net.noblesite.arm.ui.theme.WarningAmber

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ARMTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = DeepSlate
                ) { innerPadding ->
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
    val textReportExporter = remember(context) {
        TextReportExporter(context = context.applicationContext)
    }
    val coroutineScope = rememberCoroutineScope()
    var sessionState by remember { mutableStateOf(SessionState()) }
    var isBusy by remember { mutableStateOf(false) }
    var exportMessage by remember { mutableStateOf<String?>(null) }
    var sessionLabel by remember { mutableStateOf("No AnyConnect") }

    LaunchedEffect(repository) {
        sessionState = repository.loadState()
    }

    ProjectIdentity(
        state = sessionState,
        isBusy = isBusy,
        sessionLabel = sessionLabel,
        onSessionLabelChange = { sessionLabel = it },
        exportMessage = exportMessage,
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
        onExport = {
            val summary = sessionState.lastSummary
            if (summary != null) {
                coroutineScope.launch {
                    isBusy = true
                    exportMessage = null
                    try {
                        val exportedReport = textReportExporter.export(summary)
                        exportMessage = "Exported ${exportedReport.fileName} to Downloads/A.R.M."
                    } catch (throwable: Throwable) {
                        exportMessage = "Export failed: ${throwable.message ?: "Unknown error"}"
                    } finally {
                        isBusy = false
                    }
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
    sessionLabel: String,
    onSessionLabelChange: (String) -> Unit,
    exportMessage: String?,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeSession = state.activeSession
    val lastSummary = state.lastSummary
    val isMonitoring = activeSession != null
    val displaySnapshot = activeSession?.startSnapshot ?: lastSummary?.stopSnapshot
    val sampleCount = when {
        activeSession != null -> 1
        lastSummary != null -> 2
        else -> 0
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepSlate)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DashboardHeader(isMonitoring = isMonitoring)

        SessionCard(
            label = sessionLabel,
            onLabelChange = onSessionLabelChange,
            isMonitoring = isMonitoring
        )

        StatusCard(
            isMonitoring = isMonitoring,
            elapsedText = lastSummary?.let { formatDuration(it.durationMillis) }
        )

        TelemetryGrid(
            snapshot = displaySnapshot,
            sampleCount = sampleCount
        )

        ControlPanel(
            isMonitoring = isMonitoring,
            hasCompletedSession = lastSummary != null,
            isBusy = isBusy,
            onStart = onStart,
            onStop = onStop,
            onExport = onExport
        )

        exportMessage?.let { message ->
            DiagnosticCard {
                Text(
                    text = message,
                    color = if (message.startsWith("Exported")) SuccessGreen else WarningAmber,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (activeSession != null) {
            DiagnosticCard(
                title = "Session",
                accentColor = SuccessGreen
            ) {
                Text(
                    text = "Session ${activeSession.id} captured its start resource snapshot.",
                    color = PrimaryText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        if (lastSummary != null) {
            SessionReview(summary = lastSummary)
        } else {
            DiagnosticCard(
                title = "Latest Report",
                accentColor = MutedSlateText
            ) {
                Text(
                    text = "No completed session yet.",
                    color = MutedSlateText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
fun DashboardHeader(
    isMonitoring: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "A.R.M.",
                color = TelemetryCyan,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Android Resource Monitor",
                color = MutedSlateText,
                style = MaterialTheme.typography.titleMedium
            )
        }
        StatusIndicator(isMonitoring = isMonitoring)
    }
}

@Composable
fun StatusIndicator(
    isMonitoring: Boolean,
    modifier: Modifier = Modifier
) {
    val statusColor = if (isMonitoring) SuccessGreen else MutedSlateText
    val statusText = if (isMonitoring) "Monitoring" else "Idle"
    Row(
        modifier = modifier
            .border(1.dp, statusColor.copy(alpha = 0.5f), MaterialTheme.shapes.small)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .semantics { contentDescription = "Status $statusText" },
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .background(statusColor)
        )
        Text(
            text = statusText,
            color = PrimaryText,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
fun SessionCard(
    label: String,
    onLabelChange: (String) -> Unit,
    isMonitoring: Boolean,
    modifier: Modifier = Modifier
) {
    DiagnosticCard(
        title = "Session",
        accentColor = if (isMonitoring) SuccessGreen else TelemetryCyan,
        modifier = modifier
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = onLabelChange,
            enabled = !isMonitoring,
            singleLine = true,
            label = { Text("Session label") },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = PanelSlate,
                unfocusedContainerColor = PanelSlate,
                disabledContainerColor = PanelSlateElevated,
                focusedTextColor = PrimaryText,
                unfocusedTextColor = PrimaryText,
                disabledTextColor = MutedSlateText,
                focusedLabelColor = TelemetryCyan,
                unfocusedLabelColor = MutedSlateText,
                disabledLabelColor = MutedSlateText,
                focusedIndicatorColor = TelemetryCyan,
                unfocusedIndicatorColor = MutedSlateText,
                disabledIndicatorColor = MutedSlateText.copy(alpha = 0.5f)
            )
        )
        Text(
            text = if (isMonitoring) "Locked while monitoring is active." else "Edit before monitoring starts.",
            color = MutedSlateText,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun StatusCard(
    isMonitoring: Boolean,
    elapsedText: String?,
    modifier: Modifier = Modifier
) {
    DiagnosticCard(
        title = "Status",
        accentColor = if (isMonitoring) SuccessGreen else MutedSlateText,
        modifier = modifier
    ) {
        Text(
            text = if (isMonitoring) "Monitoring" else "Idle",
            color = if (isMonitoring) SuccessGreen else MutedSlateText,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = elapsedText?.let { "Last duration $it" } ?: "Awaiting baseline session.",
            color = MutedSlateText,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun TelemetryGrid(
    snapshot: ResourceSnapshot?,
    sampleCount: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TelemetryCard(
                label = "Available RAM",
                value = snapshot?.let { formatBytes(it.availableMemoryBytes) } ?: "--",
                accentColor = TelemetryCyan,
                modifier = Modifier.weight(1f)
            )
            TelemetryCard(
                label = "Used RAM",
                value = snapshot?.let { formatBytes(usedMemoryBytes(it)) } ?: "--",
                accentColor = WarningAmber,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TelemetryCard(
                label = "Battery",
                value = snapshot?.let { formatBatteryPercent(it.batteryPercent) } ?: "--",
                secondaryValue = "Temp unavailable",
                accentColor = batteryAccent(snapshot),
                modifier = Modifier.weight(1f)
            )
            TelemetryCard(
                label = "App network RX/TX",
                value = snapshot?.let {
                    "${formatOptionalBytes(it.totalRxBytes)} / ${formatOptionalBytes(it.totalTxBytes)}"
                } ?: "--",
                secondaryValue = "Device-level source",
                accentColor = TelemetryCyan,
                modifier = Modifier.weight(1f)
            )
        }
        TelemetryCard(
            label = "Samples",
            value = sampleCount.toString(),
            secondaryValue = "Start/stop snapshots",
            accentColor = SuccessGreen,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun TelemetryCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    secondaryValue: String? = null,
    accentColor: Color = TelemetryCyan
) {
    Card(
        modifier = modifier
            .height(96.dp)
            .border(1.dp, accentColor.copy(alpha = 0.35f), MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(containerColor = PanelSlate),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = label.uppercase(),
                    color = MutedSlateText,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    text = value,
                    color = PrimaryText,
                    style = MaterialTheme.typography.titleLarge,
                    fontFamily = FontFamily.Monospace
                )
                secondaryValue?.let {
                    Text(
                        text = it,
                        color = MutedSlateText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            PulseLine(
                color = accentColor,
                modifier = Modifier
                    .width(54.dp)
                    .height(28.dp)
                    .semantics { contentDescription = "$label telemetry pulse" }
            )
        }
    }
}

@Composable
fun PulseLine(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val h = size.height
        val w = size.width
        val path = Path().apply {
            moveTo(0f, h * 0.58f)
            lineTo(w * 0.18f, h * 0.58f)
            lineTo(w * 0.28f, h * 0.38f)
            lineTo(w * 0.42f, h * 0.70f)
            lineTo(w * 0.58f, h * 0.30f)
            lineTo(w * 0.76f, h * 0.58f)
            lineTo(w, h * 0.48f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
fun ControlPanel(
    isMonitoring: Boolean,
    hasCompletedSession: Boolean,
    isBusy: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = onStart,
                enabled = !isMonitoring && !isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = TelemetryCyan,
                    contentColor = DeepSlate,
                    disabledContainerColor = PanelSlateElevated,
                    disabledContentColor = MutedSlateText
                ),
                modifier = Modifier
                    .height(52.dp)
                    .weight(1f)
            ) {
                Text(text = "Start Monitoring")
            }
            Button(
                onClick = onStop,
                enabled = isMonitoring && !isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CriticalRed,
                    contentColor = PrimaryText,
                    disabledContainerColor = PanelSlateElevated,
                    disabledContentColor = MutedSlateText
                ),
                modifier = Modifier
                    .height(52.dp)
                    .weight(1f)
            ) {
                Text(text = "Stop Monitoring")
            }
        }
        OutlinedButton(
            onClick = onExport,
            enabled = hasCompletedSession && !isBusy,
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = TelemetryCyan,
                disabledContentColor = MutedSlateText
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .border(
                    1.dp,
                    if (hasCompletedSession && !isBusy) TelemetryCyan else MutedSlateText.copy(alpha = 0.4f),
                    MaterialTheme.shapes.medium
                )
        ) {
            Text(text = "Generate Report")
        }
    }
}

@Composable
fun SessionReview(
    summary: SessionSummary,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DiagnosticCard(
            title = "Latest Session",
            accentColor = TelemetryCyan
        ) {
            Text(
                text = "Session ${summary.id} ran for ${formatDuration(summary.durationMillis)}.",
                color = PrimaryText,
                style = MaterialTheme.typography.bodyMedium
            )
            SnapshotDetails(
                title = "Start snapshot",
                snapshot = summary.startSnapshot
            )
            SnapshotDetails(
                title = "Stop snapshot",
                snapshot = summary.stopSnapshot
            )
            SessionDeltaDetails(summary = summary)
        }
        GeneratedSummaryDetails(summary = generateSessionSummary(summary))
    }
}

@Composable
fun GeneratedSummaryDetails(
    summary: GeneratedSessionSummary,
    modifier: Modifier = Modifier
) {
    DiagnosticCard(
        title = summary.title,
        accentColor = SuccessGreen,
        modifier = modifier
    ) {
        summary.lines.forEach { line ->
            Text(
                text = line,
                color = PrimaryText,
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
            color = TelemetryCyan,
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = "Memory available: ${formatBytes(snapshot.availableMemoryBytes)} of ${formatBytes(snapshot.totalMemoryBytes)}",
            color = PrimaryText,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Battery: ${formatBatteryPercent(snapshot.batteryPercent)}",
            color = PrimaryText,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Network RX/TX: ${formatOptionalBytes(snapshot.totalRxBytes)} / ${formatOptionalBytes(snapshot.totalTxBytes)}",
            color = PrimaryText,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Low memory: ${if (snapshot.lowMemory) "Yes" else "No"}",
            color = PrimaryText,
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
            color = TelemetryCyan,
            style = MaterialTheme.typography.titleSmall
        )
        Text(
            text = "Available memory: ${formatSignedBytes(memoryDelta)}",
            color = PrimaryText,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Battery: ${batteryDelta?.let { "$it%" } ?: "Unavailable"}",
            color = PrimaryText,
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Network RX/TX: ${formatOptionalBytes(rxDelta)} / ${formatOptionalBytes(txDelta)}",
            color = PrimaryText,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
fun DiagnosticCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    accentColor: Color = TelemetryCyan,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, accentColor.copy(alpha = 0.30f), MaterialTheme.shapes.medium),
        colors = CardDefaults.cardColors(containerColor = PanelSlate),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            title?.let {
                Text(
                    text = it.uppercase(),
                    color = MutedSlateText,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            content()
        }
    }
}

fun usedMemoryBytes(snapshot: ResourceSnapshot): Long {
    return (snapshot.totalMemoryBytes - snapshot.availableMemoryBytes).coerceAtLeast(0L)
}

fun batteryAccent(snapshot: ResourceSnapshot?): Color {
    val percent = snapshot?.batteryPercent ?: return MutedSlateText
    return when {
        percent <= 15 -> CriticalRed
        percent <= 30 -> WarningAmber
        else -> SuccessGreen
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
            sessionLabel = "No AnyConnect",
            onSessionLabelChange = {},
            exportMessage = null,
            onStart = {},
            onStop = {},
            onExport = {}
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
