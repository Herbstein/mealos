import SwiftUI

enum AppTab: Hashable { case home, plan, recipes, shop, assistant }

struct RootView: View {
    @State private var selectedTab: AppTab = .home
    @EnvironmentObject private var cloud: CloudService
    var body: some View {
        TabView(selection: $selectedTab) {
            NavigationStack { HomeView(tab: $selectedTab).modifier(SettingsAccess()) }
                .tabItem { Label("Home", systemImage: "house") }.tag(AppTab.home)
            NavigationStack { PlanView().modifier(SettingsAccess()) }
                .tabItem { Label("Plan", systemImage: "calendar") }.tag(AppTab.plan)
            NavigationStack { LibraryView().modifier(SettingsAccess()) }
                .tabItem { Label("Recipes", systemImage: "book.closed") }.tag(AppTab.recipes)
            NavigationStack { GroceryView().modifier(SettingsAccess()) }
                .tabItem { Label("Shop", systemImage: "basket") }.tag(AppTab.shop)
            NavigationStack { AssistantView().modifier(SettingsAccess()) }
                .tabItem { Label("Assistant", systemImage: "text.bubble") }.tag(AppTab.assistant)
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

private struct SettingsAccess: ViewModifier {
    @State private var presented = false
    func body(content: Content) -> some View {
        content.toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button { presented = true } label: {
                    Image(systemName: "gearshape").frame(minWidth: 44, minHeight: 44)
                }.accessibilityLabel("Settings")
            }
        }.sheet(isPresented: $presented) {
            NavigationStack {
                SettingsView().toolbar {
                    ToolbarItem(placement: .confirmationAction) { Button("Done") { presented = false } }
                }
            }
        }
    }
}
