import SwiftUI

// MARK: - Staff List View
struct StaffListView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @State private var users: [User] = [.sample, .sampleAdmin, .sampleStaff]
    @State private var searchText = ""
    @State private var selectedRole: UserRole?
    @State private var showAddStaff = false
    
    var filteredUsers: [User] {
        var result = users
        
        if !searchText.isEmpty {
            result = result.filter {
                $0.fullName.localizedCaseInsensitiveContains(searchText) ||
                $0.email.localizedCaseInsensitiveContains(searchText)
            }
        }
        
        if let role = selectedRole {
            result = result.filter { $0.role == role }
        }
        
        return result
    }
    
    var body: some View {
        VStack(spacing: 0) {
            // Role Filter
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 12) {
                    FilterChipView(title: "All", isSelected: selectedRole == nil) {
                        selectedRole = nil
                    }
                    
                    ForEach(UserRole.allCases, id: \.self) { role in
                        FilterChipView(title: role.displayName, isSelected: selectedRole == role) {
                            selectedRole = role
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
                TextField("Search staff", text: $searchText)
            }
            .padding(10)
            .background(Color(.systemGray6))
            .cornerRadius(10)
            .padding()
            
            // Staff List
            List {
                ForEach(filteredUsers) { user in
                    NavigationLink(destination: StaffDetailView(user: user)) {
                        StaffRow(user: user)
                    }
                }
            }
        }
        .navigationTitle("Staff Management")
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showAddStaff = true }) {
                    Image(systemName: "person.badge.plus")
                }
            }
        }
        .sheet(isPresented: $showAddStaff) {
            AddStaffSheet { newUser in
                users.append(newUser)
                showAddStaff = false
            }
        }
    }
}

// MARK: - Filter Chip View (Local to avoid conflicts)
struct FilterChipView: View {
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

// MARK: - Staff Row
struct StaffRow: View {
    let user: User
    
