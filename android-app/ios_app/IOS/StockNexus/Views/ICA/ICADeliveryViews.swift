import SwiftUI

// MARK: - ICA Deliveries List View
struct ICADeliveriesView: View {
    @State private var deliveries: [ICADelivery] = ICADelivery.sampleList
    @State private var searchText = ""
    @State private var selectedStatus: ICADelivery.DeliveryStatus? = nil
    @State private var showCreateSheet = false
    
    var filteredDeliveries: [ICADelivery] {
        var result = deliveries
        
        if !searchText.isEmpty {
            result = result.filter { $0.referenceNumber.localizedCaseInsensitiveContains(searchText) }
        }
        
        if let status = selectedStatus {
            result = result.filter { $0.status == status }
        }
        
        return result
    }
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Search and Filter
                VStack(spacing: 12) {
                    SearchBar(text: $searchText, placeholder: "Search deliveries...")
                    
                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) {
                            ICAFilterChip(title: "All", isSelected: selectedStatus == nil) {
                                selectedStatus = nil
                            }
                            
                            ForEach(ICADelivery.DeliveryStatus.allCases, id: \.self) { status in
                                ICAFilterChip(title: status.rawValue.capitalized, isSelected: selectedStatus == status) {
                                    selectedStatus = status
                                }
                            }
                        }
                    }
                }
                .padding()
                .background(Color(.systemBackground))
                
                // Deliveries List
                if filteredDeliveries.isEmpty {
                    EmptyStateView(
                        icon: "truck.box",
                        title: "No Deliveries",
                        message: "No deliveries found matching your criteria."
                    )
                } else {
                    List(filteredDeliveries) { delivery in
                        NavigationLink(destination: ICADeliveryDetailView(delivery: delivery)) {
                            ICADeliveryRow(delivery: delivery)
                        }
                    }
                    .listStyle(PlainListStyle())
                }
            }
            .navigationTitle("ICA Deliveries")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showCreateSheet = true }) {
                        Image(systemName: "plus")
                    }
                }
            }
            .sheet(isPresented: $showCreateSheet) {
                CreateDeliveryView()
            }
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
}

// MARK: - ICA Filter Chip (local to avoid conflicts)
struct ICAFilterChip: View {
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

// MARK: - ICA Delivery Row
struct ICADeliveryRow: View {
    let delivery: ICADelivery
    
    var body: some View {
        HStack(spacing: 12) {
            // Status Icon
            ZStack {
                Circle()
                    .fill(delivery.status.color.opacity(0.2))
                    .frame(width: 44, height: 44)
                
                Image(systemName: delivery.status == .delivered ? "checkmark" : "truck.box")
                    .foregroundColor(delivery.status.color)
            }
            
            // Delivery Info
            VStack(alignment: .leading, spacing: 4) {
                Text(delivery.referenceNumber)
                    .font(.headline)
                
                Text("\(delivery.items.count) items")
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                Text(delivery.scheduledDate.formatted(date: .abbreviated, time: .shortened))
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            ICAStatusBadge(status: delivery.status.rawValue.capitalized, color: delivery.status.color)
        }
        .padding(.vertical, 4)
    }
}

// MARK: - ICA Status Badge (local to avoid conflicts)
struct ICAStatusBadge: View {
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

// MARK: - ICA Delivery Detail View
struct ICADeliveryDetailView: View {
    let delivery: ICADelivery
    @State private var showReceiveSheet = false
    
    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Status Card
                VStack(spacing: 16) {
                    HStack {
                        Text("Delivery Status")
                            .font(.headline)
                        Spacer()
                        ICAStatusBadge(status: delivery.status.rawValue.capitalized, color: delivery.status.color)
                    }
                    
                    Divider()
                    
                    InfoRow(title: "Reference", value: delivery.referenceNumber, icon: "doc.text")
                    InfoRow(title: "Scheduled", value: delivery.scheduledDate.formatted(date: .long, time: .shortened), icon: "calendar")
                    
                    if let receivedDate = delivery.receivedDate {
                        InfoRow(title: "Received", value: receivedDate.formatted(date: .long, time: .shortened), icon: "checkmark.circle")
                    }
                }
                .padding()
                .background(Color(.systemBackground))
                .cornerRadius(12)
                .shadow(color: Color.black.opacity(0.05), radius: 5, x: 0, y: 2)
                
