# Admin Source Reference

The BusPass admin implementation was provided from:

https://github.com/priyanka352/BUS-ADMIN

Live deployment:

https://buspassadmin.web.app/

## Security note

The upstream public repository currently contains a Firebase Admin SDK service-account JSON file and a hardcoded Google Maps API key. These credentials are sensitive or security-relevant and have intentionally not been copied into this branch.

Before migrating the complete admin source into this repository:

1. Revoke/rotate the exposed Firebase service-account key in Google Cloud / Firebase.
2. Rotate or restrict the exposed Google Maps API key.
3. Replace hardcoded configuration with environment variables where appropriate.
4. Verify Firebase Authentication and Realtime Database security rules.
5. Keep service-account credentials only in trusted server-side secret storage.

This branch is reserved for the BusPass admin panel and can receive the sanitized admin source after credential rotation.
