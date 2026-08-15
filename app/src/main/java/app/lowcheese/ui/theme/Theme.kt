package app.lowcheese.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Cheddar,
    onPrimary = Ink,
    primaryContainer = CheddarSoft,
    onPrimaryContainer = Cheddar,
    secondary = CreamMuted,
    onSecondary = Ink,
    tertiary = LiveCoral,
    onTertiary = Cream,
    background = Ink,
    onBackground = Cream,
    surface = Ink,
    onSurface = Cream,
    surfaceVariant = InkElevated,
    onSurfaceVariant = CreamMuted,
    surfaceContainerLowest = Ink,
    surfaceContainerLow = InkElevated,
    surfaceContainer = InkElevated,
    surfaceContainerHigh = InkHighest,
    surfaceContainerHighest = InkHighest,
    outline = LineDark,
    outlineVariant = LineDark,
    error = LiveCoral,
    onError = Cream,
    inverseSurface = Cream,
    inverseOnSurface = Ink,
    scrim = Color(0xCC0B0B0A),
)

private val LightColorScheme = lightColorScheme(
    primary = CheddarDeep,
    onPrimary = Paper,
    primaryContainer = CheddarSoftLight,
    onPrimaryContainer = InkOnPaper,
    secondary = MutedOnPaper,
    onSecondary = Paper,
    tertiary = LiveCoral,
    onTertiary = Paper,
    background = Paper,
    onBackground = InkOnPaper,
    surface = Paper,
    onSurface = InkOnPaper,
    surfaceVariant = PaperElevated,
    onSurfaceVariant = MutedOnPaper,
    surfaceContainerLowest = Paper,
    surfaceContainerLow = PaperElevated,
    surfaceContainer = PaperElevated,
    surfaceContainerHigh = Color(0xFFF3EBD8),
    surfaceContainerHighest = Color(0xFFEDE4CE),
    outline = LineLight,
    outlineVariant = LineLight,
    error = LiveCoral,
    onError = Paper,
    inverseSurface = Ink,
    inverseOnSurface = Cream,
    scrim = Color(0x990B0B0A),
)

@Composable
fun LowcheeseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content,
    )
}
