# Firebase sync setup

MealOS 0.6 keeps its local-first behavior and adds optional Google sign-in. After sign-in, Firestore synchronizes personal recipes, weekly plans, pantry selections, favorites, ratings, notes, and checked grocery items. Core recipes remain bundled with the app. Active timers, scanned images, and Recipe Assistant conversations remain on the device.

## Firebase Console

The Android app is registered as `com.mealos` in Firebase project `mealos-af8fc`. Keep both the debug and permanent release SHA-1 and SHA-256 fingerprints registered. Enable Google under **Authentication → Sign-in method**.

Create a Cloud Firestore Standard database. Use production mode and choose the region deliberately; the database location cannot be changed later. The checked-in `google-services.json` contains public Firebase application identifiers, not a service credential.

## First owner sign-in

Install a build, open the navigation drawer, and choose **Sign in with Google**. In Firebase Console, open **Authentication → Users** and copy that account's UID.

Replace `REPLACE_WITH_FIREBASE_OWNER_UID` in `firestore.rules` with the copied UID, then deploy the rules:

```sh
firebase use mealos-af8fc
firebase deploy --only firestore:rules
```

The rules allow only that owner UID to access `/users/{uid}/mealos/*`, validate the synchronized document shape, and reject deletes. Until the placeholder is replaced and deployed, sign-in succeeds but cloud sync correctly reports that it needs attention; use **Retry sync** afterward.

Set these Railway variables:

- `FIREBASE_PROJECT_ID=mealos-af8fc`
- `MEALOS_ALLOWED_FIREBASE_UID=<copied UID>`

Redeploy Railway. Firestore data remains isolated by the signed-in UID, while Railway additionally rejects every UID except the configured owner.

## Sync behavior

On the first successful connection, an empty cloud account is seeded from the device. If the cloud account already contains MealOS data, the cloud copy initializes that device. Subsequent changes are written after a short debounce and Firestore's Android cache keeps reads and writes available offline. Preferences/planning and personal recipes are separate cloud documents so editing a recipe does not overwrite a plan change.

Firestore resolves simultaneous edits to the same document with last-write-wins behavior. MealOS exposes connecting, saving, synced, offline, and error states in the drawer. Signing out stops synchronization but does not erase the local copy.
