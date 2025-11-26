package com.ims.android.ui.toast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ims.android.R
import com.ims.android.data.model.UserOnlineEvent
import kotlinx.coroutines.delay

@Composable
fun UserOnlineToastHost(modifier: Modifier = Modifier) {
    val eventFlow = UserOnlineToastManager.event
    val userEvent by eventFlow.collectAsState()

    var visible by remember { mutableStateOf(false) }

    // When a new event arrives, show toast briefly
    LaunchedEffect(userEvent) {
        if (userEvent != null) {
            visible = true
            delay(3000)
            visible = false
            // clear the event after hiding
            UserOnlineToastManager.post(null)
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
        modifier = modifier
            .fillMaxWidth()
    ) {
        // Compact rectangular 'glass' bar without border or rounded corners
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.08f),
                            Color.White.copy(alpha = 0.04f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            val name = userEvent?.userName ?: "Someone"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                if (!userEvent?.photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = userEvent?.photoUrl,
                        contentDescription = "user photo",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        placeholder = painterResource(id = R.drawable.ic_person_placeholder),
                        error = painterResource(id = R.drawable.ic_person_placeholder)
                    )
                } else {
                    // fallback placeholder
                    androidx.compose.foundation.Image(
                        painter = painterResource(id = R.drawable.ic_person_placeholder),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "is now online",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }

                // small green online dot
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00C853))
                )
            }
        }
    }
}
