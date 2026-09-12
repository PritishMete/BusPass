# BusPass Admin Panel

The `admin` branch is the administrative side of the BusPass platform.

> **Live Admin Panel:** https://buspassadmin.web.app/
>
> **Android App:** [`main` branch](https://github.com/PritishMete/BusPass/tree/main)
>
> **Admin source reference:** https://github.com/priyanka352/BUS-ADMIN

## Overview

BusPass combines a native Android application for passengers and conductors with a web-based administrative panel for operational management and monitoring.

The admin panel is built with React/Vite and Firebase-backed application data.

## Admin Capabilities

The current administrative implementation includes:

- Administrator authentication
- Dashboard and operational statistics
- Bus creation and assignment
- Route management
- Bus/live-location monitoring
- NFC assignment
- Pending reports and report history
- Administrative messaging
- Settings and password management

## Live Deployment

**https://buspassadmin.web.app/**

The public admin frontend is deployed with Firebase Hosting.

## Technology Stack

- React 19
- Vite
- React Router
- Material UI
- Emotion
- Firebase Authentication / Realtime Database integration
- Framer Motion
- Lucide React
- date-fns

## Platform Architecture

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

## Repository Branches

| Branch | Purpose |
| --- | --- |
| [`main`](https://github.com/PritishMete/BusPass/tree/main) | Android passenger + conductor application |
| [`admin`](https://github.com/PritishMete/BusPass/tree/admin) | Administrative web panel |

## Admin Source

The current admin implementation was supplied from:

https://github.com/priyanka352/BUS-ADMIN

See [`ADMIN_SOURCE.md`](ADMIN_SOURCE.md) for the migration/security note.

## Local Admin Development

For the React/Vite admin project:

```bash
npm install
npm run dev
```

Production build:

```bash
npm run build
```

Firebase Hosting configuration should target the same BusPass Firebase project used by the live admin deployment.

## Environment Configuration

Do not hardcode production secrets or unrestricted API keys in source code.

Use local environment variables for values such as:

```text
VITE_GOOGLE_MAPS_API_KEY=
```

A template is provided in `.env.example`.

## Security

Never commit:

```text
.env
.env.*
service-account*.json
*-firebase-adminsdk-*.json
private keys
admin passwords
API secrets
```

Firebase Admin SDK credentials belong only in trusted server-side environments or secret managers. They must never be shipped to a React browser bundle.

> **Security notice:** the provided upstream repository contains a Firebase Admin SDK service-account JSON file in its public history and a hardcoded Google Maps API key. Those credentials have intentionally not been copied here. The exposed credentials should be revoked/rotated before further production use.

---

**BusPass Admin** is the operational layer of the BusPass ecosystem, supporting route, vehicle, report and journey administration alongside the Android passenger/conductor experience.
