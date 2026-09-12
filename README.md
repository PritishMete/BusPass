# 🖥️ BusPass Admin & Backend Operations

<p align="center">
  <strong>Operational web panel for the BusPass transport platform</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=111" alt="React 19">
  <img src="https://img.shields.io/badge/Vite-Frontend-646CFF?style=for-the-badge&logo=vite&logoColor=white" alt="Vite">
  <img src="https://img.shields.io/badge/Firebase-Backend-FFCA28?style=for-the-badge&logo=firebase&logoColor=111" alt="Firebase Backend">
  <a href="https://buspassadmin.web.app/"><img src="https://img.shields.io/badge/Live_Admin-Open-2563EB?style=for-the-badge" alt="Live Admin Panel"></a>
</p>

The `admin` branch represents the **administrative and backend-operations side** of BusPass.

It complements the Android passenger/conductor application on [`main`](https://github.com/PritishMete/BusPass/tree/main) by providing transport operators with a dedicated web interface for monitoring and managing the shared Firebase-backed system.

> 🌐 **Live Admin Panel:** https://buspassadmin.web.app/  
> 📱 **Android App:** [`main`](https://github.com/PritishMete/BusPass/tree/main)  
> 🚌 **Project Repository:** https://github.com/PritishMete/BusPass

---

## ✨ Admin Responsibilities

The admin side is responsible for operational workflows that should not live inside the passenger/conductor mobile experience.

### Dashboard & Monitoring

- View operational statistics
- Inspect routes, conductors, travellers and bookings
- Monitor platform activity

### Route & Bus Operations

- Add buses
- Create and edit route stops
- Manage route data
- Monitor live bus positions

### NFC & Validation Support

- Assign NFC UIDs to registered travellers
- Check existing NFC assignments
- Support ESP32/Web Serial based NFC workflows

### Safety & Communication

- Review emergency reports
- Approve or decline submitted reports
- View report history
- Send administrative messages

### Account & Settings

- Administrator authentication
- Session persistence
- Password management
- Protected admin routes

---

## 🏗️ Platform Architecture

```text
                        BUSPASS PLATFORM
                               │
             ┌─────────────────┴─────────────────┐
             │                                   │
             ▼                                   ▼
      ANDROID APPLICATION                  ADMIN WEB PANEL
 Passenger + Conductor                  React / Vite / MUI
             │                                   │
             └─────────────────┬─────────────────┘
                               │
                               ▼
                    FIREBASE BACKEND / CORE
                 Authentication · Realtime Database
                               │
          ┌────────────────────┼────────────────────┐
          ▼                    ▼                    ▼
      Users & Auth        Routes & Bookings      Operations
                                               Locations
                                               Reports
                                               NFC data
```

The admin panel is not a separate isolated product. It operates on the same transport data used by the Android application.

---

## 🧰 Technology Stack

### Frontend

- React 19
- Vite
- React Router
- Material UI
- Emotion
- Framer Motion
- Lucide React

### Backend / Data Layer

- Firebase Authentication
- Firebase Realtime Database
- Firebase Hosting

### Browser & Hardware Integration

- Google Maps JavaScript API
- Web Serial API
- ESP32-assisted NFC scanning workflow

---

## 🌐 Live Deployment

**https://buspassadmin.web.app/**

The administrative frontend is deployed through Firebase Hosting.

---

## 🌿 Branch Structure

| Branch | Purpose |
| --- | --- |
| [`main`](https://github.com/PritishMete/BusPass/tree/main) | Android passenger + conductor application |
| [`admin`](https://github.com/PritishMete/BusPass/tree/admin) | Admin panel and backend-operations context |

---

## 🚀 Local Development

For a local React/Vite admin environment:

```bash
npm install
npm run dev
```

Production build:

```bash
npm run build
```

Preview the production build:

```bash
npm run preview
```

---

## ⚙️ Environment Configuration

Do not hardcode unrestricted credentials in browser source.

Use local environment variables where applicable:

```text
VITE_GOOGLE_MAPS_API_KEY=
```

A safe example file is provided as:

```text
.env.example
```

---

## 🔐 Security

Never commit:

```text
.env
.env.*
service-account*.json
*-firebase-adminsdk-*.json
private keys
admin passwords
unrestricted API secrets
```

Important production rules:

- Firebase Admin SDK credentials belong only in trusted server-side environments or secret managers.
- Realtime Database rules must enforce authenticated, role-appropriate access.
- Google Maps keys should be restricted by domain and API scope.
- Administrative passwords must never be included in public documentation.
- Browser code must never contain private service-account credentials.

> **Security note:** the original upstream admin source exposed a Firebase Admin SDK service-account JSON file and a hardcoded Google Maps API key. Those sensitive credentials were intentionally not copied into this branch and should be revoked/rotated in the original project.

---

## 🔗 Related Resources

| Resource | Link |
| --- | --- |
| Android App | https://github.com/PritishMete/BusPass/tree/main |
| Live Admin Panel | https://buspassadmin.web.app/ |
| Main Repository | https://github.com/PritishMete/BusPass |
| Portfolio Case Study | https://pritish-mete.onrender.com/projects/showcase/project.html?id=0 |
| Original Admin Source Reference | https://github.com/priyanka352/BUS-ADMIN |

See [`ADMIN_SOURCE.md`](ADMIN_SOURCE.md) for migration and source-security notes.

---

## 📌 Summary

**BusPass Admin** is the operational control layer of the BusPass ecosystem.

Together with the Android passenger/conductor application and Firebase backend, it supports a complete workflow spanning **journey booking, digital ticketing, realtime location, QR/NFC validation, emergency handling and transport administration**.
