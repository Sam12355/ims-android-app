import SwiftUI

struct MoveoutListView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @State private var selectedTab: MoveoutTab = .active
    @State private var moveoutLists: [MoveoutList] = MoveoutList.samples
    @State private var showGenerateSheet = false
    @State private var expandedListId: String?
    
    enum MoveoutTab: String, CaseIterable {
        case active = "Active"
        case completed = "Completed"
    }
    
    var filteredLists: [MoveoutList] {
        moveoutLists.filter { list in
            switch selectedTab {
            case .active:
                return list.status == .active
            case .completed:
                return list.status == .completed
            }
        }
    }
    
    var body: some View {
        NavigationView {
            VStack(spacing: 0) {
                // Tab Picker
                Picker("Tab", selection: $selectedTab) {
                    ForEach(MoveoutTab.allCases, id: \.self) { tab in
                        Text(tab.rawValue).tag(tab)
                    }
                }
                .pickerStyle(.segmented)
                .padding()
                
                // Lists
                if filteredLists.isEmpty {
                    emptyStateView
                } else {
                    listsView
                }
            }
            .navigationTitle("Moveout Lists")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: { showGenerateSheet = true }) {
                        Image(systemName: "plus.circle.fill")
                            .foregroundColor(.stockNexusRed)
                    }
                }
            }
            .sheet(isPresented: $showGenerateSheet) {
                GenerateMoveoutSheet { newList in
                    moveoutLists.insert(newList, at: 0)
                    showGenerateSheet = false
                }
            }
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
    
    // MARK: - Empty State
    private var emptyStateView: some View {
        VStack(spacing: 16) {
            Spacer()
            
            Image(systemName: "list.bullet.rectangle")
                .font(.system(size: 60))
                .foregroundColor(.secondary)
            
            Text("No \(selectedTab.rawValue.lowercased()) moveout lists")
                .font(.headline)
                .foregroundColor(.secondary)
            
            if selectedTab == .active {
                Button(action: { showGenerateSheet = true }) {
                    Text("Generate New List")
                        .stockNexusSecondaryButton()
                        .frame(maxWidth: 200)
                }
            }
            
            Spacer()
        }
    }
    
    // MARK: - Lists View
    private var listsView: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                ForEach(filteredLists) { list in
                    MoveoutListCardView(
                        list: list,
                        isExpanded: expandedListId == list.id
                    ) {
                        withAnimation {
                            if expandedListId == list.id {
                                expandedListId = nil
                            } else {
                                expandedListId = list.id
                            }
                        }
                    } onItemComplete: { itemId, quantity in
                        completeItem(listId: list.id, itemId: itemId, quantity: quantity)
                    }
                }
            }
            .padding()
        }
    }
    
    // MARK: - Complete Item
    private func completeItem(listId: String, itemId: String, quantity: Int) {
        guard let listIndex = moveoutLists.firstIndex(where: { $0.id == listId }),
              let itemIndex = moveoutLists[listIndex].items.firstIndex(where: { $0.id == itemId }) else {
            return
        }
        
        moveoutLists[listIndex].items[itemIndex].isCompleted = true
        moveoutLists[listIndex].items[itemIndex].completedQuantity = quantity
        moveoutLists[listIndex].items[itemIndex].completedAt = Date()
        moveoutLists[listIndex].items[itemIndex].completedByName = authViewModel.currentUser?.fullName
        
        // Check if all items are completed
        if moveoutLists[listIndex].isFullyCompleted {
            moveoutLists[listIndex].status = .completed
            moveoutLists[listIndex].completedAt = Date()
        }
    }
}

// MARK: - Moveout List Card View
struct MoveoutListCardView: View {
    let list: MoveoutList
    let isExpanded: Bool
    let onToggle: () -> Void
    let onItemComplete: (String, Int) -> Void
    
    @State private var showProcessDialog = false
    @State private var selectedItem: MoveoutItem?
    @State private var completedQuantity = ""
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            // Header
            Button(action: onToggle) {
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Text(list.title)
                            .font(.headline)
                            .foregroundColor(.primary)
                        
                        Spacer()
                        
                        Image(systemName: isExpanded ? "chevron.up" : "chevron.down")
                            .foregroundColor(.secondary)
                    }
                    
