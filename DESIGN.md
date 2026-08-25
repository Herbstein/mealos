---
name: MealOS
description: A confident mise en place board for planning, shopping, and cooking.
colors:
  deep-enamel: "#083224"
  raised-enamel: "#234633"
  warm-cream: "#F3F0E8"
  warm-line: "#D1C3B0"
  decisive-orange: "#C84F13"
  timer-yellow: "#C9A442"
  supporting-sage: "#6F8477"
  light-surface-variant: "#E6DFD2"
  light-on-surface-variant: "#41573B"
  light-error: "#B3261E"
  dark-primary: "#9CC9AC"
  dark-on-primary: "#06281C"
  dark-primary-container: "#1C4935"
  dark-on-primary-container: "#D8F4DF"
  dark-secondary: "#FFB38B"
  dark-on-secondary: "#542000"
  dark-tertiary: "#E5C76B"
  dark-on-tertiary: "#3A3000"
  dark-background: "#061C14"
  dark-on-background: "#E5EFE8"
  dark-surface: "#10271D"
  dark-surface-variant: "#233A2E"
  dark-on-surface-variant: "#C0CEC4"
  dark-outline: "#82978A"
  dark-error: "#FFB4AB"
  second-timer-orange: "#FF7A32"
typography:
  display-large:
    fontFamily: "Barlow Condensed"
    fontSize: "64sp"
    fontWeight: 600
    lineHeight: "60sp"
  display-medium:
    fontFamily: "Barlow Condensed"
    fontSize: "52sp"
    fontWeight: 600
    lineHeight: "50sp"
  headline-large:
    fontFamily: "Barlow Condensed"
    fontSize: "38sp"
    fontWeight: 600
    lineHeight: "40sp"
  headline-medium:
    fontFamily: "Barlow Condensed"
    fontSize: "32sp"
    fontWeight: 600
    lineHeight: "34sp"
  title-large:
    fontSize: "22sp"
    fontWeight: 700
    lineHeight: "28sp"
  title-medium:
    fontSize: "17sp"
    fontWeight: 600
    lineHeight: "23sp"
  body-large:
    fontSize: "18sp"
    lineHeight: "26sp"
  body-medium:
    fontSize: "15sp"
    lineHeight: "22sp"
  label-large:
    fontSize: "15sp"
    fontWeight: 600
    lineHeight: "20sp"
rounded:
  chip: "10dp"
  list: "16dp"
  preview: "18dp"
  tray-compact: "22dp"
  tray-large: "28dp"
  round: "50%"
spacing:
  xs: "6dp"
  sm: "8dp"
  control: "12dp"
  inset: "16dp"
  component: "18dp"
  screen: "20dp"
  tray: "22dp"
  roomy: "24dp"
components:
  decisive-button:
    backgroundColor: "{colors.decisive-orange}"
    textColor: "{colors.warm-cream}"
    height: "60dp"
  current-step-tray:
    backgroundColor: "{colors.warm-cream}"
    textColor: "{colors.deep-enamel}"
    rounded: "{rounded.tray-large}"
    padding: "{spacing.tray}"
  step-preview:
    backgroundColor: "{colors.warm-cream}"
    textColor: "{colors.deep-enamel}"
    rounded: "{rounded.preview}"
    padding: "12dp 16dp"
  static-chip:
    backgroundColor: "{colors.warm-cream}"
    textColor: "{colors.deep-enamel}"
    rounded: "{rounded.chip}"
    padding: "8dp 12dp"
  timer-shelf:
    backgroundColor: "{colors.raised-enamel}"
    textColor: "{colors.warm-cream}"
    rounded: "{rounded.tray-large}"
    padding: "{spacing.component}"
  timer-control:
    backgroundColor: "{colors.deep-enamel}"
    textColor: "{colors.timer-yellow}"
    rounded: "{rounded.tray-compact}"
    padding: "{spacing.inset}"
  round-icon-control:
    backgroundColor: "{colors.raised-enamel}"
    textColor: "{colors.warm-cream}"
    rounded: "{rounded.round}"
    size: "56dp"
---

# Design System: MealOS

## Overview

**Creative North Star: "Mise en Place Board"**

MealOS feels like a prepared kitchen station. Deep green enamel holds persistent working state while warm cream trays make instructions and ingredients immediately readable. Yellow is reserved for active timing, and burnt orange marks the decisive action that moves the cook forward.

The system is tactile, confident, and practical: large condensed action language, human-scale controls, generous working surfaces, and flat tonal layering that can survive a busy kitchen. It rejects generic dashboard grids and decorative food-blog styling; its identity comes from purposeful trays, shelves, rules, and controls rather than ornamental imagery.

**Key Characteristics:**

