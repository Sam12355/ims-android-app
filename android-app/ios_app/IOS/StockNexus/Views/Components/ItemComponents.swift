import SwiftUI

// MARK: - Item Card View
struct ItemCardView: View {
    let item: Item
    var onTap: (() -> Void)? = nil
    
    var body: some View {
        Button(action: { onTap?() }) {
            HStack(spacing: 12) {
                // Item Image/Icon
                ZStack {
                    RoundedRectangle(cornerRadius: 8)
                        .fill(Color(.systemGray6))
                        .frame(width: 60, height: 60)
                    
                    if let imageUrl = item.imageUrl, !imageUrl.isEmpty {
                        AsyncImage(url: URL(string: imageUrl)) { image in
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fill)
                        } placeholder: {
                            Image(systemName: "cube.box")
                                .foregroundColor(.secondary)
                        }
                        .frame(width: 60, height: 60)
                        .clipShape(RoundedRectangle(cornerRadius: 8))
                    } else {
                        Image(systemName: "cube.box")
                            .font(.title2)
                            .foregroundColor(.secondary)
                    }
                }
                
                // Item Details
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.name)
                        .font(.headline)
                        .foregroundColor(.primary)
                        .lineLimit(1)
                    
                    Text(item.category.rawValue)
                        .font(.caption)
                        .foregroundColor(.secondary)
                    
                    HStack {
                        Text("Qty: \(item.currentStock)")
                            .font(.subheadline)
                            .foregroundColor(item.stockLevel.color)
                        
                        Spacer()
                        
                        ItemStatusBadge(
                            status: item.stockLevel.rawValue.capitalized,
                            color: item.stockLevel.color
                        )
                    }
                }
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding()
            .background(Color(.systemBackground))
            .cornerRadius(12)
            .shadow(color: Color.black.opacity(0.05), radius: 5, x: 0, y: 2)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - Item Status Badge (local)
struct ItemStatusBadge: View {
    let status: String
    let color: Color
    
    var body: some View {
        Text(status)
            .font(.caption)
            .fontWeight(.medium)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(color.opacity(0.1))
            .foregroundColor(color)
            .cornerRadius(4)
    }
}

// MARK: - Item List Row
struct ItemListRow: View {
    let item: Item
    @Binding var selectedQuantity: Int
    var showQuantitySelector: Bool = true
    
    var body: some View {
        HStack(spacing: 12) {
            // Item Image
            ZStack {
                RoundedRectangle(cornerRadius: 8)
                    .fill(Color(.systemGray6))
                    .frame(width: 50, height: 50)
                
                Image(systemName: "cube.box")
                    .foregroundColor(.secondary)
            }
            
            // Item Info
            VStack(alignment: .leading, spacing: 2) {
                Text(item.name)
                    .font(.body)
                    .fontWeight(.medium)
                
                Text("Available: \(item.currentStock)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            if showQuantitySelector {
                QuantityStepper(value: $selectedQuantity, maxValue: item.currentStock)
            }
        }
        .padding(.vertical, 8)
    }
}

// MARK: - Item Detail View
struct ItemDetailView: View {
    let item: Item
    @Environment(\.presentationMode) private var presentationMode
    @State private var stockOutQuantity = 1
    @State private var showStockOutSheet = false
    
    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Item Image
                ZStack {
                    RoundedRectangle(cornerRadius: 16)
                        .fill(Color(.systemGray6))
                        .frame(height: 200)
                    
                    if let imageUrl = item.imageUrl, !imageUrl.isEmpty {
                        AsyncImage(url: URL(string: imageUrl)) { image in
                            image
                                .resizable()
                                .aspectRatio(contentMode: .fit)
                        } placeholder: {
                            ProgressView()
                        }
                        .frame(height: 200)
                        .clipShape(RoundedRectangle(cornerRadius: 16))
                    } else {
                        Image(systemName: "cube.box")
                            .font(.system(size: 60))
                            .foregroundColor(.secondary)
                    }
                }
                .padding(.horizontal)
                
