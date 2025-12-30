# WellSpend 💰

**WellSpend** is a comprehensive, open-source personal finance manager for Android, built with **Kotlin** and **Jetpack Compose**. It is designed to be modern, intuitive, and privacy-focused, keeping all your financial data securely on your device.

Track expenses, manage multiple accounts, set budgets, and gain insights into your spending habits—all in one beautiful app.

---

## ✨ Features

### 🏦 Account Management
*   **Multiple Accounts**: Create and manage multiple accounts (e.g., Cash, Bank, Mobile Wallet).
*   **Balance Tracking**: Automatically tracking of current balances based on income and expenses.
*   **Transfers**: Seamlessly transfer money between accounts.
*   **Safe Deletion**: Smart handling of deleted accounts—orphan transactions are preserved and labeled.

### 💸 Expense & Income Tracking
*   **Transaction Types**: Support for **Expenses**, **Income**, and **Transfers**.
*   **Recurring Transactions**: Set up automatic weekly or monthly recurring bills and salary.
*   **Transaction Fees**: Track extra fees separately from the base amount (e.g., ATM fees, transfer charges).
*   **Smart Categorization**: Extensive list of built-in categories with color-coded icons.
*   **Swipe Actions**: Quickly **Edit** or **Delete** transactions and accounts with simple swipe gestures.

### 📊 Budgets & Analytics
*   **Budgeting**: Set monthly spending limits for each category.
*   **Visual Indicators**: Real-time progress bars show how close you are to your limit (turning red when exceeded).
*   **Donut Chart**: Beautiful visualization of your monthly spending breakdown.
*   **Monthly Reports**: detailed summary comparing your spending to the previous month.

### 🎨 Modern UI/UX
*   **Material 3 Design**: Fully compliant with the latest Material Design guidelines.
*   **Dark & Light Themes**: Native support for dark mode, with a curated slate-based palette.
*   **Dynamic Color**: Supports Android 12+ wallpaper-based theming (optional).
*   **Edge-to-Edge**: Immersive design with matching system navigation bars.

### 💾 Data & Privacy
*   **Offline First**: 100% local storage using **Room Database**. No internet required, no data collection.
*   **Backup & Restore**: Export your data to a secure JSON file and restore it anytime.
    *   *Backward compatible with older backup files.*

---

## 🛠️ Tech Stack

*   **Language**: [Kotlin](https://kotlinlang.org/)
*   **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
*   **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern
*   **Local Database**: [Room](https://developer.android.com/training/data-storage/room) (SQLite)
*   **Concurrency**: Coroutines & Flow
*   **Navigation**: Compose Navigation with Custom Bottom Bar
*   **Serialization**: Gson

---

## 🚀 Getting Started

1.  **Clone the repository**:
    ```bash
    git clone https://github.com/sfsakhawat999/WellSpend.git
    ```
2.  **Open in Android Studio** (Hedgehog or later recommended).
3.  **Sync Gradle** to download dependencies.
4.  **Run** the app on an emulator or physical device (Min SDK 26 / Android 8.0+).

---

## 🤝 Contributing

Contributions are welcome! If you have ideas for new features or bug fixes:
1.  Fork the repository.
2.  Create a new branch (`git checkout -b feature/AmazingFeature`).
3.  Commit your changes (`git commit -m 'Add some AmazingFeature'`).
4.  Push to the branch (`git push origin feature/AmazingFeature`).
5.  Open a Pull Request.

---

## 📄 License

This project is licensed for personal and educational use. Feel free to explore, learn, and modify it for your own needs. 
