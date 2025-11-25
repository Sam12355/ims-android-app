package com.ims.android.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ims.android.data.model.User
import com.ims.android.data.repository.EnhancedAuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    authRepository: EnhancedAuthRepository,
    onNavigateToAuth: () -> Unit,
    onNavigateToDashboard: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(Unit) {
        scope.launch {
            // Show splash for a minimum time
            delay(2000)
            
            // Check if user is already logged in
            val session = authRepository.getSession()
            if (session != null) {
                // Validate token
                val isValid = authRepository.isTokenValid()
                if (isValid) {
                    // Navigate to dashboard with current user
                    onNavigateToDashboard(session.user)
                } else {
                    // Clear invalid session and go to auth
                    authRepository.clearSession()
                    onNavigateToAuth()
                }
            } else {
                // No session, go to auth
                onNavigateToAuth()
            }
        }
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE6002A),
                        Color(0xFFCC0024)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Icon(
                imageVector = Icons.Default.Store,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.White
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // App Name
            Text(
                text = "Stock Nexus",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = "Inventory Management System",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            // Loading indicator
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Loading...",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}