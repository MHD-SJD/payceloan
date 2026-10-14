# PayCeylon

PayCeylon is an Android wallet for Sri Lanka, built with Kotlin and Jetpack Compose. It has a
fiat (LKR) wallet and a crypto wallet (USDT, BTC, ETH, SOL), with sending, receiving, swapping,
top-ups, withdrawals and account settings.

The Android app uses Firebase phone authentication, caches API snapshots in a private SQLite
database, and connects to the local FastAPI/PostgreSQL demo backend. Money movement and linked
cards/banks are simulated only; no real financial service is contacted.

See [`backend/README.md`](backend/README.md) for local setup and
[`docs/BACKEND_API.md`](docs/BACKEND_API.md) for the API contract.

## Features

- **Onboarding and sign-in:** three swipeable onboarding pages, Sri Lankan phone number entry,
  6-digit SMS code with resend countdown.
- **Home:** fiat / crypto wallet switcher (full-screen transition, crypto switches the app to a
  dark theme), balance card with hide/show (blurred), month-on-month change pill, linked card
  chip, recent transactions, and a glass action bar that drags open for more actions.
- **Send:** QR scanner (CameraX + ML Kit) or search by username, amount keypad, balance and
  network-fee checks.
- **Receive:** branded QR code and shareable payment link, with an optional requested amount.
- **Swap** between LKR and tokens at indicative rates (0.5% fee).
- **Top Up / Withdraw** (fiat only): top up from a linked card (1.5% fee) or bank (free), withdraw
  to a bank (LKR 25 fee). Crypto is bought and sold by swapping with LKR.
- **Transactions:** full list with filters, and details with fees, reference, on-chain hash
  (crypto) and a shareable receipt.
- **Notifications** dropdown on the bell: transactions plus account and security changes.
- **Settings:** personal details (photo, name, username, phone with SMS check, email),
  cards & bank accounts (card brand detected from the number, with its logo), security and
  notification toggles, privacy policy, terms, help, log out.
- **Deep links:** `payceylon://pay?to=<handle>[&amount=][&currency=]` opens Send Money.

## Build and run

Requirements: Android Studio (recent stable), Android SDK 37, JDK 17+ (Android Studio's bundled
JDK works). The Gradle wrapper (9.5) downloads everything else.

```bash
./gradlew :app:assembleDebug          # Windows: .\gradlew.bat :app:assembleDebug
./gradlew :app:testDebugUnitTest      # unit tests
```

The debug APK is at `app/build/outputs/apk/debug/app-debug.apk`. Or open the folder in Android
Studio and run the `app` configuration. Minimum Android version: 7.0 (API 24). Blur effects
need Android 12+; older versions fall back to plain tints and fades.

## Project structure

```
app/src/main/java/com/payceylon/app/
├── MainActivity.kt            Entry point, deep-link intake
├── data/                       Firebase-backed API client and SQLite response cache
└── ui/
    ├── PayCeylonApp.kt        Navigation (single-activity, state-driven) and screen wiring
    ├── auth/                  Onboarding, phone number, OTP
    ├── wallet/                Home, Send, Receive, Swap, Top Up, Withdraw, transactions
    │   └── WalletViewModel.kt Wallet state and actions
    ├── account/               Settings, profile, cards & banks, notifications, legal
    │   └── AccountState.kt    Profile, linked accounts, settings, notifications ← backend seam
    ├── components/            Shared UI: dropdowns, text field, QR code, motion helpers
    └── theme/                 Colours (fiat / crypto palettes), Inter typography
docs/BACKEND_API.md            REST contract for the backend
backend/                       FastAPI service, PostgreSQL schema, and tests
images/                        Source design assets from Figma
```

## Local backend

The API is in `backend/main.py` and uses parameterized `asyncpg` queries; its PostgreSQL schema
is `backend/schema.sql`. The debug Android build connects to the Firebase Auth emulator and local
API at `10.0.2.2`. Follow [`backend/README.md`](backend/README.md) to start the local services.

### Security rules

- The demo add-card flow must never send or store full card numbers or CVVs. It sends a
  fictitious token and masked details only.
- Firebase owns and refreshes ID tokens; SQLite caches API data, not credentials.
- App backup is disabled (`allowBackup="false"`).
- Money is `BigDecimal` in the app and decimal strings in JSON, never floating point.

## Demo limitations

- The Firebase Auth emulator is configured for local development. It is not a production Firebase
  project configuration.
- Simulated card tokens, balances, rates, addresses, swaps, peer transfers, top-ups and withdrawals
  do not represent money and must not be used with real financial details.
- Biometric approval is a stored preference; Android biometric payment approval is not wired up.
- The SQLite database is a cache. PostgreSQL is the persistent server-side source of truth.

## Credits

- [Inter](https://rsms.me/inter/) typeface (SIL Open Font License).
- Token icons: [web3icons](https://github.com/0xa3k5/web3icons) (MIT).
- Card network logos: [svg-credit-card-payment-icons](https://github.com/aaronfagan/svg-credit-card-payment-icons)
  (Apache 2.0), converted to vector drawables.
- Glass blur: [Haze](https://github.com/chrisbanes/haze) (Apache 2.0).
