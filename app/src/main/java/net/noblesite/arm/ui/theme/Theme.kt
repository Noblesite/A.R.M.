package net.noblesite.arm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val ArmColorScheme = darkColorScheme(
    primary = TelemetryCyan,
    onPrimary = DeepSlate,
    secondary = SuccessGreen,
    onSecondary = DeepSlate,
    tertiary = WarningAmber,
    error = CriticalRed,
    background = DeepSlate,
    onBackground = PrimaryText,
    surface = PanelSlate,
    onSurface = PrimaryText,
    surfaceVariant = PanelSlateElevated,
    onSurfaceVariant = MutedSlateText,
    outline = MutedSlateText
)

private val ArmShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(8.dp)
)

@Composable
fun ARMTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ArmColorScheme,
        shapes = ArmShapes,
        typography = Typography,
        content = content
    )
}
