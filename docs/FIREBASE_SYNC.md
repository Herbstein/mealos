# Firebase sync setup

MealOS 0.6 keeps its local-first behavior and adds optional Google sign-in. After sign-in, Firestore synchronizes personal recipes, weekly plans, pantry selections, favorites, ratings, notes, and checked grocery items. Core recipes remain bundled with the app. Active timers, scanned images, and Recipe Assistant conversations remain on the device.

## Firebase Console

The Android app is registered as `com.mealos` in Firebase project `mealos-af8fc`. Keep both the debug and permanent release SHA-1 and SHA-256 fingerprints registered. Enable Google under **Authentication → Sign-in method**.

Create a Cloud Firestore Standard database. Use production mode and choose the region deliberately; the database location cannot be changed later. The checked-in `google-services.json` contains public Firebase application identifiers, not a service credential.

## Approving users

Deploy the checked-in rules once:

```sh
firebase use mealos-af8fc
firebase deploy --only firestore:rules
```

Install a build and choose **Sign in with Google**. In Firebase Console, open **Authentication → Users** and copy the account's UID. Then open **Firestore Database → Data**, create a top-level collection named `mealosAccess`, and create a document whose document ID is that exact UID. Add one Boolean field named `enabled` with the value `true`.

Repeat only the access-document step for each approved friend. The rules do not need to be redeployed when the allowlist changes. Setting `enabled` to `false` or deleting the access document revokes cloud sync. Clients cannot list, read, or edit the access collection; it is managed through the Firebase Console or an administrative service.

Each approved account can access only its own `/users/{uid}/mealos/*` documents. The rules also validate the synchronized document shape and reject client deletes. Before approval, Google sign-in succeeds but cloud sync reports that it needs attention; use **Retry sync** after adding the access document.

Set these Railway variables:

- `FIREBASE_PROJECT_ID=mealos-af8fc`
- `MEALOS_ALLOWED_FIREBASE_UIDS=<UID 1>,<UID 2>,<UID 3>`

Redeploy Railway after changing its list. Firestore and Railway have separate authorization boundaries, so an approved user must appear in both allowlists to use both cloud sync and Recipe Assistant. The old singular `MEALOS_ALLOWED_FIREBASE_UID` variable remains supported while migrating, but should be removed after the plural variable is configured.

## Sync behavior

On the first successful connection, an empty cloud account is seeded from the device. If the cloud account already contains MealOS data, the cloud copy initializes that device. Subsequent changes are written after a short debounce and Firestore's Android cache keeps reads and writes available offline. Preferences/planning and personal recipes are separate cloud documents so editing a recipe does not overwrite a plan change.

Firestore resolves simultaneous edits to the same document with last-write-wins behavior. MealOS exposes connecting, saving, synced, offline, and error states in the drawer. Signing out stops synchronization but does not erase the local copy.
