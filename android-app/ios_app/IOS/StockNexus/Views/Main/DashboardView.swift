import SwiftUI

struct DashboardView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @StateObject private var viewModel = DashboardViewModel()
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 20) {
                    // Stats Cards
                    statsCardsSection
                    
                    // Weather Widget
                    weatherWidget
                    
                    // Calendar Events
                    calendarEventsSection
                    
                    // Moveout Lists (for Manager/Assistant Manager)
                    if authViewModel.currentUser?.role.canManageMoveoutLists == true {
                        moveoutListsSection
                    }
                    
                    // Quick Actions
                    quickActionsSection
                }
                .padding()
            }
            .navigationTitle("Dashboard")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    NavigationLink(destination: DashboardNotificationsView()) {
                        Image(systemName: "bell.fill")
                            .foregroundColor(.stockNexusRed)
                    }
                }
            }
            .onAppear {
                Task {
                    await viewModel.loadDashboard(branchId: authViewModel.currentUser?.branchId ?? "")
                }
            }
            .sheet(isPresented: $viewModel.showStatsDetail) {
                if let category = viewModel.selectedStatsCategory {
                    StatsDetailView(category: category, items: viewModel.getItemsForCategory(category))
                }
            }
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
    
    // MARK: - Stats Cards Section
    private var statsCardsSection: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 16) {
                StatsCard(
                    title: "Total Items",
                    value: "\(viewModel.totalItems)",
                    icon: "shippingbox",
                    color: .blue
                ) {
                    viewModel.selectedStatsCategory = .total
                    viewModel.showStatsDetail = true
                }
                
                StatsCard(
                    title: "Low Stock",
                    value: "\(viewModel.lowStockItems)",
                    icon: "exclamationmark.triangle",
                    color: .orange
                ) {
                    viewModel.selectedStatsCategory = .low
                    viewModel.showStatsDetail = true
                }
                
                StatsCard(
                    title: "Critical",
                    value: "\(viewModel.criticalStockItems)",
                    icon: "xmark.octagon",
                    color: .red
                ) {
                    viewModel.selectedStatsCategory = .critical
                    viewModel.showStatsDetail = true
                }
            }
            .padding(.horizontal, 4)
        }
    }
    
    // MARK: - Weather Widget
    private var weatherWidget: some View {
        HStack {
            if let weather = viewModel.weather {
                Image(systemName: weather.icon)
                    .font(.system(size: 40))
                    .foregroundColor(.blue)
                
                VStack(alignment: .leading) {
                    Text(weather.temperatureString)
                        .font(.title)
                        .fontWeight(.bold)
                    
                    Text(weather.condition)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                VStack(alignment: .trailing) {
                    Text(weather.cityName)
                        .font(.headline)
                    
                    Text(Date().formatted(as: "EEEE, MMM d"))
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding()
        .background(Color.secondaryBackground)
        .cornerRadius(12)
    }
    
    // MARK: - Calendar Events Section
    private var calendarEventsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Calendar Events")
                    .font(.headline)
                
                Spacer()
                
                if authViewModel.currentUser?.role.canAddCalendarEvents == true {
                    Button {
                        // Add event
                    } label: {
                        Image(systemName: "plus.circle.fill")
                            .foregroundColor(.stockNexusRed)
                    }
                }
            }
            
            if viewModel.todayEvents.isEmpty && viewModel.upcomingEvents.isEmpty {
                Text("No upcoming events")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding()
            } else {
                // Today's Events
                if !viewModel.todayEvents.isEmpty {
                    Text("Today")
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundColor(.stockNexusRed)
                    
                    ForEach(viewModel.todayEvents) { event in
                        CalendarEventCard(event: event)
                    }
                }
                
                // Upcoming Events
                if !viewModel.upcomingEvents.prefix(3).isEmpty {
                    Text("Upcoming")
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundColor(.secondary)
                        .padding(.top, 8)
                    
                    ForEach(viewModel.upcomingEvents.prefix(3)) { event in
                        CalendarEventCard(event: event)
                    }
                }
            }
        }
        .padding()
        .background(Color.secondaryBackground)
        .cornerRadius(12)
    }
    
    // MARK: - Moveout Lists Section
    private var moveoutListsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Active Moveout Lists")
                    .font(.headline)
                
                Spacer()
                
                NavigationLink(destination: MoveoutListView()) {
                    Text("View All")
                        .font(.subheadline)
                        .foregroundColor(.stockNexusRed)
                }
            }
            
            if viewModel.activeMoveoutLists.isEmpty {
                Text("No active moveout lists")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity, alignment: .center)
                    .padding()
            } else {
                ForEach(viewModel.activeMoveoutLists.prefix(2)) { list in
                    DashboardMoveoutListCard(list: list)
                }
            }
        }
        .padding()
        .background(Color.secondaryBackground)
        .cornerRadius(12)
    }
    
    // MARK: - Quick Actions Section
    private var quickActionsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Quick Actions")
                .font(.headline)
            
            LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                QuickActionButton(
                    title: "Stock Out",
                    icon: "minus.circle.fill",
                    color: .stockNexusRed
                ) {
                    // Navigate to stock out
                }
                
                if authViewModel.currentUser?.role.canManageItems == true {
                    QuickActionButton(
                        title: "Stock In",
                        icon: "plus.circle.fill",
                        color: .green
                    ) {
                        // Navigate to stock in
                    }
                }
                
                QuickActionButton(
                    title: "Scan Barcode",
                    icon: "barcode.viewfinder",
                    color: .blue
                ) {
                    // Open barcode scanner
                }
                
                QuickActionButton(
                    title: "Settings",
                    icon: "gear",
                    color: .gray
                ) {
                    // Navigate to settings
                }
            }
        }
        .padding()
        .background(Color.secondaryBackground)
        .cornerRadius(12)
    }
}

