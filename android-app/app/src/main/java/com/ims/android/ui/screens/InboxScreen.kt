package com.ims.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ims.android.data.model.Thread
import java.util.UUID

@Composable
fun ComposeMessageDialog(
    onDismiss: () -> Unit,
    onUserSelected: (String, String) -> Unit,
    users: List<Pair<String, String>>
) {
    var selectedUser by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var showUserList by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    
    // Filter users based on search query
    val filteredUsers = remember(searchQuery, users) {
        if (searchQuery.isBlank()) {
            users
        } else {
            users.filter { (_, name) ->
                name.contains(searchQuery, ignoreCase = true)
            }
        }
    }
    
    // Find selected user name
    val selectedUserName = remember(selectedUser, users) {
        users.find { it.first == selectedUser }?.second
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1C1C1E),
        confirmButton = {
            Button(
                onClick = {
                    if (selectedUser != null && message.isNotBlank() && !isSending) {
                        isSending = true
                        onUserSelected(selectedUser!!, message)
                    }
                },
                enabled = selectedUser != null && message.isNotBlank() && !isSending,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black,
                    disabledContainerColor = Color.Gray
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Send", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) { 
                Text("Cancel", fontWeight = FontWeight.Bold) 
            }
        },
        title = { 
            Text(
                "New Message",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            ) 
        },
        text = {
            Column {
                // To field with dropdown
                Text(
                    "To:",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = if (selectedUser != null && !showUserList) selectedUserName ?: "" else searchQuery,
                    onValueChange = { 
                        searchQuery = it
                        showUserList = true
                        if (it.isBlank()) selectedUser = null
                    },
                    placeholder = { 
                        Text("Search contact", color = Color.Gray) 
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showUserList = !showUserList },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    trailingIcon = {
                        IconButton(onClick = { showUserList = !showUserList }) {
                            Icon(
                                imageVector = if (showUserList) Icons.Default.ArrowBack else Icons.Default.Person,
                                contentDescription = "Toggle user list",
                                tint = Color.White
                            )
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // User list dropdown
                if (showUserList) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        color = Color(0xFF2C2C2E),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (users.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Loading users...", color = Color.Gray)
                            }
                        } else if (filteredUsers.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No users found", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.padding(8.dp)
                            ) {
                                items(filteredUsers) { (userId, userName) ->
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedUser = userId
                                                searchQuery = userName
                                                showUserList = false
                                            },
                                        color = if (selectedUser == userId) Color(0xFF0084FF).copy(alpha = 0.3f) else Color.Transparent
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 12.dp, horizontal = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Avatar
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF0084FF)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = userName.take(2).uppercase(),
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 14.sp
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = userName,
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    color = Color.White
                                                )
                                            )
                                        }
                                    }
                                    if (userId != filteredUsers.last().first) {
                                        Divider(
                                            color = Color.Gray.copy(alpha = 0.3f),
                                            thickness = 0.5.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Message field
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    placeholder = { Text("Type a message...", color = Color.Gray) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.Gray,
                        cursorColor = Color.White,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    minLines = 3
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun InboxScreen(
    threads: List<Thread>,
    messages: Map<String, List<com.ims.android.data.model.Message>>,
    onThreadClick: (Thread) -> Unit,
    onComposeClick: () -> Unit,
    onlineMembers: List<String> = emptyList(),
    currentUserId: String = "",
    onRefresh: suspend () -> Unit = {}
) {
    var selectedProfileImage by remember { mutableStateOf<String?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            scope.launch {
                isRefreshing = true
                onRefresh()
                isRefreshing = false
            }
        }
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pullRefresh(pullRefreshState)
        ) {
            if (threads.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No messages yet.\nPull down to refresh.",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(threads) { thread ->
                        val unreadCount = messages[thread.user2Id]?.count { message ->
                            message.senderId == thread.user2Id && message.readAt == null
                        } ?: 0
                        
                        ThreadListItem(
                            thread = thread,
                            onClick = { onThreadClick(thread) },
                            isOnline = onlineMembers.contains(thread.user2Id),
                            currentUserId = currentUserId,
                            unreadCount = unreadCount,
                            onAvatarClick = { photoUrl ->
                                selectedProfileImage = photoUrl
                            }
                        )
                        Divider(color = Color.LightGray, thickness = 0.5.dp)
                    }
                }
            }
            
            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
        
        // Floating Action Button for compose
        FloatingActionButton(
            onClick = { onComposeClick() },
            containerColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = "Compose New Message",
                tint = Color.Black
            )
        }
    }
    
    // Full-size profile image dialog - square with small close button
    if (selectedProfileImage != null) {
        Dialog(onDismissRequest = { selectedProfileImage = null }) {
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                // Square image container with close button
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .aspectRatio(1f)
                ) {
                    coil.compose.AsyncImage(
                        model = selectedProfileImage,
                        contentDescription = "Profile picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                    
                    // Small close button at top-right corner with margin from container
                    IconButton(
                        onClick = { selectedProfileImage = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 12.dp, end = 12.dp)
                            .size(20.dp)
                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White,
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThreadListItem(
    thread: Thread,
    onClick: () -> Unit,
    isOnline: Boolean = false,
    currentUserId: String = "",
    unreadCount: Int = 0,
    onAvatarClick: (String) -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (unreadCount > 0) Color(0xFF252528) else Color(0xFF1C1C1E)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // User avatar with online indicator
            Box {
                if (thread.displayPhoto != null) {
                    coil.compose.AsyncImage(
                        model = thread.displayPhoto,
                        contentDescription = "User avatar",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .clickable {
                                // Show full-size image
                                onAvatarClick(thread.displayPhoto)
                            }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0084FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (thread.displayName ?: "U").take(2).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                }
                
                // Green online indicator - filled circle
                if (isOnline && thread.user2Id != currentUserId) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd)
                            .clip(CircleShape)
                            .background(Color(0xFF00D856))
                    )
                }
            }
            
            // User name and message preview
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = thread.displayName ?: "Unknown User",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.SemiBold,
                    color = Color.White,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tap to open conversation",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            
            // Unread count badge
            if (unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE6002A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
