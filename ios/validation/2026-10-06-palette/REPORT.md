# Forkware palette validation — October 6, 2026

Applied the approved installed-icon direction to the active Apple client: charcoal, peach actions with charcoal labels, ivory reading surfaces/text, and butter-yellow timers. Native controls and layouts are retained. Cooking and Assistant reading panels now adapt to system appearance. The icon artwork is unchanged.

## Verification

- Universal Debug simulator build: passed (Xcode, signing disabled).
- Computed contrast: charcoal on peach 9.34:1; light-mode action text on ivory 5.56:1; peach on raised charcoal 7.94:1; yellow timer on charcoal 11.28:1; ivory on raised charcoal 13.51:1; dark text on light reading panels 15.33:1.
- iPad Pro 11-inch, iOS 26.5: both existing selected UI tests passed, including planning, shopping, cooking, timer start/pause, restore, and accessibility-size cooking.
- iPhone 17 Pro, iOS 26.5: accessibility cooking passed. Plan/shop/cook reached timer start/pause and landscape capture, then failed to locate the grocery row after relaunch in landscape. The failure hierarchy shows the shopping-window controls filling the viewport; the test does not scroll to the row. Persistence is not established by this failed assertion. No shopping logic was changed.
- The launch argument used by the existing dark test did not set dark appearance on the iPhone. Repeated that check with simulator appearance explicitly set to dark: passed, screenshot visually confirmed.
- Reviewed native screenshots of the light iPhone planner and dark iPad cooking/timer screens, plus iPhone dark accessibility cooking. Checked colour-role separation and visible label contrast. No browser mockups used.
- `git diff --check`: passed.

Screenshots in this folder come from simulator test attachments. Full result bundles remain at `/tmp/forkware-palette.xcresult` and `/tmp/forkware-palette-dark.xcresult`. Physical-device, Increase Contrast, and Reduce Transparency checks were not performed. The simulator iPhone remains in dark appearance for review.

## Grocery restoration test correction

Fixed `testPlanShopCookAndRestore` to reveal the grocery row with bounded list-directed drags before tapping it or checking its restored state. The drag stays clear of navigation/tab bars in landscape. The test still relaunches in landscape and asserts that the grocery checkmark and cooking step persist.

The corrected test passed on both iPhone 17 Pro and iPad Pro 11-inch (iOS 26.5). Result: `/tmp/forkware-grocery-test-isolated.xcresult`. Used a separate derived-data directory after a shared-directory run executed an older test binary. This resolves the iPhone restoration-test failure recorded above.
