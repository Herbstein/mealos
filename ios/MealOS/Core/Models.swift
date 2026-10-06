import Foundation

public enum IngredientCategory: String, Codable, CaseIterable, Sendable {
    case produce = "PRODUCE", meat = "MEAT", dairy = "DAIRY", bakery = "BAKERY", pantry = "PANTRY", frozen = "FROZEN"
    public var label: String { rawValue.capitalized }
}
public enum Portion: String, Codable, CaseIterable, Sendable {
    case one = "ONE", two = "TWO", dinnerPlusLunch = "DINNER_PLUS_LUNCH"
    public var servings: Int { self == .one ? 1 : self == .two ? 2 : 4 }
    public var label: String { self == .one ? "Just me" : self == .two ? "Dinner for two" : "Dinner + lunch tomorrow" }
}
public enum RecipeOrigin: String, Codable, Sendable {
    case core = "CORE", manual = "MANUAL", imported = "CHATGPT_IMPORT"
}
public struct Ingredient: Codable, Equatable, Identifiable, Sendable {
    public var id: String = UUID().uuidString
    public var name = ""
    public var amount: Double = 1
    public var unit = "count"
    public var category: IngredientCategory = .pantry
    public var pantryStaple = false
    public init() {}
    public func quantity(for recipe: Recipe, portion: Portion) -> String {
        Self.format(amount * Double(portion.servings) / Double(max(1, recipe.baseServings))) + " " + unit
    }
    public static func format(_ amount: Double) -> String {
        amount.formatted(.number.precision(.fractionLength(0...2)))
    }
    public static func key(for name: String) -> String {
        let key = name.lowercased().replacingOccurrences(of: "[^a-z0-9]+", with: "-", options: .regularExpression)
            .trimmingCharacters(in: CharacterSet(charactersIn: "-"))
        return key.isEmpty ? UUID().uuidString : key
    }
}
public struct RecipeStep: Codable, Equatable, Sendable {
    public var title = ""
    public var instruction = ""
    public var equipment = "Kitchen"
    public var timerSeconds: Int?
    public var tip: String?
    public init() {}
    public func timerID(recipeID: String, index: Int) -> String {
        // Stable across launches; editing or reordering a step starts a distinct timer.
        // Existing active timers keep their original labels on the timer shelf.
        let key = "\(title)|\(instruction)|\(equipment)|\(timerSeconds ?? 0)"
        let hash = key.utf8.reduce(UInt64(14695981039346656037)) { ($0 ^ UInt64($1)) &* 1099511628211 }
        return "\(recipeID):\(index):\(String(hash, radix: 16))"
    }
}
public struct Recipe: Codable, Equatable, Identifiable, Sendable {
    public var id = UUID().uuidString
    public var name = ""
    public var shortName = ""
    public var effortMinutes = 10
    public var totalMinutes = 30
    public var baseServings = 2
    public var equipment: [String] = []
    public var tags: [String] = []
    public var leftoverNote = ""
    public var ingredients: [Ingredient] = []
    public var steps: [RecipeStep] = []
    public var description = ""
    public var origin: RecipeOrigin = .manual
    public init() {}
    public var validationErrors: [String] {
        var errors: [String] = []
        if name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { errors.append("Add a recipe name.") }
        if shortName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { errors.append("Add a short name.") }
        if baseServings < 1 { errors.append("Servings must be at least 1.") }
        if effortMinutes < 0 || totalMinutes < 1 || totalMinutes < effortMinutes { errors.append("Check the recipe times; total must include hands-on time.") }
        if ingredients.isEmpty { errors.append("Add at least one ingredient.") }
        for (i, item) in ingredients.enumerated() {
            if item.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || item.unit.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || !item.amount.isFinite || item.amount <= 0 {
                errors.append("Ingredient \(i + 1) needs a name, unit, and amount above zero.")
            }
        }
        if steps.isEmpty { errors.append("Add at least one cooking step.") }
        for (i, step) in steps.enumerated() {
            if [step.title, step.instruction, step.equipment].contains(where: { $0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }) {
                errors.append("Step \(i + 1) needs a title, instruction, and equipment.")
            }
            if let timer = step.timerSeconds, timer <= 0 { errors.append("Step \(i + 1) needs a timer above zero or no timer.") }
        }
        return errors
    }
}
public struct PlanEntry: Codable, Equatable, Identifiable, Sendable {
    public var day: Int
    public var recipe: String
    public var portion: Portion
    public var id: Int { day }
    public init(day: Int, recipe: String, portion: Portion) { self.day = day; self.recipe = recipe; self.portion = portion }
}
public struct UserState: Codable, Equatable, Sendable {
    public var schemaVersion = 2
    public var favorites: Set<String> = []
    public var ratings: [String: Int] = [:]
    public var notes: [String: String] = [:]
    public var pantry: Set<String> = ["olive-oil", "salt", "black-pepper", "garlic-powder", "italian-seasoning"]
    public var checkedGroceries: Set<String> = []
    public var plan: [PlanEntry] = []
    public init() {}
    private enum CodingKeys: String, CodingKey { case schemaVersion, favorites, ratings, notes, pantry, checkedGroceries, plan }
    public init(from decoder: Decoder) throws {
        let c = try decoder.container(keyedBy: CodingKeys.self)
        let version = try c.decode(Int.self, forKey: .schemaVersion)
        guard (1...2).contains(version) else { throw MealOSError("Update MealOS to read this plan.") }
        favorites = try c.decode(Set<String>.self, forKey: .favorites)
        ratings = try c.decode([String: Int].self, forKey: .ratings)
        notes = try c.decode([String: String].self, forKey: .notes)
        pantry = try c.decode(Set<String>.self, forKey: .pantry)
        checkedGroceries = try c.decode(Set<String>.self, forKey: .checkedGroceries)
        plan = try c.decode([PlanEntry].self, forKey: .plan)
        if version == 1 {
            guard plan.allSatisfy({ (0...6).contains($0.day) }) else { throw MealOSError("Invalid legacy plan days.") }
            let monday = PlanCalendar.weekStart()
            plan = plan.map { PlanEntry(day: monday + $0.day, recipe: $0.recipe, portion: $0.portion) }
        }
        guard Set(plan.map(\.day)).count == plan.count else { throw MealOSError("Duplicate plan dates.") }
    }

}
public struct GroceryItem: Identifiable, Equatable, Sendable {
    public var id: String
    public var name: String
    public var amount: Double
    public var unit: String
    public var category: IngredientCategory
    public var plannedDays: Set<Int> = []
    public var pantryStaple = false
    public var quantity: String { Ingredient.format(amount) + " " + unit }
}
public enum Groceries {
    public static func aggregate(plan: [PlanEntry], recipes: [Recipe], pantry: Set<String>) -> [GroceryItem] {
        var result: [String: GroceryItem] = [:]
        for entry in plan {
            guard let recipe = recipes.first(where: { $0.id == entry.recipe }) else { continue }
            for ingredient in recipe.ingredients where !pantry.contains(ingredient.id) {
                let key = "\(ingredient.id):\(ingredient.unit)"
                let amount = ingredient.amount * Double(entry.portion.servings) / Double(max(1, recipe.baseServings))
                if result[key] != nil { result[key]!.amount += amount; result[key]!.plannedDays.insert(entry.day); result[key]!.pantryStaple = result[key]!.pantryStaple || ingredient.pantryStaple }
                else { result[key] = GroceryItem(id: key, name: ingredient.name, amount: amount, unit: ingredient.unit, category: ingredient.category, plannedDays: [entry.day], pantryStaple: ingredient.pantryStaple) }
            }
        }
        return result.values.sorted { a, b in
            let categories = IngredientCategory.allCases
            return a.category == b.category ? a.name < b.name : categories.firstIndex(of: a.category)! < categories.firstIndex(of: b.category)!
        }
    }
}
public struct CookingTimer: Codable, Identifiable, Equatable, Sendable {
    public var id: String
    public var label: String
    public var duration: TimeInterval
    public var endAt: Date?
    public var remaining: TimeInterval
    public init(id: String, label: String, duration: TimeInterval) {
        self.id = id; self.label = label; self.duration = duration; remaining = duration
    }
    public func seconds(at now: Date) -> Int { max(0, Int(ceil(endAt?.timeIntervalSince(now) ?? remaining))) }
    public mutating func start(at now: Date) {
        if let deadline = endAt {
            if deadline > now { return }
            remaining = duration
        }
        endAt = now.addingTimeInterval(remaining > 0 ? remaining : duration)
    }
    public mutating func pause(at now: Date) { remaining = Double(seconds(at: now)); endAt = nil }
    public mutating func reset() { endAt = nil; remaining = duration }
}

