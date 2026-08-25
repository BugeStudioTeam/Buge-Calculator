package com.buge.calculator.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.buge.calculator.R
import com.buge.calculator.data.AppSettings
import com.buge.calculator.data.ThemeMode
import com.buge.calculator.data.ThemeSource

val GoogleSansFlex = FontFamily(
    Font(R.font.google_sans_flex_regular, FontWeight.Normal),
    Font(R.font.google_sans_flex_medium, FontWeight.Medium),
    Font(R.font.google_sans_flex_bold, FontWeight.Bold)
)

private val BugeTypography = Typography(
    displayLarge = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Normal),
    displayMedium = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Normal),
    headlineLarge = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium),
    headlineMedium = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium),
    titleLarge = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium),
    titleMedium = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontFamily = GoogleSansFlex),
    bodyMedium = TextStyle(fontFamily = GoogleSansFlex),
    labelLarge = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontFamily = GoogleSansFlex, fontWeight = FontWeight.Medium)
)

private val BugeShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(22.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(30.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(38.dp)
)

@Composable
fun BugeTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = when {
        settings.themeSource == ThemeSource.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> seededDark(seedFor(settings.themeSource, settings.customColor))
        else -> seededLight(seedFor(settings.themeSource, settings.customColor))
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
        }
    }
    MaterialTheme(colorScheme = colorScheme, typography = BugeTypography, shapes = BugeShapes, content = content)
}

private fun seedFor(source: ThemeSource, custom: Color): Color = when (source) {
    ThemeSource.DYNAMIC -> Color(0xFF6750A4)
    ThemeSource.LAVENDER -> Color(0xFF6750A4)
    ThemeSource.OCEAN -> Color(0xFF006A6A)
    ThemeSource.FOREST -> Color(0xFF386A20)
    ThemeSource.SUNSET -> Color(0xFF9B443D)
    ThemeSource.CUSTOM -> custom
}

private fun seededLight(seed: Color): ColorScheme {
    val light = Color.White
    val surface = lerp(light, seed, 0.045f)
    return lightColorScheme(
        primary = seed,
        onPrimary = Color.White,
        primaryContainer = lerp(light, seed, 0.18f),
        onPrimaryContainer = lerp(Color.Black, seed, 0.35f),
        secondary = lerp(seed, Color(0xFF625B71), 0.45f),
        onSecondary = Color.White,
        secondaryContainer = lerp(light, seed, 0.12f),
        onSecondaryContainer = Color(0xFF1D192B),
        tertiary = lerp(seed, Color(0xFF7D5260), 0.55f),
        onTertiary = Color.White,
        tertiaryContainer = lerp(light, seed, 0.15f),
        onTertiaryContainer = Color(0xFF31111D),
        background = surface,
        onBackground = Color(0xFF1C1B1F),
        surface = surface,
        onSurface = Color(0xFF1C1B1F),
        surfaceVariant = lerp(light, seed, 0.09f),
        onSurfaceVariant = Color(0xFF49454F),
        outline = Color(0xFF79747E)
    )
}

private fun seededDark(seed: Color): ColorScheme {
    val dark = Color(0xFF121214)
    return darkColorScheme(
        primary = lerp(seed, Color.White, 0.35f),
        onPrimary = lerp(seed, Color.Black, 0.55f),
        primaryContainer = lerp(seed, dark, 0.46f),
        onPrimaryContainer = lerp(seed, Color.White, 0.58f),
        secondary = lerp(seed, Color.White, 0.45f),
        onSecondary = Color(0xFF332D41),
        secondaryContainer = lerp(seed, dark, 0.50f),
        onSecondaryContainer = lerp(seed, Color.White, 0.58f),
        tertiary = lerp(seed, Color.White, 0.50f),
        onTertiary = Color(0xFF492532),
        tertiaryContainer = lerp(seed, dark, 0.55f),
        onTertiaryContainer = lerp(seed, Color.White, 0.64f),
        background = dark,
        onBackground = Color(0xFFE6E1E5),
        surface = dark,
        onSurface = Color(0xFFE6E1E5),
        surfaceVariant = lerp(dark, seed, 0.24f),
        onSurfaceVariant = Color(0xFFCAC4D0),
        outline = Color(0xFF938F99)
    )
}
