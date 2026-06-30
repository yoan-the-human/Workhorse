# Workhorse - Time Balance Tracker

Workhorse is a sophisticated Android application designed for professionals who want to maintain a precise balance of their working hours. Built with a focus on modern UI and data-driven insights, Workhorse helps you track daily work shifts, break durations, and long-term consistency through a high-end dashboard.

## 🚀 Features

- **Precise Time Tracking**: Start and end your workday with a single tap. Track "Away" time (breaks) with a real-time ticker to ensure you stay within your targets.
- **Dynamic Time Balance**: Instantly see your "Current Balance" – a cumulative measure of your time debt or overwork across your entire history.
- **Advanced Analytics**:
  - **KPI Cards**: Monitor average offsets for Morning starts, Evening finishes, and Middle breaks.
  - **Consistency Heatmap**: A GitHub-style 228-day grid map visualizing your daily performance (Wins vs. Debt).
  - **Streak System**: Track your longest and current streaks for starting early, staying late, and managing breaks.
- **Interactive Data Visualization**: Integrated trend charts to visualize your work habits over time.
- **Robust Data Management**:
  - **JSON Backup/Restore**: Easily export your data to a JSON file or share it via the Android share sheet.
  - **Manual Entry**: Edit or add historical records to ensure your data is always accurate.
- **Modern Dark UI**: A sophisticated, dark-themed interface built entirely with Jetpack Compose, featuring smooth animations and glowing accents.

## 🛠 Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose)
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- **Concurrency**: Kotlin Coroutines & Flow
- **Design System**: Material Design 3

## 📦 Getting Started

### Prerequisites
- Android Studio Ladybug | 2024.2.1 or newer
- JDK 17
- Android SDK 34+

### Installation
1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/workhorse.git
   ```
2. Open the project in Android Studio.
3. Sync Project with Gradle Files.
4. Run the app on an emulator or a physical device.

## 💾 Backup & Restore
Workhorse allows you to keep your data safe through its built-in JSON utility:
- **Export**: Share your backup directly from the Dashboard.
- **Import**: Restore from a `.json` file or paste raw JSON text directly into the app.

## 📜 License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---
*Built for those who work hard and track harder.*
