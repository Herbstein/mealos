import SwiftUI
import PhotosUI
import VisionKit

struct AssistantView: View {
    @EnvironmentObject private var store: MealStore
    @EnvironmentObject private var account: AppleAccountService
    @AppStorage("assistant-composer") private var composer = ""
    @State private var scan = false
    @State private var scanning = false
    @State private var photo: PhotosPickerItem?
    @State private var draft: Recipe?
    @State private var clear = false
    @FocusState private var composerFocused: Bool
    @ScaledMetric(relativeTo: .body) private var composerLineHeight = 24

    var body: some View {
        GeometryReader { geometry in
            VStack(spacing: 0) {
                ScrollViewReader { proxy in
                    ScrollView {
                        LazyVStack(alignment: .leading, spacing: 24) {
                            if store.assistant.messages.isEmpty {
                                VStack(alignment: .leading, spacing: 16) {
                                    Text("Make it a Forkware recipe.").font(MealTheme.display(44))
                                    Text("Describe a meal, paste a recipe, or scan a printed page. You’ll review the draft before saving it.").font(.title3)
                                    Text("Scans are read on this device. Only text is sent when you tap Send.").font(.footnote).foregroundStyle(.secondary)
                                }.padding(.vertical, 20)
                            }
                            ForEach(store.assistant.messages) { message in
                                VStack(alignment: .leading, spacing: 10) {
                                    Text(message.role == "user" ? "You" : "Recipe Assistant").font(.headline)
                                    Text(message.content).textSelection(.enabled)
                                }.padding(20).frame(maxWidth: .infinity, alignment: .leading)
                                    .foregroundStyle(message.role == "user" ? MealTheme.ivory : MealTheme.text)
                                    .background(message.role == "user" ? MealTheme.charcoal : MealTheme.surface, in: RoundedRectangle(cornerRadius: 20))
                                    .id(message.id)
                            }
                            if !store.assistant.sources.isEmpty {
                                VStack(alignment: .leading, spacing: 12) {
                                    Text("Sources").font(.headline)
                                    ForEach(Array(store.assistant.sources.enumerated()), id: \.offset) { _, source in
                                        if let url = URL(string: source.url), url.scheme == "https" { Link(source.title, destination: url).frame(minHeight: 44, alignment: .leading) }
                                    }
                                }
                            }
                            ForEach(store.assistant.questions, id: \.self) { question in Text(question).font(.headline) }
                            if let recipe = store.assistant.draft {
                                Button { draft = recipe } label: { Label("Review \(recipe.shortName)", systemImage: "square.and.pencil").font(.headline).frame(maxWidth: .infinity, minHeight: 48) }.buttonStyle(.borderedProminent).tint(MealTheme.peach).foregroundStyle(MealTheme.charcoal)
                            }
                            if let notice = store.assistantNotice {
                                Text(notice).font(.footnote).foregroundStyle(.secondary).accessibilityIdentifier("assistant-status")
                            }
                            if store.canTryPCC, !store.sending {
                                if let reason = RecipeAssistant.pccUnavailableReason {
                                    Text(reason).font(.footnote).foregroundStyle(.secondary)
                                } else {
                                    Button("Continue with Apple Private Cloud Compute") {
                                        Task { await RecipeAssistant.send(store: store, account: account, usePCC: true) }
                                    }.frame(minHeight: 44).accessibilityIdentifier("assistant-pcc-fallback")
                                    Text("Sends recent conversation and the current draft to Apple. Web search is unavailable.").font(.footnote).foregroundStyle(.secondary)
                                }
                            }
                            if store.sending { ProgressView("Working on your recipe…").frame(maxWidth: .infinity, minHeight: 44) }
                            if !store.sending, store.assistant.messages.last?.role == "user" {
                                Button("Retry last request") { Task { await RecipeAssistant.send(store: store, account: account) } }.frame(minHeight: 44)
                            }
                            Color.clear.frame(height: 1).id("bottom")
                        }.padding(20).frame(maxWidth: 800).frame(maxWidth: .infinity)
                    }
                        .scrollDismissesKeyboard(.interactively)
                        .accessibilityIdentifier("assistant-conversation")
                        .onChange(of: store.assistant.messages.count) { _, _ in proxy.scrollTo("bottom", anchor: .bottom) }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .clipped()

                composeBar(availableHeight: geometry.size.height)
            }
            .frame(width: geometry.size.width, height: geometry.size.height, alignment: .top)
        }
        .background(MealTheme.background)
        .navigationTitle("Recipe Assistant")
        .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                Menu {
                    Picker("Model", selection: Binding(get: { store.assistant.model }, set: { model in store.updateAssistant { $0.model = model } })) {
                        Text("Sonnet").tag("sonnet"); Text("Opus").tag("opus"); Text("Haiku").tag("claude-haiku-4-5")
                    }
                    .disabled(store.sending)
                    Button("New conversation", systemImage: "square.and.pencil") { clear = true }.disabled(store.sending)
                } label: { Image(systemName: "ellipsis.circle").frame(minWidth: 44, minHeight: 44).accessibilityLabel("Assistant options") }
            }
            .confirmationDialog("Clear this conversation and draft?", isPresented: $clear, titleVisibility: .visible) {
                Button("Clear conversation", role: .destructive) { store.updateAssistant { let model = $0.model; $0 = AssistantHistory(); $0.model = model }; composer = ""; store.assistantNotice = nil; store.canTryPCC = false }
            }
            .sheet(item: $draft) { RecipeEditor(recipe: $0, imported: true) }
            .sheet(isPresented: $scan) { DocumentScanner { result in Task { await readScan(result) } } }
            .onChange(of: photo) { _, item in
                guard let item else { return }
                Task {
                    do {
                        guard let data = try await item.loadTransferable(type: Data.self), let image = UIImage(data: data) else { throw MealOSError("This photo could not be opened.") }
                        await readScan(.success([image]))
                    } catch { store.error = error.localizedDescription }
                    photo = nil
                }
            }
    }
    private func composeBar(availableHeight: CGFloat) -> some View {
        // The keyboard reduces the available safe area. Keep the editor bounded
        // so the conversation can shrink instead of displacing the whole screen.
        let maximumLines = max(1, min(6, Int(availableHeight * 0.25 / composerLineHeight)))
        return VStack(spacing: 12) {
            if scanning { ProgressView("Reading the recipe on this device…") }
            TextField("Describe a meal or paste a recipe", text: $composer, axis: .vertical).lineLimit(1...maximumLines)
                .focused($composerFocused)
                .accessibilityIdentifier("assistant-composer-field")
                .padding(12).background(Color(uiColor: .secondarySystemBackground), in: RoundedRectangle(cornerRadius: 12))
            HStack(spacing: 12) {
                if VNDocumentCameraViewController.isSupported {
                    Button { composerFocused = false; scan = true } label: { Image(systemName: "doc.viewfinder").frame(minWidth: 44, minHeight: 44) }.accessibilityLabel("Scan printed recipe").disabled(scanning)
                }
                PhotosPicker(selection: $photo, matching: .images) { Image(systemName: "photo").frame(minWidth: 44, minHeight: 44) }.accessibilityLabel("Read recipe from photo").disabled(scanning)
                Spacer(minLength: 0)
                if composerFocused {
                    Button { composerFocused = false } label: {
                        Image(systemName: "keyboard.chevron.compact.down")
                            .frame(minWidth: 44, minHeight: 44)
                    }
                    .accessibilityLabel("Hide keyboard")
                    .accessibilityIdentifier("assistant-hide-keyboard")
                }
                Button {
                    composerFocused = false
                    let text = composer.trimmingCharacters(in: .whitespacesAndNewlines)
                    store.updateAssistant { $0.messages.append(AssistantMessage(role: "user", content: text)) }; composer = ""
                    Task { await RecipeAssistant.send(store: store, account: account) }
                } label: { Label("Send", systemImage: "arrow.up").frame(minHeight: 44) }.buttonStyle(.borderedProminent).tint(MealTheme.peach).foregroundStyle(MealTheme.charcoal)
                    .disabled(composer.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || store.sending || scanning)
            }
        }.padding(.horizontal, 20).padding(.vertical, 12).frame(maxWidth: 840).frame(maxWidth: .infinity).background(.regularMaterial)
    }
    private func readScan(_ result: Result<[UIImage], Error>) async {
        scanning = true; defer { scanning = false }
        do { let images = try result.get(); let text = try await RecipeOCR.text(from: images); composer += (composer.isEmpty ? "" : "\n\n") + text }
        catch { store.error = error.localizedDescription }
    }
}
