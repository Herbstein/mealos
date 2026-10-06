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

## Dated meal plans

Preferences payload schema 2 stores each plan entry's `day` as a calendar epoch day
(days since 1970-01-01), shared by Android and Apple. The Firestore envelope stays
at schema 1, as do recipe documents. Update both clients before editing a shared
plan; older clients cannot read the new preferences payload.

Legacy cloud and Apple plans map Monday–Sunday to the week of migration. Legacy
Android device-only plans map their rolling day offsets to the migration date.
The original format did not record a date, so earlier dates cannot be recovered.
Migration is persisted locally and upgraded in the cloud, preventing dates from
moving on later launches. Each date holds one dinner with its own portion choice.

The calendar navigates consecutive Monday–Sunday weeks. Shopping totals cover a
selected 1–4 week window (two weeks initially), retaining pantry exclusions and
separating incompatible units. Usage counts show meals and weeks per ingredient;
these are a basis for comparing pack sizes, not automatic pack-size or shelf-life
recommendations. Grocery checkmarks are scoped to the window and quantity.