    var body: some View {
        HStack(spacing: 12) {
            // Avatar
            ZStack {
                Circle()
                    .fill(Color.stockNexusRed.opacity(0.1))
                    .frame(width: 45, height: 45)
                
                Text(user.fullName.prefix(1).uppercased())
                    .font(.headline)
                    .foregroundColor(.stockNexusRed)
            }
            
            // Info
            VStack(alignment: .leading, spacing: 4) {
                Text(user.fullName)
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Text(user.role.displayName)
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                if let position = user.position {
                    Text(position)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            }
            
            Spacer()
            
            // Status
            Circle()
                .fill(user.status == .active ? Color.green : (user.status == .pending ? Color.orange : Color.gray))
                .frame(width: 10, height: 10)
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Staff Detail View
struct StaffDetailView: View {
    let user: User
    @State private var showActivateAlert = false
    @State private var showDeactivateAlert = false
    
    var body: some View {
        Form {
            // Profile Section
            Section {
                HStack {
                    Spacer()
                    
                    VStack(spacing: 12) {
                        ZStack {
                            Circle()
                                .fill(Color.stockNexusRed.opacity(0.1))
                                .frame(width: 80, height: 80)
                            
                            Text(user.fullName.prefix(1).uppercased())
                                .font(.largeTitle)
                                .foregroundColor(.stockNexusRed)
                        }
                        
                        Text(user.fullName)
                            .font(.title2)
                            .fontWeight(.bold)
                        
                        Text(user.role.displayName)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    
                    Spacer()
                }
                .listRowBackground(Color.clear)
            }
            
            // Contact Info
            Section(header: Text("Contact Information")) {
                HStack {
                    Text("Email")
                    Spacer()
                    Text(user.email)
                        .foregroundColor(.secondary)
                }
                
                if let phone = user.phone {
                    HStack {
                        Text("Phone")
                        Spacer()
                        Text(phone)
                            .foregroundColor(.secondary)
                    }
                }
            }
            
            // Work Info
            Section(header: Text("Work Information")) {
                if let position = user.position {
                    HStack {
                        Text("Position")
                        Spacer()
                        Text(position)
                            .foregroundColor(.secondary)
                    }
                }
                
                if let branchName = user.branchName {
                    HStack {
                        Text("Branch")
                        Spacer()
                        Text(branchName)
                            .foregroundColor(.secondary)
                    }
                }
                
                HStack {
                    Text("Role")
                    Spacer()
                    Text(user.role.displayName)
                        .foregroundColor(.secondary)
                }
                
                HStack {
                    Text("Status")
                    Spacer()
                    Text(user.status.rawValue.capitalized)
                        .foregroundColor(statusColor)
                }
            }
            
            // Actions
            Section {
                if user.status == .pending {
                    Button("Activate Account") {
                        showActivateAlert = true
                    }
                    .foregroundColor(.green)
                } else if user.status == .active {
                    Button("Deactivate Account") {
                        showDeactivateAlert = true
                    }
                    .foregroundColor(.red)
                }
            }
        }
        .navigationTitle("Staff Details")
        .alert(isPresented: $showActivateAlert) {
            Alert(
                title: Text("Activate Account"),
                message: Text("Are you sure you want to activate this account?"),
                primaryButton: .default(Text("Activate")) {
                    // Activate user
                },
                secondaryButton: .cancel()
            )
        }
    }
    
    private var statusColor: Color {
        switch user.status {
        case .active: return .green
        case .pending: return .orange
        case .inactive: return .gray
        }
    }
}

// MARK: - Add Staff Sheet
struct AddStaffSheet: View {
    @Environment(\.presentationMode) var presentationMode
    let onCreate: (User) -> Void
    
    @State private var fullName = ""
    @State private var email = ""
    @State private var phone = ""
    @State private var position = ""
    @State private var selectedRole: UserRole = .staff
    
    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("Personal Information")) {
                    TextField("Full Name", text: $fullName)
                    TextField("Email", text: $email)
                        .keyboardType(.emailAddress)
                        .autocapitalization(.none)
                    TextField("Phone (Optional)", text: $phone)
                        .keyboardType(.phonePad)
                    TextField("Position (Optional)", text: $position)
                }
                
                Section(header: Text("Role Assignment")) {
                    Picker("Role", selection: $selectedRole) {
                        ForEach(UserRole.allCases, id: \.self) { role in
                            Text(role.displayName).tag(role)
                        }
                    }
                }
            }
            .navigationTitle("Add Staff Member")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") {
                        presentationMode.wrappedValue.dismiss()
                    }
                }
                
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Add") {
                        let newUser = User(
                            id: UUID().uuidString,
                            fullName: fullName,
                            email: email,
                            phone: phone.isEmpty ? nil : phone,
                            position: position.isEmpty ? nil : position,
                            role: selectedRole,
                            status: .pending,
                            branchId: "branch1",
                            branchName: "ICA Maxi Örnsköldsvik",
                            photoURL: nil,
                            createdAt: Date(),
                            updatedAt: Date()
                        )
                        onCreate(newUser)
                    }
                    .disabled(fullName.isEmpty || email.isEmpty)
                }
            }
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
}

// MARK: - Organization Management View
struct OrganizationManagementView: View {
    @State private var selectedTab = 0
    
    var body: some View {
        VStack(spacing: 0) {
            Picker("", selection: $selectedTab) {
                Text("Regions").tag(0)
                Text("Districts").tag(1)
                Text("Branches").tag(2)
            }
            .pickerStyle(.segmented)
            .padding()
            
            switch selectedTab {
            case 0:
                RegionListView()
            case 1:
                DistrictListView()
            case 2:
                BranchListView()
            default:
                EmptyView()
            }
        }
        .navigationTitle("Organization")
    }
}

// MARK: - Region List View
struct RegionListView: View {
    @State private var regions = Region.samples
    @State private var showAddSheet = false
    
