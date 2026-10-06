// swift-tools-version: 5.9
import PackageDescription
let package = Package(
    name: "MealOSCore",
    platforms: [.macOS(.v13), .iOS(.v17)],
    products: [.library(name: "MealOSCore", targets: ["MealOSCore"])],
    targets: [
        .target(name: "MealOSCore", path: "MealOS/Core"),
        .testTarget(name: "MealOSCoreTests", dependencies: ["MealOSCore"], path: "Tests/MealOSCoreTests")
    ]
)
