package com.mealos.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.mealos.R

val Enamel = Color(0xFF083224)
val RaisedEnamel = Color(0xFF234633)
val Cream = Color(0xFFF3F0E8)
val WarmLine = Color(0xFFD1C3B0)
val ActionOrange = Color(0xFFC84F13)
val TimerYellow = Color(0xFFC9A442)
val Sage = Color(0xFF6F8477)

private val DisplayFamily = FontFamily(Font(R.font.barlow_condensed_semibold, FontWeight.SemiBold))

private val LightColors = lightColorScheme(
    primary = Enamel,
    onPrimary = Cream,
    primaryContainer = RaisedEnamel,
    onPrimaryContainer = Cream,
    secondary = ActionOrange,
    onSecondary = Color.White,
    tertiary = TimerYellow,
    onTertiary = Enamel,
    background = Enamel,
    onBackground = Cream,
    surface = Cream,
    onSurface = Enamel,
    surfaceVariant = Color(0xFFE6DFD2),
    onSurfaceVariant = Color(0xFF41573B),
    outline = Sage,
    error = Color(0xFFB3261E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9CC9AC),
    onPrimary = Color(0xFF06281C),
    primaryContainer = Color(0xFF1C4935),
    onPrimaryContainer = Color(0xFFD8F4DF),
    secondary = Color(0xFFFFB38B),
    onSecondary = Color(0xFF542000),
    tertiary = Color(0xFFE5C76B),
    onTertiary = Color(0xFF3A3000),
    background = Color(0xFF061C14),
    onBackground = Color(0xFFE5EFE8),
    surface = Color(0xFF10271D),
    onSurface = Color(0xFFE5EFE8),
    surfaceVariant = Color(0xFF233A2E),
    onSurfaceVariant = Color(0xFFC0CEC4),
    outline = Color(0xFF82978A),
    error = Color(0xFFFFB4AB)
)

private val MealOsTypography = Typography(
    displayLarge = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold, fontSize = 64.sp, lineHeight = 60.sp),
    displayMedium = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold, fontSize = 52.sp, lineHeight = 50.sp),
    displaySmall = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold, fontSize = 44.sp, lineHeight = 44.sp),
    headlineLarge = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold, fontSize = 38.sp, lineHeight = 40.sp),
    headlineMedium = TextStyle(fontFamily = DisplayFamily, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, lineHeight = 34.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontSize = 15.sp, lineHeight = 22.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 20.sp)
)

@Composable
fun MealOsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = MealOsTypography,
        shapes = Shapes(),
        content = content
    )
}
