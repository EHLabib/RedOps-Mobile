# RedOps Mobile Android module

Run `bootstrap-gradle.bat` in this directory once to generate the Gradle wrapper, then open this `android` directory in Android Studio.

Implemented in `app/src/main/java/com/redops/mobile/RedOpsApp.kt`:
- Dashboard and navigation
- Engagement management with authorization confirmation
- Finding management and resolution status
- Manual assessment notebook
- Report text generation and Android share intent
- Local persistence through SharedPreferences

Build requirements:
- Android Studio with Android SDK platform 34
- JDK 17 (Android Studio's bundled JBR is suitable)
- Gradle 8.7 / Android Gradle Plugin 8.4.2

No backend or internet connection is required at runtime. The app intentionally does not conduct active security scans.
