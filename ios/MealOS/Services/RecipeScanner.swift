import SwiftUI
import VisionKit
import Vision
import PhotosUI

struct DocumentScanner: UIViewControllerRepresentable {
    var completion: (Result<[UIImage], Error>) -> Void
    @Environment(\.dismiss) private var dismiss
    func makeCoordinator() -> Coordinator { Coordinator(parent: self) }
    func makeUIViewController(context: Context) -> VNDocumentCameraViewController {
        let controller = VNDocumentCameraViewController(); controller.delegate = context.coordinator; return controller
    }
    func updateUIViewController(_ controller: VNDocumentCameraViewController, context: Context) {}
    final class Coordinator: NSObject, VNDocumentCameraViewControllerDelegate {
        let parent: DocumentScanner
        init(parent: DocumentScanner) { self.parent = parent }
        func documentCameraViewControllerDidCancel(_ controller: VNDocumentCameraViewController) { parent.dismiss() }
        func documentCameraViewController(_ controller: VNDocumentCameraViewController, didFailWithError error: Error) { parent.completion(.failure(error)); parent.dismiss() }
        func documentCameraViewController(_ controller: VNDocumentCameraViewController, didFinishWith scan: VNDocumentCameraScan) {
            guard scan.pageCount <= 12 else { parent.completion(.failure(MealOSError("Scan up to 12 pages at a time."))); parent.dismiss(); return }
            parent.completion(.success((0..<scan.pageCount).map { scan.imageOfPage(at: $0) })); parent.dismiss()
        }
    }
}
enum RecipeOCR {
    static func text(from images: [UIImage]) async throws -> String {
        try await Task.detached(priority: .userInitiated) {
            var pages: [String] = []
            for image in images {
                guard let data = image.jpegData(compressionQuality: 0.9) else { continue }
                let request = VNRecognizeTextRequest(); request.recognitionLevel = .accurate; request.usesLanguageCorrection = true
                try VNImageRequestHandler(data: data).perform([request])
                let text = request.results?.compactMap { $0.topCandidates(1).first?.string }.joined(separator: "\n") ?? ""
                if !text.isEmpty { pages.append(text) }
            }
            guard !pages.isEmpty else { throw MealOSError("No readable text was found. Try a clearer, well-lit photo.") }
            return pages.joined(separator: "\n\n")
        }.value
    }
}