    var body: some View {
        List {
            ForEach(regions) { region in
                Text(region.name)
            }
            .onDelete { offsets in
                regions.remove(atOffsets: offsets)
            }
        }
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Button(action: { showAddSheet = true }) {
                    Image(systemName: "plus")
                }
            }
        }
        .sheet(isPresented: $showAddSheet) {
            AddRegionSheet { name in
                let region = Region(id: UUID().uuidString, name: name, createdAt: Date(), updatedAt: Date())
                regions.append(region)
                showAddSheet = false
            }
        }
    }
}

struct AddRegionSheet: View {
    @Environment(\.presentationMode) var presentationMode
    let onCreate: (String) -> Void
    @State private var name = ""
    
    var body: some View {
        NavigationView {
            Form {
                TextField("Region Name", text: $name)
            }
            .navigationTitle("Add Region")
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Cancel") { presentationMode.wrappedValue.dismiss() }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Add") { onCreate(name) }
                        .disabled(name.isEmpty)
                }
            }
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
}

// MARK: - District List View
struct DistrictListView: View {
    @State private var districts = District.samples
    
    var body: some View {
        List {
            ForEach(districts) { district in
                VStack(alignment: .leading) {
                    Text(district.name)
                        .font(.headline)
                    if let regionName = district.regionName {
                        Text(regionName)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                }
            }
        }
    }
}

// MARK: - Branch List View
struct BranchListView: View {
    @State private var branches = Branch.samples
    
    var body: some View {
        List {
            ForEach(branches) { branch in
                VStack(alignment: .leading, spacing: 4) {
                    Text(branch.name)
                        .font(.headline)
                    if let address = branch.address {
                        Text(address)
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    if let districtName = branch.districtName {
                        Text(districtName)
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                }
            }
        }
    }
}

// MARK: - Analytics View
struct AnalyticsView: View {
    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Stock Distribution Chart
                ChartCard(title: "Stock Distribution") {
                    // Pie chart placeholder
                    HStack(spacing: 20) {
                        ForEach(ItemCategory.allCases.prefix(4), id: \.self) { category in
                            VStack {
                                Circle()
                                    .fill(randomColor)
                                    .frame(width: 40, height: 40)
                                Text(category.icon)
                                    .font(.caption)
                            }
                        }
                    }
                }
                
                // Weekly Movement Chart
                ChartCard(title: "Weekly Stock Movement") {
                    // Bar chart placeholder
                    HStack(alignment: .bottom, spacing: 8) {
                        ForEach(0..<7, id: \.self) { day in
                            VStack {
                                RoundedRectangle(cornerRadius: 4)
                                    .fill(Color.stockNexusRed)
                                    .frame(width: 30, height: CGFloat.random(in: 50...150))
                                Text(["M", "T", "W", "T", "F", "S", "S"][day])
                                    .font(.caption2)
                            }
                        }
                    }
                }
                
                // Stock Alerts
                ChartCard(title: "Stock Level Summary") {
                    HStack(spacing: 20) {
                        StatBox(title: "Normal", value: "45", color: .green)
                        StatBox(title: "Low", value: "12", color: .orange)
                        StatBox(title: "Critical", value: "3", color: .red)
                    }
                }
            }
            .padding()
        }
        .navigationTitle("Analytics")
    }
    
    private var randomColor: Color {
        Color(red: .random(in: 0.3...0.8), green: .random(in: 0.3...0.8), blue: .random(in: 0.3...0.8))
    }
}

struct ChartCard<Content: View>: View {
    let title: String
    @ViewBuilder let content: Content
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text(title)
                .font(.headline)
            
            content
        }
        .padding()
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.secondaryBackground)
        .cornerRadius(12)
    }
}

struct StatBox: View {
    let title: String
    let value: String
    let color: Color
    
    var body: some View {
        VStack {
            Text(value)
                .font(.title)
                .fontWeight(.bold)
                .foregroundColor(color)
            Text(title)
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(color.opacity(0.1))
        .cornerRadius(8)
    }
}

// MARK: - More Reports View (renamed to avoid conflict)
struct MoreReportsView: View {
    let reportTypes = [
        ("Stock Report", "shippingbox"),
        ("Movement Report", "arrow.left.arrow.right"),
        ("Low Stock Alert", "exclamationmark.triangle"),
        ("Expiry Report", "calendar.badge.exclamationmark"),
        ("Usage Analytics", "chart.bar")
    ]
    
