# Android-Logs

[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.0-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg?logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![AGP](https://img.shields.io/badge/AGP-8.9.0-green.svg?logo=android)](https://developer.android.com/studio/releases/gradle-plugin)
[![Gradle](https://img.shields.io/badge/Gradle-8.14.3-02303A.svg?logo=gradle)](https://gradle.org)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26-orange.svg)](https://developer.android.com)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-35-brightgreen.svg)](https://developer.android.com)
[![Java](https://img.shields.io/badge/Java-17-red.svg?logo=openjdk)](https://openjdk.org)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)
[![Android CI](https://github.com/xRahul/Android-Logs/actions/workflows/android.yml/badge.svg)](https://github.com/xRahul/Android-Logs/actions/workflows/android.yml)

**Android-Logs** is a real-time Android device telemetry and event logging system built with modern Android development (MAD) practices. It captures critical OS broadcasts, screen state changes, battery events, Wi-Fi scans, device authentication attempts, and background location coordinates with sub-millisecond full-text search indexing, seamless backward compatibility, and strict Android 14/15 privacy compliance.

---

## Key Features

- **Continuous System Telemetry**: Captures real-time device lifecycle events (screen on/off, battery changes, connectivity status, Wi-Fi network scans, lock/unlock cycles, location coordinates).
- **Sub-Millisecond Search (SQLite FTS)**: Powered by Room 2.6 with SQLite FTS virtual table indexing for instant, zero-lag full-text search across thousands of log records.
- **Infinite Smooth Scrolling**: Integrated with AndroidX Paging 3 to paginate large datasets efficiently with minimal memory overhead.
- **Modern Jetpack Compose Material 3 UI**: Clean, responsive, edge-to-edge UI featuring filter chips, live service control cards, collapsible payload viewers, and floating quick-navigation controls.
- **Tap-to-Clipboard & SAF Export**: Copy formatted log payloads to clipboard with a single tap, or export entire datasets via Android Storage Access Framework (SAF).
- **100% Backward-Compatible File Sync**: Simultaneously maintains legacy text and CSV log formats (`allLogs.txt`, `allActions.csv`, `location.csv`, `passwordAttempts.csv`, `deviceUsed.csv`, `wifi.csv`) asynchronously without blocking the UI.
- **Privacy & Security by Design**: Automatic redaction of sensitive credentials, passwords, PINs, and auth tokens via `PiiSanitizer`, paired with Android 14+ `RECEIVER_NOT_EXPORTED` broadcast isolation.

---

## Architecture Overview

Android-Logs follows **Clean Architecture** and **MVI / MVVM** patterns, strictly separating concerns across presentation, domain, data, and background service layers.

```mermaid
graph TD
    subgraph System Broadcasts & OS Triggers
        B1[Dynamic Broadcasts: Screen, Battery, Connectivity, Wi-Fi] -->|Intent| TS[TelemetryService / DynamicReceiver]
        B2[Manifest Broadcasts: Boot, Device Admin] -->|Intent| AR[AllReceivers]
        B3[Device Unlock / Dialog Triggers] -->|Work Request| LW[LogLocationWorker]
    end

    subgraph Core Telemetry & Domain
        TS -->|Log Event| LR[LogRepository]
        AR -->|Log Event| LR
        LW -->|Location Event| LR
        LR -->|Sanitize Sensitive Fields| PS[PiiSanitizer]
        LR -->|Format for Display/Export| LF[LogFormatter]
    end

    subgraph Storage & Data Layer
        LR -->|Asynchronous Room Insert| DB[(Room Database + SQLite FTS)]
        LR -->|Non-Blocking Coroutine Sync| FW[LegacyFileWriter]
        FW -->|Append Text/CSV| FS[External Files: AllLogs/]
        LR -->|Stream Document Export| EX[LogExporter / SAF]
    end

    subgraph Presentation Layer (Jetpack Compose Material 3)
        DB -->|PagingSource Flow| VM[MainViewModel]
        VM -->|PagingData + StateFlow| UI[LogListScreen / Compose M3]
        UI -->|Search / Filter Queries| VM
        UI -->|Service Start/Stop Toggle| TS
        UI -->|Tap Event| CB[System Clipboard]
    end
```

### Layer Breakdown

1. **Presentation Layer (`in.rahulja.getlogs.ui`)**:
   - **Jetpack Compose Material 3**: `MainActivity`, `LogListScreen`, `LogItemCard`, `ServiceControlCard`, and `PermissionRequestDialog`.
   - **`MainViewModel`**: Manages UI state with Kotlin `StateFlow` and handles debounced search queries streaming `Flow<PagingData<LogEntity>>`.
   - **Theme**: Dynamic Material 3 color schemes, typography, and shape theming (`Theme.kt`, `Color.kt`, `Type.kt`).

2. **Domain Layer (`in.rahulja.getlogs.model`, `in.rahulja.getlogs.data`, `in.rahulja.getlogs.util`)**:
   - **`PiiSanitizer`**: Deep recursive JSON sanitizer that redacts credentials, passwords, secrets, and auth tokens.
   - **`LogFormatter`**: Human-readable timestamp, action, and JSON payload formatting for display, clipboard, and export.
   - **Models**: `LogEntity`, `LogFtsEntity` (FTS virtual table entity), and `LogType` categorization enum.

3. **Data Layer (`in.rahulja.getlogs.data`, `in.rahulja.getlogs.util`)**:
   - **`LogDatabase` & `LogDao`**: Room SQLite database with FTS indexing, offering sub-millisecond full-text queries and PagingSource integration.
   - **`LogRepository`**: Single source of truth coordinating database persistence, PII sanitization, and legacy file synchronization.
   - **`LegacyFileWriter`**: Thread-safe, non-blocking coroutine writer preserving legacy `.txt` and `.csv` files in scoped external storage (`AllLogs/`).
   - **`LogExporter`**: Streams full log history to user-selected URIs via Storage Access Framework (`ACTION_CREATE_DOCUMENT`).

4. **Service & Background Layer (`in.rahulja.getlogs.service`, `in.rahulja.getlogs.receiver`)**:
   - **`TelemetryService`**: Android 14/15 compliant Foreground Service managing runtime-registered system broadcast receivers with `RECEIVER_NOT_EXPORTED`.
   - **`DynamicReceiver`**: Captures screen status, battery levels, network changes, and Wi-Fi scans.
   - **`LogLocationWorker`**: AndroidX `CoroutineWorker` querying Google Play Services `FusedLocationProviderClient` with graceful permission fallback.
   - **`AllReceivers`**: Device Admin and system event broadcast receiver.

---

## Room SQLite FTS vs Legacy File Storage Benchmark

Android-Logs includes automated benchmark tests (`RoomVsFileBenchmarkTest.kt`) comparing Room SQLite FTS indexing with sequential file scanning (`grep`):

| Operation | Room Database + SQLite FTS | Legacy File I/O (`allLogs.txt`) | Advantage |
| :--- | :--- | :--- | :--- |
| **Search (500 entries)** | **~1-3 ms** (Indexed B-Tree / FTS) | **~12-25 ms** (Sequential file scan) | **10x Faster** |
| **Paging / Memory** | O(Page Size) with Paging 3 | O(File Size) reverse seek / file read | **Zero OOM Risk** |
| **Filtering & Sorting** | Structured SQL queries | Full-file regex parsing | **Reliable & Typed** |
| **Data Integrity** | ACID compliant SQLite transactions | Susceptible to partial write truncation | **Crash Resilient** |

---

## Security & Android 14/15 Compliance

- **Scoped Storage & SAF**: Replaced legacy external storage permissions (`WRITE_EXTERNAL_STORAGE`) with Scoped Storage (`Context.getExternalFilesDir`) and the Android Storage Access Framework.
- **Dynamic Broadcast Isolation**: Explicitly registers receivers with `Context.RECEIVER_NOT_EXPORTED` on API 26+.
- **Manifest Security**: All manifest receivers enforce `android:exported="false"` unless required for OS device administration.
- **Runtime Permissions**: Transparent onboarding and request flows for `POST_NOTIFICATIONS` (Android 13+), `ACCESS_FINE_LOCATION`, and `ACCESS_COARSE_LOCATION`.
- **PII Redaction**: All sensitive key-value pairs (e.g. `password`, `pin`, `token`, `secret`, `credential`) are automatically masked with `[REDACTED]` before writing to disk or database.

---

## Build & Test Instructions

### Prerequisites
- **JDK**: Java 17 (e.g. Eclipse Temurin 17)
- **Android SDK**: API 35 (Compile / Target SDK: 35, Min SDK: 26)
- **Gradle**: 8.14.3 (Wrapper included)

### Commands

```bash
# Run all unit tests and Robolectric test suites
./gradlew testDebugUnitTest

# Run static code analysis with Detekt
./gradlew detekt

# Assemble Debug APK
./gradlew assembleDebug

# Run full verification suite (linting, tests, and checks)
./gradlew check testDebugUnitTest detekt assembleDebug
```

---

## Continuous Integration (CI/CD)

The repository uses GitHub Actions (`.github/workflows/android.yml`) for automated continuous integration on every push and pull request to `main` and `master`:
1. Checks out repository with Git LFS support.
2. Sets up JDK 17 with Gradle dependency caching.
3. Executes Detekt static code analysis.
4. Executes the complete unit & Robolectric test suite.
5. Assembles the debug APK to ensure compilation integrity.

---

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.