public enum PlanCalendar {
    // Use UTC only to encode civil dates; local dates and DST never shift a meal.
    public static func day(_ date: Date = Date()) -> Int {
        let local = Calendar(identifier: .gregorian)
        var utc = local; utc.timeZone = TimeZone(secondsFromGMT: 0)!
        return Int(utc.date(from: local.dateComponents([.year, .month, .day], from: date))!.timeIntervalSince1970 / 86400)
    }
    public static func date(_ day: Int) -> Date {
        var utc = Calendar(identifier: .gregorian); utc.timeZone = TimeZone(secondsFromGMT: 0)!
        let parts = utc.dateComponents([.year, .month, .day], from: Date(timeIntervalSince1970: Double(day) * 86400))
        return Calendar(identifier: .gregorian).date(from: parts)!
    }
    public static func weekStart(_ day: Int = day()) -> Int { day - ((day + 3) % 7 + 7) % 7 }
    public static func monthStart(_ day: Int) -> Int {
        day - Calendar(identifier: .gregorian).component(.day, from: date(day)) + 1
    }
    public static func movingMonth(_ day: Int, by offset: Int) -> Int {
        let calendar = Calendar(identifier: .gregorian)
        return self.day(calendar.date(byAdding: .month, value: offset, to: date(day))!)
    }
    /// Complete Monday–Sunday rows, including adjacent-month dates.
    public static func monthDays(_ day: Int) -> [Int] {
        let first = monthStart(day)
        let last = movingMonth(first, by: 1) - 1
        return Array(weekStart(first)..<(weekStart(last) + 7))
    }
    public static func label(_ day: Int) -> String { date(day).formatted(.dateTime.weekday(.wide).month(.abbreviated).day()) }
    public static func range(_ start: Int, weeks: Int = 1) -> String {
        date(start).formatted(.dateTime.month(.abbreviated).day().year()) + " – " + date(start + weeks * 7 - 1).formatted(.dateTime.month(.abbreviated).day().year())
    }
}
