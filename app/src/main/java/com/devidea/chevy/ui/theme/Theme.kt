package com.devidea.chevy.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Blue80,
    secondary = BlueGrey80,
    tertiary = Pink80,
    background = NightBackground,
    outline = NightLightShadow,
    onTertiary = NightDarkShadow
)

private val LightColorScheme = lightColorScheme(
    primary = Blue40,
    secondary = BlueGrey40,
    tertiary = Pink40,
    background = MainBackground,
    outline = LightShadow,
    onTertiary = DarkShadow


    /* Other default colors to override
    background = Color(0xFFFFFBFE),
    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

val LightAccentScheme = lightColorScheme(
    // — 포인트 (primary) —
    primary               = AccentOrange,
    onPrimary             = Color.White,
    primaryContainer      = AccentOrangeContainer,
    onPrimaryContainer    = Color.Black,
    inversePrimary        = AccentOrangeContainer,

    // — 보조 강조는 그레이로 유지 —
    secondary             = Grey500,
    onSecondary           = Color.Black,
    secondaryContainer    = Grey100,
    onSecondaryContainer  = Color.Black,

    // — 배경 / 표면 —
    background            = Color.White,
    onBackground          = Color.Black,
    surface               = Color.White,
    onSurface             = Color.Black,
    surfaceVariant        = Grey100,
    onSurfaceVariant      = Color.Black,

    // — 테두리·아웃라인 등 —
    outline               = Grey400,
    inverseSurface        = Grey900,
    inverseOnSurface      = Color.White,

    // — 에러 색상 (기존 그레이톤 유지) —
    error                 = Grey600,
    onError               = Color.White,
    errorContainer        = Grey200,
    onErrorContainer      = Color.Black
)
@Composable
fun CarProjectTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightAccentScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}