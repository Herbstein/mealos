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

To publish an update (use the next semantic version):

```sh
git tag v0.4.0
git push origin v0.4.0
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
- A fold-aware manual recipe editor with ingredients, steps, timers, equipment, leftovers, and cooking preview
- Versioned `.mealosrecipe` files that can be shared to or opened with MealOS for review before saving
- A built-in ChatGPT recipe prompt; copied recipes remain local and require no in-app AI subscription
- A native, locally persisted Recipe Assistant with a stateless Railway request gateway
- Printed-recipe and gallery import through ML Kit document scanning and on-device OCR
- Core recipes remain structured in `RecipeCatalog.kt`; personal and imported recipes persist locally through the same simple model

See `docs/RECIPE_ASSISTANT.md` for Railway and release configuration, `docs/CHATGPT_RECIPE_PROMPT.md` for the no-server fallback, `PRODUCT.md` for product decisions, and `DESIGN.md` for the visual system.
