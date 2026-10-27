# SpendTag — Daily Balance (Android)

**An expense tracker that fills itself in.** SpendTag reads the bank and UPI SMS alerts already arriving on your phone, turns them into transactions automatically, and learns how you categorize your spending so you tag less and less over time. Everything stays on the device — no account, no server.

Built with **Kotlin** and **Jetpack Compose**.

---

## Features

- **Automatic expense capture** — parses Indian bank and UPI SMS alerts the moment they arrive. OTPs, promotional messages and balance alerts are filtered out, and the raw SMS text is never stored.
- **Smart tagging that learns** — tag a merchant the same way 3 times and SpendTag tags it automatically from then on. Before that, it suggests the likely category, and for new merchants it guesses from keywords. Incoming money is marked as income or transfer.
- **One-tap tagging from notifications** — categorize a payment straight from its notification without opening the app.
- **Reminders** — a gentle nudge 2 hours after a skipped payment, and a 9 PM evening digest of anything still untagged.
- **Split bills with friends** — split a payment, track who owes what, and send a polite WhatsApp reminder that includes a UPI payment link.
- **Insights** — spending by category (donut chart), monthly trend line and top payees.
- **Cash expenses** — add cash spending manually so the balance stays complete.
- **Encrypted backup and restore** — backups are encrypted with AES-256-GCM using a key held in the Android Keystore, and restores run in a single database transaction.
- **CSV export** — share your transactions as a standard CSV file.
- **App lock** — lock screen to keep your finances private.

## Tech stack

| Area | Technology |
|---|---|
| Language | Kotlin 2.0 |
| UI | Jetpack Compose, Material 3, Navigation Compose |
| Architecture | MVVM — ViewModels + repositories, Kotlin coroutines and Flow |
| Storage | Room (SQLite) |
| Background work | WorkManager |
| Security | Android Keystore, AES-256-GCM |
| Build | Gradle (Kotlin DSL) with a version catalog, KSP |
| Testing | JUnit 4, coroutines-test, AndroidX instrumented tests |

Min SDK 26 (Android 8.0), target SDK 34. Money is stored as whole paise (`Long`) to avoid floating-point rounding errors.

## Screens

Onboarding · Home · Pending (untagged) · Payments · Payment detail · Insights · Friends · Settings

## Project structure

```
app/src/main/java/com/spendtag/app/
├── core/
│   ├── sms/          # SMS filter and parser for bank/UPI alerts
│   ├── smarttag/     # Learning tag engine and confidence rules
│   ├── database/     # Room database, DAOs and entities
│   ├── security/     # Keystore key management, AES-GCM encryption, app lock
│   ├── backup/       # Encrypted backup and restore
│   ├── work/         # Reminder and evening digest workers
│   ├── split/        # Bill splitting and WhatsApp/UPI reminders
│   ├── export/       # CSV export
│   └── notification/ # Notifications with one-tap tag actions
├── data/repository/  # Repositories over the DAOs
├── feature/          # One package per screen (Compose UI + ViewModel)
├── receiver/         # SMS and notification-action broadcast receivers
└── navigation/       # Screen routes
```

## Tests

47 tests covering the SMS parser, SMS receiver, smart tagging engine, split calculations, encryption, backup serialization, CSV export, currency formatting, notifications, insights pacing, the home ViewModel and the Room database.

Run them in Android Studio by right-clicking `app/src/test` (unit tests) or `app/src/androidTest` (instrumented tests, needs a device or emulator) and choosing **Run Tests**.

## Build and run

1. Open the project in **Android Studio** (Koala or newer, JDK 17).
2. Let Gradle sync, then run the `app` configuration on a device or emulator with Android 8.0+.
3. On first launch, allow SMS and notification permissions so expenses can be captured automatically.

> SMS capture needs a real phone with bank/UPI alerts. On an emulator you can send test SMS from the emulator's extended controls.

## Privacy

- All data stays on your device. There is no backend and no analytics.
- Only transaction alerts are processed; the original SMS text is never saved.
- Backups are encrypted before they leave the app.

## Author

[Kathan Jain](https://github.com/KathanJain2007)
