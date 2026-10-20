# Exam Rescue — Sri Lankan GCE O/L Study Companion

**Exam Rescue** is a native, offline-first Android application designed specifically for Sri Lankan General Certificate of Education Ordinary Level (GCE O/L) students studying in Tamil or English medium.

---

## 🌟 Key Features

1. **100% Offline-First Core**
   - Syllabus tracking, subjects, topics, and confidence levels.
   - Deterministic study planner with transparent priority scoring (exam proximity, weak confidence, overdue revision, never studied).
   - Focus study session timer that survives process death and backgrounding via timestamp delta calculations.
   - Question practice with immediate solution explanations.
   - Automatic Mistake Notebook with 1-tap conversion to flashcards or study tasks.
   - Spaced Repetition (SM-2) flashcards with interval calculations and due date tracking.
   - Comprehensive study notes with instant text search.
   - All core features work in Airplane Mode with zero account, zero server, and zero tracking.

2. **Bilingual Design (Tamil & English)**
   - Authentic Sri Lankan Tamil Unicode rendering (`values-ta/strings.xml`) and English (`values/strings.xml`).
   - Independent selection of App Interface Language and Exam Learning Medium.
   - Full support for technical terms in both languages.

3. **Optional User-Controlled AI Learning Assistant**
   - Choose your provider: Google Gemini, OpenRouter, Featherless AI, xAI (Grok), or Custom OpenAI-compatible endpoints.
   - 10 specialized study agent workflows:
     - Study Planner Coach
     - Bilingual Concept Tutor
     - Active Recall Coach
     - Mistake Detective
     - Flashcard Builder
     - Notes Summarizer
     - Exam Countdown Coach
     - Practice Paper Builder
     - Answer Feedback Coach
     - Focus Session Coach
   - Strictly enforced tool allowlist with explicit student preview and confirmation before writing any AI proposals to the database.

4. **Security & Privacy Truth**
   - Student API keys are encrypted locally using the hardware-backed **Android Keystore** (AES/GCM/NoPadding).
   - Zero telemetry, analytics, or background tracking.
   - API keys and private study notes are never logged or exported in backups.

---

## 🛠️ Building & Installing Locally

### Prerequisites
- Android Studio Ladybug / Meerkat or later
- JDK 17 or JDK 21
- Android SDK with API 36 (`minSdk 24`, `targetSdk 36`)

### Commands
```bash
# Clone the repository
git clone https://github.com/<your-username>/exam-rescue.git
cd exam-rescue

# Run unit tests
gradle :app:testDebugUnitTest

# Assemble debug APK
gradle :app:assembleDebug
```
The output APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

Install on connected Android device or emulator:
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔑 AI Provider Configuration

1. In the app, open **Settings** (gear icon in the top bar).
2. Under **AI Assistant Configuration**, select your provider:
   - **Google Gemini**: Get an API key from Google AI Studio.
   - **OpenRouter**: Use your OpenRouter key for models like Llama or Claude.
   - **Featherless AI**: Use your Featherless API key.
   - **xAI**: Use your Grok API key.
   - **Custom**: Enter your own OpenAI-compatible HTTPS endpoint.
3. Tap **Test Connection** to verify your key and network connectivity.
4. Tap **Save** to securely encrypt the key into the Android Keystore.

*Note: Core study features never require an API key or internet access.*

---

## 🚀 GitHub Actions CI/CD

The repository includes an automated workflow at `.github/workflows/android.yml`.

- Every `push` or `pull_request` runs unit tests and compiles a downloadable debug APK artifact.
- **Downloading APK Artifact:**
  1. Go to the **Actions** tab on your GitHub repository.
  2. Click on the latest workflow run.
  3. Scroll down to the **Artifacts** section and click `exam-rescue-debug-apk` to download the APK.
- **Release Signing (Tags `v*`):**
  To enable signed release APK builds on version tags, configure the following GitHub Repository Secrets:
  - `ANDROID_KEYSTORE_BASE64`
  - `ANDROID_KEYSTORE_PASSWORD`
  - `ANDROID_KEY_ALIAS`
  - `ANDROID_KEY_PASSWORD`

> ⚠️ **Important:** Pushing commits to GitHub builds CI artifacts, but does **not** automatically publish the application to the Google Play Store. Google Play distribution requires a Google Play Console developer account.

---

## 📄 License & Attribution
Designed for Sri Lankan students preparing for the GCE Ordinary Level examination.
All bundled syllabus packs are sample reference materials and can be modified or customized by the student.
