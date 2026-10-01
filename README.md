# Naomi-Chan™ Official Network PDA

Android warehouse client for **Naomi-Chan™ Blackpool Fulfilment Centre (BFC)**, designed primarily for the **SUNMI V2** handheld terminal.

The application provides a touch-first mobile companion to the Naomi-Chan BFC platform for warehouse scanning, local operational workflows and receipt/label printing.

## Platform

- Kotlin / Android
- Jetpack Compose / Material 3
- Android minSdk 24, targetSdk 36
- Java/Kotlin JVM 11
- Room local persistence
- Kotlin Coroutines
- OkHttp networking
- CameraX
- Google ML Kit barcode scanning
- SUNMI printer library

The application ID is `com.naomichan.pos`. The project currently reports version **2.0.0**.

## Target hardware

The primary target is the **SUNMI V2 (T5930)** running SUNMI OS based on Android 7.1.x/API 25. The minimum SDK remains API 24 so development and compatibility are not unnecessarily restricted to one hardware revision.

The application can also run on compatible Android devices, although integrated SUNMI printer behaviour is hardware-dependent.

## Scanning

Camera scanning uses CameraX with the bundled Google ML Kit barcode model. Bundling the model allows barcode/QR recognition to work without relying on a Play Services model download on the warehouse device.

The camera is optional at the Android feature level so installation is not blocked on development/test devices without suitable camera hardware.

## Printing

The project integrates the SUNMI printer library for supported terminals. Printer behaviour should be validated on the intended physical SUNMI device before production deployment.

## Networking and security

The app requests Internet and network-state access for communication with Naomi-Chan services.

Cleartext HTTP traffic is disabled. Production communication should use HTTPS and authoritative warehouse/business state should remain server-side rather than being embedded in the Android client.

Android backup is disabled for the application.

## Local data

Room provides structured local persistence for device-side state and workflows that need local storage. Local data must not be treated as the authoritative BFC database.

## Repository layout

```text
Naomi-Chan-Official-Network-PDA/
├── app/
│   ├── src/main/
│   └── build.gradle.kts
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── gradlew / gradlew.bat
```

## Build

Requirements:

- Android Studio with Android SDK 36
- JDK compatible with the configured Android Gradle toolchain

Windows:

```powershell
.\gradlew.bat assembleDebug
```

Linux/macOS:

```bash
./gradlew assembleDebug
```

For a release build:

```powershell
.\gradlew.bat assembleRelease
```

Release signing can be supplied through `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD` and optional `KEY_ALIAS` environment variables. Signing keys and passwords must never be committed.

## Testing

Run local unit tests with:

```powershell
.\gradlew.bat test
```

The project includes Android/Compose testing dependencies plus Robolectric and Roborazzi support.

## Relationship to other Naomi-Chan systems

- **Naomi-Chan website/API** — authoritative server-side services and data.
- **Naomi-Chan BFC Windows / NaomiOS** — managed Windows warehouse terminal environment.
- **Naomi-Chan Merch Windows** — off-site/event merchandise POS.

This Android application is a warehouse client and should not become an independent source of truth for inventory, staff identity or fulfilment state.

## Ownership

Naomi-Chan™ / Naomi Extellna  
Blackpool, United Kingdom

© 2026 Naomi-Chan™
