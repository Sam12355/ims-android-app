import Foundation
import SwiftUI

// MARK: - Navigation Manager
@MainActor
class NavigationManager: ObservableObject {
    @Published var selectedTab: Tab = .dashboard
    @Published var dashboardPath: [String] = []
    @Published var inventoryPath: [String] = []
    @Published var moveoutPath: [String] = []
    @Published var morePath: [String] = []
    
    enum Tab: String, CaseIterable {
        case dashboard = "Dashboard"
        case inventory = "Inventory"
        case moveout = "Moveout"
        case more = "More"
        
        var icon: String {
            switch self {
            case .dashboard: return "house.fill"
            case .inventory: return "shippingbox.fill"
            case .moveout: return "list.bullet.rectangle.fill"
            case .more: return "ellipsis.circle.fill"
            }
        }
    }
    
    func navigateToTab(_ tab: Tab) {
        selectedTab = tab
    }
    
    func resetNavigation() {
        dashboardPath = []
        inventoryPath = []
        moveoutPath = []
        morePath = []
    }
}
