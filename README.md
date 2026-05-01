# VellumLedger

VellumLedger is a modern, privacy-focused personal finance and expense tracking application built with **Kotlin Multiplatform** and **Compose Multiplatform**. It allows users to manage their daily transactions, track multiple cards/accounts, and visualize their financial health with detailed analytics across both Android and iOS platforms.

## ✨ Features

*   **Comprehensive Dashboard**: Real-time overview of your total balance, income, and expenses.
*   **Transaction Management**: Easily add, categorize, and track your daily spending and earnings.
*   **Card & Account Tracking**: Manage multiple payment methods with custom colors and balances.
*   **Financial Analytics**: Visual charts and generated reports to help you understand your spending patterns.
*   **Data Export**: Export your transaction history to CSV for external record-keeping.
*   **Dark Mode Support**: A beautiful, theme-aware UI that adapts to your system preferences.
*   **Multi-Currency**: Support for different currency symbols tailored to your region.
*   **Offline First**: Built with SQLDelight for robust local storage, ensuring your data is always accessible.

## 🛠 Tech Stack

-   **Language**: [Kotlin](https://kotlinlang.org/)
-   **UI Framework**: [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)
-   **Architecture**: MVVM (Model-View-ViewModel) with shared logic in `commonMain`.
-   **Database**: [SQLDelight](https://cashapp.github.io/sqldelight/) for local persistence.
-   **Concurrency**: [Kotlinx Coroutines](https://github.com/Kotlin/kotlinx.coroutines).
-   **Date/Time**: [Kotlinx Datetime](https://github.com/Kotlin/kotlinx-datetime).
-   **Dependency Injection/ViewModel**: AndroidX Lifecycle ViewModel (Compose Multiplatform compatible).

## 📁 Project Structure

*   `/composeApp`: Shared UI and logic.
    *   `commonMain`: Core business logic, ViewModels, database schemas, and Compose UI.
    *   `androidMain`: Android-specific drivers and platform implementations.
    *   `iosMain`: iOS-specific drivers and platform implementations.
*   `/iosApp`: The native iOS entry point (SwiftUI wrapper).

## 🚀 Getting Started

### Prerequisites
-   Android Studio (latest version)
-   Xcode (for iOS development)
-   JDK 17 or higher

### Build and Run

#### Android
1.  Open the project in Android Studio.
2.  Select the `composeApp` run configuration.
3.  Click **Run**.
Alternatively, via terminal:
```bash
./gradlew :composeApp:assembleDebug
```

#### iOS
1.  Open the `iosApp/iosApp.xcworkspace` in Xcode.
2.  Select your target device or simulator.
3.  Click **Run**.
*Note: You can also run the iOS app directly from Android Studio using the Kotlin Multiplatform Mobile plugin.*

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.
