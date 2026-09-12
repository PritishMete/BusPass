# 🚌 BusPass — Smart Public Transport & Digital Ticketing Platform

BusPass is a connected public-transport platform built around three operating experiences:

- **Passenger Android App** — route discovery, booking, digital tickets, wallet and live journey visibility
- **Conductor Android App** — passenger lists, QR validation and live route operations
- **Admin Web Panel** — route management, bus monitoring, NFC assignment, reports and operational control

## 🌐 Live Admin Panel

**https://buspassadmin.web.app/**

## 🌿 Repository Structure

| Branch | Purpose |
| --- | --- |
| [`main`](https://github.com/PritishMete/BusPass/tree/main) | Android passenger + conductor application |
| [`admin`](https://github.com/PritishMete/BusPass/tree/admin) | BusPass admin panel / web operations |

---

## 📱 Android Application

The `main` branch contains the native Android application used by passengers and conductors.

### Passenger Features

- User registration, login and profile management
- Route and nearest-stop discovery
- Google Maps / map-based journey context
- Individual and group booking flows
- Digital QR ticket generation and presentation
- Wallet balance, recharge and passbook history
- Live bus and journey visibility
- Saved routes and referrals
- Issue reporting and emergency support

### Conductor Features

- Route and journey setup
- Active passenger lists
- QR ticket scanning and validation
- Live location broadcasting
- Booking and journey-state views
- Field-focused route operations

---

## 🖥️ Admin Panel

The `admin` branch contains the operational web interface for BusPass.

**Live Admin Panel:** https://buspassadmin.web.app/

### Admin Features

- Administrative login
- Dashboard monitoring
- Add buses and routes
- Manage route stops and route data
- Live bus-location monitoring
- NFC UID assignment
- Emergency report review
- Report history
- Administrative messaging
- Password / settings management

---

## 🏗️ System Architecture

```text
                        BUSPASS PLATFORM
                               │
          ┌────────────────────┼────────────────────┐
          │                    │                    │
          ▼                    ▼                    ▼
     PASSENGER APP       CONDUCTOR APP        ADMIN PANEL
       Android              Android               React
          │                    │                    │
          └────────────────────┼────────────────────┘
                               ▼
                 Firebase Authentication
                 Firebase Realtime Database
                               │
          ┌────────────────────┼────────────────────┐
          ▼                    ▼                    ▼
       Maps &             Ticketing &          Payments &
       Location             Validation            APIs
```

All three experiences operate on the same BusPass ecosystem while exposing role-specific workflows.

---

## 🧰 Technology Stack

### Mobile

- Java
- Android SDK
- Material Components

### Admin Web

- React
- Vite
- Material UI
- Framer Motion

### Authentication & Data

- Firebase Authentication
- Firebase Realtime Database
- Firebase Hosting

### Maps & Location

- Google Maps
- Google Places
- Google Play Services Location
- OSMDroid

### Ticketing & Device Integration

- ZXing / JourneyApps QR
- NFC
- Bluetooth
- Web Serial API

### Payments & Networking

- Razorpay Checkout
- UPI application hand-off
- Retrofit
- OkHttp
- REST API integration

---

## 🚀 Android Local Setup

1. Install Android Studio and a compatible JDK.
2. Clone the repository and stay on the `main` branch.
3. Add the project Firebase configuration at:

```text
app/google-services.json
```

4. Configure valid Google Maps / Places credentials locally.
5. Sync Gradle and run from Android Studio.

Windows build command:

```bash
gradlew.bat :app:assembleDebug
```

### Verification

```bash
gradlew.bat clean test lint assembleDebug
```

---

## 🚀 Admin Panel Development

Switch to the admin branch:

```bash
git checkout admin
```

The admin implementation is separated from the Android application and is documented in the branch README.

Live deployment:

**https://buspassadmin.web.app/**

---

## 🔐 Security Notes

- Do not commit Firebase Admin SDK service-account JSON files.
- Do not commit unrestricted Google Maps API keys.
- Keep Android signing credentials and local environment files private.
- Firebase Realtime Database rules should enforce authenticated role-based access.
- Payment verification should be performed on a trusted backend before treating a transaction as final.
- Production API keys should be restricted by platform, package, domain and API scope.

---

## 📂 Related Documentation

- [`docs/PROJECT_SUMMARY.md`](docs/PROJECT_SUMMARY.md)
- [`docs/PORTFOLIO_CASE_STUDY.md`](docs/PORTFOLIO_CASE_STUDY.md)
- [`admin` branch](https://github.com/PritishMete/BusPass/tree/admin)
- [Live Admin Panel](https://buspassadmin.web.app/)

---

## 🔗 Project Links

**Android Repository:** https://github.com/PritishMete/BusPass

**Admin Branch:** https://github.com/PritishMete/BusPass/tree/admin

**Live Admin Panel:** https://buspassadmin.web.app/

**Portfolio Case Study:** https://pritish-mete.onrender.com/projects/showcase/project.html?id=0

---

## BusPass

A smart public-transport ecosystem combining mobile ticketing, live location, QR/NFC validation, wallet/payment workflows and web-based operational administration.