                // Items Section
                VStack(alignment: .leading, spacing: 12) {
                    Text("Delivery Items")
                        .font(.headline)
                    
                    ForEach(delivery.items) { item in
                        DeliveryItemRow(item: item)
                    }
                }
                .padding()
                .background(Color(.systemBackground))
                .cornerRadius(12)
                .shadow(color: Color.black.opacity(0.05), radius: 5, x: 0, y: 2)
                
                // Notes
                if let notes = delivery.notes, !notes.isEmpty {
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Notes")
                            .font(.headline)
                        
                        Text(notes)
                            .font(.body)
                            .foregroundColor(.secondary)
                    }
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding()
                    .background(Color(.systemBackground))
                    .cornerRadius(12)
                    .shadow(color: Color.black.opacity(0.05), radius: 5, x: 0, y: 2)
                }
                
                // Action Buttons
                if delivery.status == .pending || delivery.status == .inTransit {
                    PrimaryButton(title: "Receive Delivery") {
                        showReceiveSheet = true
                    }
                }
            }
            .padding()
        }
        .background(Color(.systemGroupedBackground))
        .navigationTitle("Delivery Details")
        .navigationBarTitleDisplayMode(.inline)
        .sheet(isPresented: $showReceiveSheet) {
            ReceiveDeliveryView(delivery: delivery)
        }
    }
}

// MARK: - Info Row
struct InfoRow: View {
    let title: String
    let value: String
    let icon: String
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundColor(.stockNexusRed)
                .frame(width: 24)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.caption)
                    .foregroundColor(.secondary)
                Text(value)
                    .font(.subheadline)
            }
            
            Spacer()
        }
    }
}

// MARK: - Delivery Item Row
struct DeliveryItemRow: View {
    let item: ICADelivery.DeliveryItem
    
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(item.itemName)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Text("Expected: \(item.expectedQuantity)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            if let received = item.receivedQuantity {
                VStack(alignment: .trailing, spacing: 4) {
                    Text("Received: \(received)")
                        .font(.caption)
                        .foregroundColor(received == item.expectedQuantity ? .green : .orange)
                    
                    if received != item.expectedQuantity {
                        Text("Variance: \(received - item.expectedQuantity)")
                            .font(.caption2)
                            .foregroundColor(.red)
                    }
                }
            }
        }
        .padding(.vertical, 8)
        .padding(.horizontal, 12)
        .background(Color(.systemGray6))
        .cornerRadius(8)
    }
}

// MARK: - Create Delivery View
struct CreateDeliveryView: View {
    @Environment(\.presentationMode) var presentationMode
    @State private var referenceNumber = ""
    @State private var scheduledDate = Date()
    @State private var notes = ""
    
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Delivery Information")) {
                    TextField("Reference Number", text: $referenceNumber)
                    DatePicker("Scheduled Date", selection: $scheduledDate)
                }
                
                Section(header: Text("Notes")) {
                    TextEditor(text: $notes)
                        .frame(height: 100)
                }
            }
            .navigationTitle("New Delivery")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        presentationMode.wrappedValue.dismiss()
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Create") {
                        presentationMode.wrappedValue.dismiss()
                    }
                    .disabled(referenceNumber.isEmpty)
                }
            }
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
}

// MARK: - Receive Delivery View
struct ReceiveDeliveryView: View {
    let delivery: ICADelivery
    @Environment(\.presentationMode) var presentationMode
    @State private var receivedQuantities: [String: Int] = [:]
    
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Items to Receive")) {
                    ForEach(delivery.items) { item in
                        HStack {
                            VStack(alignment: .leading) {
                                Text(item.itemName)
                                    .font(.subheadline)
                                Text("Expected: \(item.expectedQuantity)")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                            
                            Spacer()
                            
                            Stepper(value: Binding(
                                get: { receivedQuantities[item.id] ?? item.expectedQuantity },
                                set: { receivedQuantities[item.id] = $0 }
                            ), in: 0...999) {
                                Text("\(receivedQuantities[item.id] ?? item.expectedQuantity)")
                                    .font(.headline)
                            }
                        }
                    }
                }
            }
            .navigationTitle("Receive Delivery")
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
        .onAppear {
            for item in delivery.items {
                receivedQuantities[item.id] = item.expectedQuantity
            }
        }
    }
}

struct ICADeliveryViews_Previews: PreviewProvider {
    static var previews: some View {
        ICADeliveriesView()
    }
}
