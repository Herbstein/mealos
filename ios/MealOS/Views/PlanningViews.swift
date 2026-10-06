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
private enum PlanPeriod: String, CaseIterable {
    case month = "Month", week = "Week"
}

struct PlanView: View {
    @EnvironmentObject private var store: MealStore
    @Environment(\.dynamicTypeSize) private var typeSize
    @State private var planner: DaySelection?
    @State private var cooking = false
    @State private var focusedDay = PlanCalendar.day()
    @State private var period: PlanPeriod = .month
    private var today: Int { PlanCalendar.day() }
    private var weekStart: Int { PlanCalendar.weekStart(focusedDay) }
    private var entry: PlanEntry? { planned(focusedDay) }
    private var recipe: Recipe? { meal(focusedDay) }
    private func planned(_ day: Int) -> PlanEntry? { store.state.plan.first { $0.day == day } }
    private func meal(_ day: Int) -> Recipe? {
        guard let entry = planned(day) else { return nil }
        return store.recipes.first { $0.id == entry.recipe }
    }

    var body: some View {
        GeometryReader { geometry in
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    VStack(spacing: 16) {
                        Picker("Calendar view", selection: $period) {
                            ForEach(PlanPeriod.allCases, id: \.self) { Text($0.rawValue).tag($0) }
                        }.pickerStyle(.segmented)
                        calendarNavigation
                        if period == .month {
                            if typeSize.isAccessibilitySize {
                                ScrollView(.horizontal) { monthGrid(wide: false).frame(minWidth: max(560, geometry.size.width - 64)) }
                                Text("Swipe across the calendar to see all seven days.")
                                    .font(.caption).foregroundStyle(MealTheme.secondaryText)
                            } else {
                                monthGrid(wide: geometry.size.width >= 700)
                            }
                            (typeSize.isAccessibilitySize ? AnyLayout(VStackLayout(alignment: .leading, spacing: 8)) : AnyLayout(HStackLayout(spacing: 6))) {
                                Label {
                                    Text("Meal planned").font(.caption)
                                } icon: {
                                    Circle().fill(MealTheme.action).frame(width: 5, height: 5)
                                }
                                if !typeSize.isAccessibilitySize { Spacer() }
                                Text("\(plannedCount) planned this month").font(.caption)
                            }.foregroundStyle(MealTheme.secondaryText).accessibilityElement(children: .combine)
                        }
                    }
                    if period == .week { weekAgenda }
                    else { selectedMeal }
                }
                .padding(.horizontal, geometry.size.width >= 700 ? 32 : 16)
                .padding(.top, 8).padding(.bottom, 24)
                .frame(maxWidth: 1000)
                .frame(maxWidth: .infinity)
            }
        }
        .background(MealTheme.background).foregroundStyle(MealTheme.text)
        .navigationTitle("Meal plan")
        .toolbar { ToolbarItem(placement: .topBarTrailing) { Button("Today") { focusedDay = today } } }
        .sheet(item: $planner) { DayPlanner(day: $0.id) }
        .navigationDestination(isPresented: $cooking) {
            if let entry, let recipe { CookingView(recipe: recipe, portion: entry.portion) }
        }
    }

    private var plannedCount: Int {
        let start = PlanCalendar.monthStart(focusedDay)
        let end = PlanCalendar.movingMonth(start, by: 1)
        return store.state.plan.filter { $0.day >= start && $0.day < end && meal($0.day) != nil }.count
    }

    private var calendarNavigation: some View {
        (typeSize.isAccessibilitySize ? AnyLayout(VStackLayout(alignment: .leading, spacing: 4)) : AnyLayout(HStackLayout(spacing: 8))) {
            Text(period == .month
                 ? PlanCalendar.date(focusedDay).formatted(.dateTime.month(.wide).year())
                 : PlanCalendar.range(weekStart))
                .font(period == .month ? .title2.weight(.bold) : .headline)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
                .accessibilityIdentifier("plan-period-title")
            if !typeSize.isAccessibilitySize { Spacer(minLength: 0) }
            HStack(spacing: 8) {
                Button { move(-1) } label: {
                    Image(systemName: "chevron.left").frame(minWidth: 44, minHeight: 44)
                }.accessibilityLabel(period == .month ? "Previous month" : "Previous week")
                Button { move(1) } label: {
                    Image(systemName: "chevron.right").frame(minWidth: 44, minHeight: 44)
                }.accessibilityLabel(period == .month ? "Next month" : "Next week")
            }.foregroundStyle(MealTheme.action)
        }.buttonStyle(.borderless)
    }

    private func move(_ offset: Int) {
        focusedDay = period == .month ? PlanCalendar.movingMonth(focusedDay, by: offset) : focusedDay + offset * 7
    }

    private func monthGrid(wide: Bool) -> some View {
        LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 2), count: 7), spacing: 2) {
            ForEach(0..<7) { index in
                Text(PlanCalendar.date(weekStart + index).formatted(.dateTime.weekday(.narrow)))
                    .font(.caption.weight(.semibold)).foregroundStyle(MealTheme.secondaryText)
                    .frame(maxWidth: .infinity).padding(.bottom, 4).accessibilityHidden(true)
            }
            ForEach(PlanCalendar.monthDays(focusedDay), id: \.self) { day in
                calendarDay(day, wide: wide)
            }
        }
    }

    private func calendarDay(_ day: Int, wide: Bool) -> some View {
        let selected = day == focusedDay
        let inMonth = PlanCalendar.monthStart(day) == PlanCalendar.monthStart(focusedDay)
        let recipe = meal(day)
        return Button { focusedDay = day } label: {
            VStack(alignment: wide ? .leading : .center, spacing: 5) {
                HStack(spacing: 6) {
                    Text(PlanCalendar.date(day).formatted(.dateTime.day()))
                        .font(.body.weight(selected || day == today ? .bold : .medium)).monospacedDigit()
                    if wide, recipe != nil {
                        Circle().fill(selected ? MealTheme.charcoal : MealTheme.action).frame(width: 5, height: 5)
                    }
                }
                if wide {
                    Text(recipe?.shortName ?? " ").font(.caption).lineLimit(2)
                        .frame(maxWidth: .infinity, alignment: .leading)
                } else {
                    Circle().fill(recipe == nil ? Color.clear : (selected ? MealTheme.charcoal : MealTheme.action))
                        .frame(width: 5, height: 5)
                }
            }
            .padding(.horizontal, wide ? 8 : 2).padding(.vertical, 8)
            .frame(maxWidth: .infinity, minHeight: wide ? 96 : 48, alignment: wide ? .topLeading : .center)
            .foregroundStyle(selected ? MealTheme.charcoal : (inMonth ? MealTheme.text : MealTheme.secondaryText))
            .background(selected ? MealTheme.peach : (inMonth ? MealTheme.surface : Color.clear), in: RoundedRectangle(cornerRadius: 10))
            .overlay { RoundedRectangle(cornerRadius: 10).strokeBorder(day == today ? MealTheme.action : Color.clear, lineWidth: 1.5) }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("calendar-day-\(day)")
        .accessibilityLabel("\(PlanCalendar.label(day))\(day == today ? ", Today" : ""), \(recipe?.shortName ?? "No meal planned")")
        .accessibilityAddTraits(selected ? [.isSelected] : [])
        .accessibilityHint("Shows this day’s meal and planning actions")
    }

    private var selectedMeal: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(PlanCalendar.label(focusedDay) + (focusedDay == today ? " · Today" : ""))
                .font(.headline).accessibilityAddTraits(.isHeader)
            if let entry, let recipe {
                VStack(alignment: .leading, spacing: 8) {
                    Text(recipe.name).font(.title2.weight(.semibold)).fixedSize(horizontal: false, vertical: true)
                    Text("\(entry.portion.label) · \(recipe.totalMinutes) min")
                        .font(.subheadline).foregroundStyle(MealTheme.secondaryText)
                }
                (typeSize.isAccessibilitySize ? AnyLayout(VStackLayout(alignment: .leading, spacing: 8)) : AnyLayout(HStackLayout())) {
                    Button("Change meal") { planner = DaySelection(id: focusedDay) }.frame(minHeight: 44)
                    if !typeSize.isAccessibilitySize { Spacer() }
                    NavigationLink { RecipeDetail(recipe: recipe, initialPortion: entry.portion) } label: {
                        Label("Recipe", systemImage: "book").frame(minHeight: 44)
                    }
                    Menu {
                        Button("Remove meal", role: .destructive) { remove(focusedDay) }
                    } label: { Image(systemName: "ellipsis").frame(width: 44, height: 44) }
                        .accessibilityLabel("Meal actions")
                }.font(.subheadline)
                Button { cooking = true } label: {
                    Label("Start cooking", systemImage: "play.fill").font(.headline).frame(maxWidth: .infinity, minHeight: 44)
                }.buttonStyle(.borderedProminent).tint(MealTheme.peach).foregroundStyle(MealTheme.charcoal)
            } else {
                Text("No meal planned").font(.title2.weight(.semibold))
                Text("Choose dinner for this day. Ingredients will join your grocery list.")
                    .font(.subheadline).foregroundStyle(MealTheme.secondaryText)
                Button { planner = DaySelection(id: focusedDay) } label: {
                    Label(focusedDay == today ? "Plan tonight" : "Choose a meal", systemImage: "plus")
                        .font(.headline).frame(maxWidth: .infinity, minHeight: 44)
                }.buttonStyle(.borderedProminent).tint(MealTheme.peach).foregroundStyle(MealTheme.charcoal)
            }
        }.padding(16).frame(maxWidth: .infinity, alignment: .leading)
            .background(MealTheme.surface, in: RoundedRectangle(cornerRadius: 20))
    }

    private var weekAgenda: some View {
        VStack(spacing: 0) {
            ForEach(Array(weekStart..<(weekStart + 7)), id: \.self) { day in
                HStack(alignment: .center, spacing: 12) {
                    Button { focusedDay = day; planner = DaySelection(id: day) } label: {
                        HStack(alignment: .top, spacing: 16) {
                            VStack(spacing: 4) {
                                Text(PlanCalendar.date(day).formatted(.dateTime.weekday(.abbreviated))).font(.caption.weight(.semibold))
                                Text(PlanCalendar.date(day).formatted(.dateTime.day())).font(.title2.weight(.semibold)).monospacedDigit()
                            }.frame(minWidth: 48).foregroundStyle(day == today ? MealTheme.action : MealTheme.secondaryText)
                            VStack(alignment: .leading, spacing: 6) {
                                if day == today { Text("Today").font(.caption).foregroundStyle(MealTheme.action) }
                                Text(meal(day)?.shortName ?? "Choose a meal").font(.headline)
                                    .foregroundStyle(meal(day) == nil ? MealTheme.action : MealTheme.text)
                                if let entry = planned(day), meal(day) != nil {
                                    Text(entry.portion.label).font(.subheadline).foregroundStyle(MealTheme.secondaryText)
                                }
                            }.frame(maxWidth: .infinity, alignment: .leading)
                        }.padding(.vertical, 18).contentShape(Rectangle())
                    }.buttonStyle(.plain).accessibilityIdentifier("plan-day-\(day)")
                    if let recipe = meal(day), let entry = planned(day) {
                        NavigationLink { RecipeDetail(recipe: recipe, initialPortion: entry.portion) } label: {
                            Image(systemName: "book").frame(width: 44, height: 44)
                        }.accessibilityLabel("View \(recipe.shortName)")
                    }
                }
                .contextMenu {
                    if meal(day) != nil {
                        Button("Start cooking", systemImage: "play.fill") { focusedDay = day; cooking = true }
                        Button("Remove meal", role: .destructive) { remove(day) }
                    }
                }
                if day < weekStart + 6 { Divider() }
            }
        }
    }

    private func remove(_ day: Int) { store.updateState { $0.plan.removeAll { $0.day == day } } }
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
