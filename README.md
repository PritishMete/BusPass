# 🚌 BusPass — Smart Public Transport Platform

<p align="center">
  <strong>Android passenger & conductor app · Admin web panel · Firebase backend</strong>
</p>

<p align="center">
  <a href="https://github.com/PritishMete/BusPass/tree/main"><img src="https://img.shields.io/badge/Android-Java-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android Java"></a>
  <a href="https://github.com/PritishMete/BusPass/tree/admin"><img src="https://img.shields.io/badge/Admin-React-61DAFB?style=for-the-badge&logo=react&logoColor=111" alt="React Admin"></a>
  <img src="https://img.shields.io/badge/Backend-Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=111" alt="Firebase Backend">
  <a href="https://buspassadmin.web.app/"><img src="https://img.shields.io/badge/Live_Admin-Open-2563EB?style=for-the-badge" alt="Live Admin Panel"></a>
</p>

BusPass is a multi-role public-transport platform designed around three connected operating views: **Passenger**, **Conductor**, and **Administrator**.

The Android application handles route discovery, journey booking, digital tickets, QR validation, wallet/payment flows and live transport activity. A separate React admin panel provides operational control, while **Firebase Authentication and Realtime Database form the shared backend and realtime data layer**.

> 🌐 **Live Admin Panel:** https://buspassadmin.web.app/  
> 📱 **Android App:** [`main`](https://github.com/PritishMete/BusPass/tree/main)  
> 🖥️ **Admin / Backend Operations:** [`admin`](https://github.com/PritishMete/BusPass/tree/admin)

---

## ✨ Platform Overview

| Layer | Purpose | Main Technologies |
| --- | --- | --- |
| **Passenger App** | Route discovery, booking, wallet, tickets, live bus visibility | Android, Java, Maps, Firebase |
| **Conductor App** | Passenger list, QR validation, route state, live location | Android, Java, ZXing, Firebase |
| **Admin Panel** | Dashboard, route/bus management, live monitoring, reports, NFC assignment | React, Vite, Material UI |
| **Backend / Realtime Core** | Authentication, shared transport data, bookings, routes, locations, reports | Firebase Auth, Realtime Database |

---

## 🧭 Core Features

### 👤 Passenger

- Account registration, authentication and profile management
- Route and nearest-stop discovery
- Map-based journey context
- Individual and group / other-passenger booking
- Digital ticket generation and QR presentation
- Wallet balance, recharge and passbook history
- Live bus and journey visibility
- Saved routes and referrals
- Emergency reporting and issue submission

### 🎫 Conductor

- Route and journey initialization
- Active passenger and booking lists
- QR ticket scanning and validation
- Live vehicle location broadcasting
- Journey-state and operational views

### 🖥️ Admin / Backend Operations

- Administrator authentication
- Dashboard and transport statistics
- Add buses and manage routes
- Monitor live bus locations
- NFC UID assignment
- Review emergency reports
- Send administrative messages
- Manage settings and credentials

---

## 🏗️ System Architecture

```text
                         BUSPASS PLATFORM
                                │
          ┌─────────────────────┼─────────────────────┐
          │                     │                     │
          ▼                     ▼                     ▼
     PASSENGER APP         CONDUCTOR APP          ADMIN PANEL
      Android / Java        Android / Java         React / Vite
          │                     │                     │
          └─────────────────────┼─────────────────────┘
                                │
                                ▼
                    FIREBASE BACKEND / CORE
                 Authentication · Realtime Database
                                │
          ┌─────────────────────┼─────────────────────┐
          │                     │                     │
          ▼                     ▼                     ▼
     Routes & Stops        Bookings & Users      Live Locations
          │                     │                     │
          └──────────── Reports · NFC · Journey State ┘

External integrations:
Google Maps · Places · ZXing QR · NFC · Razorpay · UPI · Retrofit · OkHttp
```

The three interfaces serve different users, but they operate on the same transport domain and shared realtime data.

---

## 🌿 Repository Branches

| Branch | Contains |
| --- | --- |
| [`main`](https://github.com/PritishMete/BusPass/tree/main) | Native Android passenger + conductor application |
| [`admin`](https://github.com/PritishMete/BusPass/tree/admin) | Admin-panel documentation, configuration and administrative/backend operations context |

### Live Admin Panel

**https://buspassadmin.web.app/**

The deployed admin interface is the operational control surface for BusPass.

---

## 🛠️ Technology Stack

### Mobile Application

- Java
- Android SDK
- Material Components

### Backend & Realtime Data

- Firebase Authentication
- Firebase Realtime Database
- Firebase Hosting

### Admin Panel

- React
- Vite
- React Router
- Material UI
- Framer Motion

### Maps & Location

- Google Maps
- Google Places
- Google Play Services Location
- OSMDroid

### Ticketing & Device Integration

- ZXing / JourneyApps QR
- NFC
- Bluetooth
- Web Serial for admin-side NFC assignment workflow

### Payments & Networking

- Razorpay Checkout
- UPI application hand-off
- Retrofit
- OkHttp

---

## 📱 Android Setup

### Prerequisites

- Android Studio
- Compatible JDK
- Android SDK
- Project Firebase configuration

### Run locally

1. Clone the repository:

```bash
git clone https://github.com/PritishMete/BusPass.git
cd BusPass
```

2. Make sure you are on the Android branch:

```bash
git checkout main
```

3. Add the Firebase configuration supplied by the project owner:

```text
app/google-services.json
```

4. Configure valid Google Maps / Places credentials through local or release configuration.

5. Sync Gradle and run through Android Studio, or build on Windows with:

```bash
gradlew.bat :app:assembleDebug
```

---

## ✅ Verification

The Android project can be checked with:

```bash
gradlew.bat clean test lint assembleDebug
```

Firebase, map, payment and realtime integrations require valid project-side configuration and therefore cannot be completely verified by offline unit tests alone.

---

## 🔐 Security

Do **not** commit private credentials or production secrets.

Keep the following outside source control:

```text
.env
.env.*
local.properties
key.properties
*.jks
*.keystore
service-account*.json
*-firebase-adminsdk-*.json
private API secrets
production admin passwords
```

Additional production requirements:

- Restrict Firebase Realtime Database rules by authenticated role and ownership.
- Verify Razorpay payment results on a trusted backend before treating a recharge as final.
- Restrict Google API keys by package/application and API scope.
- Keep Firebase Admin SDK credentials only in trusted server-side environments or secret managers.
- Never expose unrestricted administrative credentials in the Android or React client.

---

## 📚 Project Documentation

- [`docs/PROJECT_SUMMARY.md`](docs/PROJECT_SUMMARY.md) — concise project overview
- [`docs/PORTFOLIO_CASE_STUDY.md`](docs/PORTFOLIO_CASE_STUDY.md) — portfolio-focused case study
- [`admin` branch](https://github.com/PritishMete/BusPass/tree/admin) — admin-panel / backend operations
- [Live Admin Panel](https://buspassadmin.web.app/) — deployed operational interface

---

## 🔗 Quick Links

| Resource | Link |
| --- | --- |
| Android Source | https://github.com/PritishMete/BusPass/tree/main |
| Admin Branch | https://github.com/PritishMete/BusPass/tree/admin |
| Live Admin Panel | https://buspassadmin.web.app/ |
| Portfolio Case Study | https://pritish-mete.onrender.com/projects/showcase/project.html?id=0 |

---

## 📌 Project Summary

**BusPass** combines mobile ticketing, realtime vehicle tracking, QR/NFC validation, wallet/payment workflows and administrative operations into one connected public-transport ecosystem.

It demonstrates full-product engineering across **Android**, **React**, **Firebase backend services**, **location technology**, **digital ticketing** and **operational administration**.
