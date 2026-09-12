# BusPass — Smart Public Transport & Digital Ticketing Platform

BusPass is a multi-role public-transport platform built around two connected experiences:

- **Android App** — passenger and conductor workflows
- **Admin Panel** — operational management and monitoring

> **Live Admin Panel:** https://buspassadmin.web.app/
>
> **Admin branch:** [`admin`](https://github.com/PritishMete/BusPass/tree/admin)

## Repository Branches

| Branch | Purpose |
| --- | --- |
| [`main`](https://github.com/PritishMete/BusPass/tree/main) | Android passenger + conductor application |
| [`admin`](https://github.com/PritishMete/BusPass/tree/admin) | BusPass administrative web panel |

## Android Application

The `main` branch contains the native Android application used by passengers and conductors.

### Passenger experience

- Sign in, registration and profile management
- Route and nearest-stop discovery
- Map-based journey context
- Individual and group/other-passenger booking
- Digital ticket and QR presentation
- Wallet balance, recharge and passbook history
- Live bus/journey visibility
- Saved routes, referrals, issue reporting and emergency support

### Conductor experience

- Route and journey setup
- Active passenger lists
- QR ticket scanning and validation
- Live location broadcasting
- Booking/journey operational views

## Technology Stack

### Mobile
- Java
- Android SDK
- Material Components

### Data & Authentication
- Firebase Authentication
- Firebase Realtime Database

### Maps & Location
- Google Maps
- Google Places
- Google Play Services Location
- OSMDroid

### Ticketing & Device Capabilities
- ZXing / JourneyApps QR
- NFC
- Bluetooth

### Payments & Networking
- Razorpay Checkout
- UPI application hand-off
- Retrofit
- OkHttp

## System Overview

```text
                    BUSPASS PLATFORM
                           │
          ┌────────────────┴────────────────┐
          │                                 │
          ▼                                 ▼
   Android Application                 Admin Panel
 Passenger + Conductor                Web Operations
          │                                 │
          └───────────────┬─────────────────┘
                          ▼
              Firebase / Shared Data
```

The Android and administrative experiences operate on the same BusPass ecosystem while serving different users and workflows.

## Admin Panel

The administrative interface is maintained on the [`admin`](https://github.com/PritishMete/BusPass/tree/admin) branch.

**Live deployment:** https://buspassadmin.web.app/

The admin panel provides operational functionality such as dashboard views, bus and route management, bus-location monitoring, NFC assignment, passenger/report handling, messaging and administrative settings.

## Android Local Setup

1. Install Android Studio and a compatible JDK.
2. Clone the repository and stay on the `main` branch.
3. Obtain the project Firebase configuration from the project owner and place it at:

```text
app/google-services.json
```

4. Configure valid Google Maps/Places credentials through local/release configuration. Do not commit private API credentials.
5. Sync Gradle and run the app from Android Studio, or build from Windows with:

```bash
gradlew.bat :app:assembleDebug
```

## Verification

The Android project can be checked with:

```bash
gradlew.bat clean test lint assembleDebug
```

External integrations such as Firebase, maps and payments require valid project-side configuration and cannot be fully verified through offline unit tests alone.

## Security Notes

- Never commit `google-services.json` if it contains restricted project configuration not intended for source control.
- Firebase Realtime Database rules must restrict access by authenticated role and ownership.
- Razorpay payment verification should happen on a trusted server before a recharge is treated as final.
- Production Google API keys should be restricted by application/package and API scope.
- Service-account JSON/private keys must never be committed to Git.

## Related Documentation

- [`docs/PROJECT_SUMMARY.md`](docs/PROJECT_SUMMARY.md)
- [`docs/PORTFOLIO_CASE_STUDY.md`](docs/PORTFOLIO_CASE_STUDY.md)
- [Admin branch](https://github.com/PritishMete/BusPass/tree/admin)
- [Live Admin Panel](https://buspassadmin.web.app/)

---

**BusPass** combines mobile ticketing, QR validation, real-time location, wallet/payment workflows and administrative operations into one connected public-transport system.
