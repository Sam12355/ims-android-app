import SwiftUI

@main
struct StockNexusApp: App {
    @StateObject private var authViewModel = AuthViewModel()
    @StateObject private var navigationManager = NavigationManager()
    
    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(authViewModel)
                .environmentObject(navigationManager)
        }
    }
}
