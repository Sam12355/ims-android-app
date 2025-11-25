package com.ims.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ims.android.data.api.ApiClient
import com.ims.android.data.model.User
import com.ims.android.data.model.Branch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class StaffViewModel(apiClient: ApiClient) : ViewModel() {
    private val apiClient = apiClient
    
    var staffMembers by mutableStateOf<List<ApiClient.StaffMember>>(emptyList())
        private set
    var regions by mutableStateOf<List<ApiClient.Region>>(emptyList())
        private set
    var districts by mutableStateOf<List<ApiClient.District>>(emptyList())
        private set
    var branches by mutableStateOf<List<Branch>>(emptyList())
        private set
    var isLoading by mutableStateOf(true)
        private set
    var isSubmitting by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var successMessage by mutableStateOf<String?>(null)
        private set
    
    var currentUser by mutableStateOf<User?>(null)
        private set
    
    init {
        loadData()
    }
    
    fun loadData() {
        viewModelScope.launch {
            isLoading = true
            try {
                // Get current user profile
                currentUser = apiClient.getCurrentUser()
                android.util.Log.d("StaffScreen", "Current user: ${currentUser?.name}, role: ${currentUser?.role}, branchId: ${currentUser?.branchId}")
                
                // Load all data in parallel
                val staffResult = apiClient.getStaff()
                val regionsResult = apiClient.getRegions()
                val districtsResult = apiClient.getDistricts()
                val branchesResult = apiClient.getBranches()
                
                staffResult.onSuccess { 
                    staffMembers = it 
                    android.util.Log.d("StaffScreen", "Loaded ${it.size} staff members")
                }
                staffResult.onFailure {
                    android.util.Log.e("StaffScreen", "Failed to load staff", it)
                }
                
                regionsResult.onSuccess { 
                    regions = it 
                    android.util.Log.d("StaffScreen", "Loaded ${it.size} regions")
                }
                districtsResult.onSuccess { 
                    districts = it 
                    android.util.Log.d("StaffScreen", "Loaded ${it.size} districts")
                }
                branchesResult.onSuccess { 
                    branches = it 
                    android.util.Log.d("StaffScreen", "Loaded ${it.size} branches")
                    it.forEach { branch ->
                        android.util.Log.d("StaffScreen", "Branch: ${branch.name}, id: ${branch.id}, districtId: ${branch.districtId}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("StaffScreen", "Error loading data", e)
                errorMessage = "Failed to load data: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    fun createStaff(
        name: String,
        email: String,
        phone: String,
        position: String,
        role: String,
        password: String,
        photoUrl: String,
        branchId: String?,
        regionId: String?,
        districtId: String?
    ) {
        viewModelScope.launch {
            isSubmitting = true
            errorMessage = null
            
            try {
                val staffData = mutableMapOf<String, Any?>(
                    "name" to name.trim(),
                    "email" to email.trim(),
                    "role" to role,
                    "password" to password
                )
                
                if (phone.isNotBlank()) staffData["phone"] = phone.trim()
                if (position.isNotBlank()) staffData["position"] = position.trim()
                if (photoUrl.isNotBlank()) staffData["photo_url"] = photoUrl.trim()
                
                when (role) {
                    "staff", "assistant_manager", "manager" -> {
                        staffData["branch_id"] = branchId
                    }
                    "district_manager" -> {
                        staffData["district_id"] = districtId
                    }
                    "regional_manager" -> {
                        staffData["region_id"] = regionId
                    }
                }
                
                apiClient.createStaff(staffData).fold(
                    onSuccess = {
                        successMessage = "Staff member created successfully"
                        // Don't reload data - update silently
                    },
                    onFailure = {
                        errorMessage = "Failed to create staff: ${it.message}"
                    }
                )
            } finally {
                isSubmitting = false
            }
        }
    }
    
    fun updateStaff(
        staffId: String,
        name: String,
        email: String,
        phone: String,
        position: String,
        role: String,
        password: String,
        photoUrl: String,
        branchId: String?,
        regionId: String?,
        districtId: String?
    ) {
        viewModelScope.launch {
            isSubmitting = true
            errorMessage = null
            
            try {
                val staffData = mutableMapOf<String, Any?>(
                    "name" to name.trim(),
                    "email" to email.trim(),
                    "role" to role
                )
                
                if (phone.isNotBlank()) staffData["phone"] = phone.trim()
                if (position.isNotBlank()) staffData["position"] = position.trim()
                if (photoUrl.isNotBlank()) staffData["photo_url"] = photoUrl.trim()
                if (password.isNotBlank()) staffData["password"] = password
                
                when (role) {
                    "staff", "assistant_manager", "manager" -> {
                        staffData["branch_id"] = branchId
                    }
                    "district_manager" -> {
                        staffData["district_id"] = districtId
                    }
                    "regional_manager" -> {
                        staffData["region_id"] = regionId
                    }
                }
                
                apiClient.updateStaff(staffId, staffData).fold(
                    onSuccess = {
                        successMessage = "Staff member updated successfully"
                        // Don't reload data - update silently
                    },
                    onFailure = {
                        errorMessage = "Failed to update staff: ${it.message}"
                    }
                )
            } finally {
                isSubmitting = false
            }
        }
    }
    
    fun deleteStaff(staffId: String) {
        viewModelScope.launch {
            isSubmitting = true
            errorMessage = null
            
            try {
                apiClient.deleteStaff(staffId).fold(
                    onSuccess = {
                        successMessage = "Staff member deleted successfully"
                        // Don't reload data - update silently
                    },
                    onFailure = {
                        errorMessage = "Failed to delete staff: ${it.message}"
                    }
                )
            } finally {
                isSubmitting = false
            }
        }
    }
    
    fun clearMessages() {
        errorMessage = null
        successMessage = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffScreen() {
    val context = LocalContext.current
    val apiClient = remember { ApiClient.getInstance(context) }
    val viewModel: StaffViewModel = remember { StaffViewModel(apiClient) }
    
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedStaff by remember { mutableStateOf<ApiClient.StaffMember?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var staffToDelete by remember { mutableStateOf<ApiClient.StaffMember?>(null) }
    
    // Show success message as snackbar
    LaunchedEffect(viewModel.successMessage) {
        if (viewModel.successMessage != null) {
            delay(3000)
            viewModel.clearMessages()
        }
    }
    
    val filteredStaff = remember(viewModel.staffMembers, searchQuery) {
        val result = if (searchQuery.isBlank()) {
            viewModel.staffMembers
        } else {
            viewModel.staffMembers.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                it.email.contains(searchQuery, ignoreCase = true) ||
                it.role.contains(searchQuery, ignoreCase = true) ||
                (it.position?.contains(searchQuery, ignoreCase = true) == true)
            }
        }
        android.util.Log.d("StaffScreen", "Filtered staff count: ${result.size}, Total: ${viewModel.staffMembers.size}")
        result
    }
    
    val canManageStaff = viewModel.currentUser?.role in listOf(
        "admin", "regional_manager", "district_manager", "manager", "assistant_manager"
    )
    
    Scaffold(
        floatingActionButton = {
            if (canManageStaff) {
                FloatingActionButton(
                    onClick = { showAddDialog = true },
                    containerColor = Color(0xFFE6002A),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, "Add Staff")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Header with Search Icon
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showSearch = !showSearch }
                    ) {
                        Icon(
                            if (showSearch) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = if (showSearch) "Close search" else "Open search"
                        )
                    }
                    Text(
                        text = "Manage Staff",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Search Bar (conditionally shown)
            if (showSearch) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    placeholder = { Text("Search staff members...") },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, "Clear")
                            }
                        }
                    },
                    singleLine = true
                )
            }
            
            // Error Message
            viewModel.errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // Success Message
            viewModel.successMessage?.let { success ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF00C851).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF00C851)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = success,
                            color = Color(0xFF00C851),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            
            // Staff List
            if (viewModel.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color(0xFFE6002A))
                }
            } else if (!canManageStaff) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "You don't have permission to manage staff.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (filteredStaff.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.People,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (searchQuery.isBlank()) "No staff members found." 
                                else "No staff members matching your search.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.People,
                                contentDescription = null,
                                tint = Color(0xFFE6002A)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Staff Members (${filteredStaff.size})",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredStaff) { staff ->
                                StaffMemberCard(
                                    staff = staff,
                                    currentUserId = viewModel.currentUser?.id,
                                    onEdit = { selectedStaff = it },
                                    onDelete = {
                                        staffToDelete = it
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Add/Edit Dialog
    if (showAddDialog || selectedStaff != null) {
        StaffFormDialog(
            staff = selectedStaff,
            currentUser = viewModel.currentUser,
            regions = viewModel.regions,
            districts = viewModel.districts,
            branches = viewModel.branches,
            staffMembers = viewModel.staffMembers,
            isSubmitting = viewModel.isSubmitting,
            onDismiss = {
                showAddDialog = false
                selectedStaff = null
            },
            onSave = { name, email, phone, position, role, password, photoUrl, branchId, regionId, districtId ->
                if (selectedStaff != null) {
                    viewModel.updateStaff(
                        selectedStaff!!.id, name, email, phone, position, 
                        role, password, photoUrl, branchId, regionId, districtId
                    )
                    selectedStaff = null
                } else {
                    viewModel.createStaff(
                        name, email, phone, position, role, password, 
                        photoUrl, branchId, regionId, districtId
                    )
                    showAddDialog = false
                }
            }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog && staffToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            icon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.onSurface) },
            title = { Text("Delete Staff Member") },
            text = {
                Text("Are you sure you want to delete ${staffToDelete?.name}? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        staffToDelete?.let { viewModel.deleteStaff(it.id) }
                        showDeleteDialog = false
                        staffToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD32F2F),
                        contentColor = Color.White
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StaffMemberCard(
    staff: ApiClient.StaffMember,
    currentUserId: String?,
    onEdit: (ApiClient.StaffMember) -> Unit,
    onDelete: (ApiClient.StaffMember) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape),
                color = Color(0xFFE6002A).copy(alpha = 0.1f)
            ) {
                if (staff.photo_url != null) {
                    // TODO: Load image from URL using Coil
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color(0xFFE6002A)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = staff.name.split(" ")
                                .mapNotNull { it.firstOrNull()?.uppercase() }
                                .take(2)
                                .joinToString(""),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE6002A)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Staff Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = staff.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = staff.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (staff.phone != null) {
                    Text(
                        text = staff.phone,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    // Role Badge
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = staff.role.replace("_", " ").capitalize(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                // Access Info
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "Access: ${staff.access_count}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (staff.last_access != null) {
                        Text(
                            text = "Last: ${staff.last_access.take(10)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            // Action Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { onEdit(staff) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                if (staff.id != currentUserId) {
                    IconButton(
                        onClick = { onDelete(staff) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StaffFormDialog(
    staff: ApiClient.StaffMember?,
    currentUser: User?,
    regions: List<ApiClient.Region>,
    districts: List<ApiClient.District>,
    branches: List<Branch>,
    staffMembers: List<ApiClient.StaffMember>,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSave: (name: String, email: String, phone: String, position: String, role: String, 
            password: String, photoUrl: String, branchId: String?, regionId: String?, districtId: String?) -> Unit
) {
    var name by remember { mutableStateOf(staff?.name ?: "") }
    var email by remember { mutableStateOf(staff?.email ?: "") }
    var phone by remember { mutableStateOf(staff?.phone ?: "") }
    var position by remember { mutableStateOf(staff?.position ?: "") }
    var role by remember { mutableStateOf(staff?.role ?: "") }
    var password by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf(staff?.photo_url ?: "") }
    var showPassword by remember { mutableStateOf(false) }
    
    var selectedBranchId by remember { mutableStateOf(staff?.branch_id) }
    var selectedRegionId by remember { mutableStateOf(staff?.region_id) }
    var selectedDistrictId by remember { mutableStateOf(staff?.district_id) }
    
    var showRoleDropdown by remember { mutableStateOf(false) }
    var showBranchDropdown by remember { mutableStateOf(false) }
    var showRegionDropdown by remember { mutableStateOf(false) }
    var showDistrictDropdown by remember { mutableStateOf(false) }
    var showPositionDropdown by remember { mutableStateOf(false) }
    
    // Available positions
    val availablePositions = listOf(
        "Warehouse Assistant",
        "Warehouse Manager",
        "Inventory Clerk",
        "Stock Controller",
        "Store Keeper",
        "Operations Manager",
        "Assistant Manager",
        "Branch Manager",
        "Regional Manager",
        "District Manager"
    )
    
    // Filter branches based on user role and selections
    val filteredBranches = remember(selectedRegionId, selectedDistrictId, currentUser, branches) {
        when (currentUser?.role) {
            "admin" -> {
                if (selectedDistrictId != null) {
                    branches.filter { it.districtId == selectedDistrictId }
                } else if (selectedRegionId != null) {
                    branches.filter { it.district?.regionId == selectedRegionId }
                } else {
                    branches
                }
            }
            "regional_manager" -> {
                // Filter by region from branch context
                branches
            }
            "district_manager" -> {
                // Filter by district from current branch
                branches
            }
            "manager" -> {
                // Show all branches if branchId is null, otherwise filter to manager's branch
                if (currentUser.branchId != null) {
                    branches.filter { it.id == currentUser.branchId }
                } else {
                    branches
                }
            }
            else -> emptyList()
        }
    }
    
    // Filter districts based on selected region
    val filteredDistricts = remember(selectedRegionId, districts) {
        if (selectedRegionId != null) {
            districts.filter { it.region_id == selectedRegionId }
        } else {
            districts
        }
    }
    
    // Available roles based on current user
    val availableRoles = remember(currentUser) {
        when (currentUser?.role) {
            "admin" -> listOf("manager", "assistant_manager", "staff")
            "regional_manager" -> listOf("district_manager", "manager", "assistant_manager", "staff")
            "district_manager" -> listOf("manager", "assistant_manager", "staff")
            "manager" -> listOf("assistant_manager", "staff")
            "assistant_manager" -> listOf("staff")
            else -> emptyList()
        }
    }
    
    // Check if branch already has manager/assistant manager
    val branchHasManager = remember(selectedBranchId, staffMembers, staff) {
        if (selectedBranchId == null) false
        else staffMembers.any { 
            it.branch_id == selectedBranchId && 
            it.role == "manager" && 
            it.id != staff?.id 
        }
    }
    
    val branchHasAssistant = remember(selectedBranchId, staffMembers, staff) {
        if (selectedBranchId == null) false
        else staffMembers.any { 
            it.branch_id == selectedBranchId && 
            it.role == "assistant_manager" && 
            it.id != staff?.id 
        }
    }
    
    val isValid = name.isNotBlank() && 
                  email.isNotBlank() && 
                  role.isNotBlank() &&
                  (staff != null || password.length >= 6) &&
                  when (role) {
                      "staff", "assistant_manager", "manager" -> selectedBranchId != null
                      "district_manager" -> selectedDistrictId != null
                      "regional_manager" -> selectedRegionId != null
                      else -> true
                  }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth(0.95f)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (staff == null) "Add Staff Member" else "Edit Staff Member",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Form Content (Scrollable)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Full Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address *") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = staff == null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                    
                    // Phone
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    
                    // Branch Selection (Show first for most roles)
                    if (selectedBranchId != null || availableRoles.any { it in listOf("staff", "assistant_manager", "manager") }) {
                        ExposedDropdownMenuBox(
                            expanded = showBranchDropdown,
                            onExpandedChange = { showBranchDropdown = it }
                        ) {
                            OutlinedTextField(
                                value = filteredBranches.find { it.id == selectedBranchId }?.name ?: "Select branch",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Branch *") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                trailingIcon = {
                                    Icon(
                                        if (showBranchDropdown) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        "Expand"
                                    )
                                }
                            )
                            
                            ExposedDropdownMenu(
                                expanded = showBranchDropdown,
                                onDismissRequest = { showBranchDropdown = false }
                            ) {
                                filteredBranches.forEach { branch ->
                                    DropdownMenuItem(
                                        text = { Text(branch.name) },
                                        onClick = {
                                            selectedBranchId = branch.id
                                            showBranchDropdown = false
                                            // Clear role if branch changes
                                            role = ""
                                        }
                                    )
                                }
                            }
                        }
                        
                        if (selectedBranchId == null) {
                            Text(
                                "Please select a branch first to see available roles.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    // Role Selection (Only show after branch is selected for most roles)
                    if (selectedBranchId != null || role in listOf("regional_manager", "district_manager")) {
                        ExposedDropdownMenuBox(
                            expanded = showRoleDropdown,
                            onExpandedChange = { showRoleDropdown = it }
                        ) {
                            OutlinedTextField(
                                value = role.replace("_", " ").capitalize(),
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Role *") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                trailingIcon = {
                                    Icon(
                                        if (showRoleDropdown) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        "Expand"
                                    )
                                }
                            )
                            
                            ExposedDropdownMenu(
                                expanded = showRoleDropdown,
                                onDismissRequest = { showRoleDropdown = false }
                            ) {
                                availableRoles.forEach { roleOption ->
                                    // Filter out manager/assistant_manager if already assigned
                                    val isDisabled = when (roleOption) {
                                        "manager" -> branchHasManager
                                        "assistant_manager" -> branchHasAssistant
                                        else -> false
                                    }
                                    
                                    if (!isDisabled) {
                                        DropdownMenuItem(
                                            text = { Text(roleOption.replace("_", " ").capitalize()) },
                                            onClick = {
                                                role = roleOption
                                                showRoleDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Regional Manager - Region Selection
                    if (role == "regional_manager") {
                        ExposedDropdownMenuBox(
                            expanded = showRegionDropdown,
                            onExpandedChange = { showRegionDropdown = it }
                        ) {
                            OutlinedTextField(
                                value = regions.find { it.id == selectedRegionId }?.name ?: "Select region",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Region *") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                trailingIcon = {
                                    Icon(
                                        if (showRegionDropdown) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        "Expand"
                                    )
                                }
                            )
                            
                            ExposedDropdownMenu(
                                expanded = showRegionDropdown,
                                onDismissRequest = { showRegionDropdown = false }
                            ) {
                                regions.forEach { region ->
                                    DropdownMenuItem(
                                        text = { Text(region.name) },
                                        onClick = {
                                            selectedRegionId = region.id
                                            showRegionDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // District Manager - District Selection
                    if (role == "district_manager" && currentUser?.role == "admin") {
                        // Region Selection
                        ExposedDropdownMenuBox(
                            expanded = showRegionDropdown,
                            onExpandedChange = { showRegionDropdown = it }
                        ) {
                            OutlinedTextField(
                                value = regions.find { it.id == selectedRegionId }?.name ?: "Select region",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Region *") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                trailingIcon = {
                                    Icon(
                                        if (showRegionDropdown) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        "Expand"
                                    )
                                }
                            )
                            
                            ExposedDropdownMenu(
                                expanded = showRegionDropdown,
                                onDismissRequest = { showRegionDropdown = false }
                            ) {
                                regions.forEach { region ->
                                    DropdownMenuItem(
                                        text = { Text(region.name) },
                                        onClick = {
                                            selectedRegionId = region.id
                                            selectedDistrictId = null
                                            showRegionDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                        
                        // District Selection
                        ExposedDropdownMenuBox(
                            expanded = showDistrictDropdown,
                            onExpandedChange = { showDistrictDropdown = selectedRegionId != null && it }
                        ) {
                            OutlinedTextField(
                                value = filteredDistricts.find { it.id == selectedDistrictId }?.name ?: "Select district",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("District *") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(),
                                enabled = selectedRegionId != null,
                                trailingIcon = {
                                    Icon(
                                        if (showDistrictDropdown) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        "Expand"
                                    )
                                }
                            )
                            
                            ExposedDropdownMenu(
                                expanded = showDistrictDropdown,
                                onDismissRequest = { showDistrictDropdown = false }
                            ) {
                                filteredDistricts.forEach { district ->
                                    DropdownMenuItem(
                                        text = { Text(district.name) },
                                        onClick = {
                                            selectedDistrictId = district.id
                                            showDistrictDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { 
                            Text(if (staff == null) "Password *" else "New Password (leave blank to keep current)") 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None 
                                              else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    "Toggle password"
                                )
                            }
                        },
                        supportingText = {
                            if (staff == null && password.length < 6) {
                                Text("Password must be at least 6 characters")
                            }
                        }
                    )
                    
                    // Photo URL
                    OutlinedTextField(
                        value = photoUrl,
                        onValueChange = { photoUrl = it },
                        label = { Text("Photo URL (Optional)") },
                        placeholder = { Text("https://example.com/photo.jpg") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSubmitting
                    ) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            onSave(
                                name, email, phone, position, role, password, 
                                photoUrl, selectedBranchId, selectedRegionId, selectedDistrictId
                            )
                        },
                        enabled = isValid && !isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE6002A),
                            contentColor = Color.White
                        )
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (staff == null) "Add Staff" else "Update Staff")
                    }
                }
            }
        }
    }
}

fun String.capitalize(): String {
    return this.split(" ").joinToString(" ") { word ->
        word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