// MARK: - Dashboard Notifications View (renamed to avoid conflict)
struct DashboardNotificationsView: View {
    @State private var notifications: [AppNotification] = AppNotification.samples
    
    var body: some View {
        List {
            ForEach(notifications) { notification in
                DashboardNotificationRow(notification: notification)
            }
        }
        .navigationTitle("Notifications")
        .navigationBarTitleDisplayMode(.inline)
    }
}

// MARK: - Dashboard Notification Row (renamed to avoid conflict)
struct DashboardNotificationRow: View {
    let notification: AppNotification
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: notification.type.icon)
                .font(.title3)
                .foregroundColor(Color(notification.type.colorName))
                .frame(width: 32)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(notification.title)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Text(notification.message)
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                Text(notification.createdAt.timeAgo)
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            if !notification.isRead {
                Circle()
                    .fill(Color.stockNexusRed)
                    .frame(width: 8, height: 8)
            }
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Stats Card
struct StatsCard: View {
    let title: String
    let value: String
    let icon: String
    let color: Color
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Image(systemName: icon)
                        .font(.title2)
                        .foregroundColor(color)
                    
                    Spacer()
                }
                
                Text(value)
                    .font(.title)
                    .fontWeight(.bold)
                    .foregroundColor(.primary)
                
                Text(title)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding()
            .frame(width: 140)
            .background(color.opacity(0.1))
            .cornerRadius(12)
        }
    }
}

// MARK: - Calendar Event Card
struct CalendarEventCard: View {
    let event: CalendarEvent
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: event.eventType.icon)
                .font(.title3)
                .foregroundColor(Color(event.eventType.colorName))
                .frame(width: 32)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(event.title)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Text(event.date.formatted(as: "MMM d, h:mm a"))
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            Text(event.eventType.displayName)
                .font(.caption2)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(Color(event.eventType.colorName).opacity(0.1))
                .foregroundColor(Color(event.eventType.colorName))
                .cornerRadius(4)
        }
        .padding(.vertical, 8)
    }
}

// MARK: - Dashboard Moveout List Card (renamed to avoid conflict)
struct DashboardMoveoutListCard: View {
    let list: MoveoutList
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(list.title)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Spacer()
                
                Text("\(list.completedItemsCount)/\(list.totalItemsCount)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            ProgressView(value: list.progress)
                .accentColor(.stockNexusRed)
            
            Text("Created by \(list.createdByName ?? "Unknown")")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding()
        .background(Color.tertiaryBackground)
        .cornerRadius(8)
    }
}

// MARK: - Quick Action Button
struct QuickActionButton: View {
    let title: String
    let icon: String
    let color: Color
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundColor(color)
                
                Text(title)
                    .font(.caption)
                    .foregroundColor(.primary)
            }
            .frame(maxWidth: .infinity)
            .padding()
            .background(Color.tertiaryBackground)
            .cornerRadius(8)
        }
    }
}

// MARK: - Stats Detail View
struct StatsDetailView: View {
    let category: DashboardViewModel.StatsCategory
    let items: [Item]
    @Environment(\.presentationMode) var presentationMode
    @State private var searchText = ""
    
    var filteredItems: [Item] {
        if searchText.isEmpty {
            return items
        }
        return items.filter { $0.name.localizedCaseInsensitiveContains(searchText) }
    }
    
    var body: some View {
        NavigationView {
            VStack {
                // Search bar
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.secondary)
                    TextField("Search items", text: $searchText)
                }
                .padding(10)
                .background(Color(.systemGray6))
                .cornerRadius(10)
                .padding()
                
                List {
                    ForEach(filteredItems) { item in
                        ItemRowView(item: item)
                    }
                }
            }
            .navigationTitle(category.rawValue)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Close") {
                        presentationMode.wrappedValue.dismiss()
                    }
                }
            }
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
}

// MARK: - Item Row View
struct ItemRowView: View {
    let item: Item
    
    var body: some View {
        HStack(spacing: 12) {
            Text(item.category.icon)
                .font(.title2)
            
            VStack(alignment: .leading, spacing: 4) {
                Text(item.name)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Text(item.category.rawValue)
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            VStack(alignment: .trailing, spacing: 4) {
                Text("\(item.currentStock)")
                    .font(.headline)
                    .foregroundColor(stockLevelColor)
                
                Text("/ \(item.thresholdLevel)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
    
    private var stockLevelColor: Color {
        switch item.stockLevel {
        case .normal: return .green
        case .low: return .orange
        case .critical: return .red
        }
    }
}

struct DashboardView_Previews: PreviewProvider {
    static var previews: some View {
        DashboardView()
            .environmentObject(AuthViewModel())
    }
}
