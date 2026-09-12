# BusPass Admin Panel

The `admin` branch documents and hosts the administrative side of the BusPass platform.

> **Live Admin Panel:** https://buspassadmin.web.app/
>
> **Android App:** [`main` branch](https://github.com/PritishMete/BusPass/tree/main)
>
> **Admin source reference:** https://github.com/priyanka352/BUS-ADMIN

## Overview

The BusPass Admin Panel is the operational web interface for managing and monitoring the BusPass public-transport ecosystem. It complements the Android passenger/conductor application on the `main` branch and works with shared Firebase-backed application data.

## Admin Capabilities

Based on the current administrative project, the panel includes functionality for:

- Administrator authentication
- Dashboard and operational statistics
- Bus creation and assignment workflows
- Route management
- Bus/live-location monitoring
- NFC assignment
- Pending report review and report history
- Administrative messaging
- Settings and password management

## Live Deployment

The admin interface is deployed with Firebase Hosting:

**https://buspassadmin.web.app/**

## Technology Stack

- React 19
- Vite
- React Router
- Material UI
- Emotion
- Firebase Authentication / Firebase services
- Framer Motion
- Lucide React
- date-fns

## Relationship to the Android App

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

The Android app handles passenger and conductor journeys, while the admin interface handles operational management and monitoring.

## Branch Structure

| Branch | Purpose |
| --- | --- |
| [`main`](https://github.com/PritishMete/BusPass/tree/main) | Native Android passenger + conductor application |
| [`admin`](https://github.com/PritishMete/BusPass/tree/admin) | Administrative web panel |

To switch locally:

```bash
git checkout admin
```

## Admin Development

The current admin implementation is a React/Vite project.

Typical local workflow:

```bash
npm install
npm run dev
```

Production build:

```bash
npm run build
```

Preview the production build locally:

```bash
npm run preview
```

## Firebase Hosting

The live administrative application is available at:

```text
https://buspassadmin.web.app/
```

Deployment requires access to the correct Firebase project and authenticated Firebase CLI configuration.

## Security

Administrative systems require stricter credential handling than client applications.

Never commit any of the following:

```text
.env
.env.*
service-account*.json
*-firebase-adminsdk-*.json
private keys
admin passwords
API secrets
```

Firebase Admin SDK service-account credentials must be stored only in a secure secret-management environment and should never be shipped to a browser-based React application.

> **Important:** the provided upstream admin repository currently contains a Firebase Admin SDK service-account JSON file in its public history. That key should be revoked/rotated in Google Cloud/Firebase before the project is treated as secure. It has intentionally not been copied into this repository documentation.

## Source Origin

The administrative implementation was supplied from:

https://github.com/priyanka352/BUS-ADMIN

This repository's `admin` branch is the BusPass-side home for the administrative project and should be used for future admin-specific work.

## App Repository

For passenger/conductor Android development, use:

https://github.com/PritishMete/BusPass/tree/main

---

**BusPass Admin** provides the operational layer behind the mobile experience—connecting route, vehicle, report and journey administration with the wider BusPass platform.
