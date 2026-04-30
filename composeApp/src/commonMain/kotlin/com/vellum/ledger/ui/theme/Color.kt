package com.vellum.ledger.ui.theme

import androidx.compose.ui.graphics.Color

// Light Theme Palette (Tailwind Config)
val Primary = Color(0xFF3525CD)
val OnPrimary = Color(0xFFFFFFFF)
val PrimaryContainer = Color(0xFF4F46E5)
val OnPrimaryContainer = Color(0xFFDAD7FF)

val Secondary = Color(0xFF006E2F)
val OnSecondary = Color(0xFFFFFFFF)
val SecondaryContainer = Color(0xFF6BFF8F)
val OnSecondaryContainer = Color(0xFF007432)

val Tertiary = Color(0xFF7E3000)
val OnTertiary = Color(0xFFFFFFFF)
val TertiaryContainer = Color(0xFFA44100)
val OnTertiaryContainer = Color(0xFFFFD2BE)

val Error = Color(0xFFBA1A1A)
val OnError = Color(0xFFFFFFFF)
val ErrorContainer = Color(0xFFFFDAD6)
val OnErrorContainer = Color(0xFF93000A)

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

// Dark Theme Palette (FinTrack / LedgerSync design)
val DarkPrimary = Color(0xFFC3C0FF) // primary
val DarkOnPrimary = Color(0xFF1D00A5) // on-primary
val DarkBackground = Color(0xFF0B1326) // background
val DarkOnBackground = Color(0xFFDAE2FD) // on-background
val DarkSurface = Color(0xFF0B1326) // surface
val DarkOnSurface = Color(0xFFDAE2FD) // on-surface
val DarkSurfaceVariant = Color(0xFF2D3449) // surface-variant
val DarkOnSurfaceVariant = Color(0xFFC7C4D8) // on-surface-variant

val DarkSecondary = Color(0xFF4EDEA3) // secondary
val DarkSecondaryContainer = Color(0xFF00A572) // secondary-container
val DarkOnSecondaryContainer = Color(0xFF00311F) // on-secondary-container

val DarkError = Color(0xFFFFB4AB) // error
val DarkOnError = Color(0xFF690005) // on-error

// Semantic Mappings
val Income = Secondary
val Expense = Error
val Pending = Color(0xFFB45309)
val Synced = Secondary
val Syncing = Primary
val Failed = Error
