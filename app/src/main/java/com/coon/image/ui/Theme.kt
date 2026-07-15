package com.coon.image.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF2E7D32),
    secondary = Color(0xFF00796B),
    background = Color(0xFFF5F5F5)
)

private val Dark = darkColorScheme(
    primary = Color(0xFF66BB6A),
    secondary = Color(0xFF4DB6AC),
    background = Color(0xFF121212)
)

@Composable
fun CoonTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        content = content
    )
}
