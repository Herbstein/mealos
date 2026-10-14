import XCTest

final class MealOSUITests: XCTestCase {
    private var app: XCUIApplication!
    override func setUpWithError() throws {
        continueAfterFailure = false
        XCUIDevice.shared.orientation = .portrait
        app = XCUIApplication()
    }
    private func launch(arguments: [String] = []) {
        app.launchArguments = ["--uitesting", "--reset-ui-test-state"] + arguments
        app.launch()
        tap("Plan")
    }
    private func capture(_ name: String) {
        let screenshot = XCTAttachment(screenshot: XCUIScreen.main.screenshot())
        screenshot.name = name; screenshot.lifetime = .keepAlways; add(screenshot)
    }
    private func tap(_ label: String) {
        let button = app.buttons[label].firstMatch
        XCTAssertTrue(button.existsOrWait(timeout: 15), "Missing button: \(label)\n\(app.debugDescription)")
        button.tap()
    }
    func testAssistantKeyboardLayoutAndDismissal() throws {
        launch(arguments: ["-assistant-composer", ""])
        tap("Assistant")
        let editor = app.descendants(matching: .any).matching(identifier: "assistant-composer-field").firstMatch
        XCTAssertTrue(editor.existsOrWait(timeout: 10))
        let navigation = app.navigationBars["Recipe Assistant"]
        let initialTop = navigation.frame.minY
        editor.tap()
        XCTAssertTrue(app.keyboards.firstMatch.existsOrWait(timeout: 5))
        let text = String(repeating: "A warming lentil soup with carrots and spices.\n", count: 12)
        editor.typeText(text)
        let hide = app.buttons["assistant-hide-keyboard"]
        XCTAssertTrue(hide.isHittable)
        XCTAssertEqual(navigation.frame.minY, initialTop, accuracy: 2)
        XCTAssertLessThanOrEqual(editor.frame.maxY, app.keyboards.firstMatch.frame.minY)
        XCTAssertGreaterThan(app.scrollViews["assistant-conversation"].frame.height, 80)
        capture("assistant-keyboard-long-draft")
        hide.tap()
        XCTAssertTrue(app.keyboards.firstMatch.waitForNonExistence(timeout: 5))
        XCTAssertEqual(editor.value as? String, text)
        capture("assistant-keyboard-dismissed")
        editor.tap()
        XCTAssertTrue(app.keyboards.firstMatch.existsOrWait(timeout: 5))
        let conversation = app.scrollViews["assistant-conversation"]
        // Its accessibility frame extends behind the navigation bar. Start the
        // drag in visible conversation content, not on the navigation controls.
        let start = conversation.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.65))
        let end = conversation.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.95))
        start.press(forDuration: 0.1, thenDragTo: end)
        XCTAssertTrue(app.keyboards.firstMatch.waitForNonExistence(timeout: 5))
    }

    func testLibrarySyncSettings() throws {
        app.launchArguments = ["--uitesting", "--reset-ui-test-state"]
        app.launch()
        tap("Settings")
        XCTAssertTrue(app.staticTexts["Library sync"].existsOrWait(timeout: 10))
        XCTAssertTrue(app.staticTexts["Sign in with Apple to sync your library"].exists)
        XCTAssertFalse(app.buttons["Sign in with Google"].exists)
        XCTAssertFalse(app.buttons["Reconnect sync"].exists)
        tap("Done")
        tap("Recipes")
        XCTAssertTrue(app.buttons["Settings"].exists)
    }
    func testAssistantRequiresAppleAndKeepsFailedMessage() throws {
        // Composer drafts live in UserDefaults separately from the snapshot.
        // A preceding keyboard test intentionally leaves a draft behind.
        app.launchArguments = ["--uitesting", "--reset-ui-test-state", "-assistant-composer", ""]
        app.launch()
        tap("Assistant")
        let composer = app.descendants(matching: .any).matching(identifier: "assistant-composer-field").firstMatch
        XCTAssertTrue(composer.existsOrWait(timeout: 10))
        composer.tap(); composer.typeText("Make chicken and rice")
        tap("Send")
        let status = app.staticTexts["assistant-status"]
        XCTAssertTrue(status.existsOrWait(timeout: 10))
        XCTAssertTrue(status.label.contains("Sign in with Apple"))
        XCTAssertTrue(app.staticTexts["Make chicken and rice"].exists)
        XCTAssertFalse(app.buttons["assistant-pcc-fallback"].exists)
        capture("assistant-apple-sign-in-required")
    }
    func testAppleAccountSettings() throws {
        app.launchArguments = ["--uitesting", "--reset-ui-test-state"]
        app.launch()
        tap("Settings")
        XCTAssertTrue(app.navigationBars["Settings"].existsOrWait(timeout: 10))
        let apple = app.buttons["apple-account-sign-in"]
        XCTAssertTrue(apple.existsOrWait(timeout: 10))
        XCTAssertTrue(apple.isEnabled && apple.isHittable)
        XCTAssertFalse(app.buttons["Sign in with Google"].exists)
        capture("apple-account-settings")
        // Real Apple authorization requires a signed device and a deployed API.
    }
    func testHomeOverviewAndNavigation() throws {
        app.launchArguments = ["--uitesting", "--reset-ui-test-state"]
        app.launch()
        XCTAssertTrue(app.navigationBars["Home"].existsOrWait(timeout: 20))
        capture("home-empty")
        tap("Settings")
        XCTAssertTrue(app.navigationBars["Settings"].existsOrWait(timeout: 10))
        tap("Done")
        tap("Plan tonight")
        let chicken = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "Garlic-herb chicken")).firstMatch
        XCTAssertTrue(chicken.existsOrWait(timeout: 10)); chicken.tap()
        XCTAssertTrue(app.buttons["Start cooking"].firstMatch.existsOrWait(timeout: 10))
        var utc = Calendar(identifier: .gregorian); utc.timeZone = TimeZone(secondsFromGMT: 0)!
        let civil = Calendar.current.dateComponents([.year, .month, .day], from: Date())
        let today = Int(utc.date(from: civil)!.timeIntervalSince1970 / 86400)
        tap("home-day-\(Calendar.current.component(.weekday, from: Date()) == 1 ? today - 1 : today + 1)")
        tap("Choose a meal")
        let tacos = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "Beef tacos")).firstMatch
        for _ in 0..<6 {
            if tacos.exists && tacos.isHittable { break }
            app.collectionViews.firstMatch.swipeUp()
        }
        XCTAssertTrue(tacos.exists && tacos.isHittable); tacos.tap()
        tap("home-day-\(today)")
        capture("home-selected")
        tap("Start cooking")
        XCTAssertTrue(app.staticTexts["Prep the potatoes"].existsOrWait(timeout: 10))
        app.navigationBars.buttons.firstMatch.tap()
        let scroll = app.scrollViews.firstMatch
        for _ in 0..<4 {
            if app.buttons["home-shopping"].isHittable { break }
            scroll.swipeUp()
        }
        capture("home-lower")
        tap("home-shopping")
        XCTAssertTrue(app.navigationBars["Grocery list"].existsOrWait(timeout: 10))
        tap("Home")
        tap("home-assistant")
        XCTAssertTrue(app.navigationBars["Recipe Assistant"].existsOrWait(timeout: 10))
        tap("Home")
        tap("Browse recipes")
        XCTAssertTrue(app.navigationBars["Recipes"].existsOrWait(timeout: 10))
        app.terminate()
        app.launchArguments = ["--uitesting"]
        app.launch()
        XCTAssertTrue(app.navigationBars["Home"].existsOrWait(timeout: 10))
        XCTAssertTrue(app.buttons["Start cooking"].firstMatch.existsOrWait(timeout: 10))
    }

    func testHomeLargeText() throws {
        app.launchArguments = ["--uitesting", "--reset-ui-test-state", "-UIPreferredContentSizeCategoryName", "UICTContentSizeCategoryAccessibilityXXXL"]
        app.launch()
        XCTAssertTrue(app.navigationBars["Home"].existsOrWait(timeout: 20))
        tap("Plan tonight")
        let recipe = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "Garlic-herb chicken")).firstMatch
        XCTAssertTrue(recipe.existsOrWait(timeout: 10)); recipe.tap()
        for _ in 0..<5 { app.scrollViews.firstMatch.swipeDown() }
        capture("home-accessibility-top")
        app.scrollViews.firstMatch.swipeUp()
        capture("home-accessibility-middle")
        let scroll = app.scrollViews.firstMatch
        for index in 1...10 {
            scroll.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.75))
                .press(forDuration: 0.05, thenDragTo: scroll.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.4)))
            capture("home-accessibility-scroll-\(index)")
        }
        XCTAssertTrue(app.buttons["home-shopping"].isHittable)
        capture("home-accessibility-bottom")
    }

    func testPlanShopCookAndRestore() throws {
        launch()
        func showGrocery(_ row: XCUIElement) {
            XCTAssertTrue(app.navigationBars["Grocery list"].existsOrWait(timeout: 10))
            let list = app.collectionViews.firstMatch
            XCTAssertTrue(list.existsOrWait(timeout: 10))
            // In landscape, the shopping-window section fills the first viewport.
            // SwiftUI's List may not expose a grocery row until it scrolls into view.
            for _ in 0..<8 {
                if row.exists && row.isHittable { return }
                // Keep the drag between the bars, including on a short landscape screen.
                list.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.65))
                    .press(forDuration: 0.1, thenDragTo: list.coordinate(withNormalizedOffset: CGVector(dx: 0.5, dy: 0.3)))
            }
            XCTAssertTrue(row.exists && row.isHittable, "Grocery row not visible after scrolling\n\(app.debugDescription)")
        }
        XCTAssertTrue(app.navigationBars["Meal plan"].existsOrWait(timeout: 20))
        capture("01-plan-empty")
        tap("Plan tonight")
        let recipe = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "Garlic-herb chicken")).firstMatch
        XCTAssertTrue(recipe.existsOrWait(timeout: 10)); recipe.tap()
        XCTAssertTrue(app.buttons["Start cooking"].firstMatch.existsOrWait(timeout: 10))
        capture("02-plan-selected")
        tap("Shop")
        let grocery = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "Baby potatoes,")).firstMatch
        showGrocery(grocery); grocery.tap()
        XCTAssertTrue(grocery.label.hasSuffix(", checked"))
        capture("03-groceries")
        tap("Plan"); tap("Start cooking")
        XCTAssertTrue(app.staticTexts["Prep the potatoes"].existsOrWait(timeout: 10))
        capture("04-cooking-portrait")
        tap("Next step")
        XCTAssertTrue(app.staticTexts["Start the potatoes"].firstMatch.existsOrWait(timeout: 10))
        addUIInterruptionMonitor(withDescription: "Notifications") { alert in
            if alert.buttons["Allow"].exists { alert.buttons["Allow"].tap(); return true }
            return false
        }
        tap("Start")
        app.tap()
        XCTAssertTrue(app.buttons["Pause"].firstMatch.existsOrWait(timeout: 10))
        capture("05-timer-running")
        tap("Pause")
        XCUIDevice.shared.orientation = .landscapeLeft
        capture("06-cooking-landscape")
        app.terminate()
        app.launchArguments = ["--uitesting"]
        app.launch()
        tap("Shop")
        let restored = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "Baby potatoes,")).firstMatch
        showGrocery(restored)
        XCTAssertTrue(restored.label.hasSuffix(", checked"))
        tap("Plan"); tap("Start cooking")
        XCTAssertTrue(app.staticTexts["Start the potatoes"].firstMatch.existsOrWait(timeout: 10))
        capture("07-cooking-restored")
    }
    func testCalendarMonthSelectionAndWeek() throws {
        launch()
        capture("calendar-month-empty")
        let initialTitle = app.staticTexts["plan-period-title"].label
        tap("Next month")
        XCTAssertNotEqual(app.staticTexts["plan-period-title"].label, initialTitle)
        tap("Previous month")
        XCTAssertEqual(app.staticTexts["plan-period-title"].label, initialTitle)
        tap("Plan tonight")
        let chicken = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "Garlic-herb chicken")).firstMatch
        XCTAssertTrue(chicken.existsOrWait(timeout: 10)); chicken.tap()
        capture("calendar-month-detail")
        for _ in 0..<3 { app.scrollViews.firstMatch.swipeDown() }
        capture("calendar-month-planned")
        let selected = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@ AND label CONTAINS %@", "calendar-day-", "Garlic-herb chicken")).firstMatch
        XCTAssertTrue(selected.existsOrWait(timeout: 10))
        let plannedID = selected.identifier
        let nextDay = Int(plannedID.replacingOccurrences(of: "calendar-day-", with: ""))! + 1
        tap("calendar-day-\(nextDay)")
        XCTAssertTrue(app.staticTexts["No meal planned"].exists)
        tap("Today")
        tap("Week")
        XCTAssertTrue(app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@ AND label CONTAINS %@", "plan-day-", "Garlic-herb chicken")).firstMatch.exists)
        for _ in 0..<3 { app.scrollViews.firstMatch.swipeDown() }
        capture("calendar-week")
        tap("Month")
        tap("Next month"); tap("Today")
        XCTAssertTrue(app.buttons[plannedID].exists)
        app.terminate(); app.launchArguments = ["--uitesting"]; app.launch(); tap("Plan")
        XCTAssertTrue(app.buttons[plannedID].existsOrWait(timeout: 10))
        XCTAssertTrue(app.buttons[plannedID].label.contains("Garlic-herb chicken"))
        tap("Meal actions"); tap("Remove meal")
        XCTAssertFalse(app.buttons[plannedID].label.contains("Garlic-herb chicken"))
    }

    func testCalendarLargeText() throws {
        launch(arguments: ["-UIPreferredContentSizeCategoryName", "UICTContentSizeCategoryAccessibilityXXXL"])
        capture("calendar-accessibility-month")
        tap("Week")
        capture("calendar-accessibility-week")
        let day = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "plan-day-")).firstMatch
        XCTAssertTrue(day.existsOrWait(timeout: 10)); day.tap()
        XCTAssertTrue(app.buttons["Cancel"].existsOrWait(timeout: 10))
        tap("Cancel")
    }

    func testConsecutiveWeeksPersistAndCombineShopping() throws {
        launch()
        launch()
        func show(_ button: XCUIElement) {
            for _ in 0..<5 { if button.exists && button.isHittable { return }; app.swipeUp() }
            XCTAssertTrue(button.isHittable)
        }
        func chooseChicken() {
            let recipe = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "Garlic-herb chicken")).firstMatch
            XCTAssertTrue(recipe.existsOrWait(timeout: 5)); recipe.tap()
        }
        tap("Plan tonight"); chooseChicken()
        tap("Week")
        let next = app.buttons["Next week"].firstMatch
        show(next); next.tap()
        let day = app.buttons.matching(NSPredicate(format: "identifier BEGINSWITH %@", "plan-day-")).firstMatch
        show(day)
        let futureID = day.identifier
        day.tap(); chooseChicken()
        let future = app.buttons[futureID]
        XCTAssertTrue(future.existsOrWait(timeout: 5))
        XCTAssertTrue(future.label.contains("Garlic-herb chicken"))
        capture("multiweek-plan")
        app.terminate(); app.launchArguments = ["--uitesting"]; app.launch(); tap("Plan")
        tap("Week"); show(app.buttons["Next week"].firstMatch); tap("Next week")
        XCTAssertTrue(app.buttons[futureID].existsOrWait(timeout: 5))
        XCTAssertTrue(app.buttons[futureID].label.contains("Garlic-herb chicken"))
        tap("Shop")
        let grocery = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "Baby potatoes,")).firstMatch
        show(grocery)
        XCTAssertTrue(grocery.label.contains("2 lb"))
        capture("multiweek-shopping")
    }
    func testRecipeEditorValidationAndDiscard() throws {
        launch()
        tap("Recipes"); tap("Add recipe"); tap("Create recipe")
        tap("Save")
        XCTAssertTrue(app.navigationBars["New recipe"].exists)
        let name = app.textFields["Recipe name"]
        XCTAssertTrue(name.existsOrWait(timeout: 10)); name.tap(); name.typeText("Unsaved test meal")
        tap("Cancel")
        XCTAssertTrue(app.buttons["Keep editing"].existsOrWait(timeout: 10))
        capture("08-editor-discard")
        tap("Keep editing")
        XCTAssertEqual(name.value as? String, "Unsaved test meal")
        tap("Cancel"); tap("Discard changes")
        XCTAssertTrue(app.navigationBars["Recipes"].existsOrWait(timeout: 10))
    }
    func testCreateRecipeAndRestore() throws {
        launch()
        tap("Recipes"); tap("Add recipe"); tap("Create recipe")
        app.textFields["Recipe name"].tap(); app.textFields["Recipe name"].typeText("Test kitchen toast")
        tap("Add ingredient")
        app.textFields["Ingredient name"].tap(); app.textFields["Ingredient name"].typeText("Bread")
        // Form rows outside the iPad sheet's viewport may not exist in the
        // accessibility tree until scrolled into view. Waiting cannot reveal them.
        let addStep = app.buttons["Add cooking step"].firstMatch
        for _ in 0..<5 {
            if addStep.exists { break }
            app.collectionViews.firstMatch.swipeUp()
        }
        tap("Add cooking step")
        app.textFields["Step title"].tap(); app.textFields["Step title"].typeText("Toast the bread")
        let instruction = app.descendants(matching: .any).matching(NSPredicate(format: "(elementType == %d OR elementType == %d) AND (label == %@ OR placeholderValue == %@)", XCUIElement.ElementType.textField.rawValue, XCUIElement.ElementType.textView.rawValue, "Instruction", "Instruction")).firstMatch
        XCTAssertTrue(instruction.existsOrWait(timeout: 5)); instruction.tap(); instruction.typeText("Toast until golden and serve.")
        tap("Save")
        XCTAssertTrue(app.navigationBars["Recipes"].existsOrWait(timeout: 10))
        app.terminate(); app.launchArguments = ["--uitesting"]; app.launch(); tap("Recipes")
        let savedRecipe = app.staticTexts["Test kitchen toast"].firstMatch
        for _ in 0..<8 {
            if savedRecipe.exists && savedRecipe.isHittable { break }
            app.swipeUp()
        }
        XCTAssertTrue(savedRecipe.exists)
        XCTAssertTrue(savedRecipe.isHittable)
        capture("09-created-recipe-restored")
    }
    func testDarkAccessibilityCooking() throws {
        launch(arguments: ["-AppleInterfaceStyle", "Dark", "-UIPreferredContentSizeCategoryName", "UICTContentSizeCategoryAccessibilityXXXL"])
        tap("Plan tonight")
        let recipe = app.buttons.matching(NSPredicate(format: "label CONTAINS %@", "Garlic-herb chicken")).firstMatch
        XCTAssertTrue(recipe.existsOrWait(timeout: 10)); recipe.tap()
        tap("Start cooking")
        XCTAssertTrue(app.staticTexts["Prep the potatoes"].existsOrWait(timeout: 10))
        capture("10-cooking-dark-accessibility")
        let workspace = app.staticTexts["At your station"]
        let heading = app.staticTexts["Prep the potatoes"]
        XCTAssertGreaterThan(workspace.frame.minY, heading.frame.minY)
        XCTAssertLessThan(abs(workspace.frame.minX - heading.frame.minX), 40)
    }

}

private extension XCUIElement {
    /// Avoid the polling delay for elements already present; retain the full
    /// timeout for asynchronous transitions and slower simulator runs.
    func existsOrWait(timeout: TimeInterval) -> Bool {
        exists || waitForExistence(timeout: timeout)
    }
}
