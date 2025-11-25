package com.ims.android.ui.screens.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.ims.android.data.model.*
import com.ims.android.data.repository.EnhancedAuthRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedAuthScreen(
    authRepository: EnhancedAuthRepository,
    onLoginSuccess: (User) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentScreen by remember { mutableStateOf<AuthScreen>(AuthScreen.SignIn) }
    var formState by remember { mutableStateOf(AuthFormState()) }
    var rememberMe by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    
    val scope = rememberCoroutineScope()
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier.fillMaxSize()
        ) {
            // Left Panel - Branding (Only shown on tablets/landscape)
            if (false) { // Disabled for mobile-first approach
                BrandingPanel(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
            
            // Right Panel - Auth Forms
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Mobile Logo
                    AppLogo()
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Auth Content
                    when (currentScreen) {
                        AuthScreen.SignIn -> {
                            SignInCard(
                                formState = formState,
                                rememberMe = rememberMe,
                                passwordVisible = passwordVisible,
                                onFormStateChange = { formState = it },
                                onRememberMeChange = { rememberMe = it },
                                onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                                onSignIn = { email, password, remember ->
                                    scope.launch {
                                        formState = formState.copy(isLoading = true, errorMessage = null)
                                        
                                        val result = authRepository.signIn(
                                            SignInRequest(email, password),
                                            remember
                                        )
                                        
                                        formState = formState.copy(isLoading = false)
                                        
                                        if (result.isSuccess) {
                                            onLoginSuccess(result.getOrThrow().user)
                                        } else {
                                            formState = formState.copy(
                                                errorMessage = result.exceptionOrNull()?.message ?: "Sign in failed"
                                            )
                                        }
                                    }
                                },
                                onSwitchToSignUp = { currentScreen = AuthScreen.SignUp },
                                onForgotPassword = { currentScreen = AuthScreen.ForgotPassword }
                            )
                        }
                        
                        AuthScreen.SignUp -> {
                            SignUpCard(
                                formState = formState,
                                passwordVisible = passwordVisible,
                                confirmPasswordVisible = confirmPasswordVisible,
                                onFormStateChange = { formState = it },
                                onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                                onConfirmPasswordVisibilityToggle = { confirmPasswordVisible = !confirmPasswordVisible },
                                onSignUp = { request ->
                                    scope.launch {
                                        formState = formState.copy(isLoading = true, errorMessage = null)
                                        val result = authRepository.signUp(request)
                                        if (result.isSuccess) {
                                            // For demo, auto-login after signup
                                            onLoginSuccess(result.getOrThrow().user)
                                        } else {
                                            formState = formState.copy(
                                                isLoading = false,
                                                errorMessage = result.exceptionOrNull()?.message ?: "Sign up failed"
                                            )
                                        }
                                    }
                                },
                                onSwitchToSignIn = { currentScreen = AuthScreen.SignIn }
                            )
                        }
                        
                        AuthScreen.ForgotPassword -> {
                            ForgotPasswordCard(
                                formState = formState,
                                onFormStateChange = { formState = it },
                                onResetPassword = { email ->
                                    scope.launch {
                                        formState = formState.copy(isLoading = true, errorMessage = null)
                                        val result = authRepository.forgotPassword(PasswordResetRequest(email))
                                        if (result.isSuccess) {
                                            // Show success message and return to sign in
                                            formState = formState.copy(
                                                isLoading = false,
                                                errorMessage = null
                                            )
                                            currentScreen = AuthScreen.SignIn
                                        } else {
                                            formState = formState.copy(
                                                isLoading = false,
                                                errorMessage = result.exceptionOrNull()?.message ?: "Reset failed"
                                            )
                                        }
                                    }
                                },
                                onBackToSignIn = { currentScreen = AuthScreen.SignIn }
                            )
                        }
                        
                        else -> {
                            // Handle other screens like email verification
                            SignInCard(
                                formState = formState,
                                rememberMe = rememberMe,
                                passwordVisible = passwordVisible,
                                onFormStateChange = { formState = it },
                                onRememberMeChange = { rememberMe = it },
                                onPasswordVisibilityToggle = { passwordVisible = !passwordVisible },
                                onSignIn = { email, password, remember -> },
                                onSwitchToSignUp = { currentScreen = AuthScreen.SignUp },
                                onForgotPassword = { currentScreen = AuthScreen.ForgotPassword }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppLogo() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Store,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = Color(0xFFE6002A)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "IMS",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE6002A)
        )
    }
}

@Composable
private fun BrandingPanel(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE6002A),
                        Color(0xFFCC0024)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = Icons.Default.Store,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color.White
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Inventory Management System",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Text(
                text = "Streamline your stock management across all branches",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            BrandingFeature(
                icon = Icons.Default.People,
                title = "Multi-Role Management", 
                description = "Admin, Manager, Assistant Manager, and Staff roles"
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            BrandingFeature(
                icon = Icons.Default.BarChart,
                title = "Real-time Analytics",
                description = "Track usage patterns and inventory trends"
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            BrandingFeature(
                icon = Icons.Default.Shield,
                title = "Smart Notifications", 
                description = "Email, SMS, and WhatsApp alerts for low stock"
            )
        }
    }
}

@Composable
private fun BrandingFeature(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(8.dp),
            color = Color.White.copy(alpha = 0.2f)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}