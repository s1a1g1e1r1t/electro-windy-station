package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class VitroExtendedColors(
    val thermalAccent: Color,
    val thermalContainer: Color,
    val windAccent: Color,
    val windContainer: Color,
    val successAccent: Color,
    val blueprintBg: Color,
    val blueprintGrid: Color,
    val blueprintInk: Color
)

val LocalVitroColors = staticCompositionLocalOf {
    VitroExtendedColors(
        thermalAccent = ThermalAmber,
        thermalContainer = ThermalAmberSoft,
        windAccent = WindCyan,
        windContainer = WindCyanSoft,
        successAccent = BlueprintSuccess,
        blueprintBg = BlueprintGridBg,
        blueprintGrid = BlueprintGridLine,
        blueprintInk = BlueprintStrokePrimary
    )
}

private val LightColorScheme = lightColorScheme(
    primary = WindCyan,
    onPrimary = Color.White,
    primaryContainer = WindCyanSoft,
    onPrimaryContainer = NavyPrimary,
    secondary = NavyPrimary,
    onSecondary = Color.White,
    secondaryContainer = SurfaceCardVariant,
    onSecondaryContainer = InkDark,
    tertiary = ThermalAmber,
    onTertiary = Color.White,
    tertiaryContainer = ThermalAmberSoft,
    onTertiaryContainer = Color(0xFF4A1B00),
    background = BlueprintPaper,
    onBackground = InkDark,
    surface = SurfaceWhite,
    onSurface = InkDark,
    surfaceVariant = SurfaceCardVariant,
    onSurfaceVariant = InkMuted,
    outline = OutlineLight,
    outlineVariant = Color(0xFFDCE5EE)
)

private val DarkColorScheme = darkColorScheme(
    primary = WindCyanBright,
    onPrimary = NavyDeep,
    primaryContainer = WindCyanDarkContainer,
    onPrimaryContainer = WindCyanSoft,
    secondary = Color(0xFF90CAF9),
    onSecondary = NavyDeep,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = InkLight,
    tertiary = ThermalAmberBright,
    onTertiary = Color(0xFF2D1200),
    tertiaryContainer = ThermalAmberDarkContainer,
    onTertiaryContainer = ThermalAmberSoft,
    background = DarkSlateBg,
    onBackground = InkLight,
    surface = DarkSurface,
    onSurface = InkLight,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = InkLightMuted,
    outline = OutlineDark,
    outlineVariant = Color(0xFF213347)
)

val VitroShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun VitroDimTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) {
        VitroExtendedColors(
            thermalAccent = ThermalAmberBright,
            thermalContainer = ThermalAmberDarkContainer,
            windAccent = WindCyanBright,
            windContainer = WindCyanDarkContainer,
            successAccent = BlueprintSuccessBright,
            blueprintBg = BlueprintGridBg,
            blueprintGrid = BlueprintGridLine,
            blueprintInk = BlueprintStrokePrimary
        )
    } else {
        VitroExtendedColors(
            thermalAccent = ThermalAmber,
            thermalContainer = ThermalAmberSoft,
            windAccent = WindCyan,
            windContainer = WindCyanSoft,
            successAccent = BlueprintSuccess,
            blueprintBg = BlueprintGridBg,
            blueprintGrid = BlueprintGridLine,
            blueprintInk = BlueprintStrokePrimary
        )
    }

    CompositionLocalProvider(LocalVitroColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VitroTypography,
            shapes = VitroShapes,
            content = content
        )
    }
}
