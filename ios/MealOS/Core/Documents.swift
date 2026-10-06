import Foundation

public struct MealOSError: LocalizedError, Equatable {
    public var message: String
    public var errorDescription: String? { message }
    public init(_ message: String) { self.message = message }
}
public enum Documents {
    public static func encode<T: Encodable>(_ value: T) throws -> Data {
        let encoder = JSONEncoder(); encoder.outputFormatting = [.sortedKeys, .prettyPrinted]
        return try encoder.encode(value)
    }
    public static func decodeState(_ data: Data) throws -> UserState {
        let value = try JSONDecoder().decode(UserState.self, from: data)
        guard value.schemaVersion == 2 else { throw MealOSError("This account uses a newer data format. Update MealOS before syncing.") }
        guard Set(value.plan.map(\.day)).count == value.plan.count else {
            throw MealOSError("The saved meal plan contains invalid days.")
        }
        return value
    }
    public static func recipes(_ data: Data) throws -> [Recipe] {
        guard let values = try JSONSerialization.jsonObject(with: data) as? [[String: Any]] else { throw MealOSError("Could not read the recipe library.") }
        let recipes = try values.map(normalize)
        guard Set(recipes.map(\.id)).count == recipes.count else { throw MealOSError("The library contains duplicate recipe identifiers.") }
        return recipes
    }
    public static func importRecipe(_ data: Data) throws -> Recipe {
        guard data.count <= 2_000_000, let text = String(data: data, encoding: .utf8),
              let first = text.firstIndex(of: "{"), let last = text.lastIndex(of: "}") else { throw MealOSError("Choose a MealOS recipe file smaller than 2 MB.") }
        guard let object = try JSONSerialization.jsonObject(with: Data(text[first...last].utf8)) as? [String: Any] else {
            throw MealOSError("Could not read this recipe file.")
        }
        let value: [String: Any]
        if object["format"] as? String == "mealos.recipe" {
            guard object["schemaVersion"] as? Int == 1 else { throw MealOSError("This recipe file uses an unsupported version.") }
            guard let recipe = object["recipe"] as? [String: Any] else { throw MealOSError("The recipe is missing.") }
            value = recipe
        } else { value = object }
        var recipe = try normalize(value)
        recipe.id = UUID().uuidString; recipe.origin = .imported
        return recipe
    }
    public static func exportRecipe(_ recipe: Recipe) throws -> Data {
        struct Envelope: Encodable { let format = "mealos.recipe"; let schemaVersion = 1; let recipe: Recipe }
        return try encode(Envelope(recipe: recipe))
    }
    private static func normalize(_ input: [String: Any]) throws -> Recipe {
        var value = input
        let defaults: [String: Any] = ["id": UUID().uuidString, "name": "", "shortName": value["name"] ?? "", "description": "", "effortMinutes": 10, "totalMinutes": 30, "baseServings": 2, "equipment": [String](), "tags": [String](), "leftoverNote": "", "origin": "MANUAL"]
        for (key, fallback) in defaults where value[key] == nil || value[key] is NSNull { value[key] = fallback }
        value["origin"] = RecipeOrigin(rawValue: (value["origin"] as? String ?? "").uppercased())?.rawValue ?? "MANUAL"
        if let items = value["ingredients"] as? [[String: Any]] {
            value["ingredients"] = items.map { item in
                var item = item
                if (item["id"] as? String ?? "").isEmpty { item["id"] = Ingredient.key(for: item["name"] as? String ?? "") }
                item["pantryStaple"] = item["pantryStaple"] as? Bool ?? false
                item["category"] = IngredientCategory(rawValue: (item["category"] as? String ?? "").uppercased())?.rawValue ?? "PANTRY"
                return item
            }
        }
        if let steps = value["steps"] as? [[String: Any]] {
            value["steps"] = steps.map { item in
                var item = item; if item["equipment"] == nil { item["equipment"] = "Kitchen" }; return item
            }
        }
        let recipe = try JSONDecoder().decode(Recipe.self, from: JSONSerialization.data(withJSONObject: value))
        guard recipe.validationErrors.isEmpty else { throw MealOSError(recipe.validationErrors.joined(separator: "\n")) }
        return recipe
    }
}
public struct LocalSnapshot: Codable {
    public var state = UserState()
    public var customRecipes: [Recipe] = []
    public var timers: [CookingTimer] = []
    public var cookingSteps: [String: Int] = [:]
    public var cloudOwnerUID: String?
    public var dirtyCloudDocuments: Set<String>?
    public var assistant = AssistantHistory()
    public init() {}
}
public struct AssistantMessage: Codable, Identifiable, Equatable, Sendable {
    public var id = UUID().uuidString
    public var role: String
    public var content: String
    public init(role: String, content: String) { self.role = role; self.content = content }
}
public struct RecipeSource: Codable, Equatable, Sendable { public var title: String; public var url: String }
public struct AssistantHistory: Codable {
    public var messages: [AssistantMessage] = []
    public var model = "sonnet"
    public var draft: Recipe?
    public var sources: [RecipeSource] = []
    public var questions: [String] = []
    public init() {}
}
public enum AssistantProtocol {
    public static func request(messages: [AssistantMessage], model: String) throws -> Data {
        guard ["sonnet", "opus", "claude-haiku-4-5"].contains(model) else { throw MealOSError("Choose a supported assistant model.") }
        guard let latest = messages.last, latest.content.utf16.count <= 40_000,
              !latest.content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            throw MealOSError("Keep each message under 40,000 characters and include some text.")
        }
        var chosen: [[String: String]] = []; var used = 0
        for message in messages.suffix(30).reversed() {
            guard ["user", "assistant"].contains(message.role), message.content.utf16.count <= 40_000,
                  !message.content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { break }
            let cost = message.content.utf16.count + 32
            if used + cost > 180_000 { break }
            chosen.insert(["role": message.role, "content": message.content], at: 0); used += cost
        }
        guard !chosen.isEmpty else { throw MealOSError("This message is too long. Shorten it and try again.") }
        var data = try JSONSerialization.data(withJSONObject: ["model": model, "messages": chosen])
        while data.count > 256_000 && chosen.count > 1 {
            chosen.removeFirst()
            data = try JSONSerialization.data(withJSONObject: ["model": model, "messages": chosen])
        }
        guard data.count <= 256_000 else { throw MealOSError("This message is too large. Shorten it and try again.") }
        return data
    }
    public static func response(_ data: Data) throws -> (String, Recipe?, [RecipeSource], [String]) {
        guard let value = try JSONSerialization.jsonObject(with: data) as? [String: Any],
              let reply = value["reply"] as? String, !reply.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            throw MealOSError("The assistant returned an empty response. Try again.")
        }
        var recipe: Recipe?
        if let document = value["recipeDocument"] as? [String: Any] { recipe = try Documents.importRecipe(JSONSerialization.data(withJSONObject: document)) }
        let sources = (value["sources"] as? [[String: String]] ?? []).compactMap { item -> RecipeSource? in
            guard let title = item["title"], let url = item["url"], URL(string: url)?.scheme == "https" else { return nil }
            return RecipeSource(title: title, url: url)
        }
        return (reply, recipe, sources, value["questions"] as? [String] ?? [])
    }
}
public struct SnapshotFile {
    public var url: URL
    public init(url: URL) { self.url = url }
    public func read() throws -> LocalSnapshot {
        guard FileManager.default.fileExists(atPath: url.path) else { return LocalSnapshot() }
        return try JSONDecoder().decode(LocalSnapshot.self, from: Data(contentsOf: url))
    }
    public func write(_ value: LocalSnapshot) throws {
        try FileManager.default.createDirectory(at: url.deletingLastPathComponent(), withIntermediateDirectories: true)
        try Documents.encode(value).write(to: url, options: .atomic)
    }
}
