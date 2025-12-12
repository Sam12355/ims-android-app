import SwiftUI

struct StockOutView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @StateObject private var viewModel = InventoryViewModel()
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Category Tabs
                categoryTabsView
                
                // Search Bar
                searchBarView
                
                // Stats Summary
                statsSummaryView
                
                // Items List
                itemsListView
            }
            .navigationTitle("Stock Out")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        // Barcode scanner
                    } label: {
                        Image(systemName: "barcode.viewfinder")
                    }
                }
            }
            .onAppear {
                Task {
                    await viewModel.loadItems(branchId: authViewModel.currentUser?.branchId ?? "")
                }
            }
            .sheet(isPresented: $viewModel.showStockDialog) {
                if let item = viewModel.selectedItem {
                    StockOperationSheet(
                        item: item,
                        isStockOut: true,
                        quantity: $viewModel.stockQuantity,
                        unitType: $viewModel.stockUnitType,
                        reason: $viewModel.stockReason,
                        isSaving: viewModel.isSaving
                    ) {
                        Task {
                            await viewModel.stockOut()
                        }
                    } onCancel: {
                        viewModel.showStockDialog = false
                    }
                }
            }
            .alert("Error", isPresented: $viewModel.showError) {
                Button("OK") { viewModel.showError = false }
            } message: {
                Text(viewModel.error ?? "An error occurred")
            }
        }
        .navigationViewStyle(.stack)
    }
    
    // MARK: - Category Tabs
    private var categoryTabsView: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 12) {
                CategoryChip(
                    title: "All",
                    isSelected: viewModel.selectedCategory == nil
                ) {
                    viewModel.selectedCategory = nil
                }
                
                ForEach(ItemCategory.allCases, id: \.self) { category in
                    CategoryChip(
                        title: category.rawValue,
                        icon: category.icon,
                        isSelected: viewModel.selectedCategory == category
                    ) {
                        viewModel.selectedCategory = category
                    }
                }
            }
            .padding(.horizontal)
            .padding(.vertical, 12)
        }
        .background(Color.secondaryBackground)
    }
    
    // MARK: - Search Bar
    private var searchBarView: some View {
        HStack {
            Image(systemName: "magnifyingglass")
                .foregroundColor(.secondary)
            
            TextField("Search items...", text: $viewModel.searchText)
                .textFieldStyle(.plain)
            
            if !viewModel.searchText.isEmpty {
                Button {
                    viewModel.searchText = ""
                } label: {
                    Image(systemName: "xmark.circle.fill")
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding()
        .background(Color.secondaryBackground)
        .cornerRadius(10)
        .padding(.horizontal)
        .padding(.vertical, 8)
    }
    
    // MARK: - Stats Summary
    private var statsSummaryView: some View {
        HStack(spacing: 16) {
            StatChip(
                title: "Total",
                value: "\(viewModel.filteredItems.count)",
                color: .blue
            )
            
            StatChip(
                title: "Low",
                value: "\(viewModel.filteredItems.filter { $0.stockLevel == .low }.count)",
                color: .orange,
                isSelected: viewModel.showLowStockOnly
            ) {
                viewModel.showLowStockOnly.toggle()
                viewModel.showCriticalOnly = false
            }
            
            StatChip(
                title: "Critical",
                value: "\(viewModel.filteredItems.filter { $0.stockLevel == .critical }.count)",
                color: .red,
                isSelected: viewModel.showCriticalOnly
            ) {
                viewModel.showCriticalOnly.toggle()
                viewModel.showLowStockOnly = false
            }
        }
        .padding(.horizontal)
        .padding(.bottom, 8)
    }
    
    // MARK: - Items List
    private var itemsListView: some View {
        Group {
            if viewModel.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if viewModel.filteredItems.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: "shippingbox")
                        .font(.system(size: 50))
                        .foregroundColor(.secondary)
                    Text("No Items Found")
                        .font(.headline)
                    Text("Try adjusting your search or filters")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    LazyVStack(spacing: 12) {
                        ForEach(viewModel.filteredItems) { item in
                            StockItemCard(item: item) {
                                viewModel.prepareStockOperation(item: item)
                            }
                        }
                    }
                    .padding()
                }
            }
        }
    }
}

// MARK: - Category Chip
struct CategoryChip: View {
    let title: String
    var icon: String? = nil
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 4) {
                if let icon = icon {
                    Text(icon)
                }
                Text(title)
                    .font(.subheadline)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(isSelected ? Color.stockNexusRed : Color.tertiaryBackground)
            .foregroundColor(isSelected ? .white : .primary)
            .cornerRadius(20)
        }
    }
}

// MARK: - Stat Chip
struct StatChip: View {
    let title: String
    let value: String
    let color: Color
    var isSelected: Bool = false
    var action: (() -> Void)? = nil
    
