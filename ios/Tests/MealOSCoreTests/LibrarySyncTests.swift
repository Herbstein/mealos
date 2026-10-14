import XCTest
@testable import MealOSCore

final class LibrarySyncTests: XCTestCase {
    func testAccountSwitchAndRestartPreserveSeparateQueues() throws {
        var snapshot = LocalSnapshot()
        snapshot.activateBackendLibrary("backend:user-a")
        var a = LibrarySnapshot(); a.state.notes["rice"] = "Private A"
        snapshot.updateBackendJournal { $0.edit(a); _ = $0.prepare() }
        let operation = snapshot.backendLibraries?["backend:user-a"]?.pending
        snapshot.activateBackendLibrary("backend:device")
        XCTAssertTrue(snapshot.state.notes.isEmpty)
        snapshot.activateBackendLibrary("backend:user-b")
        XCTAssertTrue(snapshot.state.notes.isEmpty)
        var b = LibrarySnapshot(); b.state.favorites.insert("rice")
        snapshot.updateBackendJournal { $0.edit(b) }
        snapshot = try JSONDecoder().decode(LocalSnapshot.self, from: Documents.encode(snapshot))
        snapshot.activateBackendLibrary("backend:user-a")
        XCTAssertEqual(snapshot.state.notes["rice"], "Private A")
        XCTAssertEqual(snapshot.backendLibraries?["backend:user-a"]?.pending, operation)
        XCTAssertTrue(snapshot.backendLibraries?["backend:user-b"]?.dirty == true)
        snapshot.activateBackendLibrary("different-backend:user-a")
        XCTAssertTrue(snapshot.state.notes.isEmpty)
    }
    func testRejectedBatchCanBeCorrectedAfterRestart() throws {
        var journal = LibraryJournal()
        var oversized = LibrarySnapshot(); oversized.state.notes["rice"] = String(repeating: "x", count: 2_000_001)
        journal.edit(oversized)
        let rejected = try XCTUnwrap(journal.prepare())
        journal.reject(rejected)
        journal = try JSONDecoder().decode(LibraryJournal.self, from: Documents.encode(journal))
        XCTAssertNil(journal.pending)
        XCTAssertEqual(journal.library, oversized)
        var corrected = oversized; corrected.state.notes["rice"] = "Short note"
        journal.edit(corrected)
        let retry = try XCTUnwrap(journal.prepare())
        XCTAssertEqual(retry.library, corrected)
        XCTAssertNotEqual(retry.operationId, rejected.operationId)
        XCTAssertEqual(retry.baseRevision, rejected.baseRevision)
    }
    func testRejectionPreservesEditsMadeDuringUploadAndIgnoresStaleRejection() throws {
        var journal = LibraryJournal()
        var library = LibrarySnapshot(); library.state.notes["rice"] = "Original"
        journal.edit(library)
        let rejected = try XCTUnwrap(journal.prepare())
        library.state.notes["rice"] = "Corrected during upload"; journal.edit(library)
        journal.reject(rejected)
        let retry = try XCTUnwrap(journal.prepare())
        XCTAssertEqual(retry.library, library)
        journal.reject(rejected)
        XCTAssertEqual(journal.pending, retry)
        journal.acknowledge(retry, revision: 1)
        XCTAssertNil(journal.prepare())
    }
    func testBatchDebounceAndMaximumDelay() {
        let first = Date(timeIntervalSince1970: 0)
        XCTAssertEqual(SyncBatchTiming.deadline(firstEdit: first, lastEdit: first).timeIntervalSince1970, 8)
        XCTAssertEqual(SyncBatchTiming.deadline(firstEdit: first, lastEdit: first.addingTimeInterval(20)).timeIntervalSince1970, 28)
        XCTAssertEqual(SyncBatchTiming.deadline(firstEdit: first, lastEdit: first.addingTimeInterval(59)).timeIntervalSince1970, 60)
    }
    func testRestartRetriesIdenticalPayloadAndKeepsEditsDuringFlight() throws {
        var journal = LibraryJournal()
        var library = LibrarySnapshot(); library.state.notes["rice"] = "First"
        journal.edit(library)
        let pending = try XCTUnwrap(journal.prepare())
        library.state.notes["rice"] = "Second"; journal.edit(library)
        var restored = try JSONDecoder().decode(LibraryJournal.self, from: Documents.encode(journal))
        XCTAssertEqual(restored.prepare(), pending)
        XCTAssertEqual(try Documents.encode(restored.pending), try Documents.encode(pending))
        restored.acknowledge(pending, revision: 1)
        let next = try XCTUnwrap(restored.prepare())
        XCTAssertEqual(next.library.state.notes["rice"], "Second")
        XCTAssertEqual(next.baseRevision, 1)
        XCTAssertNotEqual(next.operationId, pending.operationId)
    }
    func testConflictNeverOverwritesLocalEditsAndRetainsBothChoices() throws {
        var journal = LibraryJournal()
        var local = LibrarySnapshot(); local.state.notes["rice"] = "Local"
        var remote = LibrarySnapshot(); remote.state.notes["rice"] = "Remote"
        journal.edit(local); _ = journal.prepare()
        try journal.receive(LibraryRemote(revision: 1, library: remote))
        XCTAssertEqual(journal.library, local)
        XCTAssertNil(journal.prepare())
        journal.resolve(useDevice: true)
        XCTAssertEqual(journal.recovered, [local, remote])
        XCTAssertEqual(journal.prepare()?.baseRevision, 1)
        XCTAssertEqual(journal.pending?.library, local)
    }
    func testUsingCloudClearsPendingAndPreservesDeviceCopy() throws {
        var journal = LibraryJournal(); var local = LibrarySnapshot()
        local.state.favorites.insert("rice"); journal.edit(local)
        try journal.receive(LibraryRemote(revision: 1, library: LibrarySnapshot()))
        journal.resolve(useDevice: false)
        XCTAssertEqual(journal.library, LibrarySnapshot())
        XCTAssertEqual(journal.recovered.first, local)
        XCTAssertNil(journal.prepare())
    }
    func testCleanPullAndDeletionDoNotResurrectOldState() throws {
        var journal = LibraryJournal(); var library = LibrarySnapshot()
        library.state.favorites.insert("rice")
        try journal.receive(LibraryRemote(revision: 1, library: library))
        try journal.receive(LibraryRemote(revision: 2, library: LibrarySnapshot()))
        XCTAssertTrue(journal.library.state.favorites.isEmpty)
        XCTAssertNil(journal.prepare())
        XCTAssertThrowsError(try journal.receive(LibraryRemote(revision: 1, library: library)))
    }
}
