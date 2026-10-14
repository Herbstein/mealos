import SwiftUI

@MainActor final class LibrarySyncService: ObservableObject {
    @Published private(set) var status = "Sign in with Apple to sync your library"
    @Published private(set) var busy = false
    @Published private(set) var hasConflict = false
    private weak var store: MealStore?
    private weak var account: AppleAccountService?
    private var userID: String?
    private var owner: String?
    private var generation = UUID()
    private var firstEdit: Date?
    private var scheduled: Task<Void, Never>?
    private var backgroundWork: Task<Void, Never>?
    private var backgroundID: UIBackgroundTaskIdentifier = .invalid
    private var foreground = true
    private var retryDelay: TimeInterval = 8
    private var refreshRequested = false

    func attach(_ store: MealStore, account: AppleAccountService) {
        guard self.store == nil else { return }
        self.store = store; self.account = account
        store.onLibraryChange = { [weak self] in self?.edited() }
    }
    func accountChanged() {
        guard let store, let account else { return }
        let uid = account.user?.id
        let key = account.syncNamespace + ":" + (uid ?? "device")
        guard owner != key else { return }
        scheduled?.cancel(); scheduled = nil; firstEdit = nil
        generation = UUID(); busy = false; retryDelay = 8; refreshRequested = false
        userID = nil; owner = nil
        guard store.activateLibrary(key) else { status = "Sync paused: local storage unavailable"; return }
        owner = key; userID = uid
        hasConflict = store.libraryJournal?.conflict != nil
        status = uid == nil ? "Sign in with Apple to sync your library" : "Ready to sync"
        if uid != nil { Task { await syncNow() } }
    }
    private func edited() {
        guard userID != nil else { return }
        hasConflict = store?.libraryJournal?.conflict != nil
        guard !hasConflict else { status = "Resolve the library conflict to continue syncing"; return }
        status = "Changes saved on this device"
        let now = Date(); if firstEdit == nil { firstEdit = now }
        schedule(after: max(0, SyncBatchTiming.deadline(firstEdit: firstEdit!, lastEdit: now).timeIntervalSinceNow))
    }
    private func schedule(after delay: TimeInterval) {
        guard foreground, userID != nil, !hasConflict else { return }
        scheduled?.cancel()
        scheduled = Task { [weak self] in
            do { try await Task.sleep(nanoseconds: UInt64(max(0, delay) * 1_000_000_000)) }
            catch { return }
            guard let self else { return }
            self.scheduled = nil
            await self.syncNow()
        }
    }
    func becameActive() {
        foreground = true
        accountChanged()
        Task { await syncNow() }
    }
    func enteredBackground() {
        foreground = false; scheduled?.cancel(); scheduled = nil
        guard userID != nil, store?.libraryJournal?.dirty == true || store?.libraryJournal?.pending != nil else { return }
        guard backgroundID == .invalid else { return }
        backgroundID = UIApplication.shared.beginBackgroundTask(withName: "Save library") { [weak self] in
            Task { @MainActor in self?.backgroundWork?.cancel(); self?.endBackground() }
        }
        backgroundWork = Task { [weak self] in
            guard let self else { return }
            while self.busy && !Task.isCancelled {
                do { try await Task.sleep(nanoseconds: 100_000_000) } catch { break }
            }
            if !Task.isCancelled { await self.syncNow() }
            self.endBackground()
        }
    }
    private func endBackground() {
        if backgroundID != .invalid { UIApplication.shared.endBackgroundTask(backgroundID); backgroundID = .invalid }
        backgroundWork = nil
    }
    func syncNow() async {
        guard let uid = userID, let account, let store, owner == store.snapshot.backendOwner else { return }
        if busy { refreshRequested = true; return }
        guard store.libraryJournal?.conflict == nil else { hasConflict = true; status = "Another device changed your library. Choose which copy to use."; return }
        busy = true; scheduled?.cancel(); scheduled = nil; firstEdit = nil
        let session = generation
        defer {
            if generation == session {
                busy = false
                if refreshRequested { refreshRequested = false; schedule(after: 0) }
            }
        }
        var write: LibraryWrite?
        do {
            status = "Syncing…"
            guard try store.updateLibraryJournal({ write = $0.prepare() }) else { throw MealOSError("Could not save the pending upload on this device.") }
            let remote: LibraryRemote
            if let write {
                let data = try await account.library(expectedUser: uid, write: write)
                guard generation == session else { return }
                let receipt = try JSONDecoder().decode(LibraryReceipt.self, from: data)
                try receipt.snapshot.library?.validate()
                guard receipt.revision == write.baseRevision + 1 else { throw MealOSError("The server returned an unexpected sync receipt.") }
                guard try store.updateLibraryJournal({ $0.acknowledge(write, revision: receipt.revision) }) else { throw MealOSError("Could not save the sync receipt. The upload will be retried safely.") }
                remote = receipt.snapshot
            } else {
                let data = try await account.library(expectedUser: uid)
                guard generation == session else { return }
                remote = try JSONDecoder().decode(LibraryRemote.self, from: data)
            }
            guard try store.updateLibraryJournal({ try $0.receive(remote) }) else { throw MealOSError("Could not save the downloaded library.") }
            hasConflict = store.libraryJournal?.conflict != nil
            retryDelay = 8
            status = hasConflict ? "Another device changed your library. Choose which copy to use." : "Synced"
            if store.libraryJournal?.dirty == true { edited() }
        } catch {
            guard generation == session else { return }
            if let write, let failure = error as? BackendFailure, [400, 413].contains(failure.status) {
                // The server did not commit this batch (or it was never sent).
                // Release only this payload; preserve the latest local edits.
                do {
                    guard try store.updateLibraryJournal({ $0.reject(write) }) else { throw MealOSError("Could not save upload recovery. Your edits remain on this device.") }
                    status = failure.status == 413 ? failure.localizedDescription : "The library was rejected. Correct your edits and sync again. Your changes remain on this device."
                } catch { status = error.localizedDescription }
            } else if (error as? BackendFailure)?.status == 409 {
                do {
                    let data = try await account.library(expectedUser: uid)
                    guard generation == session else { return }
                    let remote = try JSONDecoder().decode(LibraryRemote.self, from: data)
                    guard try store.updateLibraryJournal({ try $0.receive(remote) }) else { throw MealOSError("Could not save the conflict. Your local edits are retained.") }
                    hasConflict = store.libraryJournal?.conflict != nil
                    status = "Another device changed your library. Choose which copy to use."
                } catch {
                    guard generation == session else { return }
                    status = error.localizedDescription; schedule(after: retryDelay)
                    retryDelay = min(300, retryDelay * 2)
                }
            } else {
                status = error.localizedDescription
                let statusCode = (error as? BackendFailure)?.status
                if statusCode == nil || statusCode == 408 || statusCode == 429 || (statusCode ?? 0) >= 500 {
                    schedule(after: retryDelay); retryDelay = min(300, retryDelay * 2)
                }
            }
        }
    }
    func resolve(useDevice: Bool) {
        guard !busy, let store else { return }
        do {
            guard try store.updateLibraryJournal({ $0.resolve(useDevice: useDevice) }) else { throw MealOSError("Could not save your conflict choice.") }
            hasConflict = false
            Task { await syncNow() }
        } catch { status = error.localizedDescription }
    }
}
