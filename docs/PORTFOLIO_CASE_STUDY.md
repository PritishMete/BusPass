# BusPass Case Study

## Project

BusPass is an Android transit application designed around two real-world roles: the traveler buying and managing a journey, and the conductor operating the route.

## Contribution Surface

The codebase covers the end-to-end client experience: authentication, route and stop selection, booking, digital tickets, wallet and passbook views, map experiences, conductor operations, notifications and support flows.

## Key User Journey

1. A traveler signs in or creates an account.
2. The traveler selects a route and stops using the map and stop data.
3. The traveler books a ticket or books for another passenger.
4. The ticket can be displayed for conductor-side validation.
5. Wallet and passbook screens expose balance and recharge history.
6. A conductor can configure service details, share location and review current passengers.

## Technical Decisions

- Firebase Realtime Database was used for synchronized booking, profile and operational data.
- Google location services and map libraries support location-aware journeys.
- QR and external payment/UPI integrations keep the client aligned with common Android travel flows.
- SharedPreferences preserve lightweight session and presentation state between screens.

## Lessons and Next Steps

The next production iteration should introduce a typed repository layer, server-authoritative ticket/payment verification, Firebase security rules tested in the emulator, HTTPS-only network clients, structured release logging and automated UI tests for passenger booking and conductor validation.
