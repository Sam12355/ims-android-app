import Foundation
import Combine

// MARK: - Inventory ViewModel
@MainActor
class InventoryViewModel: ObservableObject {
    // MARK: - Published Properties
    @Published var items: [Item] = []
    @Published var filteredItems: [Item] = []
    @Published var receipts: [Receipt] = []
    
    @Published var isLoading = false
    @Published var isSaving = false
    @Published var error: String?
    @Published var showError = false
    
    @Published var searchText = ""
    @Published var selectedCategory: ItemCategory?
    @Published var showLowStockOnly = false
    @Published var showCriticalOnly = false
    
    // Stock operation
    @Published var selectedItem: Item?
    @Published var stockQuantity = ""
    @Published var stockUnitType = "base"
    @Published var stockReason = ""
    @Published var showStockDialog = false
    
    // Add/Edit item
    @Published var editingItem: Item?
    @Published var showAddEditSheet = false
    
    // Form fields
    @Published var itemName = ""
    @Published var itemCategory: ItemCategory = .other
    @Published var itemUnit: UnitOfMeasure = .pieces
    @Published var itemThreshold = ""
    @Published var itemLowLevel = ""
    @Published var itemCriticalLevel = ""
    @Published var itemDescription = ""
    @Published var itemSKU = ""
    @Published var itemBarcode = ""
    
    private var cancellables = Set<AnyCancellable>()
    
    init() {
        setupSearchSubscriber()
    }
    
    // MARK: - Search Subscriber
    private func setupSearchSubscriber() {
        $searchText
            .combineLatest($selectedCategory, $showLowStockOnly, $showCriticalOnly)
            .debounce(for: .milliseconds(300), scheduler: DispatchQueue.main)
            .sink { [weak self] search, category, lowOnly, criticalOnly in
                self?.filterItems(search: search, category: category, lowOnly: lowOnly, criticalOnly: criticalOnly)
            }
            .store(in: &cancellables)
    }
    
    // MARK: - Load Items
    func loadItems(branchId: String) async {
        isLoading = true
        error = nil
        
        do {
            items = try await InventoryService.shared.getItems(branchId: branchId)
            filterItems(search: searchText, category: selectedCategory, lowOnly: showLowStockOnly, criticalOnly: showCriticalOnly)
        } catch let networkError as NetworkError {
            self.error = networkError.errorDescription
            showError = true
            // Use mock data
            items = Item.samples
            filterItems(search: searchText, category: selectedCategory, lowOnly: showLowStockOnly, criticalOnly: showCriticalOnly)
        } catch {
            self.error = error.localizedDescription
            showError = true
        }
        
        isLoading = false
    }
    
    // MARK: - Filter Items
    private func filterItems(search: String, category: ItemCategory?, lowOnly: Bool, criticalOnly: Bool) {
        var result = items
        
        // Filter by search text
        if !search.isEmpty {
            result = result.filter {
                $0.name.localizedCaseInsensitiveContains(search) ||
                $0.category.rawValue.localizedCaseInsensitiveContains(search)
            }
        }
        
        // Filter by category
        if let category = category {
            result = result.filter { $0.category == category }
        }
        
        // Filter by stock level
        if criticalOnly {
            result = result.filter { $0.stockLevel == .critical }
        } else if lowOnly {
            result = result.filter { $0.stockLevel == .low || $0.stockLevel == .critical }
        }
        
        filteredItems = result
    }
    
    // MARK: - Items Grouped by Category
    var itemsByCategory: [ItemCategory: [Item]] {
        Dictionary(grouping: filteredItems) { $0.category }
    }
    
    // MARK: - Stock Out
    func stockOut() async {
        guard let item = selectedItem,
              let quantity = Int(stockQuantity),
              quantity > 0 else {
            error = "Please enter a valid quantity"
            showError = true
            return
        }
        
        if quantity > item.currentStock {
            error = "Cannot remove more than available stock"
            showError = true
            return
        }
        
        isSaving = true
        
        do {
            let updatedItem = try await InventoryService.shared.stockOut(
                itemId: item.id,
                quantity: quantity,
                unitType: stockUnitType,
                reason: stockReason.isEmpty ? nil : stockReason
            )
            
            // Update local items
            if let index = items.firstIndex(where: { $0.id == item.id }) {
                items[index] = updatedItem
            }
            filterItems(search: searchText, category: selectedCategory, lowOnly: showLowStockOnly, criticalOnly: showCriticalOnly)
            
            clearStockDialog()
            showStockDialog = false
            
        } catch let networkError as NetworkError {
            self.error = networkError.errorDescription
            showError = true
        } catch {
            self.error = error.localizedDescription
            showError = true
        }
        
        isSaving = false
    }
    
    // MARK: - Stock In
    func stockIn() async {
        guard let item = selectedItem,
              let quantity = Int(stockQuantity),
              quantity > 0 else {
            error = "Please enter a valid quantity"
            showError = true
            return
        }
        
        isSaving = true
        
        do {
            let updatedItem = try await InventoryService.shared.stockIn(
                itemId: item.id,
                quantity: quantity,
                unitType: stockUnitType,
                reason: stockReason.isEmpty ? nil : stockReason
            )
            
            // Update local items
            if let index = items.firstIndex(where: { $0.id == item.id }) {
                items[index] = updatedItem
            }
            filterItems(search: searchText, category: selectedCategory, lowOnly: showLowStockOnly, criticalOnly: showCriticalOnly)
            
            clearStockDialog()
            showStockDialog = false
            
        } catch let networkError as NetworkError {
            self.error = networkError.errorDescription
            showError = true
        } catch {
            self.error = error.localizedDescription
            showError = true
        }
        
        isSaving = false
    }
    
