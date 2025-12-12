import SwiftUI

struct SignUpView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @Environment(\.presentationMode) var presentationMode
    
    var body: some View {
        ScrollView {
            VStack(spacing: 24) {
                // Header
                VStack(spacing: 12) {
                    Image(systemName: "person.badge.plus")
                        .font(.system(size: 60))
                        .foregroundColor(.stockNexusRed)
                    
                    Text("Create Account")
                        .font(.title)
                        .fontWeight(.bold)
                    
                    Text("Fill in the details below to get started")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding(.top, 20)
                
                // Form
                VStack(spacing: 16) {
                    // Full Name Field
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Full Name")
                            .font(.subheadline)
                            .fontWeight(.medium)
                        
                        TextField("Enter your full name", text: $authViewModel.signUpFullName)
                            .textFieldStyle(.plain)
                            .textContentType(.name)
                            .stockNexusTextField()
                    }
                    
                    // Email Field
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Email")
                            .font(.subheadline)
                            .fontWeight(.medium)
                        
                        TextField("Enter your email", text: $authViewModel.signUpEmail)
                            .textFieldStyle(.plain)
                            .keyboardType(.emailAddress)
                            .textContentType(.emailAddress)
                            .autocapitalization(.none)
                            .disableAutocorrection(true)
                            .stockNexusTextField()
                    }
                    
                    // Password Field
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Password")
                            .font(.subheadline)
                            .fontWeight(.medium)
                        
                        HStack {
                            if authViewModel.showPassword {
                                TextField("Create a password", text: $authViewModel.signUpPassword)
                                    .textFieldStyle(.plain)
                            } else {
                                SecureField("Create a password", text: $authViewModel.signUpPassword)
                                    .textFieldStyle(.plain)
                            }
                            
                            Button {
                                authViewModel.showPassword.toggle()
                            } label: {
                                Image(systemName: authViewModel.showPassword ? "eye.slash" : "eye")
                                    .foregroundColor(.secondary)
                            }
                        }
                        .stockNexusTextField()
                        
                        Text("Minimum 6 characters")
                            .font(.caption)
                            .foregroundColor(.secondary)
                    }
                    
                    // Confirm Password Field
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Confirm Password")
                            .font(.subheadline)
                            .fontWeight(.medium)
                        
                        HStack {
                            if authViewModel.showConfirmPassword {
                                TextField("Confirm your password", text: $authViewModel.signUpConfirmPassword)
                                    .textFieldStyle(.plain)
                            } else {
                                SecureField("Confirm your password", text: $authViewModel.signUpConfirmPassword)
                                    .textFieldStyle(.plain)
                            }
                            
                            Button {
                                authViewModel.showConfirmPassword.toggle()
                            } label: {
                                Image(systemName: authViewModel.showConfirmPassword ? "eye.slash" : "eye")
                                    .foregroundColor(.secondary)
                            }
                        }
                        .stockNexusTextField()
                    }
                    
                    // Terms Checkbox
                    Button {
                        authViewModel.acceptedTerms.toggle()
                    } label: {
                        HStack(alignment: .top, spacing: 12) {
                            Image(systemName: authViewModel.acceptedTerms ? "checkmark.square.fill" : "square")
                                .foregroundColor(authViewModel.acceptedTerms ? .stockNexusRed : .secondary)
                            
                            Text("I agree to the Terms of Service and Privacy Policy")
                                .font(.subheadline)
                                .foregroundColor(.primary)
                                .multilineTextAlignment(.leading)
                            
                            Spacer()
                        }
                    }
                    .padding(.vertical, 8)
                    
                    // Sign Up Button
                    Button {
                        Task {
                            await authViewModel.signUp()
                        }
                    } label: {
                        HStack {
                            if authViewModel.isSigningUp {
                                ProgressView()
                                    .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            } else {
                                Text("Create Account")
                            }
                        }
                        .stockNexusPrimaryButton()
                    }
                    .disabled(authViewModel.isSigningUp)
                    .padding(.top, 8)
                    
                    // Sign In Link
                    HStack {
                        Text("Already have an account?")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                        
                        Button {
                            presentationMode.wrappedValue.dismiss()
                        } label: {
                            Text("Sign In")
                                .font(.subheadline)
                                .fontWeight(.semibold)
                                .foregroundColor(.stockNexusRed)
                        }
                    }
                    .padding(.top, 16)
                }
                .padding(.horizontal, 24)
            }
            .padding(.bottom, 40)
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarLeading) {
                Button {
                    presentationMode.wrappedValue.dismiss()
                } label: {
                    Image(systemName: "chevron.left")
                        .foregroundColor(.primary)
                }
            }
        }
        .alert(isPresented: $authViewModel.showError) {
            Alert(
                title: Text("Error"),
                message: Text(authViewModel.error ?? "An error occurred"),
                dismissButton: .default(Text("OK")) {
                    authViewModel.clearError()
                }
            )
        }
    }
}

struct SignUpView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView {
            SignUpView()
                .environmentObject(AuthViewModel())
        }
    }
}
