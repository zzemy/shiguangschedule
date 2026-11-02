package com.xingheyuzhuan.shiguangschedule.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * 顶栏、底栏和底部面板共用的玻璃配置。
 */
@Immutable
data class GlassChromeConfig(
    val enabled: Boolean = false,
    val tintAlpha: Float = 0.18f
)

val LocalGlassChromeConfig = compositionLocalOf { GlassChromeConfig() }

/**
 * 顶栏、底栏和底部面板的半透明表面色。
 */
fun glassChromeColor(surface: Color, tintAlpha: Float): Color {
    val alpha = (0.55f + tintAlpha.coerceIn(0f, 0.6f) * 0.55f).coerceIn(0.55f, 0.88f)
    return surface.copy(alpha = alpha)
}

/**
 * 玻璃边缘的高光/色散描边。
 *
 * 第一版使用渐变描边和课程色染色近似 Liquid Glass 的边缘折射，
 * 避免逐卡背景采样在多端渲染上的性能与兼容性问题。
 */
@Composable
fun rememberGlassBorderBrush(
    isDark: Boolean,
    specularIntensity: Float,
    refractionStrength: Float,
    chromaticTint: Color
): Brush {
    return remember(isDark, specularIntensity, refractionStrength, chromaticTint) {
        val specularAlpha = (0.12f + specularIntensity.coerceIn(0f, 1f) * 0.45f) *
            (0.5f + refractionStrength.coerceIn(0f, 1f) * 0.5f)
        Brush.linearGradient(
            listOf(
                Color.White.copy(alpha = if (isDark) specularAlpha * 0.55f else specularAlpha),
                chromaticTint,
                Color.Transparent,
                Color.Black.copy(alpha = if (isDark) 0.28f else 0.12f)
            )
        )
    }
}

/**
 * 没有壁纸时给玻璃材质用的演示背景，避免在纯色背景上完全看不出效果。
 */
fun glassDemoBackdropBrush(isDark: Boolean): Brush {
    return if (isDark) {
        Brush.linearGradient(
            listOf(Color(0xFF14111C), Color(0xFF211A2E), Color(0xFF0F171D))
        )
    } else {
        Brush.linearGradient(
            listOf(Color(0xFFF7F2FF), Color(0xFFFFF4E8), Color(0xFFEAF7FF))
        )
    }
}