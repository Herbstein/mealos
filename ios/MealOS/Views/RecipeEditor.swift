import SwiftUI

struct RecipeEditor: View {
    @EnvironmentObject private var store: MealStore
    @Environment(\.dismiss) private var dismiss
    @State private var recipe: Recipe
    private let original: Recipe
    var imported: Bool
    @State private var discard = false
    @State private var showErrors = false
    @State private var preview = false
    @State private var saveError: String?
    init(recipe: Recipe, imported: Bool = false) { _recipe = State(initialValue: recipe); original = recipe; self.imported = imported }
    private var dirty: Bool { recipe != original || imported }
    var body: some View {
        NavigationStack {
            Form {
                if imported { Section { Label("Imported draft · review before saving", systemImage: "doc.text.magnifyingglass") } }
                Section("Recipe") {
                    TextField("Recipe name", text: $recipe.name)
                    TextField("Short name", text: $recipe.shortName)
                    TextField("Description", text: $recipe.description, axis: .vertical)
                    Stepper("Base servings: \(recipe.baseServings)", value: $recipe.baseServings, in: 1...100)
                    numberField("Hands-on minutes", value: $recipe.effortMinutes)
                    numberField("Total minutes", value: $recipe.totalMinutes)
                    TextField("Equipment, separated by commas", text: Binding(get: { recipe.equipment.joined(separator: ", ") }, set: { recipe.equipment = $0.components(separatedBy: ",").map { $0.trimmingCharacters(in: .whitespaces) } }))
                    TextField("Tags, separated by commas", text: Binding(get: { recipe.tags.joined(separator: ", ") }, set: { recipe.tags = $0.components(separatedBy: ",").map { $0.trimmingCharacters(in: .whitespaces) } }))
                    TextField("Leftover guidance", text: $recipe.leftoverNote, axis: .vertical)
                }
                Section("Ingredients") {
                    ForEach(recipe.ingredients.indices, id: \.self) { index in
                        VStack(alignment: .leading, spacing: 12) {
                            TextField("Ingredient name", text: $recipe.ingredients[index].name)
                            HStack { TextField("Amount", value: $recipe.ingredients[index].amount, format: .number).keyboardType(.decimalPad).accessibilityLabel("Ingredient amount"); TextField("Unit", text: $recipe.ingredients[index].unit).accessibilityLabel("Ingredient unit") }
                            Picker("Category", selection: $recipe.ingredients[index].category) { ForEach(IngredientCategory.allCases, id: \.self) { Text($0.label).tag($0) } }
                            Toggle("Pantry staple", isOn: $recipe.ingredients[index].pantryStaple)
                        }.padding(.vertical, 8)
                    }.onDelete { recipe.ingredients.remove(atOffsets: $0) }.onMove { recipe.ingredients.move(fromOffsets: $0, toOffset: $1) }
                    Button("Add ingredient", systemImage: "plus") { recipe.ingredients.append(Ingredient()) }
                }
                Section("Cooking steps") {
                    ForEach(recipe.steps.indices, id: \.self) { index in
                        VStack(alignment: .leading, spacing: 12) {
                            Text("Step \(index + 1)").font(.headline)
                            TextField("Step title", text: $recipe.steps[index].title)
                            TextField("Instruction", text: $recipe.steps[index].instruction, axis: .vertical).lineLimit(3...10)
                            TextField("Equipment or work surface", text: $recipe.steps[index].equipment)
                            Toggle("Include timer", isOn: Binding(get: { recipe.steps[index].timerSeconds != nil }, set: { recipe.steps[index].timerSeconds = $0 ? 60 : nil }))
                            if recipe.steps[index].timerSeconds != nil {
                                numberField("Timer seconds", value: Binding(get: { recipe.steps[index].timerSeconds ?? 60 }, set: { recipe.steps[index].timerSeconds = $0 }))
                            }
                            TextField("Tip (optional)", text: Binding(get: { recipe.steps[index].tip ?? "" }, set: { recipe.steps[index].tip = $0.isEmpty ? nil : $0 }), axis: .vertical)
                        }.padding(.vertical, 8)
                    }.onDelete { recipe.steps.remove(atOffsets: $0) }.onMove { recipe.steps.move(fromOffsets: $0, toOffset: $1) }
                    Button("Add cooking step", systemImage: "plus") { recipe.steps.append(RecipeStep()) }
                }
                if showErrors && !recipe.validationErrors.isEmpty {
                    Section("Before saving") { ForEach(recipe.validationErrors, id: \.self) { Text($0).foregroundStyle(.red) } }
                }
                Section { Button("Preview cooking view", systemImage: "play.rectangle") { prepareNames(); if recipe.validationErrors.isEmpty { preview = true } else { showErrors = true } } }
            }.navigationTitle(imported ? "Review recipe" : original.name.isEmpty ? "New recipe" : "Edit recipe")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) { Button("Cancel") { if dirty { discard = true } else { dismiss() } } }
                    ToolbarItem(placement: .primaryAction) { EditButton() }
                    ToolbarItem(placement: .confirmationAction) { Button("Save") { prepareNames(); showErrors = true; if recipe.validationErrors.isEmpty {
                        if store.save(recipe) { dismiss() }
                        else { saveError = store.error ?? "Your recipe could not be saved. Try again."; store.error = nil }
                    } }.fontWeight(.semibold) }
                }
                .alert("Discard this unsaved recipe?", isPresented: $discard) { Button("Discard changes", role: .destructive) { dismiss() }; Button("Keep editing", role: .cancel) {} }
                .navigationDestination(isPresented: $preview) { CookingView(recipe: recipe, portion: .two) }
        }.interactiveDismissDisabled(dirty)
            .alert("Could not save recipe", isPresented: Binding(get: { saveError != nil }, set: { if !$0 { saveError = nil } })) {
                Button("Keep editing", role: .cancel) { saveError = nil }
            } message: { Text(saveError ?? "") }
    }
    private func numberField(_ label: String, value: Binding<Int>) -> some View {
        HStack { Text(label); Spacer(); TextField(label, value: value, format: .number).keyboardType(.numberPad).multilineTextAlignment(.trailing) }
    }
    private func prepareNames() {
        if recipe.shortName.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { recipe.shortName = recipe.name }
        for index in recipe.ingredients.indices {
            // Preserve established ingredient keys so pantry and grocery state remain compatible.
            if !original.ingredients.contains(where: { $0.id == recipe.ingredients[index].id }) || original.ingredients.first(where: { $0.id == recipe.ingredients[index].id })?.name != recipe.ingredients[index].name {
                recipe.ingredients[index].id = Ingredient.key(for: recipe.ingredients[index].name)
            }
        }
        recipe.tags.removeAll { $0.isEmpty }; recipe.equipment.removeAll { $0.isEmpty }
    }
}
