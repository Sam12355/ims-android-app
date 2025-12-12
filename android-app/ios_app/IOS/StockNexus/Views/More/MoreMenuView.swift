import SwiftUI

struct MoreMenuView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    
    var body: some View {
        NavigationView {
            List {
                // User Profile Section
                Section {
                    NavigationLink(destination: ProfileView()) {
                        HStack(spacing: 16) {
                            ZStack {
                                Circle()
                                    .fill(Color.stockNexusRed.opacity(0.1))
                                    .frame(width: 50, height: 50)
                                
                                Text(authViewModel.currentUser?.fullName.prefix(1).uppercased() ?? "U")
                                    .font(.title2)
                                    .fontWeight(.semibold)
                                    .foregroundColor(.stockNexusRed)
                            }
                            
                            VStack(alignment: .leading, spacing: 4) {
                                Text(authViewModel.currentUser?.fullName ?? "User")
                                    .font(.headline)
                                
                                Text(authViewModel.currentUser?.role.displayName ?? "")
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                            }
                        }
                        .padding(.vertical, 8)
                    }
                }
                
                // Inventory Section (Role-based)
                if authViewModel.currentUser?.role.canManageItems == true {
                    Section(header: Text("Inventory")) {
                        NavigationLink(destination: ManageItemsView()) {
                            Label("Manage Items", systemImage: "cube.box")
                        }
                        
                        NavigationLink(destination: StockInView()) {
                            Label("Stock In", systemImage: "plus.circle")
                        }
                    }
                }
                
                // Operations Section
                Section(header: Text("Operations")) {
                    if authViewModel.currentUser?.role.canManageICADelivery == true {
                        NavigationLink(destination: ICADeliveryView()) {
                            Label("ICA Delivery", systemImage: "shippingbox")
                        }
                    }
                }
                
                // Management Section (Admin/Manager)
                if authViewModel.currentUser?.role.canManageStaff == true {
                    Section(header: Text("Management")) {
                        NavigationLink(destination: StaffListView()) {
                            Label("Staff Management", systemImage: "person.3")
                        }
                        
                        if authViewModel.currentUser?.role.canManageBranches == true {
                            NavigationLink(destination: OrganizationManagementView()) {
                                Label("Organization", systemImage: "building.2")
                            }
                        }
                    }
                }
                
                // Reports Section
                if authViewModel.currentUser?.role.canViewAnalytics == true {
                    Section(header: Text("Reports")) {
                        NavigationLink(destination: AnalyticsView()) {
                            Label("Analytics", systemImage: "chart.pie")
                        }
                        
                        NavigationLink(destination: MoreReportsView()) {
                            Label("Reports", systemImage: "doc.text.magnifyingglass")
                        }
                        
                        if authViewModel.currentUser?.role.canViewActivityLogs == true {
                            NavigationLink(destination: ActivityLogsView()) {
                                Label("Activity Logs", systemImage: "list.bullet.rectangle")
                            }
                        }
                    }
                }
                
                // Settings Section
                Section(header: Text("Settings")) {
                    NavigationLink(destination: MoreSettingsView()) {
                        Label("Settings", systemImage: "gear")
                    }
                    
                    NavigationLink(destination: MessagingView()) {
                        Label("Messages", systemImage: "message")
                    }
                }
                
                // Sign Out Section
                Section {
                    Button(action: {
                        Task {
                            await authViewModel.signOut()
                        }
                    }) {
                        Label("Sign Out", systemImage: "rectangle.portrait.and.arrow.right")
                            .foregroundColor(.red)
                    }
                }
            }
            .navigationTitle("More")
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
}

// MARK: - Profile View
struct ProfileView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @State private var fullName = ""
    @State private var phone = ""
    @State private var position = ""
    @State private var isSaving = false
    @State private var showSuccess = false
    
    var body: some View {
        Form {
            // Profile Picture Section
            Section {
                HStack {
                    Spacer()
                    
                    ZStack {
                        Circle()
                            .fill(Color.stockNexusRed.opacity(0.1))
                            .frame(width: 100, height: 100)
                        
                        Text(authViewModel.currentUser?.fullName.prefix(1).uppercased() ?? "U")
                            .font(.system(size: 40))
                            .fontWeight(.semibold)
                            .foregroundColor(.stockNexusRed)
                    }
                    
                    Spacer()
                }
                .listRowBackground(Color.clear)
            }
            
            // Personal Info
            Section(header: Text("Personal Information")) {
                TextField("Full Name", text: $fullName)
                
                HStack {
                    Text("Email")
                    Spacer()
                    Text(authViewModel.currentUser?.email ?? "")
                        .foregroundColor(.secondary)
                }
                
                TextField("Phone", text: $phone)
                    .keyboardType(.phonePad)
                
                TextField("Position", text: $position)
            }
            
            // Role & Branch Info
            Section(header: Text("Account Information")) {
                HStack {
                    Text("Role")
                    Spacer()
                    Text(authViewModel.currentUser?.role.displayName ?? "")
                        .foregroundColor(.secondary)
                }
                
                HStack {
                    Text("Branch")
                    Spacer()
                    Text(authViewModel.currentUser?.branchName ?? "Not assigned")
                        .foregroundColor(.secondary)
                }
                
                HStack {
                    Text("Status")
                    Spacer()
                    Text(authViewModel.currentUser?.status.rawValue.capitalized ?? "")
                        .foregroundColor(authViewModel.currentUser?.status == .active ? .green : .orange)
                }
            }
            
            // Save Button
            Section {
                Button(action: saveProfile) {
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
            
            // Change Password
            Section {
                NavigationLink(destination: ChangePasswordView()) {
                    Text("Change Password")
                }
            }
        }
        .navigationTitle("Profile")
        .onAppear {
            fullName = authViewModel.currentUser?.fullName ?? ""
            phone = authViewModel.currentUser?.phone ?? ""
            position = authViewModel.currentUser?.position ?? ""
        }
        .alert(isPresented: $showSuccess) {
            Alert(
                title: Text("Success"),
                message: Text("Profile updated successfully!"),
                dismissButton: .default(Text("OK"))
            )
        }
    }
    
    private func saveProfile() {
        isSaving = true
        
        // Simulate API call
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            isSaving = false
            showSuccess = true
        }
    }
}

// MARK: - Change Password View
struct ChangePasswordView: View {
    @State private var currentPassword = ""
    @State private var newPassword = ""
    @State private var confirmPassword = ""
    @State private var isSaving = false
    @State private var showSuccess = false
    @State private var errorMessage: String?
    @State private var showError = false
    
    var body: some View {
        Form {
            Section(header: Text("Current Password")) {
                SecureField("Enter current password", text: $currentPassword)
            }
            
            Section(header: Text("New Password")) {
                SecureField("Enter new password", text: $newPassword)
                SecureField("Confirm new password", text: $confirmPassword)
            }
            
            Section {
                Button(action: changePassword) {
                    HStack {
                        Spacer()
                        if isSaving {
                            ProgressView()
                        } else {
                            Text("Change Password")
                        }
                        Spacer()
                    }
                }
                .disabled(isSaving || currentPassword.isEmpty || newPassword.isEmpty || confirmPassword.isEmpty)
            }
            
            if let error = errorMessage {
                Section {
                    Text(error)
                        .foregroundColor(.red)
                }
            }
        }
        .navigationTitle("Change Password")
        .alert(isPresented: $showSuccess) {
            Alert(
                title: Text("Success"),
                message: Text("Password changed successfully!"),
                dismissButton: .default(Text("OK"))
            )
        }
    }
    
    private func changePassword() {
        guard newPassword == confirmPassword else {
            errorMessage = "Passwords do not match"
            return
        }
        
        guard newPassword.count >= 6 else {
            errorMessage = "Password must be at least 6 characters"
            return
        }
        
        isSaving = true
        errorMessage = nil
        
        // Simulate API call
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            isSaving = false
            showSuccess = true
            currentPassword = ""
            newPassword = ""
            confirmPassword = ""
        }
    }
}

struct MoreMenuView_Previews: PreviewProvider {
    static var previews: some View {
        MoreMenuView()
            .environmentObject(AuthViewModel())
    }
}
