# VellumLedger — Offline-first Finance Tracker

> **Durable, offline-first personal finance tracking with real-time cloud synchronization.**

VellumLedger is a premium expense management application built with Kotlin Multiplatform. It prioritizes data integrity and user privacy by using an **offline-first architecture**. All data is persisted locally via SQLDelight before being synchronized with a cloud backend via a custom push-based protocol.

## 🏗 Architecture (MVVM + Repository Pattern)
The project follows a clean, decoupled architecture optimized for Kotlin Multiplatform:

- **UI Layer (Compose Multiplatform):** Reactive UI built with Material 3. Screens observe state from ViewModels via `StateFlow`.
- **ViewModel Layer:** Handles UI logic and manages state transitions. Communicates only with the Repository layer.
- **Repository Layer:** Acts as a single source of truth. Orchestrates data flow between the local database and the remote sync engine.
- **Sync Engine (Ktor):** A robust synchronization layer that handles background processing, JWT authentication, and network resilience.
- **Data Layer (SQLDelight):** High-performance local persistence with platform-specific drivers (Android/iOS).

## 🧠 Synchronization Protocol
VellumLedger treat the network as an enhancement, not a dependency:
1. **Local-First Writes:** Transactions are immediately saved locally with a `PENDING` status.
2. **Sync Queue:** Mutations are enqueued. A `SyncWorker` processes them in the background.
3. **DTO Mapping:** Domain models are mapped to `NetworkTransaction` DTOs, ensuring strict backend contract compliance.
4. **Resiliency:** Implements exponential backoff and manual retry mechanisms for failed syncs.

## 🛠 Tech Stack
- **Mobile:** Kotlin Multiplatform, Compose Multiplatform, SQLDelight, Ktor Client.
- **Backend:** Ktor Server, Exposed ORM, PostgreSQL (Railway).
- **Security:** JWT (JSON Web Tokens) Authentication.

## 🧪 Proof of Work
### API Health Check
```bash
curl https://vellum-ledger-api-production.up.railway.app/health
```

### Sync Verification
The system uses a strict push protocol:
- **Authorization**: Bearer JWT included in all sync requests.
- **Contract**: Multi-transaction push support via `PushRequest` wrapper.
- **Status Tracking**: Full lifecycle tracking: `PENDING → SYNCING → SYNCED | FAILED`.

## 🏗 Project Structure
```
VellumLedger/
├── composeApp/
│   ├── commonMain/
│   │   ├── sync/           # Ktor API, Network DTOs, UserSession
│   │   ├── repository/     # LedgerRepository (Data Orchestration)
│   │   ├── database/       # SQLDelight & LedgerDatabase implementation
│   │   └── ui/             # Screens, ViewModels, Theme
│   ├── androidMain/        # Android Drivers & Biometrics
│   └── iosMain/            # iOS Native Drivers
└── server/                 # Ktor + PostgreSQL Backend
```

---
*Built with ❤️ by Rudra Dave*