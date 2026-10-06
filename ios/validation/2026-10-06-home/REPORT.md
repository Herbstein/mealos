# Home validation — October 6, 2026

Implemented the approved “Your week, served” direction in native SwiftUI. Home launches first; five tabs are Home, Plan, Recipes, Shop, Assistant. Settings opens from a top-right gear in each root destination. The local store drives selected-day meals, upcoming meals, recipe discovery, and the current-week-plus-next shopping summary. Illustrative artwork is bundled for three matching core recipes; other recipes retain a text-first presentation.

## Checks

- Universal Debug simulator build succeeded, signing disabled.
- All 15 existing Swift core tests passed (`/tmp/forkware-home-core.log`).
- Current XCTest runners: Home overview/navigation and max-accessibility-size tests passed on iPhone 17 Pro and iPad Pro 11-inch, iOS 26.5 (`/tmp/forkware-home-fresh-runners.xcresult`). Navigation test covers default Home, Settings sheet, planning today and another date, returning to today, cooking, Shop/Assistant/Recipes shortcuts, and persisted plan after relaunch.
- Standard captures: iPhone light appearance, iPad dark appearance. Accessibility uses the largest accessibility category. These are native simulator captures, not browser mockups.
- Visual correction batch: selected-day contrast in dark mode, distinct raised discovery surface in dark mode, and compact horizontal title sizing.
- Earlier iPad test failed because a recipe row was offscreen. Picker scrolling fixed the test. Stale XCTest runner execution was detected and resolved by removing only the disposable simulator test runners before reinstallation; use the fresh-runner result above rather than earlier runs.
- Xcode project regeneration changed formatting. Build settings were compared structurally to HEAD and existing build number/team restored; no build-setting differences remain.

## Limits

Physical-device ergonomics, VoiceOver traversal, landscape/multitasking and every appearance/device combination were not verified in this change. Generated food images are serving illustrations, not exact photographs of the recipe output. Pantry inventory tracking remains future work; existing pantry exclusions continue to apply. Final independent review is recorded below.

## Review correction batch

Independent review requested stronger secondary-text contrast, intrinsic-width weekday targets at maximum Dynamic Type, and removal/reflow of decorative symbols at accessibility sizes. Applied an adaptive secondary text token (#645E57 light, #C4BEB5 dark), intrinsic weekday width with 12 pt side padding at accessibility sizes, and text-first utility rows at those sizes.

The final native build and both Home tests passed on both devices in `/tmp/forkware-home-review-fixes.xcresult`. Full large-text coverage includes overlapping scroll captures under `.impeccable/review/home/`; representative first/last captures are included here. `git diff --check` passes.

Impeccable's generic comp-to-web workflow remains open at the plates stage; it is not claimed as passed SwiftUI validation. Native source review, simulator captures and XCTest are the evidence for this implementation. No HTML/CSS detector was run for native views.

## Independent verdict

Disposition: **ship**, scoped to the three review fixes. The reviewer scored contrast, accessibility week-strip spacing, and accessibility utility rows **resolved**, with no visible regression from that batch. Reported secondary-text token contrast: 5.78:1 on ivory and 9.53:1 on charcoal. This verdict does not certify the incomplete generic web workflow.

## Compact header follow-up

At the user's request, Home now uses an inline navigation title with Settings in the same row. Top content inset is 4 pt, weekday targets have a 60 pt minimum height, and the week-to-meal gap is 12 pt. Other section spacing is unchanged. Build and both Home tests passed on iPhone and iPad, including maximum accessibility text, in `/tmp/forkware-home-compact-header.xcresult`. Standard iPhone/iPad captures were visually inspected; screenshots are in `compact-header/`.
