# MealOS native iPhone and iPad design notes

## Overview

This is the native Apple-platform extension of the **Mise en Place Board** in [the inherited design system](../DESIGN.md), informed by [the product brief](../PRODUCT.md). The authorized scope is a universal iPhone/iPad app with shared product features and single- or two-pane cooking. The palette below is the current Apple visual authority; the inherited Android specification is historical.

Evidence: source review of `MealOS/Views/*.swift`, especially `Theme.swift`, and the app-level tint in `MealOSApp.swift`. This original source review is supplemented by the [September 10 simulator validation](validation/2026-09-10/REPORT.md). Values below describe implementation decisions; the report identifies which device configurations were actually rendered and tested. The October 6 Home extension is documented from `HomeView.swift`, `RootView.swift`, and `Theme.swift` against the approved [Your week, served reference](../.impeccable/mocks/decision/week-and-table.png). That approval selects the composition; it is not evidence of rendered implementation validation. Its surface behavior lives in [the Home brief](../.impeccable/surfaces/ios-home.md).

## Colors

As of October 6, 2026, the Apple client uses the installed dark icon as its palette reference: charcoal, soft peach, butter yellow, and warm ivory. This supersedes the inherited green/orange palette for iPhone and iPad. `Theme.swift` owns the implementation.

| Role | Light | Dark |
| --- | --- | --- |
| Canvas | Ivory `#F7F3EA` | Charcoal `#191918` |
| Custom reading panel | `#FFFCF6` | Raised charcoal `#272725` |
| Custom panel text | `#242321` | Ivory `#F7F3EA` |
| Home supporting text | `#645E57` | `#C4BEB5` |
| Links, tint, selection | Deep peach `#984C31` | Peach `#F3AD88` |
| Primary filled action | Peach `#F3AD88` with charcoal `#191918` labels | Same |
| Timer digits | Butter yellow `#E7CE70` on charcoal | Same |

Cooking panels and navigation now follow system appearance. The planning feature panel and timer wells remain charcoal in both appearances. Home’s discovery shelf uses charcoal in light appearance and raised charcoal in dark appearance. Its selected day uses ivory on charcoal in light appearance and charcoal on peach in dark appearance. Yellow is reserved for timing; notification recovery links use the action tint. Native list rows, forms, secondary labels, errors, separators, and material bars retain semantic system colours and accessibility behaviour. Assistant reading panels adapt to appearance, while user messages use ivory on charcoal.

Use native prominent buttons with explicit peach fill and charcoal labels. Pale peach is not suitable for standalone text on ivory, and white labels should not be used on peach. Home uses the opaque adaptive `MealTheme.secondaryText` role for supporting copy on its custom canvas, including date labels, timings, empty-state explanations, and action-row detail. This records the source palette pairing, not a measured rendered contrast result. The icon artwork remains unchanged.

## Typography

Barlow Condensed SemiBold retains the compact display character. `MealTheme.display` uses `.custom("BarlowCondensed-SemiBold", size: ..., relativeTo: .largeTitle)`, so the source opts into Dynamic Type scaling. The default display size is 38 pt; the planning and Assistant introduction use 44 pt, the cooking step uses 48 pt, the station heading uses 34 pt, and the settings statement uses 30 pt.

Home’s featured recipe title uses 36 pt at available widths of at least 390 pt below accessibility Dynamic Type categories, and 44 pt otherwise; its empty-state headings use 44 pt. Home’s discovery heading uses the same display face at 32 pt. Body and control text use native semantic SwiftUI styles rather than the Android Material body family. Cooking instructions use `.title2`; timer digits use the rounded system `.largeTitle` with monospaced digits. Display and instruction text in cooking explicitly allow vertical expansion. Font loading, wrapping, line height, and arm's-length readability still require device review.

## Layout

Measurements here are logical Apple points, not Android dp or physical display pixels.

Cooking measures **available view width** with `GeometryReader`. At 720 pt or wider, and only when Dynamic Type is below an accessibility category, the current-step panel and station workspace receive equal flexible widths in a top-aligned horizontal layout. Each pane scrolls independently. The gap is 24 pt, with 28 pt outer padding. Below 720 pt, or at any accessibility text category, the panels stack in one scroll view with 24 pt separation and 16 pt outer padding. This keeps the breakpoint meaningful for iPad window resizing and multitasking instead of assuming that every iPad has room for two panes.

