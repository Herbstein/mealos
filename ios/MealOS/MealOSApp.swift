import SwiftUI
import UserNotifications
import GoogleSignIn
import AuthenticationServices

final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil) -> Bool {
        UNUserNotificationCenter.current().delegate = self; return true
    }
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification) async -> UNNotificationPresentationOptions { [.banner, .sound, .list] }
}
@main struct MealOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate
    @StateObject private var store = MealStore()
    @StateObject private var cloud = CloudService()
    @StateObject private var account = AppleAccountService()
    @Environment(\.scenePhase) private var scenePhase
    @State private var imported: Recipe?
    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(store).environmentObject(cloud).environmentObject(account)
                .tint(MealTheme.action)
                .task { cloud.attach(store); await account.restore() }
                .onChange(of: scenePhase) { _, phase in
                    if phase == .active { Task { await account.restore() } }
                }
                .onReceive(NotificationCenter.default.publisher(for: ASAuthorizationAppleIDProvider.credentialRevokedNotification)) { _ in
                    account.credentialRevoked()
                }
                .onOpenURL { url in
                    if GIDSignIn.sharedInstance.handle(url) { return }
                    let access = url.startAccessingSecurityScopedResource(); defer { if access { url.stopAccessingSecurityScopedResource() } }
                    do { imported = try Documents.importRecipe(Data(contentsOf: url)) } catch { store.error = error.localizedDescription }
                }
                .sheet(item: $imported) { RecipeEditor(recipe: $0, imported: true) }
                .alert("MealOS needs attention", isPresented: Binding(get: { store.error != nil }, set: { if !$0 { store.error = nil } })) {
                    Button("OK", role: .cancel) { store.error = nil }
                } message: { Text(store.error ?? "") }
        }
    }
}
