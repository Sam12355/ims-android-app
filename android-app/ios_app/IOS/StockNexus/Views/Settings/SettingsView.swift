import SwiftUI

// MARK: - Settings View
struct SettingsView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @State private var notificationsEnabled = true
    @State private var soundEnabled = true
    @State private var biometricEnabled = false
    @State private var showLogoutAlert = false
    
    var body: some View {
        NavigationView {
            List {
                // Account Section
                Section(header: Text("Account")) {
                    if let user = authViewModel.currentUser {
                        HStack(spacing: 16) {
                            AvatarView(name: user.fullName, size: 50)
                            
                            VStack(alignment: .leading, spacing: 4) {
                                Text(user.fullName)
                                    .font(.headline)
                                Text(user.email)
                                    .font(.caption)
                                    .foregroundColor(.secondary)
                                Text(user.role.rawValue.capitalized)
                                    .font(.caption)
                                    .foregroundColor(.stockNexusRed)
                            }
                        }
                        .padding(.vertical, 8)
                    }
                    
                    NavigationLink(destination: EditProfileView()) {
                        MenuItemRow(icon: "person.circle", title: "Edit Profile", showChevron: false)
                    }
                    
                    NavigationLink(destination: SettingsChangePasswordView()) {
                        MenuItemRow(icon: "lock.circle", title: "Change Password", showChevron: false)
                    }
                }
                
                // Notifications Section
                Section(header: Text("Notifications")) {
                    Toggle(isOn: $notificationsEnabled) {
                        MenuItemRow(icon: "bell", title: "Push Notifications", showChevron: false)
                    }
                    .accentColor(.stockNexusRed)
                    
                    Toggle(isOn: $soundEnabled) {
                        MenuItemRow(icon: "speaker.wave.2", title: "Sound", showChevron: false)
                    }
                    .accentColor(.stockNexusRed)
                    .disabled(!notificationsEnabled)
                }
                
                // Security Section
                Section(header: Text("Security")) {
                    Toggle(isOn: $biometricEnabled) {
                        MenuItemRow(icon: "faceid", title: "Face ID / Touch ID", showChevron: false)
                    }
                    .accentColor(.stockNexusRed)
                    
                    NavigationLink(destination: SessionsView()) {
                        MenuItemRow(icon: "laptopcomputer.and.iphone", title: "Active Sessions", showChevron: false)
                    }
                }
                
                // App Section
                Section(header: Text("App")) {
                    NavigationLink(destination: AppearanceSettingsView()) {
                        MenuItemRow(icon: "paintbrush", title: "Appearance", showChevron: false)
                    }
                    
                    NavigationLink(destination: LanguageSettingsView()) {
                        MenuItemRow(icon: "globe", title: "Language", showChevron: false)
                    }
                    
                    NavigationLink(destination: AboutView()) {
                        MenuItemRow(icon: "info.circle", title: "About", showChevron: false)
                    }
                }
                
                // Support Section
                Section(header: Text("Support")) {
                    Button(action: {
                        // TODO: Open help center
                    }) {
                        MenuItemRow(icon: "questionmark.circle", title: "Help Center", iconColor: .blue)
                    }
                    
                    Button(action: {
                        // TODO: Contact support
                    }) {
                        MenuItemRow(icon: "envelope", title: "Contact Support", iconColor: .blue)
                    }
                    
                    NavigationLink(destination: PrivacyPolicyView()) {
                        MenuItemRow(icon: "hand.raised", title: "Privacy Policy", showChevron: false)
                    }
                    
                    NavigationLink(destination: TermsOfServiceView()) {
                        MenuItemRow(icon: "doc.text", title: "Terms of Service", showChevron: false)
                    }
                }
                
                // Logout Section
                Section {
                    Button(action: { showLogoutAlert = true }) {
                        HStack {
                            Spacer()
                            Text("Log Out")
                                .foregroundColor(.red)
                            Spacer()
                        }
                    }
                }
            }
            .navigationTitle("Settings")
            .alert(isPresented: $showLogoutAlert) {
                Alert(
                    title: Text("Log Out"),
                    message: Text("Are you sure you want to log out?"),
                    primaryButton: .destructive(Text("Log Out")) {
                        Task {
                            await authViewModel.signOut()
                        }
                    },
                    secondaryButton: .cancel()
                )
            }
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
}

// MARK: - Avatar View
struct AvatarView: View {
    let name: String
    let size: CGFloat
    
    var body: some View {
        ZStack {
            Circle()
                .fill(Color.stockNexusRed.opacity(0.1))
                .frame(width: size, height: size)
            
            Text(name.prefix(1).uppercased())
                .font(.system(size: size * 0.4))
                .fontWeight(.semibold)
                .foregroundColor(.stockNexusRed)
        }
    }
}

// MARK: - Menu Item Row
struct MenuItemRow: View {
    let icon: String
    let title: String
    var showChevron: Bool = true
    var iconColor: Color = .stockNexusRed
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .foregroundColor(iconColor)
                .frame(width: 24)
            
            Text(title)
                .foregroundColor(.primary)
            
            Spacer()
            
            if showChevron {
                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
        }
    }
}

