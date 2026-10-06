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
        app.terminate(); app.launchArguments = ["--uitesting"]; app.launch()
        show(app.buttons["Next week"].firstMatch); tap("Next week")
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
