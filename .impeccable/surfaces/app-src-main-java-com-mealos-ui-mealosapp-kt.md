---
version: 1
slug: "app-src-main-java-com-mealos-ui-mealosapp-kt"
primary_target: "app/src/main/java/com/mealos/ui/MealOsApp.kt"
related_targets: []
---

# MealOS primary app surface

## Scope and mode

- Mode: Operate.
- Boundary: the native Android app shell, with cooking mode as the defining surface.
- Primary device: Samsung Galaxy Z Fold8 Ultra at 2520 × 1080 folded and 2504 × 2256 unfolded.

## Audience, job, and constraints

- One home cook planning familiar meals, shopping once, and cooking with messy hands.
- The active step, ingredients, and timers carry equal importance when unfolded.
- Navigation stays hidden until the user opens a Material drawer, preserving horizontal workspace.
- Cooking mode provides a compact previous-step preview immediately above the current step, plus next-step previews below it; every preview includes supporting instruction text and navigates without changing timer state.
- The cover screen reflows to one pane; fold/unfold keeps scroll, timers, notes, and current step.
- Android system Back, edge-to-edge insets, 48 dp targets, dark theme, large type, and screen-reader labels are mandatory.

## Chosen direction

Mise en Place Board. MealOS behaves like a prepared kitchen station: deep green enamel holds persistent working state, warm cream trays hold readable content, yellow marks active timing, and burnt orange marks the decisive next action. The memorable moment is an unfolded prep station where the current instruction and the ingredient/timer workspace balance across the hinge.

Approved comp: `.impeccable/mocks/mise-en-place-3.webp`.

Approved modifications:

- Remove the permanently visible navigation rail; use a hidden Material navigation drawer.
- Add a compact previous-step preview immediately above the current-step tray, and include concise instruction descriptions in previous and next previews.
- Do not require bespoke recipe illustrations. Replace the tray drawing with reusable ingredient/equipment pictograms and code-native instruction content.

## Sampled visual system

- Deep enamel ground: `#083224`.
- Raised enamel: `#234633`.
- Warm cream content surface: `#F3F0E8`.
- Warm neutral line/material: `#D1C3B0`.
- Decisive action orange: `#C84F13`.
- Active timing yellow: `#C9A442`.
- Supporting sage: `#6F8477`.
- Type: condensed, heavy display scale for the current action; calm humanist sans for body and controls; small labels never replace legible body text.
- Corners: large rounded prep trays; compact controls are round or pill-shaped; avoid a uniform card grid.
- Elevation: tonal enamel-on-enamel and cream-on-green contrast; minimal soft shadow; no glass effects.

## Visible ingredient inventory

| Ingredient | Medium | Commitment |
|---|---|---|
| Hidden navigation drawer | Compose Material 3 | Four destinations, opened from a 48 dp menu target; no persistent rail. |
| Recipe identity header | Semantic Compose text and reusable ingredient icon | Name, portion mode, favorite state, and overflow actions. |
| Overall progress rule | Compose Canvas/ProgressIndicator | Thin painted measurement rule with five discrete positions. |
| Current-step tray | Semantic Compose | Largest cream field; step number, instruction, equipment note, and completion action. |
| Previous-step preview | Semantic Compose | One compact row labeled “Previous,” with title and instruction preview, immediately above the current step. |
| Next-step previews | Semantic Compose | Up to two compact rows with step numbers, instruction previews, and forward affordances. |
| Ingredient tray | LazyColumn | Normalized ingredient name, scaled quantity, pantry/check state, and reusable pictogram. |
| Timer shelf | Persisted Compose state and Android alarm integration | Two simultaneous large timers with pause and +1 minute controls; countdowns survive process death and reboot. Denied alert or precise-alarm access appears below the live controls with direct recovery actions. |
| Reusable pictograms | Material Symbols or simple code-native icons | Ingredient-category and equipment icons, not bespoke recipe drawings. |
| Drawer destinations | Material NavigationDrawer | Today, Plan, Recipes, Grocery, plus Settings. |

## Unresolved decisions

- Cloud backup and multi-device sync are intentionally deferred beyond the local-first MVP.
