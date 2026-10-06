import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var store: MealStore
    @EnvironmentObject private var cloud: CloudService
    @State private var signingIn = false
    @State private var signOut = false
    var body: some View {
        List {
            Section("Your library") {
                Label(cloud.status, systemImage: cloud.account == nil ? "iphone" : "icloud")
                if let account = cloud.account {
                    Text(account).foregroundStyle(.secondary)
                    Button("Reconnect sync") { Task { await cloud.connect() } }
                    Button("Sign out", role: .destructive) { signOut = true }
                } else if cloud.configured {
                    Text("Sign in with the same Google account as your Android app to load your recipes and meal plan. If cloud data exists, it replaces the local library on first connection.").font(.footnote).foregroundStyle(.secondary)
                    Button(signingIn ? "Signing in…" : "Sign in with Google") {
                        signingIn = true
                        Task { await cloud.signIn(); signingIn = false }
                    }.disabled(signingIn)
                } else {
                    Text("Cloud sign-in is not configured in this build. Your recipes and plans are saved on this device.").foregroundStyle(.secondary)
                }
            }
            Section("Cooking") {
                Text("Timers and your current cooking step stay on the device where you started. Plan on your phone, then open the same recipe on your iPad.")
                Text("Timer alerts use notifications. Silent mode, Focus, and notification settings can affect their sound.").font(.footnote).foregroundStyle(.secondary)
                if let url = URL(string: UIApplication.openSettingsURLString) { Link("Open notification settings", destination: url) }
                if !store.snapshot.timers.isEmpty {
                    NavigationLink("Timers on this device") {
                        ScrollView { VStack(spacing: 20) {
                            ForEach(store.snapshot.timers) { TimerControl(id: $0.id, label: $0.label, duration: Int($0.duration)) }
                        }.padding() }.navigationTitle("Timers")
                    }
                }
            }
            Section("Forkware") {
                Text("Plan simply. Shop once. Cook confidently.").font(MealTheme.display(30))
                Text("Recipes, planning, shopping, and cooking work offline. Recipe Assistant needs an internet connection and an authorized Google account.").font(.footnote).foregroundStyle(.secondary)
                Text("Barlow Condensed is licensed under the SIL Open Font License. The full license is included in the app bundle.").font(.footnote).foregroundStyle(.secondary)
            }
        }.navigationTitle("Settings").scrollContentBackground(.hidden).background(MealTheme.background)
            .confirmationDialog("Sign out of cloud sync?", isPresented: $signOut, titleVisibility: .visible) {
                Button("Sign out", role: .destructive) { cloud.signOut() }
            } message: { Text("Your local library stays on this device. Any uploads still in progress may not have reached the cloud.") }
    }
}
