# A.R.M.

Android Resource Monitor

## Purpose

A.R.M. is a lightweight Android telemetry and baseline collection utility for objectively comparing device resource utilization before and after software installation or configuration changes.

The initial business objective is to determine whether Cisco AnyConnect materially impacts tablet performance, memory utilization, battery consumption, or network behavior on Android 11-14 store devices.

## Current Status

Project initialized with manual session management, resource snapshots, SQLite persistence, latest-session summary generation, local text report export, and a store-device validation matrix. A store engineer can launch A.R.M., start a baseline session, stop it, reopen the app, review a plain-language summary, and export the latest completed session as a `.txt` report.

## Current Slice

Completed slice: Store-device validation matrix and procedure.

This slice intentionally avoids claiming Android 11-14 validation results without connected Android 11-14 store-device targets. The repo now includes a repeatable validation matrix and procedure for the complete baseline and post-AnyConnect workflow, plus recorded smoke evidence from the connected Android 16 device available during this slice.

## Completed Deliverables

- Android application project named A.R.M.
- Application subtitle documented as Android Resource Monitor.
- Package, namespace, and application id set to `net.noblesite.arm`.
- Starter app screen identifies the app as A.R.M. / Android Resource Monitor.
- README established as the source of truth for scope, decisions, deliverables, validation, build instructions, and test procedure.
- Manual start/stop session flow.
- In-memory active session state.
- In-memory last session summary with duration.
- Unit tests for session start, duplicate start prevention, stop summary creation, and duration formatting.
- Android SDK resource snapshot collection at session start and stop.
- Memory snapshot fields: available memory, total memory, and low-memory status.
- Battery snapshot fields: battery percent and charging status.
- Network snapshot fields: device total RX bytes and total TX bytes when supported by the platform.
- Start/stop snapshot display and session delta display.
- Unit tests for snapshot attachment and memory, battery, and network deltas.
- Room-backed SQLite database named `arm.db`.
- `sessions` table for session id, start time, and optional end time.
- `resource_snapshots` table for start/stop memory, battery, and network readings.
- Repository-backed session start/stop flow that persists sessions and snapshots.
- Startup reload of an active session or latest completed session from SQLite.
- Unit tests for persistence model mapping.
- Latest completed session summary generation.
- Summary lines for duration, available-memory change, battery change, network RX/TX change, and low-memory observation.
- On-screen generated summary display below raw snapshot details.
- Unit tests for summary generation and signed percent formatting.
- Text report generation for the latest completed session.
- Local text report export to `Downloads/A.R.M.` using MediaStore.
- Export confirmation message in the app.
- Unit tests for report filename and report body content.
- Store-device validation matrix in `STORE_VALIDATION.md`.
- Complete manual validation procedure for Android 11-14 targets.
- Pass criteria and failure-capture fields for store testing.
- Connected instrumentation smoke test passed on SM-S928U1 running Android 16.

## Remaining Deliverables

- Android 11-14 store-device validation.

## Decision Log

- Use package name `net.noblesite.arm` to match the requested project identity.
- Use Gradle root project name `ARM` because Gradle project names cannot end with a period; keep user-facing app and README branding as A.R.M.
- Pin AndroidX dependencies to versions compatible with Android Gradle Plugin 8.13.2 and compile SDK 36 instead of advancing the whole toolchain to API 37 during the identity slice.
- Keep the app as a single Android application module unless a future requirement absolutely justifies more structure.
- Prefer Android SDK APIs, Room, Kotlin Coroutines, MediaStore, and a Foreground Service when their slices require them.
- Avoid analytics platforms, cloud services, remote APIs, dependency injection frameworks, multi-module architecture, and enterprise abstractions.
- Treat README.md as the source of truth after every vertical slice.
- Complete only one vertical slice at a time and recommend exactly one next slice.
- Keep the first slice focused on identity and project charter so future implementation decisions can be judged against the business objective.
- Keep manual session state in memory for this slice; do not introduce Room until persistence is the active slice.
- Prevent duplicate starts while a session is active so before/after runs have clear boundaries.
- Use the session start timestamp as the temporary session id until durable storage defines stable identifiers.
- Collect resource snapshots only at manual session start and stop for this slice; continuous polling is deferred because the business objective needs comparable baseline reports, not live dashboards.
- Use Android SDK APIs for resource collection: `ActivityManager.MemoryInfo`, `BatteryManager`, sticky battery status intent, and `TrafficStats`.
- Treat unsupported network counters as unavailable instead of failing a session.
- Use Room for local SQLite persistence rather than direct SQL so schema and DAO behavior stay simple and testable.
- Store only local session/snapshot data; do not add cloud sync, remote APIs, authentication, or fleet concepts.
- Keep the latest-session UI as the only read surface for this slice; broader summaries remain a future vertical slice.
- Disable session controls while SQLite writes are in flight so rapid taps cannot create ambiguous session boundaries.
- Generate summaries only from the latest completed session in this slice; multi-session comparison remains deferred until export/report workflows exist.
- Keep summary text plain and deterministic so store engineers can compare before/after runs without interpretation drift.
- Export only the latest completed session in this slice; exporting both baseline and post-install sessions together remains deferred.
- Use MediaStore Downloads for text reports so Android 11-14 devices can save reports locally without broad file permissions.
- Do not add share sheets, cloud upload, or remote destinations for this slice.
- Do not mark Android 11-14 validation complete until real store-device targets are connected and the matrix has been run.
- Keep validation evidence in a plain repo document so store engineers can execute it without external test platforms.
- Record non-target-device smoke results separately from Android 11-14 store-device validation results.

