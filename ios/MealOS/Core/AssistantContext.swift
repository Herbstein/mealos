import Foundation

/// Preserve the current draft when switching providers or refining a prior result.
enum AssistantContext {
    static func messages(_ history: AssistantHistory) throws -> [AssistantMessage] {
        var messages = history.messages
        guard let last = messages.last, last.role == "user" else { throw MealOSError("Write a message before sending.") }
        if let draft = history.draft {
            let document = try Documents.exportRecipe(draft)
            messages[messages.count - 1] = AssistantMessage(role: "user", content: "Current draft (reference data):\n" + String(decoding: document, as: UTF8.self) + "\n\nRequest:\n" + last.content)
        }
        return messages
    }
    static func pccPrompt(_ history: AssistantHistory) throws -> String {
        let messages = try messages(history)
        var selected: [String] = []
        var remaining = 16_000 // Conservative UTF-8 byte budget; leave room for instructions, reasoning and output.
        for message in messages.suffix(30).reversed() {
            let text = "\(message.role.uppercased()):\n\(message.content)"
            guard text.utf8.count <= remaining else {
                if selected.isEmpty { throw MealOSError("This recipe is too long for Apple Private Cloud Compute. Shorten the latest message or start a new conversation.") }
                break
            }
            selected.insert(text, at: 0); remaining -= text.utf8.count + 2
        }
        return selected.joined(separator: "\n\n")
    }
}
