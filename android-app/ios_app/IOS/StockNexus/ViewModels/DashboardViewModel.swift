import Foundation
import Combine

// MARK: - Dashboard ViewModel
@MainActor
class DashboardViewModel: ObservableObject {
    // MARK: - Published Properties
    @Published var totalItems = 0
    @Published var lowStockItems = 0
    @Published var criticalStockItems = 0
    @Published var items: [Item] = []
    @Published var calendarEvents: [CalendarEvent] = []
    @Published var activeMoveoutLists: [MoveoutList] = []
    @Published var weather: WeatherData?
    
    @Published var isLoading = false
    @Published var isRefreshing = false
    @Published var error: String?
    @Published var showError = false
    
    @Published var selectedStatsCategory: StatsCategory?
    @Published var showStatsDetail = false
    
    private var cancellables = Set<AnyCancellable>()
    
    // MARK: - Stats Category
    enum StatsCategory: String, CaseIterable {
        case total = "Total Items"
        case low = "Low Stock"
        case critical = "Critical Stock"
        
        var color: String {
            switch self {
            case .total: return "blue"
            case .low: return "orange"
            case .critical: return "red"
            }
        }
    }
    
    // MARK: - Load Dashboard
    func loadDashboard(branchId: String) async {
        isLoading = true
        error = nil
        
        do {
            // Load items
            items = try await InventoryService.shared.getItems(branchId: branchId)
            
            // Calculate stats
            totalItems = items.count
            lowStockItems = items.filter { $0.stockLevel == .low }.count
            criticalStockItems = items.filter { $0.stockLevel == .critical }.count
            
            // Load calendar events (mock for now)
            calendarEvents = CalendarEvent.samples
            
            // Load moveout lists
            let allLists = try await MoveoutListService.shared.getMoveoutLists(branchId: branchId, status: .active)
            activeMoveoutLists = allLists
            
            // Load weather (mock for now)
            weather = WeatherData.sample
            
        } catch let networkError as NetworkError {
            self.error = networkError.errorDescription
            showError = true
            // Use mock data for development
            loadMockData()
        } catch {
            self.error = error.localizedDescription
            showError = true
            loadMockData()
        }
        
        isLoading = false
    }
    
    // MARK: - Refresh
    func refresh(branchId: String) async {
        isRefreshing = true
        await loadDashboard(branchId: branchId)
        isRefreshing = false
    }
    
    // MARK: - Get Items by Category
    func getItemsForCategory(_ category: StatsCategory) -> [Item] {
        switch category {
        case .total:
            return items
        case .low:
            return items.filter { $0.stockLevel == .low }
        case .critical:
            return items.filter { $0.stockLevel == .critical }
        }
    }
    
    // MARK: - Today's Events
    var todayEvents: [CalendarEvent] {
        calendarEvents.filter { $0.isToday }
    }
    
    // MARK: - Upcoming Events
    var upcomingEvents: [CalendarEvent] {
        calendarEvents.filter { $0.isUpcoming && !$0.isToday }
            .sorted { $0.date < $1.date }
    }
    
    // MARK: - Mock Data
    private func loadMockData() {
        items = Item.samples
        totalItems = items.count
        lowStockItems = items.filter { $0.stockLevel == .low }.count
        criticalStockItems = items.filter { $0.stockLevel == .critical }.count
        calendarEvents = CalendarEvent.samples
        activeMoveoutLists = MoveoutList.samples.filter { $0.status == .active }
        weather = WeatherData.sample
    }
}

// MARK: - Weather Data
struct WeatherData: Codable {
    let temperature: Double
    let condition: String
    let icon: String
    let cityName: String
    
    var temperatureString: String {
        String(format: "%.0f°C", temperature)
    }
    
    static let sample = WeatherData(
        temperature: 5,
        condition: "Cloudy",
        icon: "cloud.fill",
        cityName: "Örnsköldsvik"
    )
}
