package com.ims.android.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ims.android.data.model.Profile
import com.ims.android.data.model.UserRole
import com.ims.android.data.model.OnlineMember
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBar(
    title: String,
    userProfile: Profile?,
    onNavigationClick: () -> Unit,
    onThemeToggle: () -> Unit,
    onNotificationsClick: () -> Unit,
    onSearchClick: () -> Unit,
    notificationCount: Int = 0,
    isDarkTheme: Boolean = true
    ,
    onlineMembers: List<OnlineMember> = emptyList(),
    onInboxClick: () -> Unit = {},
    onAvatarClick: (OnlineMember) -> Unit = {},
    unreadMessagesCount: Int = 0
) {
    // Debug logging for online members
    androidx.compose.runtime.LaunchedEffect(onlineMembers) {
        android.util.Log.d("TopAppBar", "👥 Online members received: ${onlineMembers.size} - $onlineMembers")
    }
    
    CenterAlignedTopAppBar(
        title = {
            // Empty title - no page name displayed
        },
        navigationIcon = {
            IconButton(onClick = onNavigationClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = "Open navigation drawer"
                )
            }
        },
        actions = {
            // Always show envelope icon first, then stacked online member avatars
            Row(
                modifier = Modifier.padding(end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Envelope icon - always visible with unread badge
                Box {
                    IconButton(onClick = onInboxClick) {
                        Icon(
                            imageVector = Icons.Default.MailOutline,
                            contentDescription = "Inbox"
                        )
                    }
                    if (unreadMessagesCount > 0) {
                        Badge(
                            containerColor = Color(0xFFE6002A),
                            contentColor = Color.White,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-2).dp, y = 6.dp)
                        ) {
                            Text(
                                text = if (unreadMessagesCount > 99) "99+" else unreadMessagesCount.toString(),
                                fontSize = 9.sp
                            )
                        }
                    }
                }
                
                // Stacked online member avatars (if any)
                if (onlineMembers.isNotEmpty()) {
                    Row(
                        modifier = Modifier,
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy((-12).dp) // More overlap
                    ) {
                        onlineMembers.take(4).forEach { member ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clickable { onAvatarClick(member) },
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .border(
                                            width = 2.dp,
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = CircleShape
                                        ),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    if (!member.photoUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = member.photoUrl,
                                            contentDescription = member.name ?: "Online user",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Text(
                                                text = member.name?.firstOrNull()?.uppercase() ?: "?",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                }
                                // Green online indicator
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .size(10.dp)
                                        .offset(x = 0.dp, y = 0.dp)
                                        .border(
                                            width = 1.5.dp,
                                            color = MaterialTheme.colorScheme.surface,
                                            shape = CircleShape
                                        ),
                                    shape = CircleShape,
                                    color = Color(0xFF22C55E)
                                ) {}
                            }
                        }
                        // Show +N indicator if more than 4 members
                        if (onlineMembers.size > 4) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .size(28.dp)
                                    .border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.surface,
                                        shape = CircleShape
                                    )
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "+${onlineMembers.size - 4}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Always show search, theme toggle, and notifications icons
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            }
            IconButton(onClick = onThemeToggle) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                    tint = if (isDarkTheme) Color(0xFFFFA500) else MaterialTheme.colorScheme.onSurface
                )
            }
            Box {
                IconButton(onClick = onNotificationsClick) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Notifications"
                    )
                }
                if (notificationCount > 0) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = (-6).dp, y = 6.dp)
                            .size(18.dp),
                        shape = CircleShape,
                        color = Color(0xFFE6002A)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = if (notificationCount > 99) "99+" else notificationCount.toString(),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = MaterialTheme.typography.labelSmall.fontSize * 0.7
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun SlideshowHeader(
    modifier: Modifier = Modifier
) {
    // This matches the web app's SlideshowHeader component
    // For now, we'll show the current branch info
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Business,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Stock Nexus",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}