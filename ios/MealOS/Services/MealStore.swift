import SwiftUI
import UserNotifications

@MainActor final class MealStore: ObservableObject {
    @Published private(set) var snapshot = LocalSnapshot()
    @Published private(set) var core: [Recipe] = []
    @Published var error: String?
    @Published var timerNotice: String?
    @Published var sending = false
    var onCloudChange: ((Bool) -> Void)?
    private var storageHealthy = true
    private var durableSnapshot = LocalSnapshot()
    private let file: SnapshotFile
    var state: UserState { snapshot.state }
    var recipes: [Recipe] { core + snapshot.customRecipes }
    var groceries: [GroceryItem] { Groceries.aggregate(plan: state.plan, recipes: recipes, pantry: state.pantry) }
    var assistant: AssistantHistory { snapshot.assistant }
    init() {
        var folder = "MealOS"
        #if DEBUG
        if ProcessInfo.processInfo.arguments.contains("--uitesting") { folder = "MealOS-UITests" }
        #endif
        let directory = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0].appendingPathComponent(folder)
        file = SnapshotFile(url: directory.appendingPathComponent("mealos-v1.json"))
        #if DEBUG
        if folder == "MealOS-UITests", ProcessInfo.processInfo.arguments.contains("--reset-ui-test-state") {
            try? FileManager.default.removeItem(at: file.url)
        }
        #endif
        do { snapshot = try file.read(); durableSnapshot = snapshot; _ = persist() } catch {
            storageHealthy = false
            self.error = "Your saved data could not be read. It has been left untouched. Restore the file or contact support before saving changes. \(error.localizedDescription)"
        }
        do {
            guard let url = Bundle.main.url(forResource: "recipes", withExtension: "json") else { throw MealOSError("The bundled recipe library is missing.") }
            core = try Documents.recipes(Data(contentsOf: url))
        } catch { self.error = error.localizedDescription }
    }
    @discardableResult private func persist() -> Bool {
        guard storageHealthy else { snapshot = durableSnapshot; return false }
        do { try file.write(snapshot); durableSnapshot = snapshot; return true }
        catch {
            snapshot = durableSnapshot
            self.error = "Could not save on this device: \(error.localizedDescription)"
            return false
        }
    }
    func updateState(_ mutation: (inout UserState) -> Void) {
        guard storageHealthy else { return }
        let previousPlan = snapshot.state.plan
        mutation(&snapshot.state)
        if previousPlan != snapshot.state.plan { snapshot.state.checkedGroceries.removeAll() }
        markDirty("preferences"); if persist() { onCloudChange?(false) }
    }
    @discardableResult func applyCloud(state: UserState? = nil, recipes: [Recipe]? = nil) -> Bool {
        guard storageHealthy else { return false }
        if let state { snapshot.state = state }
        if let recipes { snapshot.customRecipes = recipes }
        return persist()
    }
    func save(_ recipe: Recipe) -> Bool {
        guard storageHealthy else { return false }
        guard recipe.validationErrors.isEmpty else { error = recipe.validationErrors.joined(separator: "\n"); return false }
        if let i = snapshot.customRecipes.firstIndex(where: { $0.id == recipe.id }) { snapshot.customRecipes[i] = recipe }
        else { snapshot.customRecipes.append(recipe) }
        markDirty("recipes"); guard persist() else { return false }; onCloudChange?(true); return true
    }
    @discardableResult func bindCloud(to uid: String) -> Bool {
        if snapshot.cloudOwnerUID != uid { snapshot.dirtyCloudDocuments = [] }
        snapshot.cloudOwnerUID = uid; return persist()
    }
    func unbindCloud() { snapshot.cloudOwnerUID = nil; snapshot.dirtyCloudDocuments = []; persist() }
    func acknowledgeCloud(_ name: String) { snapshot.dirtyCloudDocuments?.remove(name); persist() }
    private func markDirty(_ name: String) {
        if snapshot.cloudOwnerUID != nil {
            if snapshot.dirtyCloudDocuments == nil { snapshot.dirtyCloudDocuments = [] }
            snapshot.dirtyCloudDocuments?.insert(name)
        }
    }
    func plan(day: Int, recipe: Recipe, portion: Portion) {
        updateState { $0.plan.removeAll { $0.day == day }; $0.plan.append(PlanEntry(day: day, recipe: recipe.id, portion: portion)) }
    }
    func toggleFavorite(_ id: String) { updateState { if !$0.favorites.insert(id).inserted { $0.favorites.remove(id) } } }
    func toggleGrocery(_ id: String) { updateState { if !$0.checkedGroceries.insert(id).inserted { $0.checkedGroceries.remove(id) } } }
    func setStep(_ index: Int, recipe: String) { snapshot.cookingSteps[recipe] = index; persist() }
    func updateAssistant(_ mutation: (inout AssistantHistory) -> Void) { mutation(&snapshot.assistant); persist() }
    func timer(id: String) -> CookingTimer? { snapshot.timers.first { $0.id == id } }
    func startTimer(id: String, label: String, duration: Int) async {
        var timer = timer(id: id) ?? CookingTimer(id: id, label: label, duration: Double(duration))
        timer.start(at: Date()); guard replaceTimer(timer) else { return }
        let center = UNUserNotificationCenter.current()
        do {
            let allowed = try await center.requestAuthorization(options: [.alert, .sound])
            guard allowed else { timerNotice = "Timer is running. Enable notifications in Settings to receive an alert outside MealOS."; return }
            // Permission prompts can outlive a pause/reset. Schedule only the current running timer.
            guard let current = self.timer(id: id), let end = current.endAt, end > Date() else { return }
            let content = UNMutableNotificationContent(); content.title = "\(label) is ready"; content.body = "Open MealOS to continue cooking."; content.sound = .default
            try await center.add(UNNotificationRequest(identifier: id, content: content, trigger: UNTimeIntervalNotificationTrigger(timeInterval: max(1, end.timeIntervalSinceNow), repeats: false)))
            if self.timer(id: id)?.endAt != end { center.removePendingNotificationRequests(withIdentifiers: [id]) }
            timerNotice = nil
        } catch { timerNotice = "Timer is running, but its alert could not be scheduled. \(error.localizedDescription)" }
    }
    func pauseTimer(_ id: String) {
        guard var value = timer(id: id) else { return }; value.pause(at: Date()); guard replaceTimer(value) else { return }
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [id])
    }
    func resetTimer(_ id: String) {
        guard var value = timer(id: id) else { return }; value.reset(); guard replaceTimer(value) else { return }
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: [id])
        UNUserNotificationCenter.current().removeDeliveredNotifications(withIdentifiers: [id])
    }
    @discardableResult private func replaceTimer(_ value: CookingTimer) -> Bool {
        if let i = snapshot.timers.firstIndex(where: { $0.id == value.id }) { snapshot.timers[i] = value }
        else { snapshot.timers.append(value) }; return persist()
    }
}
