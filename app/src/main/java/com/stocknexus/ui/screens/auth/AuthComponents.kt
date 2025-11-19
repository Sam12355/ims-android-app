package com.stocknexus.ui.screens.auth

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.stocknexus.data.model.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignInCard(
    formState: AuthFormState,
    rememberMe: Boolean,
    passwordVisible: Boolean,
    onFormStateChange: (AuthFormState) -> Unit,
    onRememberMeChange: (Boolean) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onSignIn: (String, String, Boolean) -> Unit,
    onSwitchToSignUp: () -> Unit,
    onForgotPassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineSmall, 
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Sign in to access your inventory management system",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            
            // Email Field
            OutlinedTextField(
                value = formState.email,
                onValueChange = { 
                    onFormStateChange(
                        formState.copy(
                            email = it,
                            errorMessage = null,
                            emailError = if (it.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches()) 
                                "Invalid email format" else null
                        )
                    )
                },
                label = { Text("Email") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                enabled = !formState.isLoading,
                singleLine = true,
                isError = formState.emailError != null,
                supportingText = formState.emailError?.let { { Text(it) } }
            )
            
            // Password Field
            OutlinedTextField(
                value = formState.password,
                onValueChange = { 
                    onFormStateChange(
                        formState.copy(
                            password = it,
                            errorMessage = null,
                            passwordError = if (it.isNotBlank() && it.length < 6) 
                                "Password must be at least 6 characters" else null
                        )
                    )
                },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onPasswordVisibilityToggle) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                enabled = !formState.isLoading,
                singleLine = true,
                isError = formState.passwordError != null,
                supportingText = formState.passwordError?.let { { Text(it) } }
            )
            
            // Remember Me & Forgot Password Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onRememberMeChange(!rememberMe) }
                ) {
                    Checkbox(
                        checked = rememberMe,
                        onCheckedChange = onRememberMeChange,
                        enabled = !formState.isLoading
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Remember me",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                
                Text(
                    text = "Forgot Password?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE6002A),
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { 
                        if (!formState.isLoading) onForgotPassword() 
                    }
                )
            }
            
            // Error Message
            if (formState.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formState.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            // Sign In Button
            Button(
                onClick = { 
                    android.util.Log.d("StockNexus", "Sign In button clicked! Email: ${formState.email}, Password length: ${formState.password.length}")
                    onSignIn(formState.email, formState.password, rememberMe) 
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = formState.isSignInValid && !formState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE6002A)
                ),
                contentPadding = PaddingValues(16.dp)
            ) {
                if (formState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (formState.isLoading) "Signing In..." else "Sign In (${formState.isSignInValid}, email:${formState.emailError}, pass:${formState.passwordError})",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Switch to Sign Up
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Sign Up",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE6002A),
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { 
                        if (!formState.isLoading) onSwitchToSignUp() 
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpCard(
    formState: AuthFormState,
    passwordVisible: Boolean,
    confirmPasswordVisible: Boolean,
    onFormStateChange: (AuthFormState) -> Unit,
    onPasswordVisibilityToggle: () -> Unit,
    onConfirmPasswordVisibilityToggle: () -> Unit,
    onSignUp: (SignUpRequest) -> Unit,
    onSwitchToSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Join your organization's inventory management system",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            
            // Full Name Field
            OutlinedTextField(
                value = formState.name,
                onValueChange = { 
                    onFormStateChange(
                        formState.copy(
                            name = it,
                            errorMessage = null,
                            nameError = if (it.isNotBlank() && it.length < 2) 
                                "Name must be at least 2 characters" else null
                        )
                    )
                },
                label = { Text("Full Name") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
                enabled = !formState.isLoading,
                singleLine = true,
                isError = formState.nameError != null,
                supportingText = formState.nameError?.let { { Text(it) } }
            )
            
            // Email Field
            OutlinedTextField(
                value = formState.email,
                onValueChange = { 
                    onFormStateChange(
                        formState.copy(
                            email = it,
                            errorMessage = null,
                            emailError = if (it.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches()) 
                                "Invalid email format" else null
                        )
                    )
                },
                label = { Text("Email") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                enabled = !formState.isLoading,
                singleLine = true,
                isError = formState.emailError != null,
                supportingText = formState.emailError?.let { { Text(it) } }
            )
            
            // Password Field
            OutlinedTextField(
                value = formState.password,
                onValueChange = { 
                    onFormStateChange(
                        formState.copy(
                            password = it,
                            errorMessage = null,
                            passwordError = if (it.isNotBlank() && it.length < 6) 
                                "Password must be at least 6 characters" else null
                        )
                    )
                },
                label = { Text("Password") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onPasswordVisibilityToggle) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                enabled = !formState.isLoading,
                singleLine = true,
                isError = formState.passwordError != null,
                supportingText = formState.passwordError?.let { { Text(it) } }
            )
            
            // Confirm Password Field
            OutlinedTextField(
                value = formState.confirmPassword,
                onValueChange = { 
                    onFormStateChange(
                        formState.copy(
                            confirmPassword = it,
                            errorMessage = null
                        )
                    )
                },
                label = { Text("Confirm Password") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null
                    )
                },
                trailingIcon = {
                    IconButton(onClick = onConfirmPasswordVisibilityToggle) {
                        Icon(
                            imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                enabled = !formState.isLoading,
                singleLine = true,
                isError = formState.confirmPassword.isNotBlank() && !formState.doPasswordsMatch,
                supportingText = if (formState.confirmPassword.isNotBlank() && !formState.doPasswordsMatch) {
                    { Text("Passwords do not match") }
                } else null
            )
            
            // Error Message
            if (formState.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formState.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            // Sign Up Button
            Button(
                onClick = { 
                    onSignUp(
                        SignUpRequest(
                            email = formState.email,
                            password = formState.password,
                            name = formState.name,
                            role = "staff" // Default role
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = formState.isSignUpValid && !formState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE6002A)
                ),
                contentPadding = PaddingValues(16.dp)
            ) {
                if (formState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (formState.isLoading) "Creating Account..." else "Create Account",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Switch to Sign In
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE6002A),
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { 
                        if (!formState.isLoading) onSwitchToSignIn() 
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordCard(
    formState: AuthFormState,
    onFormStateChange: (AuthFormState) -> Unit,
    onResetPassword: (String) -> Unit,
    onBackToSignIn: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Reset Password",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Enter your email address and we'll send you a link to reset your password",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            
            // Email Field
            OutlinedTextField(
                value = formState.email,
                onValueChange = { 
                    onFormStateChange(
                        formState.copy(
                            email = it,
                            errorMessage = null,
                            emailError = if (it.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(it).matches()) 
                                "Invalid email format" else null
                        )
                    )
                },
                label = { Text("Email") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = null
                    )
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
                enabled = !formState.isLoading,
                singleLine = true,
                isError = formState.emailError != null,
                supportingText = formState.emailError?.let { { Text(it) } }
            )
            
            // Error Message
            if (formState.errorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = formState.errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
            
            // Reset Password Button
            Button(
                onClick = { onResetPassword(formState.email) },
                modifier = Modifier.fillMaxWidth(),
                enabled = formState.isEmailValid && !formState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE6002A)
                ),
                contentPadding = PaddingValues(16.dp)
            ) {
                if (formState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = if (formState.isLoading) "Sending..." else "Send Reset Link",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            // Back to Sign In
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Remember your password? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFE6002A),
                    fontWeight = FontWeight.SemiBold,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { 
                        if (!formState.isLoading) onBackToSignIn() 
                    }
                )
            }
        }
    }
}