- Deep enamel workspace with warm cream prep surfaces.
- Condensed, heavy action typography paired with calm Material body type.
- Yellow timing state and burnt-orange completion state.
- Large trays, compact rounded previews, pills, and circular controls.
- Minimal shadow; depth comes primarily from tonal and material contrast.

## Colors

The palette behaves like enamel cookware and warm prep paper: dark greens establish the station, cream carries reading, and sparse warm accents communicate state.

### Primary

- **Deep Enamel:** The persistent app and cooking-workspace ground.
- **Raised Enamel:** A nearby tonal layer for shelves and control wells.
- **Dark Sage Primary:** The dark-theme interactive primary, paired with its dedicated on-primary color.

### Secondary

- **Decisive Orange:** Reserved for completion, starting cooking, favorites, and other consequential forward actions.
- **Soft Apricot:** The dark-theme secondary role and its dark on-secondary pairing.

### Tertiary

- **Timer Yellow:** Active timing, progress, ratings, and time-sensitive highlights.
- **Second Timer Orange:** Differentiates a simultaneous second timer without weakening the primary yellow timer.

### Neutral

- **Warm Cream:** High-legibility reading surface and light content color on enamel.
- **Warm Line:** Quiet borders around compact chips and controls.
- **Supporting Sage:** Progress rules, outlines, and low-emphasis structure.
- **Surface Variants:** Warm paper in the light theme and tonal greens in the dark theme provide subordinate containers and text roles.
- **Error Roles:** Material light and dark error pairs remain distinct from the product's decisive orange.

### Named Rules

**The Two Warm Signals Rule.** Yellow means timing or active progress; burnt orange means a decisive action. Do not swap their jobs.

**The Cream Is for Reading Rule.** Use cream as a prepared content field against enamel, not as a decorative accent scattered through the interface.

## Typography

**Display Font:** Barlow Condensed SemiBold

**Body Font:** Material 3 system sans

**Character:** Display type is compact, direct, and highly visible at arm's length. Body, title, and control roles stay calm and familiar so instructions remain readable under pressure.

### Hierarchy

- **Display Large:** The largest action or destination statement; tightly led for compact impact.
- **Display Medium:** Primary screen headings and current cooking-step titles.
- **Headline Large / Medium:** Recipe identity, tray headings, and major section boundaries.
- **Title Large / Medium:** Item names, timer labels, and compact hierarchy inside trays.
- **Body Large / Medium:** Instructions, explanatory copy, quantities, and supporting information.
- **Label Large:** Chips, preview labels, and controls; never used as a substitute for readable instruction text.

### Named Rules

**The Arm's-Length Rule.** The current cooking action gets the display scale; supporting details stay in the Material hierarchy and must not compete with it.

## Layout

The base rhythm uses compact 6–12 dp gaps, 16–18 dp component insets, 20 dp screen padding, and 22–24 dp for the roomiest tray interiors. Content is organized as prepared work zones rather than a uniform card grid.

At widths of 720 dp and above, cooking mode becomes a balanced two-pane station: the current-step pane and the ingredient/timer workspace each take half the available width, with a divider matching the physical folding feature when present and never narrower than 8 dp. Below 720 dp, those zones reflow into a single vertical working column. Fold and unfold transitions must preserve the current step, timers, notes, and useful scroll context rather than restart the task.

The recipe editor follows the same work-surface logic. At 720 dp and above, a 300 dp enamel outline holds Basics, Ingredients, Steps, and Preview beside the active cream form pane, separated by an 8 dp rule; below 720 dp, those sections become a wrapping chip row above one scrollable pane. The preview itself splits step and ingredient trays at 600 dp and stacks below that width. Save remains a full-width persistent action on every layout.

Navigation remains a hidden Material drawer so the unfolded workspace is not consumed by a permanent rail. The drawer is 312 dp wide in the implementation and opens from the standard menu target. Edge-to-edge safe drawing and navigation-bar insets remain part of every screen composition.

## Elevation & Depth

MealOS is flat and tonal by default. Enamel-on-enamel and cream-on-green contrast provide most separation; large surfaces do not depend on floating card shadows. The only explicit raised treatment is a restrained 4 dp tonal elevation beneath the persistent recipe-detail action area.

### Named Rules

**The Enamel Layer Rule.** Establish depth with adjacent tonal surfaces before adding elevation; never introduce glass, glow, or decorative drop-shadow stacks.

## Shapes

The silhouette vocabulary is generous but not uniform. Large prep trays and timer shelves use 28 dp corners; compact cards and timer controls use 22 dp; step previews use 18 dp; list rows use 16 dp; outlined chips use 10 dp. Ingredient marks, step numbers, and timer controls use true circles. This variation creates the feel of nested kitchen tools instead of a repeated dashboard tile.

## Components

### Buttons

