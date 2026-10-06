# Apple app validation — September 10, 2026

Validation used Xcode 26.6 (17F113), the iOS 26.5 simulator runtime, an iPhone 17, and an iPad Air 11-inch (M4). These are native SwiftUI screenshots and XCTest runs, not browser mockups.

## Results

| Check | Result |
| --- | --- |
| Universal simulator build | Passed |
| Shared Swift core XCTest suite | 12 passed, 0 failures |
| iPhone UI suite | 4 passed, 0 failures |
| iPad UI suite | 4 passed, 0 failures |
| Existing Recipe Assistant health endpoint | Blocked: hostname did not resolve |
| Connected physical devices | None detected |

The UI suite exercises recipe creation and persistence through app termination/relaunch; planning tonight's meal; grocery checkmarks and their recovery; cooking navigation, timer start/pause, rotation and restored cooking position; invalid editor saves and explicit keep/discard protection; and cooking with dark appearance plus the largest accessibility text category. It asserts that accessibility-sized cooking stacks its workspace below the step panel on both device sizes.

Tests run in a separate `MealOS-UITests` local data folder with cloud attachment disabled in Debug. They do not overwrite the user's recipe library or upload synthetic test recipes. The tests use real SwiftUI screens and local persistence. They do not prove authenticated backend behavior.

## Fixes made during testing

- Set Debug builds to the active architecture, matching Swift package dependencies when targeting a specific simulator. The original generic build passed, but a device-specific test build attempted to link x86_64 app objects against arm64 dependencies.
- Replaced the prominent cooking NavigationLinks inside list content with native Buttons and programmatic navigation. The original plan action was accessible and tappable but visually disappeared under the native list styling. Final screenshots show the visible action.
- Made the unsaved-recipe confirmation an alert, keeping both **Keep editing** and **Discard changes** visible on phone and tablet. The system popover had omitted the cancel-role action.
- Hid the tab bar while cooking so the cooking surface and system Back control retain their intended contrast and available space.
- Added a shared Xcode UI-test target, simulator-discovery runner, dependency lockfile, and CI test-result artifacts. Adjusted the persistence test for iPad's different search presentation, and captured the full simulator screen to avoid app-frame cropping during rotation.

## Evidence and reproduction

Run from the repository root:

```sh
export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
swift test --package-path ios
python3 ios/scripts/run_ui_tests.py
```

This Mac still selects Command Line Tools globally; the explicit `DEVELOPER_DIR` selects the installed full Xcode without changing the machine-wide setting. Simulator commands and builds require access to Apple's services and caches outside a restricted filesystem sandbox.

Full local results are in `/tmp/mealos-ui-final/iphone.xcresult` and `/tmp/mealos-ui-final/ipad.xcresult`, with per-device logs and exported screenshot attachments in that same directory. Core XCTest output is in `/tmp/mealos-xctests.log`. Representative screenshots are retained beside this report. The new GitHub Actions workflow has not been executed remotely.

## Still requiring validation

- Google sign-in, Android-to-Apple cloud synchronization, offline cloud conflict/reconnect behavior, and authorized/unauthorized assistant requests. The configured `mealos.arjunakankipati.com` hostname failed the health check with a DNS-resolution error.
- Camera scanning, photo-picker import/OCR, and `.mealosrecipe` import/export through the operating system. Recipe document parsing and serialization are covered by core tests; the full OS file flows are not yet UI-tested.
- Real-device notification delivery, Focus/Silent mode behavior, denied notification recovery, reboot behavior, and cooking readability at kitchen distance. Simulator tests cover starting/pausing a timer and local recovery, not alarm delivery guarantees.
- Interactive VoiceOver traversal, external keyboard use, reduced transparency, and actual narrow iPad multitasking windows. The observed large-text reflow is not a substitute for those checks.

This is simulator validation of the listed flows, not a release or real-device certification.

## Selected screenshots

- [iPhone meal plan](iphone-02-plan-selected.png)
- [iPhone cooking, portrait](iphone-04-cooking-portrait.png)
- [iPhone cooking, landscape](iphone-06-cooking-landscape.png)
- [iPhone cooking, accessibility text](iphone-10-cooking-dark-accessibility.png)
- [iPad cooking, portrait](ipad-04-cooking-portrait.png)
- [iPad cooking, landscape](ipad-06-cooking-landscape.png)
- [iPad cooking, accessibility text](ipad-10-cooking-dark-accessibility.png)
- [iPad unsaved-recipe alert](ipad-08-editor-discard.png)
