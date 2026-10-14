import SwiftUI
import FirebaseCore
import FirebaseAuth
import FirebaseFirestore
import GoogleSignIn

@MainActor final class CloudService: ObservableObject {
    @Published private(set) var account: String?
    @Published private(set) var status = "On this device"
    @Published private(set) var connecting = false
    @Published var error: String?
    private weak var store: MealStore?
    private var listeners: [ListenerRegistration] = []
    private var authListener: AuthStateDidChangeListenerHandle?
    private var uid: String?
    private var generation = UUID()
    private var ready = false
    private var pending: [String: Int] = [:]
    private var revisions: [String: Int] = [:]
    var configured: Bool { FirebaseApp.app() != nil }
    init() {
        if let path = Bundle.main.path(forResource: "GoogleService-Info", ofType: "plist"), let options = FirebaseOptions(contentsOfFile: path) {
            FirebaseApp.configure(options: options)
        }
    }
    func attach(_ store: MealStore) {
        #if DEBUG
        if ProcessInfo.processInfo.arguments.contains("--uitesting") { return }
        #endif
        guard self.store == nil else { return }
        self.store = store
        store.onCloudChange = { [weak self] recipes in self?.upload(recipes ? "recipes" : "preferences") }
        guard configured else { return }
        authListener = Auth.auth().addStateDidChangeListener { [weak self] _, user in
            Task { @MainActor in
                guard let self else { return }
                self.stop(); self.uid = user?.uid; self.account = user?.email
                if user != nil { await self.connect() } else { store.unbindCloud() }
            }
        }
    }
    func signIn() async {
        guard configured, let clientID = FirebaseApp.app()?.options.clientID else { error = "Google sign-in has not been configured for this app build."; return }
        guard let scene = UIApplication.shared.connectedScenes.compactMap({ $0 as? UIWindowScene }).first(where: { $0.activationState == .foregroundActive }),
              var presenter = scene.windows.first(where: \.isKeyWindow)?.rootViewController else { return }
        while let presented = presenter.presentedViewController { presenter = presented }
        do {
            GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: clientID)
            let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: presenter)
            guard let token = result.user.idToken?.tokenString else { throw MealOSError("Google did not return an identity token. Try again.") }
            let credential = GoogleAuthProvider.credential(withIDToken: token, accessToken: result.user.accessToken.tokenString)
            _ = try await Auth.auth().signIn(with: credential)
        } catch { self.error = error.localizedDescription }
    }
    func signOut() {
        do { try Auth.auth().signOut(); GIDSignIn.sharedInstance.signOut(); stop(); account = nil; uid = nil; store?.unbindCloud() }
        catch { self.error = error.localizedDescription }
    }
    private func stop() {
        generation = UUID(); ready = false; connecting = false
        listeners.forEach { $0.remove() }; listeners.removeAll(); pending.removeAll(); revisions.removeAll()
        status = "On this device"
    }
    func connect() async {
        guard let uid, let store else { return }
        stop(); let session = generation; connecting = true; status = "Connecting…"
        do {
            // Server reads distinguish an empty account from an unavailable network.
            // Validate both documents before replacing either local value.
            let base = Firestore.firestore().collection("users").document(uid).collection("mealos")
            let stateDoc = try await base.document("preferences").getDocument(source: .server)
            let recipeDoc = try await base.document("recipes").getDocument(source: .server)
            guard generation == session else { return }
            let statePayload = try payload(stateDoc)
            let state: UserState? = try statePayload.map { try Documents.decodeState(Data($0.utf8)) }
            let recipes: [Recipe]? = try payload(recipeDoc).map { try Documents.recipes(Data($0.utf8)) }
            guard generation == session else { return }
            let dirty = store.snapshot.cloudOwnerUID == uid ? store.snapshot.dirtyCloudDocuments ?? [] : []
            guard store.applyCloud(state: dirty.contains("preferences") ? nil : state, recipes: dirty.contains("recipes") ? nil : recipes), store.bindCloud(to: uid) else {
                throw MealOSError("Cloud data could not be saved on this device. Sync is paused until local storage is available.")
            }
            ready = true; connecting = false; status = "Synced"
            if !stateDoc.exists || dirty.contains("preferences") { upload("preferences") }
            else if let statePayload, isLegacyPlan(statePayload) { store.updateState { _ in } }
            if !recipeDoc.exists || dirty.contains("recipes") { upload("recipes") }
            for name in ["preferences", "recipes"] {
                let listener = base.document(name).addSnapshotListener(includeMetadataChanges: true) { [weak self] document, error in
                    Task { @MainActor in
                        guard let self, self.generation == session else { return }
                        if let error { self.status = "Sync needs attention"; self.error = error.localizedDescription; return }
                        guard let document else { return }
                        if document.metadata.hasPendingWrites || self.pending[name] != nil || store.snapshot.dirtyCloudDocuments?.contains(name) == true { self.status = "Saving…"; return }
                        do {
                            if let text = try self.payload(document) {
                                let saved: Bool
                                if name == "preferences" {
                                    saved = store.applyCloud(state: try Documents.decodeState(Data(text.utf8)))
                                    if saved && self.isLegacyPlan(text) { store.updateState { _ in } }
                                }
                                else { saved = store.applyCloud(recipes: try Documents.recipes(Data(text.utf8))) }
                                if !saved {
                                    self.stop()
                                    throw MealOSError("Sync paused because this device could not save your cloud data. Free some storage and reconnect in Settings.")
                                }
                            }
                            self.status = document.metadata.isFromCache ? "Offline · saved on this device" : (self.pending.isEmpty ? "Synced" : "Saving…")
                        } catch { self.status = "Sync needs attention"; self.error = error.localizedDescription }
                    }
                }
                listeners.append(listener)
            }
        } catch {
            guard generation == session else { return }
            connecting = false; status = "Not connected · saved on this device"; self.error = error.localizedDescription
        }
    }
    private func isLegacyPlan(_ text: String) -> Bool {
        let object = try? JSONSerialization.jsonObject(with: Data(text.utf8)) as? [String: Any]
        return object?["schemaVersion"] as? Int == 1
    }
    private func payload(_ snapshot: DocumentSnapshot) throws -> String? {
        guard snapshot.exists else { return nil }
        guard let data = snapshot.data(), data["schemaVersion"] as? Int == 1, let text = data["payload"] as? String else {
            throw MealOSError("This cloud document is not a supported MealOS format. No local data was replaced.")
        }
        return text
    }
    private func upload(_ name: String) {
        guard ready, let uid, let store else { return }
        do {
            let data = try name == "preferences" ? Documents.encode(store.state) : Documents.encode(store.snapshot.customRecipes)
            guard data.count < 900_000, let text = String(data: data, encoding: .utf8) else { throw MealOSError("The library is too large to sync. Your changes are saved on this device.") }
            let revision = (revisions[name] ?? 0) + 1; revisions[name] = revision; pending[name] = revision
            let session = generation; status = "Saving…"
            Firestore.firestore().collection("users").document(uid).collection("mealos").document(name).setData([
                "schemaVersion": 1, "payload": text, "updatedAt": FieldValue.serverTimestamp()
            ]) { [weak self] error in
                Task { @MainActor in
                    guard let self, self.generation == session, self.pending[name] == revision else { return }
                    self.pending.removeValue(forKey: name)
                    if let error { self.status = "Sync needs attention"; self.error = error.localizedDescription }
                    else { store.acknowledgeCloud(name); self.status = self.pending.isEmpty ? "Synced" : "Saving…" }
                }
            }
        } catch { self.error = error.localizedDescription; status = "Sync needs attention" }
    }
}
