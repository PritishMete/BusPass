# BusPass Project Summary

## One-Line Summary

BusPass is a two-sided Android transit workflow that connects passenger booking and wallet journeys with conductor-side validation and live route operations.

## Problem

Passengers need a single place to find routes, choose stops, book journeys, keep tickets and fund a travel wallet. Conductors need a practical way to manage service state, see passenger records and validate tickets in the field.

## Solution

The app provides separate traveler and conductor experiences backed by Firebase. Map and stop utilities support route selection, QR/NFC/Bluetooth-related validation flows support ticket operations, and local wallet/passbook screens provide transaction visibility.

## Core Modules

| Area | Capabilities |
| --- | --- |
| Identity | Login, signup, forgot-password flow, traveler/conductor routing |
| Passenger | Home, route search, normal and group booking, ticket history |
| Wallet | Balance display, UPI app hand-off, Razorpay checkout, passbook |
| Maps | Current location, nearest stops, route drawing and bus location |
| Conductor | Service controls, route map, current bookings and passenger list |
| Support | Help, referral, issue reporting and emergency notifications |

## Engineering Highlights

- Java Android implementation with a clear passenger/conductor split
- Firebase Realtime Database listeners for live booking and location updates
- Reusable adapters and model classes for routes, journeys, tickets and passbook entries
- Activity Result APIs for external UPI applications and profile image selection
- Local persistence for session state, wallet presentation and notifications

## Current Release Considerations

This is a portfolio/demo codebase rather than a production-ready payment or transit backend. Before release, configure Firebase rules, move payment verification server-side, replace the HTTP phone-validation endpoint with HTTPS, restrict maps keys by package/SHA-1, and remove or gate verbose debug logging.
