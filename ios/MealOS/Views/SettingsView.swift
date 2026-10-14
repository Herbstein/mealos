import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var store: MealStore
    @EnvironmentObject private var sync: LibrarySyncService
    @EnvironmentObject private var account: AppleAccountService
    @Environment(\.colorScheme) private var colorScheme
    @State private var appleSignOut = false
    @State private var resolveConflict = false
    var body: some View {
        List {
            Section("Forkware account") {
                if account.user != nil {
                    Label(account.status, systemImage: "person.crop.circle.badge.checkmark")
                    Button("Check account connection") { Task { await account.restore() } }.disabled(account.busy)
                    Button("Sign out of Apple account", role: .destructive) { appleSignOut = true }.disabled(account.busy)
                } else if account.configured {
                    AppleAccountButton(account: account).id(colorScheme).frame(height: 44)
                    if account.busy { ProgressView("Signing in…") }
                } else {
                    Text("Account sign-in is unavailable in this build.").foregroundStyle(.secondary)
                }
                Text("Apple sign-in connects you to Recipe Assistant and library sync across your devices.").font(.footnote).foregroundStyle(.secondary)
            }
            Section("Library sync") {
                Label(sync.status, systemImage: "arrow.triangle.2.circlepath")
                if account.user != nil {
                    Button("Sync now") { Task { await sync.syncNow() } }.disabled(sync.busy)
                    if sync.busy { ProgressView("Syncing…") }
                    if sync.hasConflict {
                        Button("Resolve library conflict") { resolveConflict = true }
                    }
                }
                Text("Edits save on this device immediately and upload together after a short pause. Changes made while signed out stay separate from your Apple library. Timers, cooking progress, and assistant history stay on this device.").font(.footnote).foregroundStyle(.secondary)
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
                Text("Recipes, planning, shopping, and cooking work offline. Claude needs an internet connection and an authorized Apple account. Apple Private Cloud Compute is offered as a fallback on supported devices.").font(.footnote).foregroundStyle(.secondary)
                Text("Barlow Condensed is licensed under the SIL Open Font License. The full license is included in the app bundle.").font(.footnote).foregroundStyle(.secondary)
            }
        }.navigationTitle("Settings").scrollContentBackground(.hidden).background(MealTheme.background)
            .alert("Account needs attention", isPresented: Binding(get: { account.error != nil }, set: { if !$0 { account.error = nil } })) {
                Button("OK", role: .cancel) { account.error = nil }
            } message: { Text(account.error ?? "") }
            .confirmationDialog("Sign out of your Apple account?", isPresented: $appleSignOut, titleVisibility: .visible) {
                Button("Sign out", role: .destructive) { Task { await account.signOut() } }
            } message: { Text("Your library and pending edits stay saved for this Apple account. Sign in again to access them.") }
            .confirmationDialog("Which library should be used?", isPresented: $resolveConflict, titleVisibility: .visible) {
                Button("Use this device’s library", role: .destructive) { sync.resolve(useDevice: true) }
                Button("Use the cloud library", role: .destructive) { sync.resolve(useDevice: false) }
            } message: { Text("Your devices have different edits. This choice replaces the entire library, including recipes, plans, and notes. Both copies are retained in this device’s recovery data.") }

    }
}