                    Text("Branch: \(list.branchName ?? "Unknown")")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                    
                    // Progress Bar
                    HStack {
                        ProgressView(value: list.progress)
                            .accentColor(.stockNexusRed)
                        
                        Text("\(list.completedItemsCount)/\(list.totalItemsCount) items")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    
                    Text("Created by: \(list.createdByName ?? "Unknown")")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            .buttonStyle(PlainButtonStyle())
            
            // Expanded Items
            if isExpanded {
                Divider()
                
                ForEach(list.items) { item in
                    MoveoutItemRow(item: item) {
                        selectedItem = item
                        completedQuantity = "\(item.requestedQuantity)"
                        showProcessDialog = true
                    }
                }
            }
        }
        .padding()
        .background(Color.secondaryBackground)
        .cornerRadius(12)
        .alert(isPresented: $showProcessDialog) {
            Alert(
                title: Text("Complete Item"),
                message: Text("Mark \(selectedItem?.itemName ?? "") as completed?"),
                primaryButton: .default(Text("Complete")) {
                    if let item = selectedItem {
                        onItemComplete(item.id, Int(completedQuantity) ?? item.requestedQuantity)
                    }
                },
                secondaryButton: .cancel()
            )
        }
    }
}

// MARK: - Moveout Item Row
struct MoveoutItemRow: View {
    let item: MoveoutItem
    let onComplete: () -> Void
    
    var body: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(item.itemName)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .strikethrough(item.isCompleted)
                    .foregroundColor(item.isCompleted ? .secondary : .primary)
                
                Text("Qty: \(item.requestedQuantity)")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            if item.isCompleted {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundColor(.green)
            } else {
                Button(action: onComplete) {
                    Text("Complete")
                        .font(.caption)
                        .fontWeight(.medium)
                        .padding(.horizontal, 12)
                        .padding(.vertical, 6)
                        .background(Color.stockNexusRed)
                        .foregroundColor(.white)
                        .cornerRadius(16)
                }
            }
        }
        .padding(.vertical, 8)
    }
}

// MARK: - Generate Moveout Sheet
struct GenerateMoveoutSheet: View {
    @Environment(\.presentationMode) var presentationMode
    let onCreate: (MoveoutList) -> Void
    
    @State private var title = ""
    @State private var selectedItems: Set<String> = []
    @State private var items = Item.samples
    
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("List Details")) {
                    TextField("Title", text: $title)
                }
                
                Section(header: Text("Select Items")) {
                    ForEach(items) { item in
                        Button(action: {
                            if selectedItems.contains(item.id) {
                                selectedItems.remove(item.id)
                            } else {
                                selectedItems.insert(item.id)
                            }
                        }) {
                            HStack {
                                Text(item.name)
                                    .foregroundColor(.primary)
                                
                                Spacer()
                                
                                if selectedItems.contains(item.id) {
                                    Image(systemName: "checkmark")
                                        .foregroundColor(.stockNexusRed)
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle("Generate Moveout List")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        presentationMode.wrappedValue.dismiss()
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Create") {
                        let moveoutItems = items.filter { selectedItems.contains($0.id) }.map { item in
                            MoveoutItem(
                                id: UUID().uuidString,
                                itemId: item.id,
                                itemName: item.name,
                                requestedQuantity: 1,
                                completedQuantity: nil,
                                isCompleted: false,
                                completedAt: nil,
                                completedByName: nil
                            )
                        }
                        
                        let newList = MoveoutList(
                            id: UUID().uuidString,
                            title: title,
                            branchId: "branch1",
                            branchName: "ICA Maxi",
                            status: .active,
                            items: moveoutItems,
                            createdById: "user1",
                            createdByName: "Current User",
                            createdAt: Date(),
                            updatedAt: Date(),
                            completedAt: nil
                        )
                        onCreate(newList)
                    }
                    .disabled(title.isEmpty || selectedItems.isEmpty)
                }
            }
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
}

struct MoveoutListView_Previews: PreviewProvider {
    static var previews: some View {
        MoveoutListView()
            .environmentObject(AuthViewModel())
    }
}
