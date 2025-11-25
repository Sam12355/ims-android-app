package com.ims.android.presentation.theme

import androidx.compose.ui.graphics.Color

// Primary brand colors matching web app
val StockNexusPrimary = Color(0xFFE6002A)
val StockNexusPrimaryVariant = Color(0xFFB8001F)
val StockNexusSecondary = Color(0xFF6C757D)

// Light theme colors
val LightPrimary = StockNexusPrimary
val LightOnPrimary = Color.White
val LightPrimaryContainer = Color(0xFFFFDAD6)
val LightOnPrimaryContainer = Color(0xFF410002)

val LightSecondary = StockNexusSecondary
val LightOnSecondary = Color.White
val LightSecondaryContainer = Color(0xFFE9ECEF)
val LightOnSecondaryContainer = Color(0xFF343A40)

val LightTertiary = Color(0xFF2D3748) // Dark charcoal instead of purple
val LightOnTertiary = Color.White
val LightTertiaryContainer = Color(0xFFE2E8F0)
val LightOnTertiaryContainer = Color(0xFF1A202C)

val LightError = Color(0xFFBA1A1A)
val LightErrorContainer = Color(0xFFFFDAD6)
val LightOnError = Color.White
val LightOnErrorContainer = Color(0xFF410002)

val LightBackground = Color(0xFFFFFBFE) // White background for light mode
val LightOnBackground = Color(0xFF1C1B1F) // Dark text on light background
val LightSurface = Color(0xFFFFFFFF) // Pure white for cards
val LightOnSurface = Color(0xFF1C1B1F) // Dark text on light surface
val LightSurfaceVariant = Color(0xFFF5F5F5) // Light gray for variants
val LightOnSurfaceVariant = Color(0xFF49454F)
val LightOutline = Color(0xFF79747E)

// Dark theme colors
val DarkPrimary = StockNexusPrimary
val DarkOnPrimary = Color.White
val DarkPrimaryContainer = Color(0xFF93000A)
val DarkOnPrimaryContainer = Color(0xFFFFDAD6)

val DarkSecondary = Color(0xFFC7C7C7)
val DarkOnSecondary = Color(0xFF2E2E2E)
val DarkSecondaryContainer = Color(0xFF454545)
val DarkOnSecondaryContainer = Color(0xFFE3E3E3)

val DarkTertiary = Color(0xFFA0AEC0) // Light gray instead of purple
val DarkOnTertiary = Color(0xFF1A202C)
val DarkTertiaryContainer = Color(0xFF2D3748)
val DarkOnTertiaryContainer = Color(0xFFE2E8F0)

val DarkError = Color(0xFFFFB4AB)
val DarkErrorContainer = Color(0xFF93000A)
val DarkOnError = Color(0xFF690005)
val DarkOnErrorContainer = Color(0xFFFFDAD6)

val DarkBackground = Color(0xFF1C1B1F)
val DarkOnBackground = Color(0xFFE6E1E5)
val DarkSurface = Color(0xFF1C1B1F)
val DarkOnSurface = Color(0xFFE6E1E5)
val DarkSurfaceVariant = Color(0xFF2B2B2B)
val DarkOnSurfaceVariant = Color(0xFFCAC4D0)
val DarkOutline = Color(0xFF938F99)

// Custom colors for specific components
val SuccessColor = Color(0xFF00C851)
val WarningColor = Color(0xFFFF8800)
val InfoColor = Color(0xFF33B5E5)

// Chart colors matching web app
val ChartColors = listOf(
    StockNexusPrimary,
    Color(0xFF00C851),
    Color(0xFF33B5E5),
    Color(0xFFFF8800),
    Color(0xFF2D3748), // Dark charcoal instead of purple
    Color(0xFFFF5722),
    Color(0xFF4CAF50),
    Color(0xFF2196F3)
)