- **Primary:** Decisive orange with cream content; the full-width cooking progression control is 60 dp tall.
- **Tonal timer action:** A translucent yellow container on cream starts a timer without competing with the decisive action.
- **Outlined / text:** Material variants support review and secondary actions without adding a new accent.
- **State:** Disabled decisive actions retain the orange family at reduced opacity, keeping intent recognizable.

### Chips

- **Static chip:** Cream field, deep-enamel content, warm-line outline, compact 10 dp corners, and 8 × 12 dp padding.
- **Portion selector:** Enamel with cream content when unselected; timer yellow with enamel content when selected.

### Cards / Containers

- **Current-step and ingredient trays:** Warm cream on deep enamel with 28 dp corners; the current step uses 22 dp internal padding.
- **Timer shelf:** Raised enamel with cream content, 28 dp corners, and 18 dp internal padding.
- **Compact cards:** 22 dp corners on tonal Material surfaces.
- **Shadow Strategy:** Tonal separation first; use the explicit 4 dp tonal elevation only for the persistent bottom action surface.

### Inputs / Fields

- **Style:** Material 3 outlined text fields for search and notes, inheriting the active color scheme rather than creating a separate form aesthetic.
- **Use:** Search stays single-line; notes may expand to multiple lines.
- **Editor use:** Group fields into named work trays rather than presenting one undifferentiated form. Ingredient and step rows use 22 dp tonal containers, with add, reorder, and remove controls kept adjacent to the item they affect.

### Navigation

- **Style:** A hidden 312 dp Material navigation drawer with icon-and-label destinations and a selected state supplied by Material 3.
- **Behavior:** The drawer closes after selection. System Back returns through cooking, detail, and primary destinations without trapping the user.

### Recipe Authoring

The recipe editor is a guided mise en place bench, not a spreadsheet. Basics, Ingredients, Steps, and Preview form a visible sequence with yellow completion marks and a cream selected state on expanded layouts; compact layouts use wrapping section chips with yellow for the active section. Imported recipes open directly in this editor with a persistent notice that they are drafts requiring review before saving.

The bottom save shelf uses raised enamel and one full-width decisive-orange action. Closing the editor or using Android Back with unsaved changes opens a native confirmation dialog with “Keep editing” as the safe escape and “Discard” as the explicit destructive choice. Validation remains in-context inside the editor rather than moving the cook to a separate error screen.

On the Recipes bench, “Create a recipe” remains the primary action. “Copy AI recipe prompt” is an outlined secondary entry with short helper text explaining the handoff: paste into ChatGPT, then share the returned JSON to MealOS for review. Copying confirms with lightweight native feedback; AI assistance never masquerades as automatic in-app generation.

### Step Preview

A compact cream row with 18 dp corners and 12 × 16 dp padding. Its orange label identifies Previous, Next, or Then; the deep-enamel title is followed by up to two lines of supporting instruction text. When present, Previous sits immediately above the current-step tray so the cook can confirm context before acting. Upcoming previews follow the current step. These rows navigate without mutating timer state.

### Timer Control

A 22 dp deep-enamel well inside the raised-enamel shelf. The timer label and large 40 sp clock share the timer accent; pause/play and +1 minute controls are 56 dp circles. At most two simultaneous timer controls are shown on the shelf.

Running timers remain the first content in the shelf. If Android has blocked notifications or precise alarms, compact recovery rows follow the countdowns with direct language and a settings action. The app never hides or disables the timer itself: denied access degrades to an inexact alarm while preserving the absolute finish time locally.

### Progress Rule

A thin 18 dp-high measurement track uses supporting sage for the line and inactive points, timer yellow for completed points, and a larger current point. It communicates discrete cooking steps rather than percentage completion.

## Do's and Don'ts

### Do:

- **Do** preserve cream reading trays against the deep enamel station.
- **Do** keep yellow tied to timing/progress and orange tied to decisive action.
- **Do** reflow cooking mode from balanced panes to one column at the observed 720 dp threshold while preserving working state.
- **Do** keep primary touch controls generous, with circular timer controls at 56 dp and standard Material icon targets elsewhere.
- **Do** use Material 3 navigation, fields, chips, dialogs, and system Back behavior as the native interaction foundation.
- **Do** keep imported status, validation, and unsaved-work protection visible inside the authoring flow without introducing a new accent language.

### Don't:

- **Don't** turn the interface into a generic dashboard or uniform card grid.
- **Don't** use food-blog photography, bespoke recipe illustration, glass effects, or decorative shadow stacks as the system's identity.
- **Don't** add a permanently visible navigation rail; protect the unfolded cooking workspace with the hidden drawer.
- **Don't** shrink instructions into label-sized type or let secondary controls compete with the current cooking action.
- **Don't** let AI-created or externally shared recipes bypass the draft review and explicit save step.
