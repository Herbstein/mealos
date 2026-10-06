import SwiftUI

/// An overview of the local meal plan, with direct routes into the working tabs.
struct HomeView: View {
    @EnvironmentObject private var store: MealStore
    @Environment(\.scenePhase) private var scenePhase
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dynamicTypeSize) private var typeSize
    @Binding var tab: AppTab
    @State private var today = PlanCalendar.day()
    @State private var selectedDay = PlanCalendar.day()
    @State private var planning = false

    private var entry: PlanEntry? { store.state.plan.first { $0.day == selectedDay } }
    private var recipe: Recipe? { entry.flatMap { entry in store.recipes.first { $0.id == entry.recipe } } }
    private var nextEntry: PlanEntry? {
        store.state.plan.filter { entry in entry.day > selectedDay && store.recipes.contains(where: { $0.id == entry.recipe }) }
            .sorted { $0.day < $1.day }.first
    }
    private var suggestion: Recipe? {
        let planned = Set(store.state.plan.filter { $0.day >= today && $0.day < today + 7 }.map(\.recipe))
        let candidates = store.recipes.filter { !planned.contains($0.id) }
        return candidates.first { $0.id == "chicken-pesto-pasta" } ?? candidates.first ?? store.recipes.first
    }
    private var shopping: (meals: Int, remaining: Int) {
        let start = PlanCalendar.weekStart(today)
        let plan = store.state.plan.filter { $0.day >= start && $0.day < start + 14 }
        let items = Groceries.aggregate(plan: plan, recipes: store.recipes, pantry: store.state.pantry)
        return (plan.count, items.filter { !store.state.checkedGroceries.contains("\(start):2:\($0.id):\($0.amount)") }.count)
    }
    private func dayTitle(_ day: Int) -> String {
        day == today ? "Tonight" : day == today + 1 ? "Tomorrow" : PlanCalendar.date(day).formatted(.dateTime.weekday(.wide))
    }
    var body: some View {
        GeometryReader { geometry in
            ScrollView {
                VStack(alignment: .leading, spacing: 26) {
                    VStack(alignment: .leading, spacing: 12) {
                        weekStrip(width: min(geometry.size.width, 900) - (geometry.size.width >= 720 ? 64 : 40))
                        mealFeature(wide: geometry.size.width >= 390 && !typeSize.isAccessibilitySize)
                    }
                    upcoming
                    discovery
                    Button { tab = .assistant } label: {
                        actionRow("Create with Assistant", detail: "Start with an ingredient or a craving.", symbol: "text.bubble")
                    }.buttonStyle(.plain).accessibilityIdentifier("home-assistant")
                    Button { tab = .shop } label: {
                        actionRow(shopping.meals == 0 ? "Build your shopping list" : shopping.remaining == 0 ? "Your shopping list is complete" : "\(shopping.remaining) items left to shop",
                                  detail: shopping.meals == 0 ? "Plan a meal to bring ingredients together." : "This week + next · Pantry items excluded",
                                  symbol: "basket")
                    }.buttonStyle(.plain).accessibilityIdentifier("home-shopping")
                }
                .padding(.horizontal, geometry.size.width >= 720 ? 32 : 20)
                .padding(.top, 4).padding(.bottom, 28)
                .frame(maxWidth: 900).frame(maxWidth: .infinity)
            }
        }
        .background(MealTheme.background)
        .navigationTitle("Home")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $planning) { DayPlanner(day: selectedDay) }
        .onChange(of: scenePhase) { _, phase in if phase == .active { refreshDay() } }
        .onReceive(NotificationCenter.default.publisher(for: .NSCalendarDayChanged)) { _ in refreshDay() }
    }
    private func refreshDay() {
        let newDay = PlanCalendar.day()
        if today != newDay { today = newDay; selectedDay = newDay }
    }
    private func weekStrip(width: CGFloat) -> some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 4) {
                ForEach(Array(PlanCalendar.weekStart(today)..<(PlanCalendar.weekStart(today) + 7)), id: \.self) { day in
                    Button { selectedDay = day } label: {
                        VStack(spacing: 5) {
                            Text(PlanCalendar.date(day).formatted(.dateTime.weekday(.abbreviated))).font(.caption)
                            Text(PlanCalendar.date(day).formatted(.dateTime.day())).font(.title3.weight(.semibold))
                            Circle().fill(store.state.plan.contains { $0.day == day } ? MealTheme.peach : .clear).frame(width: 5, height: 5)
                        }
                        .fixedSize(horizontal: typeSize.isAccessibilitySize, vertical: false)
                        .padding(.horizontal, typeSize.isAccessibilitySize ? 12 : 0)
                        .frame(minWidth: 44, minHeight: 60)
                        .frame(maxWidth: typeSize.isAccessibilitySize ? nil : .infinity)
                        .foregroundStyle(selectedDay == day ? (colorScheme == .dark ? MealTheme.charcoal : MealTheme.ivory) : MealTheme.text)
                        .background(selectedDay == day ? (colorScheme == .dark ? MealTheme.peach : MealTheme.charcoal) : .clear, in: RoundedRectangle(cornerRadius: 22))
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(PlanCalendar.label(day) + (day == today ? ", today" : "") + (store.state.plan.contains { $0.day == day } ? ", meal planned" : ", no meal planned"))
                    .accessibilityAddTraits(selectedDay == day ? .isSelected : [])
                    .accessibilityIdentifier("home-day-\(day)")
                }
            }.frame(minWidth: max(330, width))
        }
    }
    @ViewBuilder private func mealFeature(wide: Bool) -> some View {
        if let recipe, let entry {
            let layout = wide && HomeRecipeArtwork.asset(recipe) != nil ? AnyLayout(HStackLayout(alignment: .center, spacing: 18)) : AnyLayout(VStackLayout(alignment: .leading, spacing: 18))
            layout {
                VStack(alignment: .leading, spacing: 16) {
                    Text(dayTitle(selectedDay)).font(.subheadline).foregroundStyle(MealTheme.secondaryText)
                    NavigationLink { RecipeDetail(recipe: recipe, initialPortion: entry.portion) } label: {
                        Text(recipe.shortName).font(MealTheme.display(wide ? 36 : 44)).fixedSize(horizontal: false, vertical: true).foregroundStyle(MealTheme.text)
                    }.buttonStyle(.plain)
                    Text(entry.portion.label).font(.body)
                    Text("\(recipe.totalMinutes) min · \(recipe.effortMinutes) min hands-on").font(.subheadline).foregroundStyle(MealTheme.secondaryText)
                    NavigationLink { CookingView(recipe: recipe, portion: entry.portion) } label: {
                        Label("Start cooking", systemImage: "play.fill").font(.headline).frame(minHeight: 44)
                    }.buttonStyle(.borderedProminent).tint(MealTheme.peach).foregroundStyle(MealTheme.charcoal)
                    Button { tab = .plan } label: { Label("View plan", systemImage: "chevron.right").frame(minHeight: 44) }
                }.frame(maxWidth: .infinity, alignment: .leading)
                if HomeRecipeArtwork.asset(recipe) != nil {
                    HomeRecipeArtwork(recipe: recipe)
                        .frame(maxWidth: .infinity).frame(height: wide ? 250 : 220)
                        .clipShape(RoundedRectangle(cornerRadius: 22))
                }
            }
        } else {
            VStack(alignment: .leading, spacing: 16) {
                Text(dayTitle(selectedDay)).font(.subheadline).foregroundStyle(MealTheme.secondaryText)
                Text(entry == nil ? "What’s for dinner?" : "Choose something new.").font(MealTheme.display(44))
                Text(entry == nil ? "Pick a meal you love, or find a new favorite below." : "This planned recipe is no longer in your library.").foregroundStyle(MealTheme.secondaryText)
                Button(selectedDay == today ? "Plan tonight" : "Choose a meal") { planning = true }
                    .buttonStyle(.borderedProminent).tint(MealTheme.peach).foregroundStyle(MealTheme.charcoal).controlSize(.large)
                Button("View plan") { tab = .plan }.frame(minHeight: 44)
            }.frame(maxWidth: .infinity, alignment: .leading).padding(.vertical, 16)
        }
    }
    @ViewBuilder private var upcoming: some View {
        if let nextEntry, let next = store.recipes.first(where: { $0.id == nextEntry.recipe }) {
            VStack(spacing: 12) {
                Divider()
                NavigationLink { RecipeDetail(recipe: next, initialPortion: nextEntry.portion) } label: {
                    HStack(spacing: 14) {
                        if HomeRecipeArtwork.asset(next) != nil {
                            HomeRecipeArtwork(recipe: next).frame(width: 76, height: 64).clipShape(RoundedRectangle(cornerRadius: 10))
                        }
                        VStack(alignment: .leading, spacing: 5) {
                            Text(nextEntry.day == today + 1 ? "Tomorrow" : PlanCalendar.label(nextEntry.day)).font(.subheadline).foregroundStyle(MealTheme.secondaryText)
                            Text(next.shortName).font(.headline).foregroundStyle(MealTheme.text)
                        }
                        Spacer(minLength: 0)
                        Image(systemName: "chevron.right").foregroundStyle(MealTheme.action)
                    }.frame(minHeight: 64).contentShape(Rectangle())
                }.buttonStyle(.plain)
            }
        }
    }
    private var discovery: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text("What sounds good next?").font(MealTheme.display(32)).fixedSize(horizontal: false, vertical: true)
            if let suggestion {
                NavigationLink { RecipeDetail(recipe: suggestion) } label: {
                    let layout = typeSize.isAccessibilitySize ? AnyLayout(VStackLayout(alignment: .leading, spacing: 16)) : AnyLayout(HStackLayout(alignment: .center, spacing: 16))
                    layout {
                        if HomeRecipeArtwork.asset(suggestion) != nil {
                            HomeRecipeArtwork(recipe: suggestion).frame(width: typeSize.isAccessibilitySize ? 180 : 116, height: 112).clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                        VStack(alignment: .leading, spacing: 8) {
                            Text(suggestion.shortName).font(.title3.weight(.semibold))
                            Text("\(suggestion.totalMinutes) minutes · From your library").font(.subheadline)
                        }.frame(maxWidth: .infinity, alignment: .leading)
                        Image(systemName: "chevron.right").foregroundStyle(MealTheme.peach)
                    }.contentShape(Rectangle())
                }.buttonStyle(.plain).accessibilityIdentifier("home-suggestion")
            } else {
                Text("Your next favorite starts with an idea. Create a recipe with the Assistant or add one to your library.")
            }
            Button { tab = .recipes } label: { Label("Browse recipes", systemImage: "arrow.right").frame(minHeight: 44) }
                .foregroundStyle(MealTheme.peach)
        }
        .foregroundStyle(MealTheme.ivory).padding(22).frame(maxWidth: .infinity, alignment: .leading)
        .background(colorScheme == .dark ? MealTheme.surface : MealTheme.charcoal, in: RoundedRectangle(cornerRadius: 24))
    }
    private func actionRow(_ title: String, detail: String, symbol: String) -> some View {
        HStack(spacing: 16) {
            if !typeSize.isAccessibilitySize {
                Image(systemName: symbol).font(.title2).foregroundStyle(MealTheme.action).frame(width: 32)
            }
            VStack(alignment: .leading, spacing: 6) {
                Text(title).font(.headline).foregroundStyle(MealTheme.text)
                Text(detail).font(.subheadline).foregroundStyle(MealTheme.secondaryText)
            }
            Spacer(minLength: 0)
            if !typeSize.isAccessibilitySize {
                Image(systemName: "chevron.right").foregroundStyle(MealTheme.action)
            }
        }.padding(.vertical, 12).frame(minHeight: 64).contentShape(Rectangle())
    }
}

/// Generated serving illustrations are only used for the matching bundled recipes.
private struct HomeRecipeArtwork: View {
    let recipe: Recipe
    static func asset(_ recipe: Recipe) -> String? {
        guard recipe.origin == .core else { return nil }
        switch recipe.id {
        case "garlic-herb-chicken": return "HomeChicken"
        case "beef-tacos": return "HomeTacos"
        case "chicken-pesto-pasta": return "HomePasta"
        default: return nil
        }
    }
    var body: some View {
        GeometryReader { geometry in
            if let asset = Self.asset(recipe) {
                Image(asset).resizable().scaledToFill().frame(width: geometry.size.width, height: geometry.size.height).clipped()
            }
        }.accessibilityHidden(true)
    }
}
