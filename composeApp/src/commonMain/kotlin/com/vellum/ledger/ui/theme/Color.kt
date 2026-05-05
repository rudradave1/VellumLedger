package com.vellum.ledger.ui.theme

import androidx.compose.ui.graphics.Color

// Base Brand Colors
val Primary = Color(0xFF3525CD)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFF4F46E5)
val OnPrimaryContainer = Color(0xFFDAD7FF)

val Secondary = Color(0xFF10B981) // Updated to a vibrant green for success/income
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFFD1FAE5)
val OnSecondaryContainer = Color(0xFF065F46)

val Tertiary = Color(0xFF7E3000)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFA44100)
val OnTertiaryContainer = Color(0xFFFFD2BE)

val Error = Color(0xFFEF4444) // Updated to a vibrant red for errors/expenses
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFEE2E2)
val OnErrorContainer = Color(0xFF991B1B)

val Background = Color(0xFFFAF8FF)
val OnBackground = Color(0xFF131B2E)

val Surface = Color(0xFFFAF8FF)
val OnSurface = Color(0xFF131B2E)
val SurfaceVariant = Color(0xFFDAE2FD)
val OnSurfaceVariant = Color(0xFF464555)

val Outline = Color(0xFF777587)
val OutlineVariant = Color(0xFFC7C4D8)

// Surface Containers
val SurfaceContainerLowest = Color(0xFFFFFFFF)
val SurfaceContainerLow = Color(0xFFF2F3FF)
val SurfaceContainer = Color(0xFFEAEDFF)
val SurfaceContainerHigh = Color(0xFFE2E7FF)
val SurfaceContainerHighest = Color(0xFFDAE2FD)

// Dark Theme Palette
val DarkPrimary = Color(0xFFC3C0FF)
val DarkOnPrimary = Color(0xFF1D00A5)
val DarkBackground = Color(0xFF0B1326)
val DarkOnBackground = Color(0xFFDAE2FD)
val DarkSurface = Color(0xFF0B1326)
val DarkOnSurface = Color(0xFFDAE2FD)
val DarkSurfaceVariant = Color(0xFF2D3449)
val DarkOnSurfaceVariant = Color(0xFFC7C4D8)

val DarkSecondary = Color(0xFF4EDEA3)
val DarkSecondaryContainer = Color(0xFF00A572)
val DarkOnSecondaryContainer = Color(0xFF00311F)

val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)

// Semantic Mappings
val IncomeColor = Color(0xFF10B981)
val ExpenseColor = Color(0xFFEF4444)
val WarningColor = Color(0xFFF59E0B)
val InfoColor = Color(0xFF3B82F6)

val PendingColor = WarningColor
val SyncedColor = IncomeColor
val SyncingColor = Primary
val FailedColor = ExpenseColor

// Chart & Analytics Palette
val ChartPalette = listOf(
    Color(0xFF3525CD), // Indigo
    Color(0xFF6366F1), // Primary Light
    Color(0xFF8B5CF6), // Violet
    Color(0xFFEC4899), // Pink
    Color(0xFFF59E0B), // Amber
    Color(0xFF10B981)  // Emerald
)
