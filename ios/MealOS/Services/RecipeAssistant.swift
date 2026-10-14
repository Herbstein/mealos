import Foundation
import FoundationModels

@MainActor enum RecipeAssistant {
    static var pccEnabled: Bool { Bundle.main.object(forInfoDictionaryKey: "ForkwarePCCEnabled") as? String == "YES" }
    static var pccUnavailableReason: String? {
        guard pccEnabled else { return "Apple Private Cloud Compute is disabled pending Apple approval." }
        guard #available(iOS 27.0, *) else { return "Apple Private Cloud Compute requires iOS 27 or later." }
        let model = PrivateCloudComputeLanguageModel()
        guard model.isAvailable else { return "Apple Private Cloud Compute is unavailable on this device. Check Apple Intelligence settings and your connection." }
        guard !model.quotaUsage.isLimitReached else { return "Apple Private Cloud Compute's daily allowance has been reached. Try again after it resets." }
        return nil
    }
    static func send(store: MealStore, account: AppleAccountService, usePCC: Bool = false) async {
        guard !store.sending else { return }
        store.sending = true
        store.canTryPCC = false
        store.assistantNotice = nil
        defer { store.sending = false }
        let history = store.assistant
        do {
            let data: Data
            if usePCC {
                if let reason = pccUnavailableReason { throw MealOSError(reason) }
                guard #available(iOS 27.0, *) else { return }
                data = try await pcc(history)
            } else {
                data = try await account.recipeChat(AssistantProtocol.request(messages: AssistantContext.messages(history), model: history.model))
            }
            try Task.checkCancellation()
            let (reply, draft, sources, questions) = try AssistantProtocol.response(data)
            store.updateAssistant {
                $0.messages.append(AssistantMessage(role: "assistant", content: reply))
                // A clarification without a new draft must not erase prior work.
                if let draft { $0.draft = draft }
                $0.sources = sources; $0.questions = questions
            }
            store.assistantNotice = usePCC ? "Answered with Apple Private Cloud Compute. No web search was used." : "Answered with Claude."
        } catch is CancellationError {
            return
        } catch {
            let eligible = (error as? BackendFailure)?.pccFallbackAllowed == true || (error as? URLError).map { [.timedOut, .cannotConnectToHost, .cannotFindHost, .networkConnectionLost, .badServerResponse].contains($0.code) } == true
            store.canTryPCC = pccEnabled && !usePCC && eligible
            store.assistantNotice = error.localizedDescription
            if usePCC, let reason = pccUnavailableReason { store.assistantNotice = reason }
        }
    }
    @available(iOS 27.0, *)
    private static func pcc(_ history: AssistantHistory) async throws -> Data {
        guard let url = Bundle.main.url(forResource: "pcc-recipe-prompt", withExtension: "txt") else { throw MealOSError("The recipe instructions are missing from this build.") }
        let instructions = try String(contentsOf: url, encoding: .utf8)
        let session = LanguageModelSession(model: PrivateCloudComputeLanguageModel(), instructions: instructions)
        let response = try await session.respond(to: AssistantContext.pccPrompt(history), options: GenerationOptions(maximumResponseTokens: 8192))
        var text = response.content.trimmingCharacters(in: .whitespacesAndNewlines)
        if text.hasPrefix("```json") { text.removeFirst(7) }
        else if text.hasPrefix("```") { text.removeFirst(3) }
        if text.hasSuffix("```") { text.removeLast(3) }
        guard var result = try JSONSerialization.jsonObject(with: Data(text.utf8)) as? [String: Any] else { throw MealOSError("Apple returned an unreadable recipe. Your draft is still saved.") }
        result["sources"] = [] // PCC has no search tool; do not display generated citations.
        return try JSONSerialization.data(withJSONObject: result)
    }
}
