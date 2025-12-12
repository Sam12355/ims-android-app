import SwiftUI

struct SignInView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @State private var showForgotPassword = false
    @State private var showSignUp = false
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    // Logo
                    VStack(spacing: 12) {
                        Image(systemName: "shippingbox.fill")
                            .font(.system(size: 60))
                            .foregroundColor(.stockNexusRed)
                        
                        Text("Stock Nexus")
                            .font(.title)
                            .fontWeight(.bold)
                        
                        Text("Sign in to continue")
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                    .padding(.top, 40)
                    .padding(.bottom, 20)
                    
                    // Form
                    VStack(spacing: 16) {
                        // Email Field
                        VStack(alignment: .leading, spacing: 8) {
                            Text("Email")
                                .font(.subheadline)
                                .fontWeight(.medium)
                            
                            TextField("Enter your email", text: $authViewModel.signInEmail)
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
                                    TextField("Enter your password", text: $authViewModel.signInPassword)
                                        .textFieldStyle(.plain)
                                } else {
                                    SecureField("Enter your password", text: $authViewModel.signInPassword)
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
                        }
                        
                        // Remember Me & Forgot Password
                        HStack {
                            Button {
                                authViewModel.rememberMe.toggle()
                            } label: {
                                HStack(spacing: 8) {
                                    Image(systemName: authViewModel.rememberMe ? "checkmark.square.fill" : "square")
                                        .foregroundColor(authViewModel.rememberMe ? .stockNexusRed : .secondary)
                                    
                                    Text("Remember me")
                                        .font(.subheadline)
                                        .foregroundColor(.primary)
                                }
                            }
                            
                            Spacer()
                            
                            Button {
                                showForgotPassword = true
                            } label: {
                                Text("Forgot Password?")
                                    .font(.subheadline)
                                    .foregroundColor(.stockNexusRed)
                            }
                        }
                        .padding(.vertical, 8)
                        
                        // Sign In Button
                        Button {
                            Task {
                                await authViewModel.signIn()
                            }
                        } label: {
                            HStack {
                                if authViewModel.isSigningIn {
                                    ProgressView()
                                        .progressViewStyle(CircularProgressViewStyle(tint: .white))
                                } else {
                                    Text("Sign In")
                                }
                            }
                            .stockNexusPrimaryButton()
                        }
                        .disabled(authViewModel.isSigningIn)
                        .padding(.top, 8)
                        
                        // Sign Up Link
                        HStack {
                            Text("Don't have an account?")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                            
                            Button {
                                showSignUp = true
                            } label: {
                                Text("Create Account")
                                    .font(.subheadline)
                                    .fontWeight(.semibold)
                                    .foregroundColor(.stockNexusRed)
                            }
                        }
                        .padding(.top, 16)
                    }
                    .padding(.horizontal, 24)
                }
            }
            .background(
                NavigationLink(destination: SignUpView(), isActive: $showSignUp) {
                    EmptyView()
                }
            )
            .sheet(isPresented: $showForgotPassword) {
                ForgotPasswordView()
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
            .navigationBarHidden(true)
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
}

// MARK: - Forgot Password View
struct ForgotPasswordView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @Environment(\.presentationMode) var presentationMode
    
    var body: some View {
        NavigationView {
            VStack(spacing: 24) {
                VStack(spacing: 12) {
                    Image(systemName: "lock.rotation")
                        .font(.system(size: 60))
                        .foregroundColor(.stockNexusRed)
                    
                    Text("Reset Password")
                        .font(.title2)
                        .fontWeight(.bold)
                    
                    Text("Enter your email address and we'll send you a link to reset your password.")
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal)
                }
                .padding(.top, 40)
                
                VStack(alignment: .leading, spacing: 8) {
                    Text("Email")
                        .font(.subheadline)
                        .fontWeight(.medium)
                    
                    TextField("Enter your email", text: $authViewModel.forgotPasswordEmail)
                        .textFieldStyle(.plain)
                        .keyboardType(.emailAddress)
                        .textContentType(.emailAddress)
                        .autocapitalization(.none)
                        .stockNexusTextField()
                }
                .padding(.horizontal, 24)
                
                Button {
                    Task {
                        await authViewModel.sendPasswordReset()
                    }
                } label: {
                    HStack {
                        if authViewModel.isSendingReset {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        } else {
                            Text("Send Reset Link")
                        }
                    }
                    .stockNexusPrimaryButton()
                }
                .disabled(authViewModel.isSendingReset)
                .padding(.horizontal, 24)
                
                Spacer()
            }
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button {
                        presentationMode.wrappedValue.dismiss()
                    } label: {
                        Image(systemName: "xmark")
                            .foregroundColor(.primary)
                    }
                }
            }
            .alert(isPresented: $authViewModel.resetEmailSent) {
                Alert(
                    title: Text("Reset Email Sent"),
                    message: Text("Check your email for password reset instructions."),
                    dismissButton: .default(Text("OK")) {
                        presentationMode.wrappedValue.dismiss()
                    }
                )
            }
        }
        .navigationViewStyle(StackNavigationViewStyle())
    }
}

struct SignInView_Previews: PreviewProvider {
    static var previews: some View {
        SignInView()
            .environmentObject(AuthViewModel())
    }
}
