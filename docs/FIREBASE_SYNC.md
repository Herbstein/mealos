# Firebase sync setup

MealOS keeps its local-first behavior while offering optional Google sign-in. After sign-in, Firestore synchronizes personal recipes, weekly plans, pantry selections, favorites, ratings, notes, and checked grocery items. Core recipes remain bundled with the app. Active timers, scanned images, and Recipe Assistant conversations remain on the device.

## Firebase Console

The Android app is registered as `com.mealos` in Firebase project `mealos-af8fc`. Keep both the debug and permanent release SHA-1 and SHA-256 fingerprints registered. Enable Google under **Authentication → Sign-in method**.

Create a Cloud Firestore Standard database. Use production mode and choose the region deliberately; the database location cannot be changed later. The checked-in `google-services.json` contains public Firebase application identifiers, not a service credential.

## User access

Deploy the checked-in rules once:

```sh
firebase use mealos-af8fc
firebase deploy --only firestore:rules
```

After the rules are deployed, any user who signs in with Google can use cloud sync automatically. No per-user Firestore document or administrative approval is required. Each account can access only its own `/users/{uid}/mealos/*` documents; the rules also validate the synchronized document shape and reject client deletes.

The Railway allowlist below is separate. It controls access to the subscription-backed Recipe Assistant so another signed-in user cannot spend the owner's AI allowance unless explicitly approved. A user who is absent from the Railway allowlist can still use recipes, planning, grocery lists, and Firebase cloud sync.

Set these Railway variables:

- `FIREBASE_PROJECT_ID=mealos-af8fc`
- `MEALOS_ALLOWED_FIREBASE_UIDS=<UID 1>,<UID 2>,<UID 3>`

Redeploy Railway after changing its list. The old singular `MEALOS_ALLOWED_FIREBASE_UID` variable remains supported while migrating, but should be removed after the plural variable is configured.

## Sync behavior

On the first successful connection, an empty cloud account is seeded from the device. If the cloud account already contains MealOS data, the cloud copy initializes that device. Subsequent changes are written after a short debounce and Firestore's Android cache keeps reads and writes available offline. Preferences/planning and personal recipes are separate cloud documents so editing a recipe does not overwrite a plan change.

Firestore resolves simultaneous edits to the same document with last-write-wins behavior. MealOS exposes connecting, saving, synced, offline, and error states in the drawer. Signing out stops synchronization but does not erase the local copy.