    var body: some View {
        List {
            ForEach(reportTypes, id: \.0) { report in
                NavigationLink(destination: ReportDetailView(reportName: report.0)) {
                    Label(report.0, systemImage: report.1)
                }
            }
        }
        .navigationTitle("Reports")
    }
}

struct ReportDetailView: View {
    let reportName: String
    @State private var startDate = Calendar.current.date(byAdding: .day, value: -30, to: Date())!
    @State private var endDate = Date()
    
    var body: some View {
        VStack {
            // Date Range
            HStack {
                DatePicker("Start", selection: $startDate, displayedComponents: .date)
                DatePicker("End", selection: $endDate, displayedComponents: .date)
            }
            .padding()
            
            // Report Content Placeholder
            VStack(spacing: 20) {
                Image(systemName: "doc.text")
                    .font(.system(size: 60))
                    .foregroundColor(.secondary)
                
                Text("Report Preview")
                    .font(.headline)
                
                Text("Select date range and tap Generate to create report")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            
            // Generate Button
            Button(action: {
                // Generate PDF
            }) {
                Label("Generate PDF", systemImage: "square.and.arrow.up")
                    .stockNexusPrimaryButton()
            }
            .padding()
        }
        .navigationTitle(reportName)
    }
}

// MARK: - Activity Logs View
struct ActivityLogsView: View {
    @State private var logs = ActivityLog.samples
    @State private var selectedCategory: ActivityCategory = .all
    
    var filteredLogs: [ActivityLog] {
        if selectedCategory == .all {
            return logs
        }
        return logs.filter { $0.type.category == selectedCategory }
    }
    
