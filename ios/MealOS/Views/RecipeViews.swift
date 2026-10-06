import SwiftUI
import UniformTypeIdentifiers

extension UTType {
    static let mealOSRecipe = UTType(exportedAs: "com.mealos.recipe", conformingTo: .json)
}
struct RecipeFile: FileDocument {
    static var readableContentTypes: [UTType] { [.mealOSRecipe, .json] }
    var data: Data
    init(recipe: Recipe) throws { data = try Documents.exportRecipe(recipe) }
    init(configuration: ReadConfiguration) throws { data = configuration.file.regularFileContents ?? Data() }
    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper { FileWrapper(regularFileWithContents: data) }
}
struct LibraryView: View {
    @EnvironmentObject private var store: MealStore
    @State private var search = ""
    @State private var favoritesOnly = false
    @State private var editing: Recipe?
    @State private var importing = false
    @State private var isImport = false
    private var recipes: [Recipe] {
        store.recipes.filter { (!favoritesOnly || store.state.favorites.contains($0.id)) && (search.isEmpty || ($0.name + " " + $0.tags.joined(separator: " ")).localizedCaseInsensitiveContains(search)) }
    }
    var body: some View {
        List {
            Section {
                Toggle("Favorites only", isOn: $favoritesOnly)
            }
            ForEach(recipes) { recipe in
                NavigationLink { RecipeDetail(recipe: recipe) } label: { RecipeRow(recipe: recipe, favorite: store.state.favorites.contains(recipe.id)) }
            }
            if recipes.isEmpty { EmptyMealView(title: "No recipes found", symbol: "book.closed", detail: "Try another search or turn off the favorites filter.") }
        }
        .navigationTitle("Recipes").searchable(text: $search, prompt: "Find a meal or ingredient tag")
        .scrollContentBackground(.hidden).background(MealTheme.background)
        .toolbar {
            Menu {
                Button("Create recipe", systemImage: "square.and.pencil") { isImport = false; editing = Recipe() }
                Button("Import recipe file", systemImage: "square.and.arrow.down") { importing = true }
                Button("Copy ChatGPT recipe prompt", systemImage: "doc.on.doc") {
                    if let url = Bundle.main.url(forResource: "chatgpt_recipe_prompt", withExtension: "txt"), let prompt = try? String(contentsOf: url) { UIPasteboard.general.string = prompt }
                }
            } label: { Image(systemName: "plus").frame(minWidth: 44, minHeight: 44).accessibilityLabel("Add recipe") }
        }
        .sheet(item: $editing) { RecipeEditor(recipe: $0, imported: isImport) }
        .fileImporter(isPresented: $importing, allowedContentTypes: [.mealOSRecipe, .json, .plainText]) { result in
            do {
                let url = try result.get(); let access = url.startAccessingSecurityScopedResource(); defer { if access { url.stopAccessingSecurityScopedResource() } }
                editing = try Documents.importRecipe(Data(contentsOf: url)); isImport = true
            } catch { store.error = error.localizedDescription }
        }
    }
}
struct RecipeDetail: View {
    let recipe: Recipe
    var initialPortion: Portion = .two
    @EnvironmentObject private var store: MealStore
    @State private var portion: Portion = .two
    @State private var editing: Recipe?
    @State private var exporting = false
    @State private var cooking = false
    @State private var file: RecipeFile?
    private var current: Recipe { store.recipes.first { $0.id == recipe.id } ?? recipe }
    var body: some View {
        List {
            Section {
                Text(current.name).font(MealTheme.display()).fixedSize(horizontal: false, vertical: true)
                Text("\(current.totalMinutes) min total · \(current.effortMinutes) min hands-on").foregroundStyle(.secondary)
                if !current.description.isEmpty { Text(current.description) }
                Picker("Portions", selection: $portion) { ForEach(Portion.allCases, id: \.self) { Text($0.label).tag($0) } }
                Button { cooking = true } label: {
                    Label("Start cooking", systemImage: "play.fill").font(.headline).frame(maxWidth: .infinity, minHeight: 44)
                }.buttonStyle(.borderedProminent)
                Menu("Add to meal plan", systemImage: "calendar.badge.plus") {
                    ForEach(Array(["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"].enumerated()), id: \.offset) { day, label in
                        Button(label) { store.plan(day: day, recipe: current, portion: portion) }
                    }
                }.frame(minHeight: 44)
            }
            Section("Ingredients") { IngredientList(recipe: current, portion: portion) }
            Section("Method") {
                ForEach(Array(current.steps.enumerated()), id: \.offset) { index, step in
                    VStack(alignment: .leading, spacing: 8) {
                        Text("\(index + 1). \(step.title)").font(.headline)
                        Text(step.instruction)
                        Text(step.equipment).font(.caption).foregroundStyle(.secondary)
                    }.padding(.vertical, 6)
                }
            }
            if !current.leftoverNote.isEmpty { Section("Lunch tomorrow") { Text(current.leftoverNote) } }
            Section("Make it yours") {
                Picker("Rating", selection: Binding(get: { store.state.ratings[current.id] ?? 0 }, set: { rating in store.updateState { if rating == 0 { $0.ratings.removeValue(forKey: current.id) } else { $0.ratings[current.id] = rating } } })) {
                    Text("Not rated").tag(0)
                    ForEach(1...5, id: \.self) { Text("\($0) / 5").tag($0) }
                }
                TextField("Personal notes", text: Binding(get: { store.state.notes[current.id] ?? "" }, set: { note in store.updateState { $0.notes[current.id] = note } }), axis: .vertical).lineLimit(3...8)
                Button(current.origin == .core ? "Customize a copy" : "Edit recipe") {
                    var copy = current
                    if copy.origin == .core { copy.id = UUID().uuidString; copy.origin = .manual }
                    editing = copy
                }
            }
        }.navigationTitle(current.shortName).navigationBarTitleDisplayMode(.inline)
            .scrollContentBackground(.hidden).background(MealTheme.background)
            .onAppear { portion = initialPortion }
            .toolbar {
                Button { store.toggleFavorite(current.id) } label: { Image(systemName: store.state.favorites.contains(current.id) ? "heart.fill" : "heart").frame(minWidth: 44, minHeight: 44) }.accessibilityLabel("Toggle favorite")
                Button { do { file = try RecipeFile(recipe: current); exporting = true } catch { store.error = error.localizedDescription } } label: { Image(systemName: "square.and.arrow.up").frame(minWidth: 44, minHeight: 44) }.accessibilityLabel("Export recipe")
            }
            .navigationDestination(isPresented: $cooking) { CookingView(recipe: current, portion: portion) }
            .sheet(item: $editing) { RecipeEditor(recipe: $0) }
            .fileExporter(isPresented: $exporting, document: file, contentType: .mealOSRecipe, defaultFilename: current.shortName) { result in
                if case .failure(let error) = result { store.error = error.localizedDescription }
            }
    }
}
struct IngredientList: View {
    let recipe: Recipe
    let portion: Portion
    var body: some View {
        ForEach(Array(recipe.ingredients.enumerated()), id: \.offset) { _, ingredient in
            HStack(alignment: .firstTextBaseline, spacing: 16) {
                Text(ingredient.name).frame(maxWidth: .infinity, alignment: .leading)
                Text(ingredient.quantity(for: recipe, portion: portion)).fontWeight(.semibold).multilineTextAlignment(.trailing)
            }.padding(.vertical, 6)
        }
    }
}
