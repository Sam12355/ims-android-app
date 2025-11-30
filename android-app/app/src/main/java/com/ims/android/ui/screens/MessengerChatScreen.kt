package com.ims.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.ims.android.data.model.Message
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.animation.core.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessengerChatScreen(
    messages: List<Message>,
    otherUserName: String = "User",
    otherUserPhoto: String? = null,
    currentUserId: String,
    isTyping: Boolean = false,
    isOnline: Boolean = false,
    onSend: (String) -> Unit,
    onTyping: (Boolean) -> Unit = {},
    onBack: (() -> Unit)? = null
) {
    var input by remember { mutableStateOf("") }
    var showProfileImageDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    var isCurrentlyTyping by remember { mutableStateOf(false) }
    
    // Handle typing indicator with debounce
    LaunchedEffect(input) {
        if (input.isNotEmpty()) {
            if (!isCurrentlyTyping) {
                isCurrentlyTyping = true
                onTyping(true)
                android.util.Log.d("MessengerChatScreen", "⌨️ Started typing")
            }
            // Reset the stop-typing timer on each keystroke
            kotlinx.coroutines.delay(2000)
            if (isCurrentlyTyping && input.isNotEmpty()) {
                isCurrentlyTyping = false
                onTyping(false)
                android.util.Log.d("MessengerChatScreen", "⏸️ Stopped typing (timeout)")
            }
        } else if (isCurrentlyTyping) {
            // Input is empty, stop typing immediately
            isCurrentlyTyping = false
            onTyping(false)
            android.util.Log.d("MessengerChatScreen", "⏸️ Stopped typing (cleared)")
        }
    }
    
    // Auto-scroll to bottom only when the last message changes and only if the user
    // is already near the bottom or the last message was sent by the current user.
    val lastMessageId = messages.lastOrNull()?.id
    var prevLastId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(lastMessageId, isTyping) {
        try {
            if (lastMessageId == null || lastMessageId == prevLastId) return@LaunchedEffect

            val lastIndex = messages.size - 1

            // If this is the first time we're seeing the conversation (prevLastId == null)
            // always scroll to bottom — but wait until the LazyColumn has measured and
            // has items to avoid race conditions on some devices where scrolling fails.
            if (prevLastId == null) {
                // Wait for the list to have at least 1 item (layout pass finished)
                // snapshotFlow will observe composition state and suspend until the condition
                // is met; this avoids calling animateScrollToItem before the LazyColumn is ready.
                // Wait (cooperatively) for LazyColumn to populate its layout info.
                while (listState.layoutInfo.totalItemsCount == 0) {
                    kotlinx.coroutines.delay(50)
                }
                try {
                    if (lastIndex >= 0) {
                        listState.animateScrollToItem(lastIndex)
                    }
                } catch (e: Exception) {
                    // fall back to synchronous scroll if animate fails
                    listState.scrollToItem(lastIndex)
                }
                prevLastId = lastMessageId
                return@LaunchedEffect
            }

            // Determine if the user is currently scrolled near the bottom (within 3 items)
            val isAtBottom = listState.firstVisibleItemIndex >= (messages.size - 4)

            val lastMsg = messages.lastOrNull()

            if (lastMsg != null && (lastMsg.senderId == currentUserId || isAtBottom || isTyping)) {
                // Scroll to bottom where appropriate
                if (lastIndex >= 0) listState.animateScrollToItem(lastIndex)
            }

            prevLastId = lastMessageId
        } catch (e: Exception) {
            android.util.Log.e("MessengerChatScreen", "Error in auto-scroll logic", e)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Back button
                        if (onBack != null) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        }
                        
                        // User avatar with online indicator - smaller size, clickable
                        Box {
                            if (otherUserPhoto != null) {
                                AsyncImage(
                                    model = otherUserPhoto,
                                    contentDescription = "User avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape)
                                        .clickable {
                                            // Show full-size image dialog
                                            showProfileImageDialog = true
                                        }
                                )
                            } else {
                                // Default avatar with initials (not clickable since no photo)
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                        .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = otherUserName.take(2).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                            
                            // Green online indicator - filled circle
                            if (isOnline) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(Color(0xFF00D856))
                                )
                            }
                        }
                        
                        // User name
                        Text(
                            text = otherUserName,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1C1C1E),
                    titleContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF000000)
    ) { _ ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Messages list - use distinctBy to prevent duplicate key crash
                val uniqueMessages = remember(messages) { messages.distinctBy { it.id } }
                
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(items = uniqueMessages, key = { it.id }) { message ->
                        MessageBubble(
                            message = message,
                            isFromCurrentUser = message.senderId == currentUserId
                        )
                    }
                }

                Spacer(modifier = Modifier.height(0.dp))

                // Message input bar - pinned to absolute bottom
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 4.dp,
                    color = Color(0xFF1C1C1E)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Message input field
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Type a message...", color = Color.Gray) },
                            shape = RoundedCornerShape(24.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (input.isNotBlank()) {
                                        // Stop typing indicator before sending
                                        if (isCurrentlyTyping) {
                                            isCurrentlyTyping = false
                                            onTyping(false)
                                        }
                                        onSend(input.trim())
                                        input = ""
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.White,
                                unfocusedBorderColor = Color.Gray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color.White
                            ),
                            maxLines = 4
                        )

                        // Send button
                        FloatingActionButton(
                            onClick = {
                                if (input.isNotBlank()) {
                                    // Stop typing indicator before sending
                                    if (isCurrentlyTyping) {
                                        isCurrentlyTyping = false
                                        onTyping(false)
                                    }
                                    onSend(input.trim())
                                    input = ""
                                }
                            },
                            modifier = Modifier.size(48.dp),
                            containerColor = if (input.isNotBlank()) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Send,
                                contentDescription = "Send",
                                tint = if (input.isNotBlank()) 
                                    MaterialTheme.colorScheme.onPrimary 
                                else 
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } // End Column
            
            // Typing indicator overlay - positioned at bottom-right corner
            if (isTyping) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0084FF),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 80.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        repeat(3) { index ->
                            val infiniteTransition = rememberInfiniteTransition(label = "dot_$index")
                            val offsetY by infiniteTransition.animateFloat(
                                initialValue = 0f,
                                targetValue = -5f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(
                                        durationMillis = 600,
                                        delayMillis = index * 150,
                                        easing = FastOutSlowInEasing
                                    ),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "bounce_$index"
                            )
                            
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .offset(y = offsetY.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.9f))
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Full-size profile image dialog
    if (showProfileImageDialog && otherUserPhoto != null) {
        Dialog(onDismissRequest = { showProfileImageDialog = false }) {
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
                    // Square centered image
                    AsyncImage(
                        model = otherUserPhoto,
                        contentDescription = "Profile picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(12.dp))
                    )
                    
                    // Small close button at top-right corner with margin from container
                    IconButton(
                        onClick = { showProfileImageDialog = false },
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
fun MessageBubble(
    message: Message,
    isFromCurrentUser: Boolean
) {
    // Current user messages: LEFT side, WHITE bubble, BLACK text
    // Other person's messages: RIGHT side, BLUE bubble, WHITE text
    val bubbleColor = if (isFromCurrentUser) {
        Color.White // White for your messages (left)
    } else {
        Color(0xFF0084FF) // Blue for received messages (right)
    }
    
    val textColor = if (isFromCurrentUser) {
        Color.Black // Black text for your messages
    } else {
        Color.White // White text for received
    }
    
    val alignment = if (isFromCurrentUser) Alignment.Start else Alignment.End

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = if (isFromCurrentUser) 
            Arrangement.Start 
        else 
            Arrangement.End
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (isFromCurrentUser) 4.dp else 16.dp,
                bottomEnd = if (isFromCurrentUser) 16.dp else 4.dp
            ),
            color = bubbleColor,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                // Message content
                Text(
                    text = message.content,
                    color = textColor,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Timestamp with delivery status
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = formatMessageTime(message.sentAt),
                        color = textColor.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp
                    )
                    
                    // Delivery & Read receipts for sent messages
                    if (isFromCurrentUser) {
                        Spacer(modifier = Modifier.width(3.dp))
                        when {
                            message.readAt != null -> {
                                // Read - Blue double tick (very close together)
                                Text(
                                    text = "✓✓",
                                    color = Color(0xFF0084FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Normal,
                                    letterSpacing = (-3).sp
                                )
                            }
                            message.deliveredAt != null -> {
                                // Delivered - Gray single tick
                                Text(
                                    text = "✓",
                                    color = textColor.copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                            }
                            else -> {
                                // Sent - Gray single tick (clock icon alternative)
                                Text(
                                    text = "✓",
                                    color = textColor.copy(alpha = 0.3f),
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatMessageTime(instant: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}
