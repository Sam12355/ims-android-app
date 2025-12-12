// swift-tools-version:5.9
import PackageDescription

let package = Package(
    name: "StockNexus",
    platforms: [
        .iOS(.v15),
        .macOS(.v12)
    ],
    products: [
        .library(
            name: "StockNexus",
            targets: ["StockNexus"]
        )
    ],
    dependencies: [
        // Add any external dependencies here
        // Example: .package(url: "https://github.com/Alamofire/Alamofire.git", from: "5.8.0"),
    ],
    targets: [
        .target(
            name: "StockNexus",
            dependencies: [],
            path: "StockNexus"
        )
    ]
)
