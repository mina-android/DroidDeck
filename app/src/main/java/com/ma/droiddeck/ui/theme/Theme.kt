package com.ma.droiddeck.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.ma.droiddeck.R
import com.ma.droiddeck.data.model.AccentTheme
import com.ma.droiddeck.data.model.AppFontFamily

val PureBlack = Color(0xFF000000)
val OledBorder = Color(0xFF1F1F1F)
val OledSubtleSurface = Color(0xFF0A0A0A)
val TextPrimary = Color(0xFFEAEAEA)
val TextSecondary = Color(0xFF8C8C8C)
val CallAcceptGreen = Color(0xFF4CAF50)
val CallDeclineRed = Color(0xFFEF5350)

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

fun resolveComposeFontFamily(appFont: AppFontFamily): FontFamily {
    if (appFont == AppFontFamily.MONOSPACE) return FontFamily.Monospace
    val gFontName = appFont.googleFontName ?: return FontFamily.SansSerif
    val gFont = GoogleFont(gFontName)
    return FontFamily(
        Font(googleFont = gFont, fontProvider = googleFontProvider, weight = FontWeight.Light),
        Font(googleFont = gFont, fontProvider = googleFontProvider, weight = FontWeight.Normal),
        Font(googleFont = gFont, fontProvider = googleFontProvider, weight = FontWeight.Medium),
        Font(googleFont = gFont, fontProvider = googleFontProvider, weight = FontWeight.Bold)
    )
}

/**
 * Pure #000000 OLED Material 3 Theme with selectable accent colors and app-wide font family.
 */
@Composable
fun DroidDeckTheme(
    accentTheme: AccentTheme = AccentTheme.PURE_WHITE,
    appFont: AppFontFamily = AppFontFamily.DEFAULT,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val accentColor = when (accentTheme) {
        AccentTheme.PURE_WHITE -> Color(0xFFEAEAEA)
        AccentTheme.WARM_AMBER -> Color(0xFFFFB74D)
        AccentTheme.NIGHT_RED -> Color(0xFFFF5252)
        AccentTheme.SOFT_CYAN -> Color(0xFF80DEEA)
        AccentTheme.DYNAMIC_M3 -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dynamicDarkColorScheme(context).primary
            } else {
                Color(0xFF80DEEA)
            }
        }
    }

    val fontFamily = remember(appFont) { resolveComposeFontFamily(appFont) }

    val colorScheme = darkColorScheme(
        primary = accentColor,
        onPrimary = PureBlack,
        secondary = accentColor.copy(alpha = 0.78f),
        onSecondary = PureBlack,
        background = PureBlack,
        onBackground = TextPrimary,
        surface = PureBlack,
        onSurface = TextPrimary,
        surfaceVariant = OledSubtleSurface,
        onSurfaceVariant = TextSecondary,
        outline = OledBorder,
        outlineVariant = OledBorder
    )

    val typography = Typography(
        displayLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 98.sp,
            lineHeight = 104.sp,
            letterSpacing = (-2).sp,
            fontFeatureSettings = "tnum"
        ),
        displayMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Light,
            fontSize = 45.sp,
            lineHeight = 52.sp
        ),
        displaySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 36.sp,
            lineHeight = 44.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 32.sp,
            lineHeight = 40.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 20.sp,
            lineHeight = 26.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
            lineHeight = 24.sp
        ),
        titleLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
            lineHeight = 24.sp
        ),
        titleMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            lineHeight = 20.sp
        ),
        titleSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
            lineHeight = 22.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp
        ),
        bodySmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            lineHeight = 16.sp
        ),
        labelLarge = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            lineHeight = 18.sp
        ),
        labelMedium = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            fontFeatureSettings = "tnum"
        ),
        labelSmall = TextStyle(
            fontFamily = fontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
