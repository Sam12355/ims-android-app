import SwiftUI

struct PendingAccessView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @State private var isChecking = false
    
    let timer = Timer.publish(every: 30, on: .main, in: .common).autoconnect()
    
    var body: some View {
        VStack(spacing: 32) {
            Spacer()
            
            // Icon
            ZStack {
                Circle()
                    .fill(Color.orange.opacity(0.1))
                    .frame(width: 120, height: 120)
                
                Image(systemName: "clock.badge.exclamationmark")
                    .font(.system(size: 50))
                    .foregroundColor(.orange)
            }
            
            // Text
            VStack(spacing: 12) {
                Text("Pending Approval")
                    .font(.title)
                    .fontWeight(.bold)
                
                Text("Your account is pending approval from a manager or administrator.")
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
                
                Text("You'll be notified once your account is activated.")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }
            
            // Status Card
            VStack(spacing: 16) {
                HStack {
                    Image(systemName: "person.fill")
                        .foregroundColor(.stockNexusRed)
                    
                    Text(authViewModel.currentUser?.fullName ?? "User")
                        .fontWeight(.medium)
                    
                    Spacer()
                }
                
                HStack {
                    Image(systemName: "envelope.fill")
                        .foregroundColor(.stockNexusRed)
                    
                    Text(authViewModel.currentUser?.email ?? "")
                        .foregroundColor(.secondary)
                    
                    Spacer()
                }
                
                HStack {
                    Image(systemName: "clock.fill")
                        .foregroundColor(.orange)
                    
                    Text("Status: Pending")
                        .foregroundColor(.orange)
                        .fontWeight(.medium)
                    
                    Spacer()
                }
            }
            .padding()
            .background(Color.secondaryBackground)
            .cornerRadius(12)
            .padding(.horizontal, 24)
            
            Spacer()
            
            // Actions
            VStack(spacing: 16) {
                Button {
                    checkStatus()
                } label: {
                    HStack {
                        if isChecking {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                        } else {
                            Image(systemName: "arrow.clockwise")
                            Text("Check Status")
                        }
                    }
                    .stockNexusPrimaryButton()
                }
                .disabled(isChecking)
                
                Button {
                    Task {
                        await authViewModel.signOut()
                    }
                } label: {
                    Text("Sign Out")
                        .stockNexusSecondaryButton()
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 40)
        }
        .onReceive(timer) { _ in
            checkStatus()
        }
    }
    
    private func checkStatus() {
        isChecking = true
        Task {
            await authViewModel.refreshUserStatus()
            isChecking = false
        }
    }
}

struct PendingAccessView_Previews: PreviewProvider {
    static var previews: some View {
        PendingAccessView()
            .environmentObject(AuthViewModel())
    }
}
