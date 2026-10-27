# dashboard_android

English | [简体中文](README_zh.md)

A native Android client for browsing application-market summaries, lists and details.

## Overview

The application provides native market screens, application details, preferences and a WebView entry for the related web service. The data/API layer is separate from Compose screens; live content depends on the external Dashboard service.

## Technology and Dependencies

Kotlin, Jetpack Compose/Material 3, AndroidX and Gradle Wrapper. The project declares minimum API 31 and target/compile API 37. Version catalog entries include AGP 9.4.1 and Kotlin 2.2.10; use a compatible Android Studio/JDK toolchain.

## Repository Structure

- `app/src/main/` — application code and resources
- `app/build.gradle.kts` — module SDK and dependency configuration
- `gradle/libs.versions.toml` — dependency versions
- `gradlew` — Gradle launcher

## Quick Start

Open the root in Android Studio, install the configured SDK and let Gradle restore dependencies. For a debug package:

```bash
./gradlew :app:assembleDebug
```

Run the `app` configuration on an API 31+ emulator or device. Check the endpoint settings in the data layer before loading live content. Release signing is configured locally.
