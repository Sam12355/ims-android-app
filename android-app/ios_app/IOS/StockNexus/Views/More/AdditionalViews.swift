import SwiftUI

// MARK: - Manage Items View
struct ManageItemsView: View {
    @State private var items: [Item] = Item.samples
    @State private var searchText = ""
    @State private var selectedCategory: ItemCategory?
    @State private var showAddItem = false
    
    var filteredItems: [Item] {
        var result = items
        
        if !searchText.isEmpty {
            result = result.filter { $0.name.localizedCaseInsensitiveContains(searchText) }
        }
        
        if let category = selectedCategory {
            result = result.filter { $0.category == category }
        }
        
        return result
    }
    
    var body: some View {
        VStack(spacing: 0) {
            // Category Filter
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    ManageItemsFilterChip(title: "All", isSelected: selectedCategory == nil) {
                        selectedCategory = nil
                    }
                    
                    ForEach(ItemCategory.allCases, id: \.self) { category in
                        ManageItemsFilterChip(title: category.rawValue, isSelected: selectedCategory == category) {
                            selectedCategory = category
                        }
                    }
                }
                .padding(.horizontal)
            }
            .padding(.vertical, 12)
            .background(Color.secondaryBackground)
            
            // Search Bar
            HStack {
                Image(systemName: "magnifyingglass")
                    .foregroundColor(.secondary)
                TextField("Search items", text: $searchText)
            }
            .padding(10)
            .background(Color(.systemGray6))
            .cornerRadius(10)
            .padding()
            
            // Items List
            List {
                ForEach(filteredItems) { item in
                    NavigationLink(destination: ItemDetailEditView(item: item)) {
                        ManageItemRow(item: item)
                    }
                }
                .onDelete { offsets in
                    items.remove(atOffsets: offsets)
                }
            }
        }
        .navigationTitle("Manage Items")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showAddItem = true }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showAddItem) {
            AddItemSheet { newItem in
                items.append(newItem)
                showAddItem = false
            }
        }
    }
}

// MARK: - Local Filter Chip
struct ManageItemsFilterChip: View {
    let title: String
    let isSelected: Bool
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            Text(title)
                .font(.subheadline)
                .fontWeight(isSelected ? .semibold : .regular)
                .padding(.horizontal, 16)
                .padding(.vertical, 8)
                .background(isSelected ? Color.stockNexusRed : Color(.systemGray5))
                .foregroundColor(isSelected ? .white : .primary)
                .cornerRadius(20)
        }
    }
}

// MARK: - Manage Item Row
struct ManageItemRow: View {
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
                    .foregroundColor(stockColor)
                
                Text("of \(item.thresholdLevel)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
        .padding(.vertical, 4)
    }
    
    private var stockColor: Color {
        switch item.stockLevel {
        case .normal: return .green
        case .low: return .orange
        case .critical: return .red
        }
    }
}

// MARK: - Item Detail Edit View
struct ItemDetailEditView: View {
    let item: Item
    @State private var name: String = ""
    @State private var category: ItemCategory = .beverages
    @State private var currentStock: String = ""
    @State private var thresholdLevel: String = ""
    @State private var isSaving = false
    @State private var showSuccess = false
    
    var body: some View {
        Form {
            Section(header: Text("Item Information")) {
                TextField("Name", text: $name)
                
                Picker("Category", selection: $category) {
                    ForEach(ItemCategory.allCases, id: \.self) { cat in
                        Text(cat.rawValue).tag(cat)
                    }
                }
            }
            
            Section(header: Text("Stock Information")) {
                TextField("Current Stock", text: $currentStock)
                    .keyboardType(.numberPad)
                
                TextField("Threshold Level", text: $thresholdLevel)
                    .keyboardType(.numberPad)
            }
            
            Section {
                Button(action: saveItem) {
                    HStack {
                        Spacer()
                        if isSaving {
                            ProgressView()
                        } else {
                            Text("Save Changes")
                        }
                        Spacer()
                    }
                }
                .disabled(isSaving)
            }
        }
        .navigationTitle("Edit Item")
        .onAppear {
            name = item.name
            category = item.category
            currentStock = "\(item.currentStock)"
            thresholdLevel = "\(item.thresholdLevel)"
        }
        .alert(isPresented: $showSuccess) {
            Alert(
                title: Text("Success"),
                message: Text("Item updated successfully!"),
                dismissButton: .default(Text("OK"))
            )
        }
    }
    
    private func saveItem() {
        isSaving = true
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            isSaving = false
            showSuccess = true
        }
    }
}

// MARK: - Add Item Sheet
struct AddItemSheet: View {
    @Environment(\.presentationMode) var presentationMode
    let onCreate: (Item) -> Void
    
    @State private var name = ""
    @State private var category: ItemCategory = .beverages
    @State private var currentStock = ""
    @State private var thresholdLevel = ""
    @State private var barcode = ""
    
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Item Information")) {
                    TextField("Name", text: $name)
                    
                    Picker("Category", selection: $category) {
                        ForEach(ItemCategory.allCases, id: \.self) { cat in
                            Text(cat.rawValue).tag(cat)
                        }
                    }
                    
                    TextField("Barcode (Optional)", text: $barcode)
                }
                
                Section(header: Text("Stock Information")) {
                    TextField("Current Stock", text: $currentStock)
                        .keyboardType(.numberPad)
                    
                    TextField("Threshold Level", text: $thresholdLevel)
                        .keyboardType(.numberPad)
                }
            }
            .navigationTitle("Add Item")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        presentationMode.wrappedValue.dismiss()
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Add") {
                        let stock = Int(currentStock) ?? 0
                        let threshold = Int(thresholdLevel) ?? 10
                        let newItem = Item(
                            id: UUID().uuidString,
                            name: name,
                            category: category,
                            unit: .pieces,
                            currentStock: stock,
                            thresholdLevel: threshold,
                            lowLevel: threshold / 2,
                            criticalLevel: threshold / 4,
                            description: nil,
                            sku: nil,
                            barcode: barcode.isEmpty ? nil : barcode,
                            imageURL: nil,
                            storageTemperature: nil,
                            baseUnit: nil,
                            packagingUnit: nil,
                            unitsPerPackage: nil,
                            branchId: "branch1",
                            isActive: true,
                            createdAt: Date(),
                            updatedAt: Date()
                        )
                        onCreate(newItem)
                    }
                    .disabled(name.isEmpty || currentStock.isEmpty)
                }
            }
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
}

// MARK: - ICA Delivery View (placeholder)
struct ICADeliveryView: View {
    var body: some View {
        Text("ICA Delivery View")
            .navigationTitle("ICA Delivery")
    }
}

struct AdditionalViews_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            ManageItemsView()
        }
    }
}