                // Item Info Card
                VStack(spacing: 16) {
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(item.name)
                                .font(.title2)
                                .fontWeight(.bold)
                            
                            Text(item.category.rawValue)
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        
                        Spacer()
                        
                        ItemStatusBadge(
                            status: item.stockLevel.rawValue.capitalized,
                            color: item.stockLevel.color
                        )
                    }
                    
                    Divider()
                    
                    // Stock Info
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("Current Stock")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            Text("\(item.currentStock)")
                                .font(.title)
                                .fontWeight(.bold)
                                .foregroundColor(item.stockLevel.color)
                        }
                        
                        Spacer()
                        
                        VStack(alignment: .trailing, spacing: 4) {
                            Text("Threshold")
                                .font(.caption)
                                .foregroundColor(.secondary)
                            Text("\(item.thresholdLevel)")
                                .font(.title)
                                .fontWeight(.bold)
                        }
                    }
                    
                    if let barcode = item.barcode {
                        Divider()
                        
                        HStack {
                            Image(systemName: "barcode")
                                .foregroundColor(.secondary)
                            Text(barcode)
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                    }
                }
                .padding()
                .background(Color(.systemBackground))
                .cornerRadius(12)
                .shadow(color: Color.black.opacity(0.05), radius: 5, x: 0, y: 2)
                .padding(.horizontal)
                
                // Action Buttons
                VStack(spacing: 12) {
                    PrimaryButton(title: "Stock Out") {
                        showStockOutSheet = true
                    }
                    
                    SecondaryButton(title: "View History") {
                        // View history
                    }
                }
                .padding(.horizontal)
            }
            .padding(.vertical)
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle("Item Details")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showStockOutSheet) {
            QuickStockOutSheet(item: item)
        }
    }
}

// MARK: - Quick Stock Out Sheet
struct QuickStockOutSheet: View {
    let item: Item
    @Environment(\.presentationMode) var presentationMode
    @State private var quantity = 1
    @State private var notes = ""
    
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Item")) {
                    HStack {
                        Text(item.name)
                            .font(.headline)
                        Spacer()
                        Text("Available: \(item.currentStock)")
                            .foregroundColor(.secondary)
                    }
                }
                
                Section(header: Text("Quantity")) {
                    Stepper(value: $quantity, in: 1...item.currentStock) {
                        HStack {
                            Text("Quantity")
                            Spacer()
                            Text("\(quantity)")
                                .font(.headline)
                        }
                    }
                }
                
                Section(header: Text("Notes (Optional)")) {
                    TextEditor(text: $notes)
                        .frame(height: 80)
                }
            }
            .navigationTitle("Stock Out")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        presentationMode.wrappedValue.dismiss()
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Confirm") {
                        presentationMode.wrappedValue.dismiss()
                    }
                }
            }
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
}

// MARK: - Item Selection Row
struct ItemSelectionRow: View {
    let item: Item
    let isSelected: Bool
    let onSelect: () -> Void
    
    var body: some View {
        Button(action: onSelect) {
            HStack(spacing: 12) {
                // Selection indicator
                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .font(.title3)
                    .foregroundColor(isSelected ? .stockNexusRed : .secondary)
                
                // Item info
                VStack(alignment: .leading, spacing: 4) {
                    Text(item.name)
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundColor(.primary)
                    
                    Text(item.category.rawValue)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                // Stock level
                VStack(alignment: .trailing, spacing: 4) {
                    Text("\(item.currentStock)")
                        .font(.headline)
                        .foregroundColor(item.stockLevel.color)
                    
                    Text("in stock")
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            }
            .padding(.vertical, 8)
        }
        .buttonStyle(PlainButtonStyle())
    }
}

// MARK: - Compact Item Row
struct CompactItemRow: View {
    let item: Item
    
    var body: some View {
        HStack(spacing: 12) {
            Text(item.category.icon)
                .font(.title2)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(item.name)
                    .font(.subheadline)
                    .fontWeight(.medium)
            }
            
            Spacer()
            
            Text("\(item.currentStock)")
                .font(.headline)
                .foregroundColor(item.stockLevel.color)
        }
    }
}

struct ItemComponents_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            ItemDetailView(item: Item.samples.first!)
        }
    }
}
