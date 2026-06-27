# 🍃 CalmCalories

**A quiet-luxury, privacy-first, on-device AI calorie advisor for Android.**

CalmCalories helps you track nourishment and stay in your optimal calorie zone without intrusive popups, subscription walls, or data leaks.

---

## ✨ Features

- 🌿 **Quiet-Luxury Design** - A warm minimalist visual language built on champagne gold, eucalyptus green, and rich espresso crema, utilizing capsule gauges and symmetric layout structures.
- 🤖 **On-Device Gemma AI** - Completely private, local extraction of food items, portions, and calories from natural descriptions (e.g. *"2 poached eggs, buttered toast, and black coffee"*).
- 📸 **Offline AI Vision Scan** - Snap a picture or choose from the gallery. The local model scans ingredients and estimates portions directly on your phone.
- 📐 **ScientificDeficit Advisor** - Leverages the Mifflin-St Jeor equation combined with custom activity level multipliers to suggest a safe daily calorie target.
- 📊 **Intelligence Dashboards** - Visualize your weekly logs, daily hour-by-hour intake, and monthly target calendars.
- 🔐 **Privacy-First & Offline-First** - Works fully without an internet connection. No tracking cookies, no logins, and zero external network uploads.

---

## 📸 Screenshots

<p float="left">
  <img src="https://raw.githubusercontent.com/breejesh/calmcalories-android/main/art/screenshot_home.png" alt="CalmCalories Home Screen" width="260"/>
  <img src="https://raw.githubusercontent.com/breejesh/calmcalories-android/main/art/screenshot_stats.png" alt="Intelligence Dashboard" width="260"/>
  <img src="https://raw.githubusercontent.com/breejesh/calmcalories-android/main/art/screenshot_journal.png" alt="Nourishment Timeline" width="260"/>
</p>

*The clean, modern UI remains consistent across all tabs.*

---

## 🛠️ Build & Installation

### Requirements
- Android SDK 34+
- Gradle 9.0+
- JDK 17+

### Step-by-Step Build
1. Clone the repository:
   ```bash
   git clone https://github.com/breejesh/calmcalories-android.git
   cd calmcalories-android
   ```
2. Build the debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
3. Install the generated APK on your device:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

### 🧠 Gemma Model Asset Setup
Upon the first launch, the app will automatically download the **Gemma-2B** model format directly from HuggingFace to your device's private storage. 

If you prefer to load the model file manually:
1. Download `gemma-4-E2B-it.litertlm` from [HuggingFace](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm).
2. Save it directly to your device's external storage folder:
   `/Android/data/com.calmcalories.app/files/gemma-4-E2B-it.litertlm`

---

## 🏛️ Architecture & Clean Design

CalmCalories follows Android's **Clean Architecture** patterns, separating tasks into decoupled packages:
- **`ui.theme`**: Holds brand token variables (`Theme.kt`).
- **`ui.components`**: Modular composables such as `ProgressRing`, `StatCard`, and `MealRow`.
- **`ui.dialogs`**: Modals for prompt entries, manual values, and AI processing loaders.
- **`ui.screens`**: Self-contained screens (`HomeScreen`, `StatsScreen`, `JournalScreen`, `SettingsScreen`).
- **`data`**: Room DB entities, DAOs, and repository bindings.
- **`ai`**: Gemma LiteRT service runners.
