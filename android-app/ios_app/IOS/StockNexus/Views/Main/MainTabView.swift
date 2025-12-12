import SwiftUI

struct MainTabView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @EnvironmentObject var navigationManager: NavigationManager
    
    var body: some View {
        TabView(selection: $navigationManager.selectedTab) {
            DashboardView()
                .tabItem {
                    Label("Dashboard", systemImage: "house.fill")
                }
                .tag(NavigationManager.Tab.dashboard)
            
            InventoryTabView()
                .tabItem {
                    Label("Inventory", systemImage: "shippingbox.fill")
                }
                .tag(NavigationManager.Tab.inventory)
            
            if authViewModel.currentUser?.role.canManageMoveoutLists == true {
                MoveoutListView()
                    .tabItem {
                        Label("Moveout", systemImage: "list.bullet.rectangle.fill")
                    }
                    .tag(NavigationManager.Tab.moveout)
            }
            
            MoreMenuView()
                .tabItem {
                    Label("More", systemImage: "ellipsis.circle.fill")
                }
                .tag(NavigationManager.Tab.more)
        }
        .accentColor(.stockNexusRed)
    }
}

// MARK: - Inventory Tab View (Conditional based on role)
struct InventoryTabView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    
    var body: some View {
        if authViewModel.currentUser?.role.canRecordStockIn == true {
            RecordStockInView()
        } else {
            StockOutView()
        }
    }
}

struct MainTabView_Previews: PreviewProvider {
    static var previews: some View {
        MainTabView()
            .environmentObject(AuthViewModel())
            .environmentObject(NavigationManager())
    }
}
