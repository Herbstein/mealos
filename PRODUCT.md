# Product

<!-- impeccable:product-schema 1 -->

## Development status

As of October 6, 2026, Android is deprecated and no longer developed or maintained. All new client work targets Forkware for iPhone and iPad. Android-specific capabilities, hardware constraints, and interactions below document the historical implementation, not requirements for future Android work. Shared Firebase, Railway, and seed recipe data remain active dependencies of the Apple app.

## Platform

adaptive

## Stack

Active client: universal iPhone/iPad app in SwiftUI, retaining the offline-first meal model, Firebase account data, and Railway recipe assistant. The original Kotlin/Jetpack Compose Android client is retained as deprecated source.

## Users

The primary user wants to choose familiar meals, plan a week, shop once, and follow clear cooking steps without juggling multiple apps or paper notes. The original Android app targets a Samsung Galaxy Z Fold8 Ultra; the Apple client supports a regular iPhone for planning/shopping and an iPad as a kitchen screen.

## Product Purpose

MealOS turns a small, repeatable meal rotation into a low-effort weekly cooking system. Success means planning meals quickly, buying one consolidated grocery list, and cooking confidently with minimal cleanup.

## Positioning

MealOS treats the foldable device as a kitchen work surface: the cover screen is a fast planner and shopping companion, while the unfolded screen becomes a two-pane cooking station with steps, ingredients, and timers visible together.

The Apple app gives the iPhone emphasis on planning and shopping, while the iPad provides a spacious cooking station. Essential features remain available on both. Cooking timers and step positions remain device-local; live cloud handoff is deferred.

## Operating Context

- Used at home while planning, shopping, and cooking.
- Android hardware target: Samsung Galaxy Z Fold8 Ultra.
- Apple hardware: regular iPhone and iPad, with layouts based on available window space.
- Target display resolutions: 2520 × 1080 cover display and 2504 × 2256 unfolded display.
- Available cooking equipment: Ninja CRISPi DualZone glass air fryer, rice cooker, Ooni Volt, toaster, microwave, and standard pots and pans.
- Meals should favor familiar flavors, low hands-on effort, reusable ingredients, leftovers, and minimal cleanup.

## Capabilities and Constraints

- Seeded library of 15 core meals from the original meal-planning conversation.
- Recipe detail and cooking mode with large sequential steps, equipment-specific guidance, and multiple timers that survive app termination and phone restarts.
- Timer alerts recover clearly from denied Android notification access, and precise alarms degrade honestly when special access is unavailable.
- Portion choices for one, two, and “dinner + lunch tomorrow,” with recalculated ingredient quantities.
- Weekly meal planner and consolidated grocery list grouped by store category.
- Pantry staples can be excluded from shopping.
- Leftovers are explicit in planning and recipe guidance.
- Favorites, ratings, and personal notes are stored locally and optionally synchronized through the signed-in user's Firebase account.
- Offline-first behavior remains primary; planning, shopping, recipes, and cooking require no account or network. Google sign-in adds cloud recovery and device sync.
- The recipe model remains simple and data-driven so new recipes can be added without redesigning the app.
- Personal recipes can be created and edited locally with a fold-aware guided editor; core recipes are customized as personal copies, and unsaved work is protected by an explicit discard confirmation.
- MealOS recipe documents use an explicit versioned JSON schema and can enter the app through Android Share or file open, always as a clearly identified, reviewable draft.
- The Recipes screen provides a reusable ChatGPT prompt so AI-assisted creation does not require an API key or ongoing in-app AI cost.
- Recipe Assistant conversations, scan text, cited sources, and the current recipe draft live locally on the device. The Railway service receives recent context per request and stores no sessions or recipe data.
- Printed pages and gallery images enter through the ML Kit document scanner. OCR runs on-device; only extracted text is included in an assistant request.
- Generated recipes never save directly. They open in the existing editor as unsaved imported drafts with validation and discard protection.
- Recipe Assistant requests use short-lived Firebase identity tokens. Railway verifies the token and permits only UIDs in its configured allowlist; no permanent gateway credential is included in the APK.
- Firestore synchronizes personal recipes, plans, pantry state, favorites, ratings, notes, and grocery checkmarks beneath the signed-in Firebase UID. Timers and assistant conversations remain device-local.
- Avoid seafood and mushrooms. Pork should not be a core ingredient.

## Brand Commitments

- Product name: MealOS.
- Voice: calm, direct, familiar, and useful in a busy kitchen.

## Evidence on Hand

The original conversation defines the 15-meal rotation, appliance preferences, disliked foods, and required MVP workflows. No logo, photography, user research, nutrition claims, or commercial claims were supplied and none should be fabricated.

## Product Principles

- Make tonight’s next action obvious.
- Treat the unfolded phone as useful working space, not a stretched handset.
- Keep essential cooking information visible and touch targets generous.
- Prefer reliable offline behavior over account complexity.
- Reuse ingredients and equipment knowledge across the meal rotation.

## Accessibility & Inclusion

Cooking mode requires large readable type, high contrast, clear state changes, generous touch targets, screen-reader labels, and interaction patterns usable with one hand or messy hands.
