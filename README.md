# WellSpend: Personal Finance Manager

WellSpend is a modern, native Android application for personal finance management, built with Kotlin and Jetpack Compose. It offers a clean, intuitive interface to track expenses, set budgets, and visualize spending habits.

## Features

### 📊 Dashboard & Visualization

- **Donut Chart**: Visual breakdown of monthly spending by category.
- **Budget Progress**: Real-time progress bars for each category budget.
- **Monthly Summary**: Quick view of total spending vs. budget limits.

### 💸 Expense Tracking

- **Easy Entry**: Add expenses quickly with amount, description, category, and date.
- **Recurring Expenses**: Support for automatic weekly and monthly recurring expenses.
- **Categorization**: Extensive list of categories (Food, Transport, Utilities, etc.) with color-coded icons.
- **History**: Scrollable list of expenses grouped by date.

### ⚙️ Budget Management

- **Category Limits**: Set specific monthly spending limits for each category.
- **Visual Alerts**: Progress bars turn red when you exceed your budget.

### 📈 Reports

- **Monthly Report**: Detailed summary of spending compared to the previous month.
- **Category Breakdown**: Percentage-based breakdown of where your money went.

### 🎨 Customization & Theming

- **Theme Modes**: Switch between Dark, Light, and System Default themes.
- **Dynamic Colors**: Support for Material You (Android 12+) to match your wallpaper.
- **Currency**: Support for major currencies ($, €, £, etc.) and **Custom Currency Symbols**.

### 💾 Data Management

- **Local Storage**: All data is stored securely on your device using Room Database.
- **Backup & Restore**: Export your data to a JSON file and import it back to restore or transfer to another device.

## Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Room (SQLite)
- **Asynchronous**: Coroutines & Flow
- **Serialization**: Gson
- **Build System**: Gradle (Kotlin DSL)

## Setup & Build

1.  Clone the repository.
2.  Open in Android Studio.
3.  Sync Gradle project.
4.  Run on an emulator or physical device (Min SDK 26).

## License

This project is for personal use and educational purposes.
