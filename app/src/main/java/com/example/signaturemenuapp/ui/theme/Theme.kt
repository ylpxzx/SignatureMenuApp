package com.example.signaturemenuapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SignatureColorScheme = lightColorScheme(
    primary = Forest,
    onPrimary = Paper,
    secondary = Leaf,
    onSecondary = Paper,
    tertiary = Ginger,
    onTertiary = Color.White,
    background = Rice,
    onBackground = Ink,
    surface = Paper,
    onSurface = Ink,
    surfaceVariant = Mint,
    onSurfaceVariant = Ash,
    outline = Color(0xFFDCD7C8),
    error = Tomato,
)

@Composable
fun SignatureMenuAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SignatureColorScheme,
        typography = Typography,
        content = content,
    )
}
