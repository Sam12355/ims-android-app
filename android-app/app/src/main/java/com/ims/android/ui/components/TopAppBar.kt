package com.ims.android.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
    onlineMembers: List<OnlineMember> = emptyList()
) {
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
            // Admin/Manager: show online members (excluding self) to the left of the search icon
            val isAdminOrManager = userProfile?.userRole == UserRole.ADMIN || userProfile?.userRole == UserRole.MANAGER
            // Filter out current user from online members
            val otherOnlineMembers = onlineMembers.filter { it.id != userProfile?.id }
            
            if (isAdminOrManager && otherOnlineMembers.isNotEmpty()) {
                Row(
                    modifier = Modifier.padding(end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy((-6).dp) // Overlap avatars slightly
                ) {
                    // Show up to 4 small avatars with online indicator
                    otherOnlineMembers.take(4).forEach { member ->
                        Box(
                            modifier = Modifier.size(34.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Avatar with border for circle effect
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
                            
                            // Green online indicator dot
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
                                color = Color(0xFF22C55E) // Green
                            ) {
                                // Empty - just a green dot
                            }
                        }
                    }

                    // Show count if more than 4
                    if (otherOnlineMembers.size > 4) {
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
                                    text = "+${otherOnlineMembers.size - 4}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Search icon
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            }
            
            // Theme toggle - Sun/Moon icon
            IconButton(onClick = onThemeToggle) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                    tint = if (isDarkTheme) Color(0xFFFFA500) else MaterialTheme.colorScheme.onSurface // Orange sun in dark mode
                )
            }
            
            // Notifications with badge
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