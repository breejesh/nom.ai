<p align="center">
  <img src="screenshots/logo.png" width="100" height="100" alt="NomAI Logo">
</p>

<h1 align="center">NomAI</h1>
<p align="center"><strong>World's First Free & Open-Source Offline LLM Calorie Tracker</strong></p>

<p align="center">
  <strong>A quiet-luxury, privacy-first, offline-local AI calorie advisor for Android.</strong><br>
  Estimate nutrition, track goals, and analyze metrics fully on-device. No accounts, no data leaks.
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Android-8.0+-4D7C0F?style=flat-square&logo=android" alt="Android">
  <img src="https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=flat-square&logo=kotlin" alt="Kotlin">
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-0891B2?style=flat-square&logo=jetpackcompose" alt="Jetpack Compose">
  <img src="https://img.shields.io/badge/Privacy-100%25%20On--Device-15803D?style=flat-square" alt="Privacy First">
  <img src="https://img.shields.io/badge/License-MIT-059669?style=flat-square" alt="License">
</p>

---

### 🛡️ Why NomAI?

Most calorie tracking apps are bloated, demand monthly subscriptions, show intrusive ads, and sell your private eating habits to third-party ad networks.

**NomAI is the antidote.** It is built on three core pillars:
- 💡 **100% Free & Open-Source (FOSS):** No payment gateways, no locked features, no premium tiers. Free forever.
- 🔒 **Local & Offline-First:** Powered by Google's on-device **Gemma-2B** models. All meal text analysis and vision scanning run entirely inside your phone. No internet connection needed, and zero data ever leaves your device.
- 🔑 **Zero Logins, Zero Tracking:** No sign-up forms, no email collection, no cookies. Just open the app and start tracking.

---

## 📸 Screenshots

> [!TIP]
> Place your actual app mockups inside the `screenshots/` directory using these file names to display them below:

<p align="center">
  <img src="screenshots/today_dashboard.png" width="260" alt="Today Dashboard">
  <img src="screenshots/stats_insights.png" width="260" alt="Stats Insights">
  <img src="screenshots/settings_theme.png" width="260" alt="Settings & Appearance">
</p>

---

## ✨ Key Features

- 🤖 **Local Gemma-2B AI Extractor**
  - Fully private, local extraction of food items, portion sizes, and calories from natural descriptions (e.g. *"2 scrambled eggs on whole wheat toast"*).
  - Mathematical macro alignment: automatically balances calorie outputs against protein (4 kcal/g), carbs (4 kcal/g), and fats (9 kcal/g).
- 📸 **On-Device Vision Scan**
  - Snap a meal photo or select from your library. The local Gemma model extracts portion estimates and nutrition offline.
- 📐 **Scientific Deficit Calculator**
  - Estimates your daily BMR/TDEE goals using the Mifflin-St Jeor equation and custom physical activity multipliers.
- 📊 **Intelligence Dashboard**
  - Dynamic parameter switching in Stats (Calories, Protein, Carbs, Fat) using a custom popup selector.
  - Interactive week-by-week bar charts, hourly daily charts, and daily compliance calendars.
- 🎨 **Minimalist Design & Dark Mode**
  - Warm minimalist typography and dynamic light/dark system themes.
  - Segmented Day/Night controller in settings with WbSunny and NightsStay icon controls.
- 🔐 **Guaranteed Privacy**
  - Offline-first execution. No sign-ups, no cookies, and zero external cloud uploads.

---

## 🛠️ Build & Installation

### Prerequisites
- Android Studio Ladybug (or newer)
- Android SDK 26+

### Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/breejesh/nomai-android.git
   ```
2. Open the project in Android Studio.
3. Sync Gradle and build the project.
4. **AI Setup:** Download your local Gemma-2B weights inside the app's Setup screen.
