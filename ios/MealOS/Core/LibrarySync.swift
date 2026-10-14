import Foundation

struct LibrarySnapshot: Codable, Equatable {
    var state = UserState()
    var recipes: [Recipe] = []
    func validate() throws {
        _ = try Documents.decodeState(Documents.encode(state))
        _ = try Documents.recipes(Documents.encode(recipes))
        guard recipes.allSatisfy({ $0.validationErrors.isEmpty }) else { throw MealOSError("The synced library contains an invalid recipe. Your local library was retained.") }
    }
}
struct LibraryWrite: Codable, Equatable {
    var operationId = UUID().uuidString
    var baseRevision: Int64
    var library: LibrarySnapshot
}
struct LibraryRemote: Codable {
    var revision: Int64
    var library: LibrarySnapshot?
}
struct LibraryReceipt: Decodable { var revision: Int64; var snapshot: LibraryRemote }

/// The visible library, retry payload and acknowledgement live in one atomic file.
/// Never mutate a pending request: a retry must use exactly its original payload.
struct LibraryJournal: Codable {
    var library = LibrarySnapshot()
    var revision: Int64 = 0
    var dirty = false
    var pending: LibraryWrite?
    var conflict: LibraryRemote?
    var recovered: [LibrarySnapshot] = []
    mutating func edit(_ value: LibrarySnapshot) { library = value; dirty = true }
    mutating func prepare() -> LibraryWrite? {
        guard conflict == nil else { return nil }
        if let pending { return pending }
        guard dirty else { return nil }
        let write = LibraryWrite(baseRevision: revision, library: library)
        pending = write; dirty = false
        return write
    }
    /// Only call after a definitive rejection (HTTP 400/413 or local size check).
    /// Ambiguous network failures retain the original operation for safe replay.
    mutating func reject(_ write: LibraryWrite) {
        guard pending?.operationId == write.operationId else { return }
        pending = nil; dirty = true
    }
    mutating func acknowledge(_ write: LibraryWrite, revision: Int64) {
        guard pending?.operationId == write.operationId else { return }
        self.revision = revision; pending = nil
    }
    mutating func receive(_ remote: LibraryRemote) throws {
        try remote.library?.validate()
        guard remote.revision >= revision else { throw MealOSError("The server returned an older library. Sync paused.") }
        if pending != nil || dirty {
            if remote.revision != revision { conflict = remote }
        } else {
            library = remote.library ?? LibrarySnapshot(); revision = remote.revision
        }
    }
    mutating func resolve(useDevice: Bool) {
        guard let conflict else { return }
        // Both alternatives remain recoverable in the on-device snapshot.
        recovered.append(library)
        if let remote = conflict.library { recovered.append(remote) }
        if !useDevice { library = conflict.library ?? LibrarySnapshot() }
        revision = conflict.revision; pending = nil; dirty = useDevice; self.conflict = nil
    }
}

struct SyncBatchTiming {
    static let debounce: TimeInterval = 8
    static let maximumDelay: TimeInterval = 60
    static func deadline(firstEdit: Date, lastEdit: Date) -> Date {
        min(lastEdit.addingTimeInterval(debounce), firstEdit.addingTimeInterval(maximumDelay))
    }
}

extension LocalSnapshot {
    mutating func activateBackendLibrary(_ owner: String) {
        guard backendOwner != owner else { return }
        if backendLibraries == nil { backendLibraries = [:] }
        let journal = backendLibraries?[owner] ?? LibraryJournal()
        backendLibraries?[owner] = journal; backendOwner = owner
        state = journal.library.state; customRecipes = journal.library.recipes
    }
    mutating func updateBackendJournal(_ mutation: (inout LibraryJournal) throws -> Void) rethrows {
        guard let owner = backendOwner, var journal = backendLibraries?[owner] else { return }
        try mutation(&journal)
        backendLibraries?[owner] = journal
        state = journal.library.state; customRecipes = journal.library.recipes
    }
}