    // MARK: - Create Item
    func createItem(branchId: String) async {
        guard validateItemForm() else { return }
        
        isSaving = true
        
        let request = CreateItemRequest(
            name: itemName,
            category: itemCategory.rawValue,
            unit: itemUnit.rawValue,
            thresholdLevel: Int(itemThreshold) ?? 0,
            lowLevel: Int(itemLowLevel) ?? 0,
            criticalLevel: Int(itemCriticalLevel) ?? 0,
            description: itemDescription.isEmpty ? nil : itemDescription,
            sku: itemSKU.isEmpty ? nil : itemSKU,
            barcode: itemBarcode.isEmpty ? nil : itemBarcode,
            imageURL: nil,
            storageTemperature: nil,
            baseUnit: nil,
            packagingUnit: nil,
            unitsPerPackage: nil,
            branchId: branchId
        )
        
        do {
            let newItem = try await InventoryService.shared.createItem(request)
            items.append(newItem)
            filterItems(search: searchText, category: selectedCategory, lowOnly: showLowStockOnly, criticalOnly: showCriticalOnly)
            
            clearItemForm()
            showAddEditSheet = false
            
        } catch let networkError as NetworkError {
            self.error = networkError.errorDescription
            showError = true
        } catch {
            self.error = error.localizedDescription
            showError = true
        }
        
        isSaving = false
    }
    
    // MARK: - Update Item
    func updateItem() async {
        guard let item = editingItem, validateItemForm() else { return }
        
        isSaving = true
        
        let request = UpdateItemRequest(
            name: itemName,
            category: itemCategory.rawValue,
            unit: itemUnit.rawValue,
            thresholdLevel: Int(itemThreshold),
            lowLevel: Int(itemLowLevel),
            criticalLevel: Int(itemCriticalLevel),
            description: itemDescription.isEmpty ? nil : itemDescription,
            sku: itemSKU.isEmpty ? nil : itemSKU,
            barcode: itemBarcode.isEmpty ? nil : itemBarcode,
            imageURL: nil,
            storageTemperature: nil,
            baseUnit: nil,
            packagingUnit: nil,
            unitsPerPackage: nil
        )
        
        do {
            let updatedItem = try await InventoryService.shared.updateItem(id: item.id, request)
            
            if let index = items.firstIndex(where: { $0.id == item.id }) {
                items[index] = updatedItem
            }
            filterItems(search: searchText, category: selectedCategory, lowOnly: showLowStockOnly, criticalOnly: showCriticalOnly)
            
            clearItemForm()
            showAddEditSheet = false
            
        } catch let networkError as NetworkError {
            self.error = networkError.errorDescription
            showError = true
        } catch {
            self.error = error.localizedDescription
            showError = true
        }
        
        isSaving = false
    }
    
    // MARK: - Delete Item
    func deleteItem(_ item: Item) async {
        do {
            try await InventoryService.shared.deleteItem(id: item.id)
            items.removeAll { $0.id == item.id }
            filterItems(search: searchText, category: selectedCategory, lowOnly: showLowStockOnly, criticalOnly: showCriticalOnly)
        } catch let networkError as NetworkError {
            self.error = networkError.errorDescription
            showError = true
        } catch {
            self.error = error.localizedDescription
            showError = true
        }
    }
    
    // MARK: - Load Receipts
    func loadReceipts(branchId: String) async {
        isLoading = true
        
        do {
            receipts = try await InventoryService.shared.getReceipts(branchId: branchId)
        } catch {
            // Use mock data
            receipts = []
        }
        
        isLoading = false
    }
    
    // MARK: - Validation
    private func validateItemForm() -> Bool {
        if itemName.isEmpty {
            error = "Please enter item name"
            showError = true
            return false
        }
        
        if Int(itemThreshold) == nil || Int(itemThreshold) ?? 0 <= 0 {
            error = "Please enter a valid threshold level"
            showError = true
            return false
        }
        
        if Int(itemLowLevel) == nil || Int(itemLowLevel) ?? 0 <= 0 {
            error = "Please enter a valid low level"
            showError = true
            return false
        }
        
        if Int(itemCriticalLevel) == nil || Int(itemCriticalLevel) ?? 0 <= 0 {
            error = "Please enter a valid critical level"
            showError = true
            return false
        }
        
        return true
    }
    
    // MARK: - Helper Methods
    func prepareForEdit(_ item: Item) {
        editingItem = item
        itemName = item.name
        itemCategory = item.category
        itemUnit = item.unit
        itemThreshold = "\(item.thresholdLevel)"
        itemLowLevel = "\(item.lowLevel)"
        itemCriticalLevel = "\(item.criticalLevel)"
        itemDescription = item.description ?? ""
        itemSKU = item.sku ?? ""
        itemBarcode = item.barcode ?? ""
        showAddEditSheet = true
    }
    
    func prepareForAdd() {
        clearItemForm()
        editingItem = nil
        showAddEditSheet = true
    }
    
    func prepareStockOperation(item: Item) {
        selectedItem = item
        stockQuantity = ""
        stockUnitType = "base"
        stockReason = ""
        showStockDialog = true
    }
    
    private func clearItemForm() {
        itemName = ""
        itemCategory = .other
        itemUnit = .pieces
        itemThreshold = ""
        itemLowLevel = ""
        itemCriticalLevel = ""
        itemDescription = ""
        itemSKU = ""
        itemBarcode = ""
        editingItem = nil
    }
    
    private func clearStockDialog() {
        selectedItem = nil
        stockQuantity = ""
        stockUnitType = "base"
        stockReason = ""
    }
}
