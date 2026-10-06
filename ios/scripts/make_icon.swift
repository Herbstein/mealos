// Render the Icon Composer SVG into the opaque legacy iOS app-icon fallback.
// Usage: swift ios/scripts/make_icon.swift <output.png>
import AppKit
import Foundation

let script = URL(fileURLWithPath: #filePath).standardizedFileURL
let resources = script.deletingLastPathComponent().deletingLastPathComponent()
    .appendingPathComponent("MealOS/Resources")
let output = CommandLine.arguments[1]
let context = CGContext(data: nil, width: 1024, height: 1024,
    bitsPerComponent: 8, bytesPerRow: 4096,
    space: CGColorSpace(name: CGColorSpace.sRGB)!,
    bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue)!
context.setFillColor(CGColor(srgbRed: 8/255, green: 50/255, blue: 36/255, alpha: 1))
context.fill(CGRect(x: 0, y: 0, width: 1024, height: 1024))
context.translateBy(x: 0, y: 1024)
context.scaleBy(x: 1, y: -1)

// The source uses only absolute M, L, C, H, V, and Z commands.
final class LogoRenderer: NSObject, XMLParserDelegate {
    func parser(_ parser: XMLParser, didStartElement name: String,
                namespaceURI: String?, qualifiedName: String?, attributes: [String: String]) {
        guard name == "path", let data = attributes["d"], let fill = attributes["fill"] else { return }
        let pattern = try! NSRegularExpression(pattern: "[A-Za-z]|[-+]?[0-9]*\\.?[0-9]+")
        let tokens = pattern.matches(in: data, range: NSRange(data.startIndex..., in: data))
            .map { String(data[Range($0.range, in: data)!]) }
        var i = 0
        func number() -> CGFloat { defer { i += 1 }; return CGFloat(Double(tokens[i])!) }
        let path = CGMutablePath()
        while i < tokens.count {
            let command = tokens[i]; i += 1
            switch command {
            case "M": path.move(to: CGPoint(x: number(), y: number()))
            case "L": path.addLine(to: CGPoint(x: number(), y: number()))
            case "C":
                let c1 = CGPoint(x: number(), y: number())
                let c2 = CGPoint(x: number(), y: number())
                path.addCurve(to: CGPoint(x: number(), y: number()), control1: c1, control2: c2)
            case "H": path.addLine(to: CGPoint(x: number(), y: path.currentPoint.y))
            case "V": path.addLine(to: CGPoint(x: path.currentPoint.x, y: number()))
            case "Z": path.closeSubpath()
            default: fatalError("Unsupported SVG command: \(command)")
            }
        }
        let rgb = UInt32(fill.dropFirst(), radix: 16)!
        context.setFillColor(CGColor(srgbRed: CGFloat((rgb >> 16) & 255)/255,
            green: CGFloat((rgb >> 8) & 255)/255, blue: CGFloat(rgb & 255)/255, alpha: 1))
        context.addPath(path)
        context.fillPath()
    }
}
let renderer = LogoRenderer()
let parser = XMLParser(contentsOf: resources.appendingPathComponent("AppIcon.icon/Assets/Forkware-FW.svg"))!
parser.delegate = renderer
precondition(parser.parse(), "Cannot parse Forkware vector source")
let bitmap = NSBitmapImageRep(cgImage: context.makeImage()!)
try bitmap.representation(using: .png, properties: [:])!.write(to: URL(fileURLWithPath: output))
