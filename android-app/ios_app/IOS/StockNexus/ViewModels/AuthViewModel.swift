import Foundation
import Combine

// MARK: - Auth ViewModel
@MainActor
class AuthViewModel: ObservableObject {
    // MARK: - Published Properties
    @Published var currentUser: User?
    @Published var isAuthenticated = false
    @Published var isLoading = true
    @Published var error: String?
    @Published var showError = false
    
    // Sign In
    @Published var signInEmail = ""
    @Published var signInPassword = ""
    @Published var rememberMe = false
    @Published var isSigningIn = false
    
    // Sign Up
    @Published var signUpFullName = ""
    @Published var signUpEmail = ""
    @Published var signUpPassword = ""
    @Published var signUpConfirmPassword = ""
    @Published var acceptedTerms = false
    @Published var isSigningUp = false
    
    // Password visibility
    @Published var showPassword = false
    @Published var showConfirmPassword = false
    
    // Forgot Password
    @Published var forgotPasswordEmail = ""
    @Published var isSendingReset = false
    @Published var resetEmailSent = false
    
    private var cancellables = Set<AnyCancellable>()
    
    // MARK: - Initialization
    init() {
        // Load saved credentials
        if let savedEmail = UserDefaults.standard.string(forKey: AppConstants.UserDefaultsKeys.lastLoginEmail),
           UserDefaults.standard.bool(forKey: AppConstants.UserDefaultsKeys.rememberMe) {
            signInEmail = savedEmail
            rememberMe = true
        }
        
        // Check auth status on init
        Task {
            await checkAuthStatus()
        }
    }
    
    // MARK: - Check Auth Status
    func checkAuthStatus() async {
        isLoading = true
        
        if let user = await AuthService.shared.checkAuthStatus() {
            currentUser = user
            isAuthenticated = true
        } else {
            isAuthenticated = false
        }
        
        isLoading = false
    }
    
    // MARK: - Sign In
    func signIn() async {
        guard validateSignIn() else { return }
        
        isSigningIn = true
        error = nil
        
        do {
            let user = try await AuthService.shared.signIn(
                email: signInEmail.trimmingCharacters(in: .whitespaces),
                password: signInPassword
            )
            
            // Save credentials if remember me is enabled
            if rememberMe {
                UserDefaults.standard.set(signInEmail, forKey: AppConstants.UserDefaultsKeys.lastLoginEmail)
                UserDefaults.standard.set(true, forKey: AppConstants.UserDefaultsKeys.rememberMe)
            } else {
                UserDefaults.standard.removeObject(forKey: AppConstants.UserDefaultsKeys.lastLoginEmail)
                UserDefaults.standard.set(false, forKey: AppConstants.UserDefaultsKeys.rememberMe)
            }
            
            currentUser = user
            isAuthenticated = true
            clearSignInFields()
            
            // Post notification
            NotificationCenter.default.post(name: AppConstants.NotificationNames.userDidLogin, object: nil)
            
        } catch let networkError as NetworkError {
            error = networkError.errorDescription
            showError = true
        } catch {
            self.error = error.localizedDescription
            showError = true
        }
        
        isSigningIn = false
    }
    
    // MARK: - Sign Up
    func signUp() async {
        guard validateSignUp() else { return }
        
        isSigningUp = true
        error = nil
        
        do {
            let user = try await AuthService.shared.signUp(
                fullName: signUpFullName.trimmingCharacters(in: .whitespaces),
                email: signUpEmail.trimmingCharacters(in: .whitespaces),
                password: signUpPassword
            )
            
            currentUser = user
            isAuthenticated = true
            clearSignUpFields()
            
        } catch let networkError as NetworkError {
            error = networkError.errorDescription
            showError = true
        } catch {
            self.error = error.localizedDescription
            showError = true
        }
        
        isSigningUp = false
    }
    
    // MARK: - Sign Out
    func signOut() async {
        do {
            try await AuthService.shared.signOut()
        } catch {
            print("Sign out error: \(error.localizedDescription)")
        }
        
        currentUser = nil
        isAuthenticated = false
        
        // Post notification
        NotificationCenter.default.post(name: AppConstants.NotificationNames.userDidLogout, object: nil)
    }
    
    // MARK: - Forgot Password
    func sendPasswordReset() async {
        guard !forgotPasswordEmail.isEmpty else {
            error = "Please enter your email"
            showError = true
            return
        }
        
        guard forgotPasswordEmail.isValidEmail else {
            error = "Please enter a valid email"
            showError = true
            return
        }
        
        isSendingReset = true
        error = nil
        
        do {
            try await AuthService.shared.forgotPassword(email: forgotPasswordEmail)
            resetEmailSent = true
        } catch let networkError as NetworkError {
            error = networkError.errorDescription
            showError = true
        } catch {
            self.error = error.localizedDescription
            showError = true
        }
        
        isSendingReset = false
    }
    
    // MARK: - Refresh User Status
    func refreshUserStatus() async {
        if let user = await AuthService.shared.checkAuthStatus() {
            currentUser = user
        }
    }
    
    // MARK: - Validation
    private func validateSignIn() -> Bool {
        if signInEmail.isEmpty {
            error = "Please enter your email"
            showError = true
            return false
        }
        
        if !signInEmail.isValidEmail {
            error = "Please enter a valid email"
            showError = true
            return false
        }
        
        if signInPassword.isEmpty {
            error = "Please enter your password"
            showError = true
            return false
        }
        
        if signInPassword.count < 6 {
            error = "Password must be at least 6 characters"
            showError = true
            return false
        }
        
        return true
    }
    
    private func validateSignUp() -> Bool {
        if signUpFullName.isEmpty {
            error = "Please enter your full name"
            showError = true
            return false
        }
        
        if signUpEmail.isEmpty {
            error = "Please enter your email"
            showError = true
            return false
        }
        
        if !signUpEmail.isValidEmail {
            error = "Please enter a valid email"
            showError = true
            return false
        }
        
        if signUpPassword.isEmpty {
            error = "Please enter a password"
            showError = true
            return false
        }
        
        if signUpPassword.count < 6 {
            error = "Password must be at least 6 characters"
            showError = true
            return false
        }
        
        if signUpPassword != signUpConfirmPassword {
            error = "Passwords do not match"
            showError = true
            return false
        }
        
        if !acceptedTerms {
            error = "Please accept the terms and conditions"
            showError = true
            return false
        }
        
        return true
    }
    
    // MARK: - Helper Methods
    private func clearSignInFields() {
        signInPassword = ""
        showPassword = false
    }
    
    private func clearSignUpFields() {
        signUpFullName = ""
        signUpEmail = ""
        signUpPassword = ""
        signUpConfirmPassword = ""
        acceptedTerms = false
        showPassword = false
        showConfirmPassword = false
    }
    
    func clearError() {
        error = nil
        showError = false
    }
}