    var body: some View {
        VStack(spacing: 0) {
            // Category Filter
            Picker("Category", selection: $selectedCategory) {
                ForEach(ActivityCategory.allCases, id: \.self) { category in
                    Text(category.displayName).tag(category)
                }
            }
            .pickerStyle(.segmented)
            .padding()
            
            // Logs List
            List(filteredLogs) { log in
                VStack(alignment: .leading, spacing: 8) {
                    HStack {
                        Image(systemName: log.type.icon)
                            .foregroundColor(.stockNexusRed)
                        
                        Text(log.type.displayName)
                            .font(.caption)
                            .fontWeight(.medium)
                        
                        Spacer()
                        
                        Text(log.createdAt.timeAgo)
                            .font(.caption2)
                            .foregroundColor(.secondary)
                    }
                    
                    Text(log.description)
                        .font(.subheadline)
                    
                    Text("by \(log.userName)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding(.vertical, 4)
            }
        }
        .navigationTitle("Activity Logs")
    }
}

// MARK: - More Settings View (renamed to avoid conflict)
struct MoreSettingsView: View {
    @AppStorage("isDarkMode") private var isDarkMode = false
    @AppStorage("notificationsEnabled") private var notificationsEnabled = true
    
    var body: some View {
        Form {
            Section(header: Text("Appearance")) {
                Toggle("Dark Mode", isOn: $isDarkMode)
            }
            
            Section(header: Text("Notifications")) {
                Toggle("Push Notifications", isOn: $notificationsEnabled)
            }
            
            Section(header: Text("About")) {
                HStack {
                    Text("Version")
                    Spacer()
                    Text(AppConstants.appVersion)
                        .foregroundColor(.secondary)
                }
                
                HStack {
                    Text("Build")
                    Spacer()
                    Text("1")
                        .foregroundColor(.secondary)
                }
            }
            
            Section {
                Link("Privacy Policy", destination: URL(string: "https://stocknexus.com/privacy")!)
                Link("Terms of Service", destination: URL(string: "https://stocknexus.com/terms")!)
            }
        }
        .navigationTitle("Settings")
    }
}

// MARK: - Messaging View
struct MessagingView: View {
    @State private var chatRooms = ChatRoom.samples
    
    var body: some View {
        List(chatRooms) { room in
            NavigationLink(destination: ChatView(room: room)) {
                HStack(spacing: 12) {
                    ZStack {
                        Circle()
                            .fill(Color.stockNexusRed.opacity(0.1))
                            .frame(width: 45, height: 45)
                        
                        Image(systemName: room.isGroup ? "person.3.fill" : "person.fill")
                            .foregroundColor(.stockNexusRed)
                    }
                    
                    VStack(alignment: .leading, spacing: 4) {
                        Text(room.name ?? "Chat")
                            .font(.subheadline)
                            .fontWeight(.medium)
                        
                        if let lastMessage = room.lastMessage {
                            Text(lastMessage)
                                .font(.caption)
                                .foregroundColor(.secondary)
                                .lineLimit(1)
                        }
                    }
                    
                    Spacer()
                    
                    VStack(alignment: .trailing, spacing: 4) {
                        if let lastMessageAt = room.lastMessageAt {
                            Text(lastMessageAt.timeAgo)
                                .font(.caption2)
                                .foregroundColor(.secondary)
                        }
                        
                        if room.unreadCount > 0 {
                            Text("\(room.unreadCount)")
                                .font(.caption2)
                                .padding(.horizontal, 6)
                                .padding(.vertical, 2)
                                .background(Color.stockNexusRed)
                                .foregroundColor(.white)
                                .cornerRadius(10)
                        }
                    }
                }
            }
        }
        .navigationTitle("Messages")
    }
}

// MARK: - Chat View
struct ChatView: View {
    let room: ChatRoom
    @State private var messages = ChatMessage.samples
    @State private var newMessage = ""
    
    var body: some View {
        VStack(spacing: 0) {
            // Messages
            ScrollView {
                LazyVStack(spacing: 12) {
                    ForEach(messages) { message in
                        MessageBubble(message: message, isCurrentUser: message.senderId == "1")
                    }
                }
                .padding()
            }
            
            // Input
            HStack(spacing: 12) {
                TextField("Type a message...", text: $newMessage)
                    .textFieldStyle(RoundedBorderTextFieldStyle())
                
                Button(action: sendMessage) {
                    Image(systemName: "paperplane.fill")
                        .foregroundColor(.white)
                        .padding(10)
                        .background(Color.stockNexusRed)
                        .cornerRadius(20)
                }
                .disabled(newMessage.isEmpty)
            }
            .padding()
            .background(Color.secondaryBackground)
        }
        .navigationTitle(room.name ?? "Chat")
        .navigationBarTitleDisplayMode(.inline)
    }
    
    private func sendMessage() {
        let message = ChatMessage(
            id: UUID().uuidString,
            content: newMessage,
            senderId: "1",
            senderName: "Current User",
            senderPhotoURL: nil,
            chatRoomId: room.id,
            createdAt: Date(),
            isRead: false,
            readBy: nil
        )
        messages.append(message)
        newMessage = ""
    }
}

struct MessageBubble: View {
    let message: ChatMessage
    let isCurrentUser: Bool
    
    var body: some View {
        HStack {
            if isCurrentUser { Spacer() }
            
            VStack(alignment: isCurrentUser ? .trailing : .leading, spacing: 4) {
                if !isCurrentUser {
                    Text(message.senderName)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
                
                Text(message.content)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(isCurrentUser ? Color.stockNexusRed : Color.secondaryBackground)
                    .foregroundColor(isCurrentUser ? .white : .primary)
                    .cornerRadius(16)
                
                Text(message.createdAt.formatted(as: "h:mm a"))
                    .font(.caption2)
                    .foregroundColor(.secondary)
            }
            
            if !isCurrentUser { Spacer() }
        }
    }
}

struct ManagementViews_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            StaffListView()
                .environmentObject(AuthViewModel())
        }
    }
}