The Android folding-feature seam is not reproduced. The native app uses a spacing gap, with no hardware-hinge rule. Current-step state is owned outside the layout branch and changes persist through the store; reflow is intended to retain the selected step. Scroll views are recreated between branches, so useful scroll-position preservation is not established by this source review.

Home uses a single scrollable composition capped at 900 pt, centered in the available window. Horizontal insets are 20 pt below 720 pt and 32 pt at wider widths; section spacing is 26 pt. Home uses an inline native navigation title alongside Settings, 4 pt top content padding, and a tighter 12 pt gap between the week strip and meal feature. At available widths of at least 390 pt, a matching food illustration sits beside the meal text; below that width or at accessibility Dynamic Type sizes, it stacks below. The photo is 250 pt high beside the text and 220 pt high when stacked. Recipes without artwork remain text-first. The week strip can scroll horizontally; day targets retain a 44 pt minimum width and 60 pt minimum height. At accessibility sizes, each day keeps its intrinsic text width plus 12 pt horizontal padding on each side rather than sharing equal width. The discovery content also stacks at accessibility sizes, while Assistant and shopping rows omit their decorative leading icons and trailing chevrons to preserve space for text. These native adaptations take priority over the approved mock’s fixed proportions.

Other screens use native lists, forms, navigation stacks, and sheets. The editor remains one form at all widths, replacing the Android 300 dp section outline and split preview composition. Preview opens the native cooking view. Assistant remains one conversation column at all widths, with content capped at 800 pt and a composer capped at 840 pt. It does not implement the Android equal-width conversation/proof panes; draft review opens the editor in a sheet.

Cooking Previous/Next controls use `ViewThatFits` to change from a horizontal row to a vertical arrangement when needed. The vertical arrangement puts Next first. Other horizontal rows, including ingredient quantities, timer controls, and editor fields, have no equivalent explicit accessibility reflow and need large-text QA.

## Elevation & Depth

Cooking uses flat tonal surfaces without explicit custom shadows, using charcoal and ivory tonal layers. Native lists and forms supply their platform container treatment. The Assistant composer and cloud-loading overlay use SwiftUI regular material. These are native utility surfaces; the inherited identity remains tonal rather than decorative glass. Actual translucency, reduced-transparency behavior, and separation from scrolling content remain unverified.

## Shapes

Native cooking trays use 24 pt rounded corners and 24 pt interior padding; timer wells use 16 pt corners and 18 pt padding. Assistant turns use 20 pt corners and padding, and its input uses 12 pt corners and padding. These are source-level adaptations of the inherited nested-tray vocabulary, not exact copies of Android's 28/22/18 dp scale. Home retains these tonal trays: the discovery shelf uses 24 pt corners and 22 pt padding; the selected day and main food image use 22 pt corners, with smaller 10–12 pt crops in supporting rows. Native controls retain their system silhouette.

## Components

- **Navigation:** Five labeled tabs—Home, Plan, Recipes, Shop, Assistant—each host a `NavigationStack`, with Home selected on launch. Each destination exposes Settings through a labeled top-trailing gear target (minimum 44 by 44 pt); it opens a native sheet with its own navigation stack and Done action. This replaces the Android hidden drawer with native navigation. Inspect the OS-specific tab presentation on iPad; no custom permanent sidebar is implemented here.
- **Home:** The selectable current-week strip leads into an open meal composition with recipe details, planned portions, and a native Start cooking link. The next valid planned meal appears below, followed by a charcoal discovery shelf, Browse recipes, Assistant, and shopping routes. The shopping summary always covers the current calendar week plus next week, uses Shop’s two-week check identifiers, and excludes pantry items. It does not follow the selected day or a separately changed Shop filter. Empty plans, missing planned recipes, and empty libraries receive explicit copy and usable routes; day rollover resets the selection to today.
- **Home imagery:** The approved addition is bundled illustrative food photography for only three matching core recipes: garlic-herb chicken, beef tacos, and chicken pesto pasta. It extends the Apple world without changing its palette, display type, or native control language. Personal recipes and other core recipes omit artwork rather than borrow another dish’s image. These are generated serving illustrations, not evidence of a cooked result. Images carry no rendered text or controls and are hidden from accessibility because adjacent native text supplies recipe identity.
- **Cooking:** A large current action, instruction, optional tip/timer, Previous/Next controls, and an inline “Up next” preview share the appearance-adaptive step tray. Ingredients occupy a separate appearance-adaptive tray. Active or previously started timers appear below it, followed by notification recovery and leftover guidance when present. The Android discrete progress rule and circular timer controls are not reproduced.
- **Timers:** Named countdown wells expose Start/Pause/Restart and Reset, with a spoken minutes-and-seconds label. Running state is also expressed in text and icons. All eligible device timers are listed; the Android two-timer shelf limit and +1 minute action are not present in this UI.
- **Touch and semantics:** Many explicit controls use minimum 44 pt height, and key icon controls use a 44 by 44 pt frame; some planning and review actions use 48 pt minimum height. Grocery rows expose quantity and checked state to accessibility. Cooking titles carry a header trait. These declarations do not establish that every target, reading order, or spoken state is correct.
- **Recipe authoring:** A native form groups recipe basics, ingredients, and cooking steps. Edit mode provides reorder/delete. Imported status and validation remain in the editor; Save is a toolbar action rather than Android's persistent full-width save shelf. Dirty forms disable interactive dismissal and Cancel opens an alert with explicit Keep editing and Discard changes actions on both phone and tablet. This avoids the system popover omitting a cancel-role action.
- **Assistant:** Sources, clarification questions, progress, retry, and explicit draft review remain visible in the conversation. The composer sits in a bottom safe-area inset, uses a multiline system field, and separates scanning, photo selection, and Send. Sending disables blank or busy submissions. Clearing a conversation requires a native confirmation dialog.
- **Shared workflows:** The native views expose planning, portions, recipe browsing, groceries, pantry, favorites, ratings, notes, import/export, authoring, cooking, Assistant, and account/settings entry points. UI presence alone does not verify service behavior or cross-platform data parity.

