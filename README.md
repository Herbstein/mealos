# MealOS / Forkware

Forkware is the actively developed, local-first meal planner for iPhone and iPad. It includes a 15-recipe core rotation, meal planning, consolidated groceries, pantry exclusions, favorites, notes, ratings, and cooking with timers.

## iPhone and iPad

The universal SwiftUI client lives in [`ios/`](ios/README.md). Open `ios/Forkware.xcodeproj` in full Xcode. See the [Apple client README](ios/README.md) for setup, validation, and TestFlight releases.

## Android — deprecated

As of October 6, 2026, the Android app is deprecated and no longer developed or maintained. No further Android features, fixes, dependency updates, or APK releases are planned. The automatic Android release workflow has been retired.

The source in `app/` and the root Gradle build remain for historical reference. See the [Android archive notes](app/README.md) for the former build and product details.

The Apple catalog exporter still reads `app/src/main/java/com/mealos/data/RecipeCatalog.kt`. Preserve this seed-data dependency until it is migrated; recipe-data updates for the Apple app remain in scope.

## Shared services and product guidance

The Apple client uses the Rust backend for Apple sign-in and Claude assistant requests, with Apple PCC fallback. Firebase remains for legacy library synchronization. The separate `bridge/` assistant is deprecated and retained only for older clients. Active development targets `ios/`, `backend/`, and shared data/services needed by the Apple app.

See [Firebase sync](docs/FIREBASE_SYNC.md), [Recipe Assistant setup](docs/RECIPE_ASSISTANT.md), [product decisions](PRODUCT.md), and the [visual system](DESIGN.md). Android-specific instructions in these documents are historical reference.

The [Rust backend](backend/README.md) contains the new Postgres and Apple authentication foundation. The [backend and recipe data plan](docs/BACKEND_PLAN.md) describes the planned migration to a Railway Serverless API and Postgres, including importing, versioning, sharing, offline sync, and estimated hosting costs.
