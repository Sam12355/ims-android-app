import SwiftUI

struct RecordStockInView: View {
    @EnvironmentObject var authViewModel: AuthViewModel
    @State private var selectedSupplier: Supplier = .gronsakshuset
    @State private var remarks = ""
    @State private var receiptImage: UIImage?
    @State private var isSubmitting = false
    @State private var showSuccess = false
    @State private var showMyReceipts = false
    @State private var showImagePicker = false
    
    var body: some View {
        NavigationView {
            ScrollView {
                VStack(spacing: 24) {
                    // Submit Receipt Form
                    submitReceiptForm
                    
                    // My Receipts Section
                    myReceiptsSection
                }
                .padding()
            }
            .navigationTitle("Record Stock In")
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        showMyReceipts = true
                    } label: {
                        Image(systemName: "list.bullet.rectangle")
                    }
                }
            }
            .sheet(isPresented: $showMyReceipts) {
                MyReceiptsView()
            }
            .sheet(isPresented: $showImagePicker) {
                ImagePicker(image: $receiptImage)
            }
            .alert("Success", isPresented: $showSuccess) {
                Button("OK") {
                    clearForm()
                }
            } message: {
                Text("Receipt submitted successfully!")
            }
        }
        .navigationViewStyle(.stack)
    }
    
    // MARK: - Submit Receipt Form
    private var submitReceiptForm: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Submit New Receipt")
                .font(.headline)
            
            // Supplier Dropdown
            VStack(alignment: .leading, spacing: 8) {
                Text("Supplier")
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                Menu {
                    ForEach(Supplier.allCases, id: \.self) { supplier in
                        Button(supplier.rawValue) {
                            selectedSupplier = supplier
                        }
                    }
                } label: {
                    HStack {
                        Text(selectedSupplier.rawValue)
                            .foregroundColor(.primary)
                        Spacer()
                        Image(systemName: "chevron.down")
                            .foregroundColor(.secondary)
                    }
                    .padding()
                    .background(Color.secondaryBackground)
                    .cornerRadius(12)
                }
            }
            
            // Remarks
            VStack(alignment: .leading, spacing: 8) {
                Text("Remarks (Optional)")
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                TextEditor(text: $remarks)
                    .frame(minHeight: 80)
                    .padding(8)
                    .background(Color.secondaryBackground)
                    .cornerRadius(12)
            }
            
            // Receipt Document
            VStack(alignment: .leading, spacing: 8) {
                Text("Receipt Document")
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                if let image = receiptImage {
                    ZStack(alignment: .topTrailing) {
                        Image(uiImage: image)
                            .resizable()
                            .aspectRatio(contentMode: .fit)
                            .frame(maxHeight: 200)
                            .cornerRadius(12)
                        
                        Button {
                            receiptImage = nil
                        } label: {
                            Image(systemName: "xmark.circle.fill")
                                .font(.title2)
                                .foregroundColor(.white)
                                .background(Circle().fill(Color.black.opacity(0.5)))
                        }
                        .padding(8)
                    }
                } else {
                    Button {
                        showImagePicker = true
                    } label: {
                        VStack(spacing: 12) {
                            Image(systemName: "camera.fill")
                                .font(.system(size: 40))
                                .foregroundColor(.secondary)
                            
                            Text("Tap to add receipt image")
                                .font(.subheadline)
                                .foregroundColor(.secondary)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 150)
                        .background(Color.secondaryBackground)
                        .cornerRadius(12)
                        .overlay(
                            RoundedRectangle(cornerRadius: 12)
                                .stroke(style: StrokeStyle(lineWidth: 2, dash: [8]))
                                .foregroundColor(.secondary.opacity(0.5))
                        )
                    }
                }
            }
            
            // Submit Button
            Button {
                submitReceipt()
            } label: {
                HStack {
                    if isSubmitting {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .white))
                    } else {
                        Image(systemName: "paperplane.fill")
                        Text("Submit Receipt")
                    }
                }
                .stockNexusPrimaryButton()
            }
            .disabled(isSubmitting)
        }
        .padding()
        .background(Color.secondaryBackground)
        .cornerRadius(12)
    }
    
    // MARK: - My Receipts Section
    private var myReceiptsSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Recent Submissions")
                    .font(.headline)
                
                Spacer()
                
                Button {
                    showMyReceipts = true
                } label: {
                    Text("View All")
                        .font(.subheadline)
                        .foregroundColor(.stockNexusRed)
                }
            }
            
            // Placeholder for recent receipts
            VStack(spacing: 12) {
                ForEach(0..<3) { index in
                    HStack {
                        VStack(alignment: .leading) {
                            Text("Gronsakshuset")
                                .font(.subheadline)
                                .fontWeight(.medium)
                            Text("Nov \(30 - index), 2025")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        
                        Spacer()
                        
                        RecordReceiptStatusBadge(status: index == 0 ? .pending : .processed)
                    }
                    .padding()
                    .background(Color.tertiaryBackground)
                    .cornerRadius(8)
                }
            }
        }
        .padding()
        .background(Color.secondaryBackground)
        .cornerRadius(12)
    }
    
    // MARK: - Submit Receipt
    private func submitReceipt() {
        isSubmitting = true
        
        // Simulate API call
        DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
            isSubmitting = false
            showSuccess = true
        }
    }
    
    // MARK: - Clear Form
    private func clearForm() {
        selectedSupplier = .gronsakshuset
        remarks = ""
        receiptImage = nil
    }
}

// MARK: - Record Receipt Status Badge
struct RecordReceiptStatusBadge: View {
    let status: ReceiptStatus
    
    var body: some View {
        Text(status.rawValue.capitalized)
            .font(.caption2)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(status == .pending ? Color.orange.opacity(0.1) : Color.green.opacity(0.1))
            .foregroundColor(status == .pending ? .orange : .green)
            .cornerRadius(4)
    }
}

// MARK: - My Receipts View
struct MyReceiptsView: View {
    @Environment(\.dismiss) var dismiss
    
    var body: some View {
        NavigationView {
            List {
                ForEach(0..<10) { index in
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text(Supplier.allCases[index % Supplier.allCases.count].rawValue)
                                .font(.subheadline)
                                .fontWeight(.medium)
                            
                            Text("Submitted: Nov \(30 - index), 2025")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                        
                        Spacer()
                        
                        RecordReceiptStatusBadge(status: index < 3 ? .pending : .processed)
                    }
                    .padding(.vertical, 4)
                }
            }
            .navigationTitle("My Receipts")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Close") {
                        dismiss()
                    }
                }
            }
        }
    }
}

// MARK: - Image Picker (UIKit wrapper for iOS 15 compatibility)
struct ImagePicker: UIViewControllerRepresentable {
    @Binding var image: UIImage?
    @Environment(\.dismiss) var dismiss
    
    func makeUIViewController(context: Context) -> UIImagePickerController {
        let picker = UIImagePickerController()
        picker.delegate = context.coordinator
        picker.sourceType = .photoLibrary
        return picker
    }
    
    func updateUIViewController(_ uiViewController: UIImagePickerController, context: Context) {}
    
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }
    
    class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let parent: ImagePicker
        
        init(_ parent: ImagePicker) {
            self.parent = parent
        }
        
        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey : Any]) {
            if let image = info[.originalImage] as? UIImage {
                parent.image = image
            }
            parent.dismiss()
        }
        
        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) {
            parent.dismiss()
        }
    }
}

// MARK: - Preview Provider
struct RecordStockInView_Previews: PreviewProvider {
    static var previews: some View {
        RecordStockInView()
            .environmentObject(AuthViewModel())
    }
}