## Recommended Next Slice

Run the validation matrix on a connected Android 11 store-device target.

## Future Ideas

- Remote telemetry.
- Cloud synchronization.
- Real-time dashboards.
- Historical trend analytics.
- Device fleet management.
- MDM integration.
- Crash analytics.
- VPN diagnostics.
- Grafana integration.
- Web portals.
- AI analysis.
- User authentication.

## Explicitly Deferred Features

- Remote telemetry is deferred because store engineers must be able to compare reports without external tooling.
- Cloud synchronization is deferred because baseline comparison can be performed locally.
- Real-time dashboards are deferred because the current objective is session-based before/after comparison.
- Historical trend analytics are deferred because the initial comparison requires two reports, not long-term fleet history.
- Device fleet management is deferred because A.R.M. targets local store-device testing.
- MDM integration is deferred because configuration distribution is outside the initial objective.
- Crash analytics is deferred because it does not directly measure Cisco AnyConnect resource impact.
- VPN diagnostics are deferred because the project measures resource impact, not VPN behavior internals.
- Grafana integration is deferred because it requires external infrastructure.
- Web portals are deferred because reporting must be available on-device.
- AI analysis is deferred because objective measurements and summaries come first.
- User authentication is deferred because the first use case is local testing by a store engineer.

## Business Objective Validation

How does this support the business objective?

This slice gives store engineers a repeatable validation procedure for proving A.R.M. can capture and export baseline and post-AnyConnect reports on Android 11-14 targets.

Can the feature be validated in store testing?

Partially in this environment. The checklist can be reviewed now, and `connectedDebugAndroidTest` passed on a connected Android 16 SM-S928U1. Full validation still requires connected Android 11-14 store-device targets.

Does this improve baseline comparison accuracy?

Yes. A fixed validation matrix improves baseline comparison accuracy by making every target device follow the same start, stop, persistence, summary, export, and report-comparison steps.

## Build Instructions

From the repository root:

```sh
./gradlew assembleDebug
```

The debug APK is generated under `app/build/outputs/apk/debug/`.

## Test Procedure

Run local unit tests:

```sh
./gradlew testDebugUnitTest
```

Run the debug build:

```sh
./gradlew assembleDebug
```

Run Android instrumented tests on a connected device or emulator:

```sh
./gradlew connectedDebugAndroidTest
```

Run store-device validation:

```sh
./gradlew assembleDebug
adb devices -l
```

Install the generated APK on each Android 11-14 target, then execute `STORE_VALIDATION.md`.

For this slice, verify that:

- The app installs as `net.noblesite.arm`.
- The launcher label is A.R.M.
- The first screen shows A.R.M. and Android Resource Monitor.
- Start is enabled before a session begins.
- Stop is disabled before a session begins.
- After tapping Start, the app shows Session running, disables Start, and enables Stop.
- After tapping Stop, the app shows No session running and displays the last session duration.
- A running session displays the start memory, battery, and network snapshot.
- A stopped session displays start and stop snapshots.
- A stopped session displays available memory, battery, and network RX/TX changes.
- If the app is closed and reopened during a running session, the active session and start snapshot return.
- If the app is closed and reopened after stopping a session, the latest completed session returns.
- Start and Stop controls are disabled while a session write is in progress.
- A stopped session displays a generated latest-session summary.
- The generated summary includes duration, available-memory change, battery change, network RX/TX change, and low-memory observation.
- A stopped session enables Export Report.
- Tapping Export Report writes `arm-session-<session id>.txt` under Downloads/A.R.M.
- The exported report includes the generated summary, start snapshot, stop snapshot, and session change details.
- Execute the `STORE_VALIDATION.md` matrix on Android 11, Android 12, Android 13, and Android 14 targets.
- Treat Android 16 smoke evidence as useful build/device sanity only, not as Android 11-14 validation completion.
