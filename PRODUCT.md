# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Stack

Native Android app. Kotlin and Jetpack Compose were delegated to Codex as the recommended implementation for a foldable-first, offline-first experience.

## Users

The primary user is the owner of a Samsung Galaxy Z Fold8 Ultra who wants to choose familiar meals, plan a week, shop once, and follow clear cooking steps without juggling multiple apps or paper notes.

## Product Purpose

MealOS turns a small, repeatable meal rotation into a low-effort weekly cooking system. Success means planning meals quickly, buying one consolidated grocery list, and cooking confidently with minimal cleanup.

## Positioning

MealOS treats the foldable device as a kitchen work surface: the cover screen is a fast planner and shopping companion, while the unfolded screen becomes a two-pane cooking station with steps, ingredients, and timers visible together.

## Operating Context

- Used at home while planning, shopping, and cooking.
- Primary hardware target: Samsung Galaxy Z Fold8 Ultra.
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
- Favorites, ratings, and personal notes are stored locally.
- Offline-first behavior; no account or server is required for the MVP.
- The recipe model remains simple and data-driven so new recipes can be added without redesigning the app.
- Personal recipes can be created and edited locally with a fold-aware guided editor; core recipes are customized as personal copies, and unsaved work is protected by an explicit discard confirmation.
- MealOS recipe documents use an explicit versioned JSON schema and can enter the app through Android Share or file open, always as a clearly identified, reviewable draft.
- The Recipes screen provides a reusable ChatGPT prompt so AI-assisted creation does not require an API key or ongoing in-app AI cost.
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
