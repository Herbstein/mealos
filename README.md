# MealOS

MealOS is a local-first Android meal planner built for the Samsung Galaxy Z Fold8 Ultra. It combines a 15-recipe core rotation, weekly planning, consolidated grocery lists, pantry exclusions, favorites, notes, ratings, leftovers, and a fold-aware cooking mode with timers.

## Run it

Open the project in Android Studio, select an Android 13+ device, and run the `app` configuration.

From a terminal:

```sh
GRADLE_USER_HOME=.gradle-user ./gradlew testDebugUnitTest assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Private releases

MealOS publishes private, signed APK releases from GitHub tags. The release signing key is held in GitHub Actions secrets and never committed.

To publish an update:

```sh
git tag v0.2.0
git push origin v0.2.0
```

The `Release MealOS` workflow tests the app, derives a monotonically increasing Android version code from the tag, builds the signed APK, and attaches it to the matching private GitHub Release.

The initial setup creates an ignored `.release` directory containing the only readable local copy of the permanent signing key and its credentials. Back up that directory in encrypted storage before publishing the first version; neither GitHub nor Android can reconstruct a lost signing key.

On the Fold, add `https://github.com/kanki6315/mealos` to Obtainium. In Obtainium's GitHub source settings, provide a fine-grained GitHub token restricted to this repository with read-only Contents access. Obtainium reads GitHub Releases; temporary GitHub Actions artifacts are not the update source.

The first signed release cannot update a debug-signed installation. Uninstall the debug build once before installing the first release build. Every later release must use the same signing key.

## Product shape

- Native Kotlin and Jetpack Compose
- Local persistence with DataStore; no account or network required
- Adaptive cover and unfolded layouts with a drawer instead of a permanent sidebar
- Equal-weight cooking panes when unfolded, including previous/next step previews
- Native cooking timers and notifications
- Recipes are structured Kotlin data in `RecipeCatalog.kt`, making the core rotation straightforward to extend

See `PRODUCT.md` for product decisions and `DESIGN.md` for the visual system once generated.
