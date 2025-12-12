import SwiftUI

struct StockInView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @StateObject private var viewModel = InventoryViewModel()
    @State private var showReceiptViewer = false
    @State private var selectedReceipt: Receipt?
    
    var body: some View {
        NavigationView {
            HStack(spacing: 0) {
                // Left Panel - Receipts List
                receiptsListPanel
                
                Divider()
                
                // Right Panel - Stock Items or Receipt Viewer
                if showReceiptViewer, let receipt = selectedReceipt {
                    ReceiptViewerPanel(receipt: receipt) {
                        showReceiptViewer = false
                        selectedReceipt = nil
                    }
                } else {
                    stockItemsPanel
                }
            }
            .navigationTitle("Stock In")
            .onAppear {
                Task {
                    await viewModel.loadItems(branchId: authViewModel.currentUser?.branchId ?? "")
                    await viewModel.loadReceipts(branchId: authViewModel.currentUser?.branchId ?? "")
                }
            }
            .sheet(isPresented: $viewModel.showStockDialog) {
                if let item = viewModel.selectedItem {
                    StockOperationSheet(
                        item: item,
                        isStockOut: false,
                        quantity: $viewModel.stockQuantity,
                        unitType: $viewModel.stockUnitType,
                        reason: $viewModel.stockReason,
                        isSaving: viewModel.isSaving
                    ) {
                        Task {
                            await viewModel.stockIn()
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
    
    // MARK: - Receipts List Panel
    private var receiptsListPanel: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                Text("Receipts")
                    .font(.headline)
                Spacer()
            }
            .padding()
            .background(Color.secondaryBackground)
            
            // Receipts List
            if viewModel.receipts.isEmpty {
                VStack {
                    Spacer()
                    Image(systemName: "doc.text")
                        .font(.system(size: 40))
                        .foregroundColor(.secondary)
                    Text("No receipts")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Spacer()
                }
            } else {
                List {
                    ForEach(viewModel.receipts) { receipt in
                        ReceiptCard(receipt: receipt, isSelected: selectedReceipt?.id == receipt.id) {
                            selectedReceipt = receipt
                            showReceiptViewer = true
                        }
                    }
                }
                .listStyle(.plain)
            }
        }
        .frame(width: 280)
    }
    
    // MARK: - Stock Items Panel
    private var stockItemsPanel: some View {
        VStack(spacing: 0) {
            // Search and Filters
            VStack(spacing: 12) {
                // Search Bar
                HStack {
                    Image(systemName: "magnifyingglass")
                        .foregroundColor(.secondary)
                    
                    TextField("Search items...", text: $viewModel.searchText)
                        .textFieldStyle(.plain)
                }
                .padding()
                .background(Color.secondaryBackground)
                .cornerRadius(10)
                
                // Filter Chips
                HStack(spacing: 12) {
                    StockInFilterChip(title: "All", isSelected: !viewModel.showLowStockOnly && !viewModel.showCriticalOnly) {
                        viewModel.showLowStockOnly = false
                        viewModel.showCriticalOnly = false
                    }
                    
                    StockInFilterChip(title: "Low Stock", isSelected: viewModel.showLowStockOnly) {
                        viewModel.showLowStockOnly = true
                        viewModel.showCriticalOnly = false
                    }
                    
                    StockInFilterChip(title: "Critical", isSelected: viewModel.showCriticalOnly) {
                        viewModel.showLowStockOnly = false
                        viewModel.showCriticalOnly = true
                    }
                    
                    Spacer()
                }
            }
            .padding()
            
            // Items Grid
            if viewModel.isLoading {
                ProgressView()
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else {
                ScrollView {
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                        ForEach(viewModel.filteredItems) { item in
                            StockInItemCard(item: item) {
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

// MARK: - Stock In Filter Chip
struct StockInFilterChip: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.subheadline)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(isSelected ? Color.stockNexusRed : Color.secondaryBackground)
                .foregroundColor(isSelected ? .white : .primary)
                .cornerRadius(20)
        }
    }
}

// MARK: - Receipt Card
struct ReceiptCard: View {
    let receipt: Receipt
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 8) {
                HStack {
                    Text(receipt.supplier.rawValue)
                        .font(.subheadline)
                        .fontWeight(.medium)
                    
                    Spacer()
                    
                    ReceiptStatusBadge(status: receipt.status)
                }
                
                Text(receipt.submitterName)
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                Text(receipt.createdAt.formatted(as: "MMM d, yyyy"))
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding()
            .background(isSelected ? Color.stockNexusRed.opacity(0.1) : Color.clear)
            .cornerRadius(8)
        }
        .buttonStyle(.plain)
    }
}

// MARK: - Receipt Status Badge
struct ReceiptStatusBadge: View {
    let status: ReceiptStatus
    
    var body: some View {
        Text(status.rawValue.capitalized)
            .font(.caption2)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(status == .pending ? Color.orange.opacity(0.1) : Color.green.opacity(0.1))
            .foregroundColor(status == .pending ? .orange : .green)
            .cornerRadius(4)
    }
}

// MARK: - Receipt Viewer Panel
struct ReceiptViewerPanel: View {
    let receipt: Receipt
    let onClose: () -> Void
    
    var body: some View {
        VStack(spacing: 0) {
            // Header
            HStack {
                VStack(alignment: .leading) {
                    Text(receipt.supplier.rawValue)
                        .font(.headline)
                    Text("Submitted by \(receipt.submitterName)")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                Button(action: onClose) {
                    Image(systemName: "xmark.circle.fill")
                        .font(.title2)
                        .foregroundColor(.secondary)
                }
            }
            .padding()
            .background(Color.secondaryBackground)
            
            // Document Viewer
            if let url = receipt.documentURL {
                VStack {
                    Spacer()
                    Image(systemName: "doc.richtext")
                        .font(.system(size: 80))
                        .foregroundColor(.secondary)
                    Text("Document Preview")
                        .font(.headline)
                        .foregroundColor(.secondary)
                    Text(url)
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Spacer()
                }
            } else {
                VStack {
                    Spacer()
                    Image(systemName: "doc.slash")
                        .font(.system(size: 60))
                        .foregroundColor(.secondary)
                    Text("No document attached")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    Spacer()
                }
            }
            
            // Remarks
            if let remarks = receipt.remarks, !remarks.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Remarks")
                        .font(.subheadline)
                        .fontWeight(.medium)
                    
                    Text(remarks)
                        .font(.body)
                        .foregroundColor(.secondary)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding()
                .background(Color.secondaryBackground)
            }
        }
    }
}

// MARK: - Stock In Item Card
struct StockInItemCard: View {
    let item: Item
    let onStockIn: () -> Void
    
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Text(item.category.icon)
                    .font(.title2)
                
                Spacer()
                
                Button(action: onStockIn) {
                    Image(systemName: "plus.circle.fill")
                        .font(.title2)
                        .foregroundColor(.green)
                }
            }
            
            Text(item.name)
                .font(.subheadline)
                .fontWeight(.medium)
                .lineLimit(2)
            
            Text(item.category.rawValue)
                .font(.caption)
                .foregroundColor(.secondary)
            
            HStack {
                Text("Stock: \(item.currentStock)")
                    .font(.caption)
                    .foregroundColor(stockLevelColor)
                
                Spacer()
                
                Text("/ \(item.thresholdLevel)")
                    .font(.caption)
                    .foregroundColor(.secondary)
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

// MARK: - Preview Provider
struct StockInView_Previews: PreviewProvider {
    static var previews: some View {
        StockInView()
            .environmentObject(AuthViewModel())
    }
}
