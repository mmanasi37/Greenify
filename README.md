# Greenify

Greenify is an Android app built with Kotlin to help users understand and reduce their carbon footprint through calculators, eco tips, and community-focused sustainability features.

## Developer

- Mekere Manasi

## Features

- Authentication with Firebase (register, login, and forgot password)
- Carbon footprint calculators for food and electricity
- Eco tips integration from Firebase Firestore
- Sustainability challenge and community screens
- E-store screen for eco-friendly product ideas

## Tech Stack

- Kotlin + Android SDK
- Gradle (Kotlin DSL)
- Firebase Authentication
- Firebase Firestore
- Material Components

## Requirements

- Java 21 (recommended)
- Android SDK Platform 33
- Android Build Tools 33.x
- Gradle Wrapper (included)

## Setup

1. Clone the repository:

```bash
git clone https://github.com/mmanasi37/Greenify.git
cd Greenify
```

2. Configure Android SDK path in `local.properties`:

```properties
sdk.dir=/path/to/Android/Sdk
```

3. Ensure `app/google-services.json` is present for Firebase.

4. Build the app:

```bash
./gradlew clean build
```

## Run on Device

1. Enable Developer Options and USB debugging on your Android phone.
2. Connect the phone and verify connection:

```bash
adb devices
```

3. Install debug APK:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Project Structure

- `app/src/main/java/com/greenify/greenifykt/` - Kotlin activities
- `app/src/main/res/layout/` - XML UI layouts
- `app/src/main/res/drawable/` - drawables and UI assets
- `app/src/main/AndroidManifest.xml` - app components

## Notes

- The project currently targets `compileSdk = 33`.
- Firebase-backed features require internet connectivity.

## License

This project is licensed under the MIT License.
