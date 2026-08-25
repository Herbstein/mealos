# MealOS

MealOS is a local-first Android meal planner built for the Samsung Galaxy Z Fold8 Ultra. It combines a 15-recipe core rotation, weekly planning, consolidated grocery lists, pantry exclusions, favorites, notes, ratings, leftovers, and a fold-aware cooking mode with timers.

## Run it

Open the project in Android Studio, select an Android 13+ device, and run the `app` configuration.

From a terminal:

```sh
GRADLE_USER_HOME=.gradle-user ./gradlew testDebugUnitTest assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

## Product shape

- Native Kotlin and Jetpack Compose
- Local persistence with DataStore; no account or network required
- Adaptive cover and unfolded layouts with a drawer instead of a permanent sidebar
- Equal-weight cooking panes when unfolded, including previous/next step previews
- Native cooking timers and notifications
- Recipes are structured Kotlin data in `RecipeCatalog.kt`, making the core rotation straightforward to extend

See `PRODUCT.md` for product decisions and `DESIGN.md` for the visual system once generated.
