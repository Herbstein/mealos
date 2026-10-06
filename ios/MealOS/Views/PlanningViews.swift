import SwiftUI

private struct WeekNavigation: View {
    @Binding var start: Int
    var body: some View {
        VStack(spacing: 8) {
            HStack {
                Button { start -= 7 } label: { Image(systemName: "chevron.left").frame(minWidth: 44, minHeight: 44) }.accessibilityLabel("Previous week")
                Spacer()
                Text(PlanCalendar.range(start)).font(.headline).multilineTextAlignment(.center)
                Spacer()
                Button { start += 7 } label: { Image(systemName: "chevron.right").frame(minWidth: 44, minHeight: 44) }.accessibilityLabel("Next week")
            }.buttonStyle(.borderless)
            if start != PlanCalendar.weekStart() {
                Button("This week") { start = PlanCalendar.weekStart() }.frame(minHeight: 44).buttonStyle(.borderless)
            }
        }
    }
}
struct PlanView: View {
    @EnvironmentObject private var store: MealStore
    @State private var selectedDay: DaySelection?
    @State private var cooking = false
    @State private var weekStart = PlanCalendar.weekStart()
    private var today: Int { PlanCalendar.day() }
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
            Section { WeekNavigation(start: $weekStart) }
            Section("Your week") {
                ForEach(Array(weekStart..<(weekStart + 7)), id: \.self) { day in
                    HStack(alignment: .center, spacing: 16) {
                        Button { selectedDay = DaySelection(id: day) } label: {
                            VStack(alignment: .leading, spacing: 6) {
                                Text(PlanCalendar.label(day) + (day == today ? " · Today" : "")).font(.subheadline.weight(.semibold)).foregroundStyle(.secondary)
                                if let entry = store.state.plan.first(where: { $0.day == day }), let recipe = store.recipes.first(where: { $0.id == entry.recipe }) {
                                    Text(recipe.shortName).font(.headline).foregroundStyle(.primary)
                                    Text(entry.portion.label).font(.caption).foregroundStyle(.secondary)
                                } else { Text("Choose a meal").foregroundStyle(MealTheme.action) }
                            }.frame(maxWidth: .infinity, alignment: .leading).padding(.vertical, 8).contentShape(Rectangle())
                        }.buttonStyle(.plain).accessibilityIdentifier("plan-day-\(day)")
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
                .navigationTitle(PlanCalendar.label(day)).navigationBarTitleDisplayMode(.inline)
                .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } } }
                .onAppear { portion = store.state.plan.first(where: { $0.day == day })?.portion ?? .two }
        }
    }
}
struct GroceryView: View {
    @EnvironmentObject private var store: MealStore
    @State private var showPantry = false
    @State private var weekStart = PlanCalendar.weekStart()
    @State private var weeks = 2
    private var plan: [PlanEntry] { store.state.plan.filter { $0.day >= weekStart && $0.day < weekStart + weeks * 7 } }
    private var groceries: [GroceryItem] { Groceries.aggregate(plan: plan, recipes: store.recipes, pantry: store.state.pantry) }
    private func checkID(_ item: GroceryItem) -> String { "\(weekStart):\(weeks):\(item.id):\(item.amount)" }
    var body: some View {
        List {
            Section("Shopping window") {
                WeekNavigation(start: $weekStart)
                Picker("Consecutive weeks", selection: $weeks) {
                    ForEach(1...4, id: \.self) { Text("\($0) \($0 == 1 ? "week" : "weeks")").tag($0) }
                }
                Text(PlanCalendar.range(weekStart, weeks: weeks)).font(.subheadline).foregroundStyle(.secondary)
                Text("Totals cover this window. Repeated ingredients can help you compare larger packs; pantry items you already have stay excluded.").font(.subheadline).foregroundStyle(.secondary)
            }
            if plan.isEmpty {
                EmptyMealView(title: "Plan a meal first", symbol: "basket", detail: "Add meals in this date range to build a combined shopping list.")
            } else if groceries.isEmpty {
                EmptyMealView(title: "Your pantry has it covered", symbol: "checkmark", detail: "All ingredients in your plan are excluded as pantry items.")
            } else {
                Section {
                    Text("\(groceries.filter { !store.state.checkedGroceries.contains(checkID($0)) }.count) items left · \(plan.count) planned meals").foregroundStyle(.secondary)
                }
                ForEach(IngredientCategory.allCases, id: \.self) { category in
                    let items = groceries.filter { $0.category == category }
                    if !items.isEmpty {
                        Section(category.label) {
                            ForEach(items) { item in
                                let checked = store.state.checkedGroceries.contains(checkID(item))
                                Button { store.toggleGrocery(checkID(item)) } label: {
                                    HStack(spacing: 16) {
                                        Image(systemName: checked ? "checkmark.circle.fill" : "circle").font(.title2).foregroundStyle(checked ? MealTheme.action : .secondary)
                                        VStack(alignment: .leading, spacing: 4) {
                                            Text(item.name).strikethrough(checked).foregroundStyle(.primary)
                                            Text(item.quantity).font(.subheadline).foregroundStyle(.secondary)
                                            Text("\(item.pantryStaple ? "Staple · " : "")Used in \(item.plannedDays.count) meals across \(Set(item.plannedDays.map { PlanCalendar.weekStart($0) }).count) weeks")
                                                .font(.caption).foregroundStyle(.secondary)
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
