# MealOS for Android — deprecated

As of October 6, 2026, the Android app is deprecated and no longer developed or maintained. No further features, fixes, dependency updates, or APK releases are planned. Active development continues in [Forkware for iPhone and iPad](../ios/README.md).

The Android source and root Gradle build remain for historical reference. The instructions below describe the retired app; run historical build commands from the repository root.

`src/main/java/com/mealos/data/RecipeCatalog.kt` remains the seed-data source consumed by `ios/scripts/export_catalog.py`. Preserve that file and path until the Apple catalog pipeline is migrated. Shared recipe-data changes needed by the Apple app are still allowed; they do not resume Android development.

## Historical local build

Open the project in Android Studio, select an Android 13+ device, and run the `app` configuration.

From a terminal:

```sh
GRADLE_USER_HOME=.gradle-user ./gradlew testDebugUnitTest assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Releases retired

Android APK releases have ended. The tag-triggered `Release MealOS` workflow has been removed; creating a version tag no longer publishes an APK. Existing releases and signing material are retained as historical artifacts.

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
- A locally remembered Sonnet, Opus, or Haiku selector with a server-side model allowlist
- Optional Google sign-in with Firestore sync for recipes, plans, pantry, favorites, ratings, notes, and grocery state
- Printed-recipe and gallery import through ML Kit document scanning and on-device OCR
- Core recipes remain structured in `RecipeCatalog.kt`; personal and imported recipes persist locally through the same simple model

See `../docs/FIREBASE_SYNC.md` for Firebase and Firestore setup, `../docs/RECIPE_ASSISTANT.md` for Railway configuration, `../docs/CHATGPT_RECIPE_PROMPT.md` for the no-server fallback, `../PRODUCT.md` for product decisions, and `../DESIGN.md` for the visual system.
