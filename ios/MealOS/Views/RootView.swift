import SwiftUI

enum AppTab: Hashable { case home, plan, recipes, shop, assistant }

struct RootView: View {
    @State private var selectedTab: AppTab = .home
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
