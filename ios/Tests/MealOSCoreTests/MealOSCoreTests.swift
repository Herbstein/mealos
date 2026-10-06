import XCTest
@testable import MealOSCore

final class MealOSCoreTests: XCTestCase {
    private func catalog() throws -> [Recipe] {
        let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent().deletingLastPathComponent()
        return try Documents.recipes(Data(contentsOf: root.appendingPathComponent("MealOS/Resources/recipes.json")))
    }
    func testAllOriginalRecipesAreValidAndRoundTrip() throws {
        let recipes = try catalog()
        XCTAssertEqual(recipes.count, 15)
        XCTAssertEqual(Set(recipes.map(\.id)).count, 15)
        for recipe in recipes {
            XCTAssertTrue(recipe.validationErrors.isEmpty, recipe.name)
            let decoded = try Documents.importRecipe(Documents.exportRecipe(recipe))
            XCTAssertNotEqual(decoded.id, recipe.id)
            XCTAssertEqual(decoded.origin, .imported)
            XCTAssertEqual(decoded.ingredients, recipe.ingredients)
            XCTAssertEqual(decoded.steps, recipe.steps)
        }
    }
    func testGroceryScalingCombinesMealsAndExcludesPantry() throws {
        let recipes = try catalog()
        let plan = [PlanEntry(day: 0, recipe: "garlic-herb-chicken", portion: .one), PlanEntry(day: 1, recipe: "bbq-chicken", portion: .dinnerPlusLunch)]
        let list = Groceries.aggregate(plan: plan, recipes: recipes, pantry: ["olive-oil", "salt"])
        XCTAssertEqual(list.first { $0.id == "chicken-breast:lb" }?.amount, 2.5)
        XCTAssertEqual(list.first { $0.id == "baby-potatoes:lb" }?.amount, 2.5)
        XCTAssertFalse(list.contains { $0.id == "olive-oil:tbsp" })
        XCTAssertEqual(list.first?.category, .produce)
    }
    func testDifferentUnitsStaySeparateAndMissingRecipesAreIgnored() throws {
        var recipe = try catalog()[0]
        var ingredient = recipe.ingredients[0]; ingredient.unit = "oz"; ingredient.amount = 16
        recipe.ingredients.append(ingredient)
        let items = Groceries.aggregate(plan: [.init(day: 0, recipe: recipe.id, portion: .two), .init(day: 1, recipe: "missing", portion: .two)], recipes: [recipe], pantry: [])
        XCTAssertEqual(items.filter { $0.id.hasPrefix("chicken-breast:") }.count, 2)
    }
    func testAndroidCloudStateSchema() throws {
        let raw = #"{"schemaVersion":1,"favorites":["bbq-chicken"],"ratings":{"bbq-chicken":5},"notes":{"bbq-chicken":"More sauce"},"pantry":["salt"],"checkedGroceries":["chicken-breast:lb"],"plan":[{"day":0,"recipe":"bbq-chicken","portion":"DINNER_PLUS_LUNCH"}]}"#
        let state = try Documents.decodeState(Data(raw.utf8))
        XCTAssertEqual(state.plan[0].portion.servings, 4)
        XCTAssertEqual(state.notes["bbq-chicken"], "More sauce")
        XCTAssertEqual(try Documents.decodeState(Documents.encode(state)), state)
        XCTAssertThrowsError(try Documents.decodeState(Data(raw.replacingOccurrences(of: "\"schemaVersion\":1", with: "\"schemaVersion\":99").utf8)))
    }
    func testDatedPlansKeepConsecutiveWeeksAfterRoundTrip() throws {
        var state = UserState()
        state.plan = [PlanEntry(day: 20732, recipe: "bbq-chicken", portion: .one), PlanEntry(day: 20739, recipe: "bbq-chicken", portion: .dinnerPlusLunch)]
        let decoded = try Documents.decodeState(Documents.encode(state))
        XCTAssertEqual(decoded.plan, state.plan)
        XCTAssertEqual(decoded.schemaVersion, 2)
        let groceries = Groceries.aggregate(plan: decoded.plan, recipes: try catalog(), pantry: ["salt"])
        let chicken = try XCTUnwrap(groceries.first { $0.id == "chicken-breast:lb" })
        XCTAssertEqual(chicken.amount, 2.5)
        XCTAssertEqual(chicken.plannedDays, [20732, 20739])
        XCTAssertEqual(Set(chicken.plannedDays.map { PlanCalendar.weekStart($0) }).count, 2)
    }
    func testLegacyPlanMigrationIsPersistedOnlyOnce() throws {
        var legacy = UserState()
        legacy.schemaVersion = 1
        legacy.plan = [PlanEntry(day: 0, recipe: "bbq-chicken", portion: .two), PlanEntry(day: 6, recipe: "pizza", portion: .one)]
        let migrated = try Documents.decodeState(Documents.encode(legacy))
        XCTAssertEqual(migrated.plan.map(\.day), [PlanCalendar.weekStart(), PlanCalendar.weekStart() + 6])
        XCTAssertEqual(try Documents.decodeState(Documents.encode(migrated)), migrated)
    }
    func testCalendarAcrossYearLeapDayAndDaylightSaving() throws {
        for day in [19722, 19723, 19782, 19783, 19792, 20030] {
            XCTAssertEqual(PlanCalendar.day(PlanCalendar.date(day)), day)
            XCTAssertEqual(PlanCalendar.weekStart(day + 7), PlanCalendar.weekStart(day) + 7)
            XCTAssertTrue((PlanCalendar.weekStart(day)..<(PlanCalendar.weekStart(day) + 7)).contains(day))
        }
    }
    func testMinimalImportedRecipeUsesAndroidDefaults() throws {
        let raw = #"{"name":"Toast","ingredients":[{"name":"Bread","amount":2,"unit":"slices","category":"bakery"}],"steps":[{"title":"Toast","instruction":"Toast the bread."}]}"#
        let recipe = try Documents.importRecipe(Data("```json\n\(raw)\n```".utf8))
        XCTAssertEqual(recipe.shortName, "Toast")
        XCTAssertEqual(recipe.ingredients[0].id, "bread")
        XCTAssertEqual(recipe.ingredients[0].category, .bakery)
        XCTAssertEqual(recipe.steps[0].equipment, "Kitchen")
        XCTAssertThrowsError(try Documents.importRecipe(Data(raw.replacingOccurrences(of: "\"amount\":2", with: "\"amount\":0").utf8)))
    }
    func testInvalidImportAndFutureSchemaNeverSave() throws {
        XCTAssertThrowsError(try Documents.importRecipe(Data("not json".utf8)))
        let recipe = try catalog()[0]
        let json = String(data: try Documents.exportRecipe(recipe), encoding: .utf8)!
        XCTAssertThrowsError(try Documents.importRecipe(Data(json.replacingOccurrences(of: "\"schemaVersion\" : 1", with: "\"schemaVersion\" : 99").utf8)))
        var invalid = recipe; invalid.steps[0].timerSeconds = -1
        XCTAssertFalse(invalid.validationErrors.isEmpty)
    }
    func testTimerPauseResumeExpiryAndPersistence() throws {
        let start = Date(timeIntervalSince1970: 1_000)
        var timer = CookingTimer(id: "test", label: "Chicken", duration: 120)
        timer.start(at: start)
        XCTAssertEqual(timer.seconds(at: start.addingTimeInterval(30)), 90)
        timer.pause(at: start.addingTimeInterval(30))
        XCTAssertEqual(timer.seconds(at: start.addingTimeInterval(60)), 90)
        timer.start(at: start.addingTimeInterval(60))
        let restored = try JSONDecoder().decode(CookingTimer.self, from: Documents.encode(timer))
        XCTAssertEqual(restored.seconds(at: start.addingTimeInterval(160)), 0)
        timer.reset(); XCTAssertEqual(timer.seconds(at: start), 120); XCTAssertNil(timer.endAt)
    }
    func testRestartAfterPausedTimerExpiresUsesFullDuration() throws {
        let start = Date(timeIntervalSince1970: 1000)
        var timer = CookingTimer(id: "test", label: "Toast", duration: 60)
        timer.start(at: start); timer.pause(at: start.addingTimeInterval(20))
        timer.start(at: start.addingTimeInterval(30))
        timer.start(at: start.addingTimeInterval(100))
        XCTAssertEqual(timer.seconds(at: start.addingTimeInterval(100)), 60)
    }
    func testRecipeChangesDoNotReuseStaleTimerIdentity() throws {
        var step = try catalog()[0].steps[1]
        let original = step.timerID(recipeID: "recipe", index: 1)
        XCTAssertEqual(original, step.timerID(recipeID: "recipe", index: 1))
        step.timerSeconds = 900
        XCTAssertNotEqual(original, step.timerID(recipeID: "recipe", index: 1))
        XCTAssertNotEqual(original, step.timerID(recipeID: "recipe", index: 2))
    }
    func testSnapshotRoundTripAndCorruptionIsNotAnEmptyLibrary() throws {
        let folder = FileManager.default.temporaryDirectory.appendingPathComponent(UUID().uuidString)
        defer { try? FileManager.default.removeItem(at: folder) }
        let file = SnapshotFile(url: folder.appendingPathComponent("state.json"))
        XCTAssertTrue(try file.read().customRecipes.isEmpty)
        var snapshot = LocalSnapshot(); snapshot.customRecipes = try catalog(); snapshot.state.notes["bbq-chicken"] = "Test"
        snapshot.cloudOwnerUID = "owner"; snapshot.dirtyCloudDocuments = ["preferences"]
        try file.write(snapshot)
        XCTAssertEqual(try file.read().state, snapshot.state)
        XCTAssertEqual(try file.read().dirtyCloudDocuments, ["preferences"])
        XCTAssertEqual(try file.read().cloudOwnerUID, "owner")
        XCTAssertEqual(try file.read().customRecipes, snapshot.customRecipes)
        try Data("broken".utf8).write(to: file.url)
        XCTAssertThrowsError(try file.read())
        XCTAssertEqual(try String(contentsOf: file.url, encoding: .utf8), "broken")
    }
    func testAssistantRequestRespectsUTF8ServerLimit() throws {
        let messages = (0..<10).map { _ in AssistantMessage(role: "user", content: String(repeating: "菜", count: 39_000)) }
        let data = try AssistantProtocol.request(messages: messages, model: "sonnet")
        XCTAssertTrue(data.count <= 256_000)
        let value = try XCTUnwrap(JSONSerialization.jsonObject(with: data) as? [String: Any])
        let chosen = try XCTUnwrap(value["messages"] as? [[String: String]])
        XCTAssertEqual(chosen.count, 2)
    }
    func testAssistantProtocolModelsAndHistoryLimits() throws {
        let messages = (0..<40).map { AssistantMessage(role: $0 % 2 == 0 ? "user" : "assistant", content: "Message \($0)") }
        let data = try AssistantProtocol.request(messages: messages, model: "sonnet")
        let request = try XCTUnwrap(JSONSerialization.jsonObject(with: data) as? [String: Any])
        let selected = try XCTUnwrap(request["messages"] as? [[String: String]])
        XCTAssertEqual(selected.count, 30); XCTAssertEqual(selected.last?["content"], "Message 39")
        XCTAssertThrowsError(try AssistantProtocol.request(messages: messages, model: "unsupported"))
        XCTAssertThrowsError(try AssistantProtocol.request(messages: [.init(role: "user", content: String(repeating: "x", count: 180_001))], model: "sonnet"))
    }
}
