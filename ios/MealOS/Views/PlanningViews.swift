import SwiftUI

private let weekDays = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
struct PlanView: View {
    @EnvironmentObject private var store: MealStore
    @State private var selectedDay: DaySelection?
    @State private var cooking = false
    private var today: Int { (Calendar.current.component(.weekday, from: Date()) + 5) % 7 }
    private var tonight: PlanEntry? { store.state.plan.first { $0.day == today } }
    var body: some View {
        List {
            Section {
                VStack(alignment: .leading, spacing: 16) {
                    Text("Dinner, decided.").font(MealTheme.display(44))
                    if let entry = tonight, let recipe = store.recipes.first(where: { $0.id == entry.recipe }) {
                        Text(recipe.name).font(.title2.weight(.semibold))
                        Text(entry.portion.label).font(.subheadline)
                        Button { cooking = true } label: {
                            Label("Start cooking", systemImage: "play.fill").font(.headline).frame(maxWidth: .infinity, minHeight: 48)
                        }.buttonStyle(.borderedProminent).tint(MealTheme.cream).foregroundStyle(MealTheme.enamel)
                    } else {
                        Text("Choose tonight’s meal, then let your grocery list take shape.")
                        Button("Plan tonight") { selectedDay = DaySelection(id: today) }
                            .buttonStyle(.borderedProminent).tint(MealTheme.cream).foregroundStyle(MealTheme.enamel).controlSize(.large)
                    }
                }.padding(.vertical, 14).foregroundStyle(MealTheme.cream)
            }.listRowBackground(MealTheme.enamel)
            Section("Your week") {
                ForEach(0..<7) { day in
                    HStack(alignment: .center, spacing: 16) {
                        Button { selectedDay = DaySelection(id: day) } label: {
                            VStack(alignment: .leading, spacing: 6) {
                                Text(weekDays[day] + (day == today ? " · Today" : "")).font(.subheadline.weight(.semibold)).foregroundStyle(.secondary)
                                if let entry = store.state.plan.first(where: { $0.day == day }), let recipe = store.recipes.first(where: { $0.id == entry.recipe }) {
                                    Text(recipe.shortName).font(.headline).foregroundStyle(.primary)
                                    Text(entry.portion.label).font(.caption).foregroundStyle(.secondary)
                                } else { Text("Choose a meal").foregroundStyle(MealTheme.action) }
                            }.frame(maxWidth: .infinity, alignment: .leading).padding(.vertical, 8).contentShape(Rectangle())
                        }.buttonStyle(.plain)
                        if let entry = store.state.plan.first(where: { $0.day == day }), let recipe = store.recipes.first(where: { $0.id == entry.recipe }) {
                            NavigationLink { RecipeDetail(recipe: recipe, initialPortion: entry.portion) } label: {
                                Image(systemName: "book").frame(minWidth: 44, minHeight: 44)
                            }.buttonStyle(.borderless).accessibilityLabel("View \(recipe.shortName)")
                        }
                    }
                    .swipeActions { Button("Remove", role: .destructive) { store.updateState { $0.plan.removeAll { $0.day == day } } } }
                }
            }
        }
        .scrollContentBackground(.hidden).background(MealTheme.background)
        .navigationTitle("Meal plan")
        .sheet(item: $selectedDay) { DayPlanner(day: $0.id) }
        .navigationDestination(isPresented: $cooking) {
            if let entry = tonight, let recipe = store.recipes.first(where: { $0.id == entry.recipe }) {
                CookingView(recipe: recipe, portion: entry.portion)
            }
        }
    }
}
private struct DaySelection: Identifiable { let id: Int }
struct DayPlanner: View {
    let day: Int
    @EnvironmentObject private var store: MealStore
    @Environment(\.dismiss) private var dismiss
    @State private var portion: Portion = .two
    @State private var search = ""
    var body: some View {
        NavigationStack {
            List {
                Section { Picker("Portions", selection: $portion) { ForEach(Portion.allCases, id: \.self) { Text($0.label).tag($0) } } }
                Section("Choose a meal") {
                    ForEach(store.recipes.filter { search.isEmpty || $0.name.localizedCaseInsensitiveContains(search) }) { recipe in
                        Button { store.plan(day: day, recipe: recipe, portion: portion); dismiss() } label: {
                            RecipeRow(recipe: recipe, favorite: store.state.favorites.contains(recipe.id)).foregroundStyle(.primary)
                        }
                    }
                }
            }.searchable(text: $search, prompt: "Find a recipe")
                .navigationTitle(weekDays[day]).navigationBarTitleDisplayMode(.inline)
                .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } } }
                .onAppear { portion = store.state.plan.first(where: { $0.day == day })?.portion ?? .two }
        }
    }
}
struct GroceryView: View {
    @EnvironmentObject private var store: MealStore
    @State private var showPantry = false
    var body: some View {
        List {
            if store.state.plan.isEmpty {
                EmptyMealView(title: "Plan a meal first", symbol: "basket", detail: "Ingredients from your weekly plan will appear here, combined and ready to shop.")
            } else if store.groceries.isEmpty {
                EmptyMealView(title: "Your pantry has it covered", symbol: "checkmark", detail: "All ingredients in your plan are excluded as pantry items.")
            } else {
                Section {
                    Text("\(store.groceries.filter { !store.state.checkedGroceries.contains($0.id) }.count) items left · \(store.state.plan.count) planned meals").foregroundStyle(.secondary)
                }
                ForEach(IngredientCategory.allCases, id: \.self) { category in
                    let items = store.groceries.filter { $0.category == category }
                    if !items.isEmpty {
                        Section(category.label) {
                            ForEach(items) { item in
                                let checked = store.state.checkedGroceries.contains(item.id)
                                Button { store.toggleGrocery(item.id) } label: {
                                    HStack(spacing: 16) {
                                        Image(systemName: checked ? "checkmark.circle.fill" : "circle").font(.title2).foregroundStyle(checked ? MealTheme.action : .secondary)
                                        VStack(alignment: .leading, spacing: 4) {
                                            Text(item.name).strikethrough(checked).foregroundStyle(.primary)
                                            Text(item.quantity).font(.subheadline).foregroundStyle(.secondary)
                                        }
                                        Spacer()
                                    }.frame(minHeight: 48).contentShape(Rectangle())
                                }.buttonStyle(.plain).accessibilityLabel("\(item.name), \(item.quantity), \(checked ? "checked" : "not checked")")
                            }
                        }
                    }
                }
                Section { Button("Uncheck all items") { store.updateState { $0.checkedGroceries.removeAll() } } }
            }
        }.navigationTitle("Grocery list")
            .scrollContentBackground(.hidden).background(MealTheme.background)
            .toolbar { Button("Pantry", systemImage: "cabinet") { showPantry = true } }
            .sheet(isPresented: $showPantry) { NavigationStack { PantryView().toolbar { ToolbarItem(placement: .confirmationAction) { Button("Done") { showPantry = false } } } } }
    }
}
struct PantryView: View {
    @EnvironmentObject private var store: MealStore
    @State private var search = ""
    private var ingredients: [Ingredient] {
        var seen = Set<String>()
        return store.recipes.flatMap(\.ingredients).filter { seen.insert($0.id).inserted }
            .filter { search.isEmpty || $0.name.localizedCaseInsensitiveContains(search) }.sorted { $0.name < $1.name }
    }
    var body: some View {
        List {
            Section { Text("Items you already have are excluded from your grocery list.").foregroundStyle(.secondary) }
            ForEach(ingredients) { item in
                Toggle(item.name, isOn: Binding(get: { store.state.pantry.contains(item.id) }, set: { selected in
                    store.updateState { if selected { $0.pantry.insert(item.id) } else { $0.pantry.remove(item.id) } }
                })).padding(.vertical, 4)
            }
        }.navigationTitle("Pantry").searchable(text: $search)
    }
}
