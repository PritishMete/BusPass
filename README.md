# BusPass

BusPass is an Android public-transport companion app for passengers and conductors. It combines route discovery, journey booking, digital tickets, wallet recharge and passbook history with conductor-side passenger and live-location workflows.

## Product Scope

- Passenger sign-in, registration, profile and saved routes
- Route and nearest-stop discovery with map support
- Normal and group/other-passenger booking flows
- QR ticket display and conductor-side validation
- Wallet balance, recharge and passbook history
- Emergency notifications, issue reporting and referrals
- Conductor route setup, live location and passenger list views
- Optional SMS ticket fallback and UPI app hand-off

## Technology

- Java Android application, namespace `com.pritish.smartbuss`
- Android Gradle Plugin 8.10.1, compile SDK 34, minimum SDK 29
- Firebase Authentication and Realtime Database
- Google Maps/Places/Location, OSMDroid and route helpers
- Retrofit/OkHttp, ZXing/JourneyApps QR scanning, Razorpay Checkout
- Lottie, Material components, MPAndroidChart and supporting UI libraries

## Local Setup

1. Install Android Studio and a JDK supported by the installed Android Gradle Plugin.
2. Obtain the project Firebase configuration from the project owner and place it at `app/google-services.json`. This file is intentionally ignored by Git.
3. Add valid Google Maps/Places configuration through the Android resource/local release process. Do not commit API keys.
4. Open the project in Android Studio or run `gradlew.bat :app:assembleDebug` from the repository root.

## Verification

The repository CI workflow runs:

```text
gradlew.bat clean test lint assembleDebug
```

CI requires the `GOOGLE_SERVICES_JSON_B64` secret. Payment, Firebase and map integrations also require valid project-side configuration and cannot be verified by an offline unit test alone.

## Security Notes

- Firebase Realtime Database rules are an operational requirement and are not included in this client repository.
- Razorpay payment verification must happen on a trusted server before treating a recharge as final.
- The current phone-validation client points at an HTTP endpoint; production deployments should migrate that service to HTTPS.
- Release builds should use restricted Google API keys, Play App Signing, and a backend-mediated payment flow.

See [docs/PROJECT_SUMMARY.md](docs/PROJECT_SUMMARY.md) for a concise project brief and [docs/PORTFOLIO_CASE_STUDY.md](docs/PORTFOLIO_CASE_STUDY.md) for showcase-ready detail.