    var body: some View {
        Button {
            action?()
        } label: {
            HStack(spacing: 4) {
                Text(value)
                    .font(.headline)
                    .foregroundColor(isSelected ? .white : color)
                
                Text(title)
                    .font(.caption)
                    .foregroundColor(isSelected ? .white.opacity(0.8) : .secondary)
            }
            .padding(.horizontal, 12)
            .padding(.vertical, 8)
            .background(isSelected ? color : color.opacity(0.1))
            .cornerRadius(8)
        }
        .disabled(action == nil)
    }
}

// MARK: - Stock Item Card
struct StockItemCard: View {
    let item: Item
    let onStockOut: () -> Void
    
    var body: some View {
        HStack(spacing: 12) {
            // Item Icon/Image
            ZStack {
                Circle()
                    .fill(stockLevelColor.opacity(0.1))
                    .frame(width: 50, height: 50)
                
                Text(item.category.icon)
                    .font(.title2)
            }
            
            // Item Details
            VStack(alignment: .leading, spacing: 4) {
                Text(item.name)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Text("\(item.category.rawValue) • \(item.unit.displayName)")
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                HStack(spacing: 4) {
                    Text("Current: \(item.currentStock)")
                        .font(.caption)
                    Text("/")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Text("Thresh: \(item.thresholdLevel)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                // Progress Bar
                GeometryReader { geometry in
                    ZStack(alignment: .leading) {
                        Rectangle()
                            .fill(Color.gray.opacity(0.2))
                            .frame(height: 6)
                            .cornerRadius(3)
                        
                        Rectangle()
                            .fill(stockLevelColor)
                            .frame(width: geometry.size.width * item.stockPercentage, height: 6)
                            .cornerRadius(3)
                    }
                }
                .frame(height: 6)
            }
            
            Spacer()
            
            // Stock Out Button
            Button(action: onStockOut) {
                Image(systemName: "minus.circle.fill")
                    .font(.title)
                    .foregroundColor(.stockNexusRed)
            }
        }
        .padding()
        .background(Color.secondaryBackground)
        .cornerRadius(12)
    }
    
    private var stockLevelColor: Color {
        switch item.stockLevel {
        case .normal: return .green
        case .low: return .orange
        case .critical: return .red
        }
    }
}

// MARK: - Stock Operation Sheet
struct StockOperationSheet: View {
    let item: Item
    let isStockOut: Bool
    @Binding var quantity: String
    @Binding var unitType: String
    @Binding var reason: String
    let isSaving: Bool
    let onConfirm: () -> Void
    let onCancel: () -> Void
    
    var body: some View {
        NavigationView {
            VStack(spacing: 20) {
                // Item Info
                HStack {
                    Text(item.category.icon)
                        .font(.largeTitle)
                    
                    VStack(alignment: .leading) {
                        Text(item.name)
                            .font(.headline)
                        Text("Current: \(item.currentStock) \(item.unit.displayName)")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    
                    Spacer()
                }
                .padding()
                .background(Color.secondaryBackground)
                .cornerRadius(12)
                
                // Unit Type Picker
                VStack(alignment: .leading, spacing: 8) {
                    Text("Unit Type")
                        .font(.subheadline)
                        .fontWeight(.medium)
                    
                    Picker("Unit Type", selection: $unitType) {
                        Text("Base Unit").tag("base")
                        Text("Package").tag("package")
                    }
                    .pickerStyle(.segmented)
                }
                
                // Quantity Input
                VStack(alignment: .leading, spacing: 8) {
                    Text("Quantity to \(isStockOut ? "Remove" : "Add")")
                        .font(.subheadline)
                        .fontWeight(.medium)
                    
                    TextField("Enter quantity", text: $quantity)
                        .keyboardType(.numberPad)
                        .stockNexusTextField()
                }
                
                // Reason Input
                VStack(alignment: .leading, spacing: 8) {
                    Text("Reason (Optional)")
                        .font(.subheadline)
                        .fontWeight(.medium)
                    
                    TextField("Enter reason", text: $reason)
                        .stockNexusTextField()
                }
                
                Spacer()
                
                // Confirm Button
                Button(action: onConfirm) {
                    HStack {
                        if isSaving {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        } else {
                            Text(isStockOut ? "Remove Stock" : "Add Stock")
                        }
                    }
                    .stockNexusPrimaryButton()
                }
                .disabled(isSaving || quantity.isEmpty)
            }
            .padding()
            .navigationTitle(isStockOut ? "Stock Out" : "Stock In")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel", action: onCancel)
                }
            }
        }
    }
}

// MARK: - Preview Provider
struct StockOutView_Previews: PreviewProvider {
    static var previews: some View {
        StockOutView()
            .environmentObject(AuthViewModel())
    }
}
