# Prefix Call Blocker

**Prefix Call Blocker** is an offline Android app that automatically blocks incoming calls when the caller number starts with one of the prefixes saved by the user.

## How it works

If the saved prefixes are:

- `0700`
- `020`
- `0809`

then:

- `07001234567` → blocked
- `07009876543` → blocked
- `0201234567` → blocked
- `08001234567` → allowed

Matching is prefix-based (`startsWith`) rather than exact-number matching.

If the prefix list is empty, every incoming call is allowed.

## Android implementation

The app uses the official Android `CallScreeningService` API. The user must choose Prefix Call Blocker as the device's Call Screening app before screening can take effect.

No `READ_CALL_LOG` or `READ_PHONE_STATE` permission is requested. The screening service receives the call details supplied by Android.

## Number normalization

Formatting characters such as spaces, hyphens, and parentheses are ignored for matching.

For convenience on Nigerian numbers, an incoming `+234700...` number can match a saved local prefix such as `0700`. International prefixes can also be entered using `+`, for example `+234700`.

This app does not attempt to guess arbitrary country-code conversions. For other countries, save the prefix in the same numbering format normally supplied by the device/carrier, or use the international `+` form.

## Storage and privacy

Prefixes, the blocking switch, and the small local blocked-call history are stored on the device using Preferences DataStore.

The app has no account, server, Firebase, analytics, AI service, or network permission. Phone numbers are not uploaded anywhere.

## Build with GitHub Actions

1. Create a new GitHub repository.
2. Upload this project to the repository.
3. Open the repository's **Actions** tab.
4. Run **Build Prefix Call Blocker** manually, or push a commit to trigger the workflow.
5. Open the completed workflow run.
6. Download the `prefix-call-blocker-debug-apk` artifact.

The workflow installs JDK 17, the Android SDK platform/build tools, and Gradle 9.6. No Android Studio installation is required on your computer.

## Local command-line build

With Android SDK, JDK 17 and Gradle 9.6 installed:

```bash
gradle assembleDebug
```

The APK is produced at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Install

Download the APK artifact, install it on an Android phone, and open Prefix Call Blocker.

Android may require you to allow installation from the source used to install the APK.

## First setup

1. Open Prefix Call Blocker.
2. Turn **Call blocking** on.
3. Tap **Set as Call Screening App**.
4. Approve Prefix Call Blocker in the Android system role/settings screen.
5. Add prefixes such as `0700`, `020`, `0809`, `090`, or `091`.

The app shows **Call Screening: ✓ Active** only when Android reports that this app currently holds the Call Screening role on Android versions where the role API is available.

On older Android versions, the exact settings screen varies by manufacturer; choose Prefix Call Blocker as the system's call-screening/spam-screening service if the device exposes that option.

## Compatibility

- Minimum SDK: Android 7.0 (API 24), because `CallScreeningService` was introduced in API 24.
- Target/compile SDK: Android API 37.
- JDK: 17.
- Android Gradle Plugin: 9.4.0.
- Gradle: 9.6.

OEM dialer behavior can vary. The app cannot control vendor-specific call-screening restrictions, carrier behavior, or devices that do not expose a selectable call-screening service.

## Important limitation

Android requires the call-screening service to answer promptly. The service therefore performs only a small local DataStore lookup and prefix comparison and fails open (allows the call) if local storage cannot be read quickly.

## Project structure

```text
PrefixCallBlocker/
├── .github/workflows/build.yml
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/hallysam/prefixcallblocker/
│       │   ├── MainActivity.kt
│       │   ├── CallScreeningService.kt
│       │   ├── PrefixRepository.kt
│       │   └── PhoneNumberUtils.kt
│       └── res/
│           ├── drawable/
│           │   ├── bg_card.xml
│           │   └── ic_launcher.xml
│           └── values/
│               ├── colors.xml
│               ├── strings.xml
│               └── themes.xml
├── build.gradle.kts
├── gradle.properties
├── settings.gradle.kts
└── README.md
```
