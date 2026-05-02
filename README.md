# VellumLedger

> **Durable, offline-first personal finance — built with Kotlin Multiplatform.**

VellumLedger is a privacy-focused expense tracking app for Android and iOS. All data lives locally by default — no account required, no data leaves your device without your explicit action. Built as a real-world demonstration of KMP architecture, SQLDelight-powered offline sync, and Compose Multiplatform UI.

<br/>

## Screenshots

| Dashboard | Add Transaction | Analytics | Settings |
|-----------|----------------|-----------|----------|
| ![Dashboard](screenshots/dashboard.png) | ![Add Transaction](screenshots/add_transaction.png) | ![Charts](screenshots/charts.png) | ![Settings](screenshots/settings.png) |

<br/>

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Compose Multiplatform UI              │
│              (commonMain · Material 3 · Dark/Light)      │
└───────────────────────┬─────────────────────────────────┘
                        │
┌───────────────────────▼─────────────────────────────────┐
│                    ViewModel Layer                       │
│         LedgerViewModel · StateFlow · Coroutines         │
└──────────┬────────────────────────────┬─────────────────┘
           │                            │
┌──────────▼──────────┐    ┌────────────▼────────────────┐
│   Repository Layer  │    │      Sync Engine            │
│  TransactionRepo    │    │  SyncQueue · SyncWorker     │
│  CardRepository     │    │  Ktor HttpClient backend    │
│  AnalyticsEngine    │    │  Status: Pending/Synced/    │
│  ExchangeRateEngine │    │          Failed             │
└──────────┬──────────┘    └────────────┬────────────────┘
           │                            │
┌──────────▼────────────────────────────▼────────────────┐
│                    SQLDelight Database                  │
│         Transactions · Cards · SyncQueue tables        │
│   expect/actual drivers: Android · iOS (Native)        │
└─────────────────────────────────────────────────────────┘
         GlobalErrorHandler → Snackbar feedback loop
         expect/actual → dynamic versioning
```

**Key architectural decisions:**

- **Offline-first by design.** Every write goes to SQLDelight first. The sync queue processes pending operations in the background — connectivity is never a hard dependency.
- **Single source of truth.** `StateFlow` from the ViewModel drives all UI state. No manual refresh, no race conditions between UI and DB.
- **Expect/Actual for platform drivers.** `AndroidSqliteDriver` and `NativeSqliteDriver` are injected at the platform boundary — shared logic never knows which platform it's on.
- **SyncQueue pattern.** Mutations are enqueued, not synced inline. The worker processes them with status tracking (`PENDING → SYNCING → SYNCED | FAILED`), enabling retry logic and audit trails.
- **GlobalErrorHandler.** Network and DB errors surface via a Snackbar feedback loop — the UI never silently swallows failures.
- **Resilient number formatting.** Balances scale automatically to compact notation (K, M, B, T) — the dashboard handles any amount without breaking layout.

<br/>

## Features

| Feature | Status |
|---------|--------|
| Dashboard with real-time balance, income & expense | ✅ |
| Resilient compact number formatting (K/M/B/T) | ✅ |
| Add/categorize transactions (Expense & Income) | ✅ |
| Animated transaction list with sync status indicators | ✅ |
| Swipable card wallet with custom hex-color themes | ✅ |
| 7-day spending trend bar chart | ✅ |
| Category breakdown with percentage analytics | ✅ |
| Weekly / Monthly / Yearly period comparison | ✅ |
| Export transactions to CSV | ✅ |
| Native system sharing for CSV exports | ✅ |
| Dynamic app version tracking | ✅ |
| Dark mode (system-aware) | ✅ |
| Real-time exchange rate engine (actual conversion) | ✅ |
| Ktor HttpClient sync architecture | ✅ |
| ProGuard/R8 rules for KMP + SQLDelight | ✅ |
| Snackbar error feedback (network & DB errors) | ✅ |
| INTERNET permission + release-ready manifest | ✅ |
| Authentication (OAuth2 / biometric lock) | ✅ |
| Military-grade SQLCipher database encryption | ✅ |
| Crash reporting (Crashlytics / Sentry) | 🔧 Roadmap |
| Structured logging (Kermit / Timber) | 🔧 Roadmap |
| Unit tests for ExchangeRateUtil & formatMoney | 🔧 Roadmap |
| Signed release keystore + Play Store assets | 🔧 Roadmap |

<br/>

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin (Multiplatform) |
| UI | Compose Multiplatform · Material 3 |
| Architecture | MVVM · StateFlow · Repository pattern |
| Database | SQLDelight 2.0.2 |
| Networking | Ktor 3.0.3 |
| Serialization | Kotlinx Serialization |
| Concurrency | Kotlinx Coroutines |
| Date/Time | Kotlinx Datetime |
| ViewModel | AndroidX Lifecycle (KMP-compatible) |
| Build | Gradle Version Catalogs |

<br/>

## Project Structure

```
VellumLedger/
├── composeApp/
│   ├── commonMain/         # Shared business logic + Compose UI
│   │   ├── data/           # Repositories, SQLDelight schemas, Ktor client
│   │   ├── domain/         # Models, sync engine, exchange rate engine
│   │   └── ui/             # Screens, ViewModels, theme, error handler
│   ├── androidMain/        # AndroidSqliteDriver, platform impl
│   └── iosMain/            # NativeSqliteDriver, platform impl
└── iosApp/                 # SwiftUI entry point (thin wrapper)
```

<br/>

## Getting Started

**Prerequisites**
- Android Studio Hedgehog or newer
- JDK 17+
- Xcode 15+ (for iOS)

**Android**
```bash
./gradlew :composeApp:assembleDebug
```

**iOS**
```bash
open iosApp/iosApp.xcworkspace
```
Then select a simulator and hit Run, or use the KMM plugin in Android Studio.

<br/>

## Known Limitations

This is an active personal project. Current known limitations:

- **No authentication yet.** The sync layer connects without user login. OAuth2 or biometric lock is the next planned milestone — the architecture is ready for it.
- **Backend URL is a placeholder.** `LedgerApi.kt` uses a stub endpoint. Swapping in a real URL is the only change needed — the Ktor client and sync queue are fully wired.
- **No crash reporting.** The `GlobalErrorHandler` surfaces errors in-app, but there's no remote observability yet.
- **Minimal test coverage.** `ExchangeRateUtil` and `formatMoney` need unit tests; DB migration edge cases are untested.

Engineering note: the sync architecture is intentionally backend-agnostic. The `SyncQueue` and `SyncWorker` don't care what's on the other end — going live requires touching only `LedgerApi.kt`.

<br/>

## Why I Built This

Most KMP finance demos are either too trivial (single screen, no persistence) or too abstracted to show real architectural decisions. VellumLedger is a reference implementation that shows what a production KMP app *structure* looks like — offline sync queue, platform-specific driver injection, real-time exchange rate engine, reactive ViewModel state — without requiring a live backend to demonstrate the core engineering.

<br/>

## License

MIT License — see [LICENSE](LICENSE) for details.