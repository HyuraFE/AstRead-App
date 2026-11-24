package fun.pylexia.astread.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

val MidnightBackground = Color(0xFF0D0F17)
val MidnightSurface = Color(0xFF161926)
val MidnightSurfaceVariant = Color(0xFF202436)
val GoldAccent = Color(0xFFE5A93B)
val TextPrimary = Color(0xFFF4F4F6)
val TextSecondary = Color(0xFF9E9EA8)

val DarkColorScheme = darkColorScheme(
    primary = GoldAccent,
    onPrimary = MidnightBackground,
    background = MidnightBackground,
    onBackground = TextPrimary,
    surface = MidnightSurface,
    onSurface = TextPrimary,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = TextSecondary
)
