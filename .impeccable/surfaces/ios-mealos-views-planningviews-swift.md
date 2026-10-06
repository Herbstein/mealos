---
version: 1
slug: "ios-mealos-views-planningviews-swift"
primary_target: "ios/MealOS/Views/PlanningViews.swift"
related_targets: ["ios/MealOS/Core/Models.swift"]
---

# Plan calendar

Mode: Operate. Extend the existing native Plan surface and its charcoal, peach, and ivory identity. User selected a month calendar with the selected day's meal below, plus a week view. Preserve one meal per civil date, portions, offline persistence, recipe access, cooking, and grocery aggregation.

## Direction contract
THESIS: See open dates and planned dinners before choosing the next meal. Replace the oversized tonight panel with a usable calendar.
OWN-WORLD: Existing adaptive Forkware tokens, SF text, native navigation and segmented control. Peach selects a date; an outline identifies today; a small meal marker identifies planned dates.
STORY: Browse a month, select a date, choose or change dinner. Switch to a seven-day agenda for meal names and portions together.
FIRST VIEWPORT: Native Meal plan title and Today action; Month/Week control; date navigation; Monday-first month grid; selected date and meal action below. iPad cells expose recipe names; iPhone uses compact markers and a full-width detail below.
FORM: User-selected calendar extension; no concept seed required for this bounded addition. Signature interaction: date selection updates the meal detail in place. Native sheets handle recipe selection.
FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance


## Implemented surface

`PlanView` defaults to Month and keeps a selected civil date. The month uses complete Monday–Sunday rows, adjacent-month dates, a peach selection, an action-tint today outline, meal dots, and a count of valid planned meals. Date selection updates the meal panel below. At available widths of at least 700 pt, calendar cells also show short recipe names. Week uses seven agenda rows with portions, recipe links, and native meal-picker entry points. Existing planning, removal, recipe detail, and cooking actions use the stored date and portion.

The source caps the composition at 1000 pt, uses 16/32 pt horizontal insets, 10 pt cell corners, and a 20 pt meal panel with 16 pt padding. Native semantic SF type, adaptive `MealTheme` surfaces, peach actions, and system controls extend the incumbent Apple system. Accessibility Dynamic Type stacks navigation and meal controls and makes the month grid horizontally scrollable at a minimum 560 pt width. These are surface implementation details, not new global tokens.

`PlanCalendar` retains civil-date encoding and adds month start, month movement, and complete month-row helpers. The underlying plan schema remains one meal per date; grocery aggregation continues through the existing shopping window.

## Evidence and limits

The [finish review](../review/plan/finish-review.md) records a ship verdict from twelve native calendar captures in `../review/plan/`: empty month, planned month, selected meal detail, week agenda, accessibility month, and accessibility week on each platform. The reviewed configurations are iPhone 17 Pro in light appearance and iPad Pro 11 in dark appearance, with default and accessibility XXXL text as captured. That rendered evidence supports this surface in those configurations; it does not establish all-device, all-theme, VoiceOver, keyboard, rotation, multitasking, or physical-hardware approval.

The parent reported 16 passing core tests, including leap-month/year date cases; two focused calendar UI tests passed on both platforms, and the initial cross-week grocery-persistence test passed on iPhone. The documenter inspected the source and review report but did not rerun those tests. Test definitions are in `ios/UITests/MealOSUITests.swift`; the captures and finish report supply the rendered evidence separately from source inspection.

No new raster asset was shipped. `DESIGN.md` and `.impeccable/design.json` remain unchanged: root tokens and sidecar previews still describe the historical Android green/orange system, while the root notice delegates current Apple authority to `ios/DESIGN_NOTES.md` and `Theme.swift`. That pre-existing platform drift is not repaired or adopted as new Apple guidance in this bounded calendar extension.
