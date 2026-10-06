# Home — Your week, served

Mode: Operate. Platform: iOS/iPadOS.

## Authority and scope

Approved by the user on October 6, 2026. Reference: [Your week, served](../mocks/decision/week-and-table.png). This extends the existing Apple world defined by [native design notes](../../ios/DESIGN_NOTES.md) and `Theme.swift`: charcoal, ivory, peach, native controls, and Barlow display type remain. The newly approved visual ingredient is illustrative food photography for matching bundled recipes. The root `DESIGN.md` and design sidecar retain historical Android documentation; this surface does not rewrite them.

## Composition and navigation

Home launches first. Five native tabs expose Home, Plan, Recipes, Shop, and Assistant. Each tab has its own navigation stack and a top-right Settings toolbar action opening a sheet with Done.

The current-week strip selects a date. An open meal composition pairs the selected recipe title, planned portions, total and hands-on time, and Start cooking action with matching food imagery when available. The title opens recipe detail with the planned portion; Start cooking passes that same portion into cooking. View plan selects the Plan tab.

A preview shows the next valid planned meal after the selected date. It says Tomorrow only when that meal is tomorrow relative to today; otherwise it uses the actual date. With no valid later meal, the preview is absent. The discovery shelf uses charcoal in light appearance and raised charcoal in dark appearance, and links to a real library recipe, preferring chicken pesto pasta among recipes unplanned in the next seven days, then another unplanned recipe, then the first library recipe. Browse recipes and Create with Assistant remain directly reachable.

The shopping row covers **this calendar week plus next week** regardless of the selected day. It uses the same aggregation, pantry exclusions, and two-week check identifiers as Shop. It shows a build-list prompt for no planned meals, a completion state for zero remaining items, or the remaining item count. Opening Shop selects its tab; Home does not reset a previously changed Shop date/range filter. Pantry inventory remains future scope.

## Imagery and honest fallbacks

Generated serving illustrations are bundled locally: `HomeChicken` for core `garlic-herb-chicken`, `HomeTacos` for core `beef-tacos`, and `HomePasta` for core `chicken-pesto-pasta`. Both recipe origin and ID gate artwork. Other core recipes and all personal/custom recipes use an honest text-first layout without substitute images. Artwork is illustrative, not a photograph of a verified cooked result. Images contain no interface text or controls and are hidden from accessibility; native recipe text carries the identity.

## State and adaptation

All content derives from the local store. An unplanned selected date asks “What’s for dinner?” and opens the selected day’s planner; a missing recipe says it is no longer in the library and offers a replacement. An empty library keeps explanatory discovery copy plus library and Assistant routes. Calendar-day notifications and reactivation refresh today; when the date changes, selection resets to the new today and the week strip updates.

The user requested a compact header after implementation review: Home and Settings share the inline native navigation bar; content begins with 4 pt top padding and the week-to-meal gap is 12 pt. Home uses the available window width, with a centered 900 pt maximum composition, 20 pt horizontal padding below 720 pt and 32 pt above. Meal text and artwork sit beside each other only at widths of at least 390 pt and below accessibility Dynamic Type categories; otherwise they stack. The featured recipe title uses a 36 pt display size under that width/type condition and 44 pt otherwise, including the text-first recipe layout; empty-state headings remain 44 pt. The photo is 250 pt high in the horizontal feature and 220 pt when stacked. The discovery content stacks at accessibility sizes. The week strip scrolls when needed, with a minimum 44 pt target width and 60 pt height. At accessibility sizes, day labels retain intrinsic width with 12 pt horizontal padding on each side instead of equal-width distribution. Assistant and shopping rows omit their decorative leading icons and trailing chevrons at accessibility sizes to preserve text width. The selected day uses ivory text on charcoal in light appearance and charcoal text on peach in dark appearance. Supporting copy on the custom canvas uses `MealTheme.secondaryText` (`#645E57` light / `#C4BEB5` dark); this is source palette evidence, not a measured rendered contrast claim. Day buttons expose their date, today/planned state, and selected trait. Native Dynamic Type and available-width adaptation outrank mockup proportions.

## Implementation comparison and validation boundary

Source comparison against `HomeView.swift`, `RootView.swift`, and `Theme.swift` confirms the chosen hierarchy: week selection, open meal feature, next-meal preview, dark discovery shelf, then useful destinations. Palette and typography reuse the existing native theme; imagery is the scoped extension. The reference’s tomorrow example becomes a date-aware next-planned-meal preview, and recipes without matching artwork retain text-first layouts. Native tabs, sheets, flexible text, and accessibility reflow replace fixed mockup geometry. The implementation uses the native Home navigation title instead of the reference’s oversized display heading, places Browse recipes below the discovery item rather than beside its heading, and renders Assistant/shopping as plain action rows instead of white rounded cards with colored icon wells. The implementation also adds real total/hands-on times and explicit shopping-window copy in place of the reference’s sample shopping ingredients. These are observable source-to-reference differences, not verified rendered equivalence.

This brief records source evidence and user approval of the reference. It does not claim simulator screenshots, completed interaction tests, VoiceOver verification, or final visual acceptance. Device review must cover light/dark appearance, narrow windows, large text, empty and missing content, day rollover, navigation and Settings, portion handoff, and shopping parity with Shop set to the current-week two-week range.

Generic workflow gates and legacy tool/plate state are not native validation evidence. Acceptance must rely on native tests, rendered review, and the independent reviewer’s final verdict; these docs do not infer completion from workflow status.
