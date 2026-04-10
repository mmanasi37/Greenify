# Greenify

Greenify is an Android sustainability app built with Kotlin and Jetpack Compose. It helps users reduce carbon impact through calculators, eco tips, challenges, social sharing, and a live eStore catalog.

## Developer

- Mekere Manasi

## What Is Implemented

- Firebase authentication flow (register, login, forgot password)
- Dashboard with eco tip highlights and navigation cards
- Carbon calculators:
	- Electricity
	- Food
	- Transport
- Emissions repositories with cloud-first calculation and offline fallbacks
- Category-based eco tips with advanced tip insight panel
- Challenge module:
	- Daily and weekly goals
	- Points, level progression, weekly CO2 tracking
	- Undo support for accidental actions
- Community module:
	- Social feed style UI
	- Post text + image support
	- Likes, leaderboard, milestones
	- Profile-linked author name/avatar
- eStore module:
	- Live Firestore product catalog (real-time updates)
	- Local fallback catalog when cloud is unavailable
	- Search and category filtering
	- Cart summary with K (Kina) pricing
	- Admin tools: seed catalog + in-app add product form
- Settings module:
	- Theme mode toggle
	- Reminder preference
	- eStore auto-refresh toggle
	- Impact unit toggle
	- Local progress reset
	- Account summary + sign out
- Profile module:
	- Edit full name, location, bio
	- Pick profile image from device
	- Upload avatar to Firebase Storage

## Tech Stack

- Kotlin
- Android SDK
- Jetpack Compose + Material 3
- Gradle (Kotlin DSL)
- Firebase Authentication
- Firebase Firestore
- Firebase Storage
- Firebase Functions (scaffolded support)

## Requirements

- Java 21 recommended
- Android Studio with Android SDK installed
- Gradle wrapper (included)
- Firebase project configured for Android app package `com.greenify.greenifykt`

## Firebase Setup

1. Create/select your Firebase project.
2. Register Android app with package name `com.greenify.greenifykt`.
3. Download `google-services.json` and place it in [app/google-services.json](app/google-services.json).
4. Enable Authentication provider(s) you use (Email/Password recommended).
5. Create Firestore database.
6. Create Firebase Storage bucket.

### Firestore Collections Used

- `tips`
- `tip_events`
- `community_posts`
- `community_profiles`
- `estore_products`

### Storage Paths Used

- `profile_avatars/{uid}/...`
- `community_posts/{uid}/...`

## Build and Run

1. Clone project:

```bash
git clone https://github.com/mmanasi37/Greenify.git
cd Greenify
```

2. Configure SDK path in `local.properties`:

```properties
sdk.dir=/path/to/Android/Sdk
```

3. Build debug APK:

```bash
./gradlew assembleDebug
```

4. Install on connected Android device:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Notes for Admin Features

- eStore write actions (seed/add product) require Firestore rules that allow admin writes.
- Image uploads require Storage rules that allow authenticated upload/read as needed.
- If cloud access fails, app falls back to local catalog/tips in key modules.

## Project Structure

- [app/src/main/java/com/greenify/greenifykt](app/src/main/java/com/greenify/greenifykt) - app activities, screens, repositories, theme helpers
- [app/src/main/res](app/src/main/res) - drawables, layouts, values
- [app/src/main/AndroidManifest.xml](app/src/main/AndroidManifest.xml) - activity declarations
- [functions](functions) - Firebase Functions scaffold and deployment notes

## License

This project is licensed under the MIT License.