// MARK: - Edit Profile View
struct EditProfileView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @State private var fullName = ""
    @State private var phone = ""
    @State private var isSaving = false
    @State private var showSuccess = false
    
    var body: some View {
        Form {
            Section(header: Text("Profile Photo")) {
                HStack {
                    Spacer()
                    AvatarView(name: authViewModel.currentUser?.fullName ?? "U", size: 80)
                    Spacer()
                }
                .listRowBackground(Color.clear)
            }
            
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
            }
            
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
        }
        .navigationTitle("Edit Profile")
        .onAppear {
            fullName = authViewModel.currentUser?.fullName ?? ""
            phone = authViewModel.currentUser?.phone ?? ""
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
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            isSaving = false
            showSuccess = true
        }
    }
}

// MARK: - Settings Change Password View (renamed to avoid conflict)
struct SettingsChangePasswordView: View {
    @State private var currentPassword = ""
    @State private var newPassword = ""
    @State private var confirmPassword = ""
    @State private var isSaving = false
    @State private var showSuccess = false
    @State private var errorMessage: String?
    
    var body: some View {
        Form {
            Section(header: Text("Current Password")) {
                SecureField("Enter current password", text: $currentPassword)
            }
            
            Section(header: Text("New Password")) {
                SecureField("Enter new password", text: $newPassword)
                SecureField("Confirm new password", text: $confirmPassword)
            }
            
            if let error = errorMessage {
                Section {
                    Text(error)
                        .foregroundColor(.red)
                }
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
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            isSaving = false
            showSuccess = true
            currentPassword = ""
            newPassword = ""
            confirmPassword = ""
        }
    }
}

// MARK: - Sessions View
struct SessionsView: View {
    var body: some View {
        List {
            Section(header: Text("Current Session")) {
                HStack {
                    Image(systemName: "iphone")
                        .font(.title2)
                        .foregroundColor(.stockNexusRed)
                    
                    VStack(alignment: .leading) {
                        Text("This Device")
                            .font(.headline)
                        Text("iPhone • Active Now")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    
                    Spacer()
                    
                    Circle()
                        .fill(Color.green)
                        .frame(width: 8, height: 8)
                }
            }
        }
        .navigationTitle("Active Sessions")
    }
}

// MARK: - Appearance Settings View
struct AppearanceSettingsView: View {
    @AppStorage("isDarkMode") private var isDarkMode = false
    
    var body: some View {
        Form {
            Section {
                Toggle("Dark Mode", isOn: $isDarkMode)
            }
        }
        .navigationTitle("Appearance")
    }
}

// MARK: - Language Settings View
struct LanguageSettingsView: View {
    @State private var selectedLanguage = "English"
    let languages = ["English", "Swedish", "Norwegian", "Danish"]
    
    var body: some View {
        List {
            ForEach(languages, id: \.self) { language in
                Button(action: { selectedLanguage = language }) {
                    HStack {
                        Text(language)
                            .foregroundColor(.primary)
                        Spacer()
                        if selectedLanguage == language {
                            Image(systemName: "checkmark")
                                .foregroundColor(.stockNexusRed)
                        }
                    }
                }
            }
        }
        .navigationTitle("Language")
    }
}

// MARK: - About View
struct AboutView: View {
    var body: some View {
        List {
            Section {
                HStack {
                    Spacer()
                    VStack(spacing: 12) {
                        Image(systemName: "shippingbox.fill")
                            .font(.system(size: 60))
                            .foregroundColor(.stockNexusRed)
                        
                        Text("Stock Nexus")
                            .font(.title2)
                            .fontWeight(.bold)
                        
                        Text("Version \(AppConstants.appVersion)")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    Spacer()
                }
                .listRowBackground(Color.clear)
            }
            
            Section(header: Text("Information")) {
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
        }
        .navigationTitle("About")
    }
}

// MARK: - Privacy Policy View
struct PrivacyPolicyView: View {
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("Privacy Policy")
                    .font(.title)
                    .fontWeight(.bold)
                
                Text("Last updated: \(Date().formatted(as: "MMMM d, yyyy"))")
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                Text("This privacy policy describes how Stock Nexus collects, uses, and protects your personal information.")
                    .font(.body)
                
                Text("Information We Collect")
                    .font(.headline)
                
                Text("We collect information you provide directly to us, such as your name, email address, and other contact information when you register for an account.")
                    .font(.body)
            }
            .padding()
        }
        .navigationTitle("Privacy Policy")
    }
}

// MARK: - Terms of Service View
struct TermsOfServiceView: View {
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text("Terms of Service")
                    .font(.title)
                    .fontWeight(.bold)
                
                Text("Last updated: \(Date().formatted(as: "MMMM d, yyyy"))")
                    .font(.caption)
                    .foregroundColor(.secondary)
                
                Text("By using Stock Nexus, you agree to these terms of service.")
                    .font(.body)
                
                Text("Use of Service")
                    .font(.headline)
                
                Text("You must be at least 18 years old to use this service. You are responsible for maintaining the confidentiality of your account.")
                    .font(.body)
            }
            .padding()
        }
        .navigationTitle("Terms of Service")
    }
}

struct SettingsView_Previews: PreviewProvider {
    static var previews: some View {
        SettingsView()
            .environmentObject(AuthViewModel())
    }
}