## Do's and Don'ts

Do preserve the charcoal/ivory station, peach actions, yellow timers, and condensed action language. Do retain explicit draft review, readable cooking instructions, visible timer recovery, and native navigation semantics. Do keep Home’s illustrative photography tied to its matching core recipe and preserve usable text-first fallbacks. Do base cooking expansion on available window width and keep accessibility categories in one column. Do treat the differences above as current implementation facts, not evidence that every Android visual feature has been reproduced.

Don't claim screenshot, accessibility, contrast, device, or visual approval from source inspection. Don't treat device class, screen resolution, or a wide simulator window as proof of usable two-pane layout.

### Required device QA before visual acceptance

- Build and run on supported iPhone and iPad hardware or simulators; verify the bundled Barlow face loads and inspect every primary destination in light and dark mode.
- Inspect compact iPhone portrait and landscape, full-width iPad portrait and landscape, and supported iPad multitasking/window widths immediately below and above 720 pt. Confirm no clipped text or controls, useful pane proportions, reachable navigation, safe-area clearance, and retained step/timer state during resizing and rotation. Check scroll continuity explicitly.
- Repeat cooking and authoring at standard, largest non-accessibility, and all accessibility Dynamic Type categories. Confirm accessibility sizes stack cooking; check long recipe names, instructions, equipment names, ingredient quantities, timer labels, and horizontal editor rows. Verify reading distance and touch targets with practical kitchen use.
- Use VoiceOver to traverse tabs, steps, ingredients, timer controls, grocery checkmarks, fields, validation, sheets, and dialogs. Check focus after step changes, modal dismissal, and errors; verify timer updates do not cause disruptive announcements. Test external-keyboard navigation on iPad.
- Open the keyboard in notes, authoring, and Assistant at narrow and wide window sizes. Confirm the composer, Send, Save, Cancel, and validation remain reachable, including with long content. Verify unsaved-work dismissal and draft-review flows.
- Measure rendered contrast for adaptive ivory/charcoal panels, peach actions, disabled controls, yellow timers, native secondary text, and material surfaces. Include Increase Contrast, Reduce Transparency, and Bold Text.
- Verify Home’s selected dates and calendar-day rollover, empty plan/library and missing-recipe states, matching-image and text-first layouts, planned portions into detail/cooking, next-meal labels, all five tabs, Settings dismissal, and shopping totals against Shop’s current-week two-week view. Repeat Home at narrow window widths and accessibility Dynamic Type sizes. These checks are pending in this source review. Generic design-workflow gates and legacy tool/plate status do not establish native validation; use native test and rendered-review evidence for acceptance.
- Exercise empty, loading, retry, error, notification-denied, timer-running/paused/finished, imported-draft, and long-content states. On physical hardware, check scanner/photo sheets and timer notification recovery under foreground/background, relaunch, Focus, and notification-setting changes. These are required behavior checks, not completed validation.
