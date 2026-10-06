import SwiftUI

struct RootView: View {
    @EnvironmentObject private var cloud: CloudService
    var body: some View {
        TabView {
            NavigationStack { PlanView() }.tabItem { Label("Plan", systemImage: "calendar") }
            NavigationStack { LibraryView() }.tabItem { Label("Recipes", systemImage: "book.closed") }
            NavigationStack { GroceryView() }.tabItem { Label("Shop", systemImage: "basket") }
            NavigationStack { AssistantView() }.tabItem { Label("Assistant", systemImage: "text.bubble") }
            NavigationStack { SettingsView() }.tabItem { Label("Settings", systemImage: "gearshape") }
        }
        .disabled(cloud.connecting)
        .overlay {
            if cloud.connecting {
                VStack(spacing: 16) {
                    ProgressView("Loading your cloud library…")
                    Text("Your existing cloud library initializes this device.").font(.footnote)
                    Button("Continue offline") { cloud.signOut() }
                }.padding(28).background(.regularMaterial, in: RoundedRectangle(cornerRadius: 20))
            }
        }
        .alert("Cloud sync needs attention", isPresented: Binding(get: { cloud.error != nil }, set: { if !$0 { cloud.error = nil } })) {
            Button("OK", role: .cancel) { cloud.error = nil }
        } message: { Text(cloud.error ?? "") }
    }
}
