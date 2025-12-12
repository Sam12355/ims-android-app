import SwiftUI

// MARK: - Reports View
struct ReportsView: View {
    @State private var selectedReportType: ReportType = .inventory
    @State private var dateRange: DateRange = .week
    
    enum ReportType: String, CaseIterable {
        case inventory = "Inventory"
        case stockMovement = "Stock Movement"
        case lowStock = "Low Stock"
        case deliveries = "Deliveries"
    }
    
    enum DateRange: String, CaseIterable {
        case week = "This Week"
        case month = "This Month"
        case quarter = "This Quarter"
        case year = "This Year"
    }
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 20) {
                    // Report Type Picker
                    Picker("Report Type", selection: $selectedReportType) {
                        ForEach(ReportType.allCases, id: \.self) { type in
                            Text(type.rawValue).tag(type)
                        }
                    }
                    .pickerStyle(.segmented)
                    .padding(.horizontal)
                    
                    // Date Range Picker
                    Picker("Date Range", selection: $dateRange) {
                        ForEach(DateRange.allCases, id: \.self) { range in
                            Text(range.rawValue).tag(range)
                        }
                    }
                    .pickerStyle(.menu)
                    .padding(.horizontal)
                    
                    // Summary Cards
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 16) {
                        StockSummaryCard(title: "Total Items", value: "1,234", icon: "cube.box", color: .blue)
                        StockSummaryCard(title: "Stock In", value: "456", icon: "arrow.down.circle", color: .green)
                        StockSummaryCard(title: "Stock Out", value: "321", icon: "arrow.up.circle", color: .orange)
                        StockSummaryCard(title: "Low Stock", value: "23", icon: "exclamationmark.triangle", color: .red)
                    }
                    .padding(.horizontal)
                    
                    // Chart Section
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Stock Movement Trend")
                            .font(.headline)
                            .padding(.horizontal)
                        
                        StockMovementChart()
                            .frame(height: 250)
                            .padding()
                            .background(Color(.systemBackground))
                            .cornerRadius(12)
                            .shadow(color: Color.black.opacity(0.05), radius: 5, x: 0, y: 2)
                            .padding(.horizontal)
                    }
                    
                    // Category Breakdown
                    VStack(alignment: .leading, spacing: 12) {
                        Text("Category Breakdown")
                            .font(.headline)
                            .padding(.horizontal)
                        
                        CategoryBreakdownChart()
                            .frame(height: 200)
                            .padding()
                            .background(Color(.systemBackground))
                            .cornerRadius(12)
                            .shadow(color: Color.black.opacity(0.05), radius: 5, x: 0, y: 2)
                            .padding(.horizontal)
                    }
                    
                    // Export Button
                    VStack(spacing: 12) {
                        SecondaryButton(title: "Export Report") {
                            // TODO: Export functionality
                        }
                    }
                    .padding()
                }
                .padding(.vertical)
            }
            .background(Color(.systemGroupedBackground))
            .navigationTitle("Reports")
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
}

// MARK: - Stock Summary Card
struct StockSummaryCard: View {
    let title: String
    let value: String
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundColor(color)
                Spacer()
            }
            
            Text(value)
                .font(.title2)
                .fontWeight(.bold)
            
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding()
        .background(Color(.systemBackground))
        .cornerRadius(12)
        .shadow(color: Color.black.opacity(0.05), radius: 5, x: 0, y: 2)
    }
}

// MARK: - Stock Movement Chart (placeholder without Charts framework)
struct StockMovementChart: View {
    var body: some View {
        VStack {
            HStack(alignment: .bottom, spacing: 8) {
                ForEach(0..<7, id: \.self) { index in
                    VStack {
                        Spacer()
                        RoundedRectangle(cornerRadius: 4)
                            .fill(Color.stockNexusRed)
                            .frame(width: 30, height: CGFloat([80, 120, 60, 140, 100, 90, 110][index]))
                        Text(["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"][index])
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                }
            }
            .frame(maxHeight: 200)
        }
    }
}

// MARK: - Category Breakdown Chart (placeholder)
struct CategoryBreakdownChart: View {
    let categories: [(String, Color, Double)] = [
        ("Beverages", .blue, 30),
        ("Food", .green, 25),
        ("Cleaning", .orange, 20),
        ("Other", .gray, 25)
    ]
    
    var body: some View {
        HStack(spacing: 20) {
            // Simple bar representation
            ForEach(categories, id: \.0) { category in
                VStack {
                    RoundedRectangle(cornerRadius: 4)
                        .fill(category.1)
                        .frame(width: 40, height: CGFloat(category.2 * 4))
                    
                    Text(category.0)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                    
                    Text("\(Int(category.2))%")
                        .font(.caption)
                        .fontWeight(.medium)
                }
            }
        }
    }
}

struct ReportsView_Previews: PreviewProvider {
    static var previews: some View {
        ReportsView()
    }
}
