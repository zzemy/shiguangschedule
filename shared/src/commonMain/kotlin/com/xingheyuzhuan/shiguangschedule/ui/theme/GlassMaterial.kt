package com.xingheyuzhuan.shiguangschedule.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp

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
 *
 * 浅色主题偏乳白，深色主题偏中性灰而非纯黑；
 * 透明度保持在能透出背景的区间，避免变成一块实色面板。
 */
fun glassChromeColor(surface: Color, tintAlpha: Float, isDark: Boolean): Color {
    val tint = tintAlpha.coerceIn(0f, 0.6f)
    return if (isDark) {
        val neutral = lerp(surface, Color(0xFF26262E), 0.72f)
        neutral.copy(alpha = (0.44f + tint * 0.35f).coerceIn(0.44f, 0.66f))
    } else {
        val milky = lerp(surface, Color.White, 0.35f)
        milky.copy(alpha = (0.56f + tint * 0.30f).coerceIn(0.56f, 0.74f))
    }
}

/**
 * 课程色进入玻璃后不再直接使用深色主题的深色块，
 * 而是用浅色变体并轻轻提亮，让颜色表现为“透出来的柔光”而不是染色塑料。
 */
fun glassTintColor(lightVariant: Color, isDark: Boolean): Color {
    return lerp(lightVariant, Color.White, if (isDark) 0.04f else 0.02f)
}

/**
 * 估算玻璃卡片最终呈现的表面色，用于计算文字对比度。
 * 玻璃会抬升深色主题的表面亮度，不能再用课程块色直接推算。
 */
fun glassTextBackdrop(
    surface: Color,
    tintColor: Color,
    tintAlpha: Float,
    isDark: Boolean
): Color {
    val frost = if (isDark) {
        surface.copy(alpha = 0.58f)
    } else {
        Color.White.copy(alpha = 0.64f)
    }
    val base = frost.compositeOver(surface)
    val tint = (0.10f + tintAlpha.coerceIn(0f, 0.6f) * 0.45f).coerceIn(0f, 0.8f)
    return tintColor.copy(alpha = tint).compositeOver(base)
}

/**
 * 柔光玻璃卡片表面：磨砂底色 + 课程色漫射 + 顶部高光 + 底部厚度 + 边缘折射。
 *
 * 参考 HyperOS 柔光玻璃的漫反射观感与 Liquid Glass 的边缘光学：
 * 颜色只作为低透明度的光晕参与，不再和深色背景叠成脏色。
 */
fun Modifier.glassCourseSurface(
    shape: Shape,
    isDark: Boolean,
    tintColor: Color,
    tintAlpha: Float,
    specularIntensity: Float,
    refractionStrength: Float,
    chromaticAberration: Float,
    floating: Boolean = false
): Modifier = drawWithContent {
    val outline = shape.createOutline(size, layoutDirection, this)
    val outlinePath = Path().apply { addOutline(outline) }

    val tintLevel = (tintAlpha.coerceIn(0f, 0.6f) / 0.6f).coerceIn(0f, 1f)
    val specular = specularIntensity.coerceIn(0f, 1f)
    val refraction = refractionStrength.coerceIn(0f, 1f)
    val chromatic = chromaticAberration.coerceIn(0f, 1f)

    val flatTintAlpha = if (isDark) {
        0.10f + 0.38f * tintLevel
    } else {
        0.10f + 0.36f * tintLevel
    }
    val bloomAlpha = if (isDark) {
        0.16f + 0.50f * tintLevel
    } else {
        0.16f + 0.46f * tintLevel
    }

    clipPath(outlinePath) {
        // 磨砂底色：浅色放大白，深色用深色薄纱 + 顶部提亮，不依赖 near-black surface
        if (isDark) {
            drawRect(Color(0xFF0B0B0F).copy(alpha = if (floating) 0.46f else 0.16f))
            drawRect(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = if (floating) 0.18f else 0.10f),
                    0.45f to Color.White.copy(alpha = 0.04f),
                    1f to Color.Transparent
                )
            )
        } else {
            drawRect(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = if (floating) 0.82f else 0.48f),
                    0.55f to Color.White.copy(alpha = if (floating) 0.64f else 0.36f),
                    1f to Color.White.copy(alpha = if (floating) 0.52f else 0.24f)
                )
            )
        }

        // 课程色漫射：从左上角铺开的光晕，再叠一层很薄的均匀染色
        drawRect(
            Brush.radialGradient(
                colors = listOf(
                    tintColor.copy(alpha = bloomAlpha),
                    tintColor.copy(alpha = bloomAlpha * 0.52f),
                    Color.Transparent
                ),
                center = Offset(size.width * 0.22f, size.height * 0.04f),
                radius = size.maxDimension * 1.02f
            )
        )
        drawRect(tintColor.copy(alpha = flatTintAlpha))

        // 顶部天光，模拟玻璃表面的柔和高光
        drawRect(
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = specular * (if (isDark) 0.14f else 0.34f)),
                0.16f to Color.White.copy(alpha = specular * 0.10f),
                0.48f to Color.Transparent
            )
        )

        // 底部厚度阴影，让玻璃不显得像一张贴纸
        drawRect(
            Brush.verticalGradient(
                0.62f to Color.Transparent,
                1f to Color.Black.copy(alpha = if (isDark) 0.12f else 0.04f)
            )
        )
    }

    drawContent()

    // 边缘折射：左上亮边、课程色色散、右下暗边
    val rimBrush = Brush.linearGradient(
        colorStops = arrayOf(
            0f to Color.White.copy(
                alpha = ((if (isDark) 0.52f else 0.86f) * specular * (0.55f + 0.45f * refraction))
                    .coerceIn(0f, 0.92f)
            ),
            0.26f to Color.White.copy(alpha = (if (isDark) 0.10f else 0.16f) * specular),
            0.52f to tintColor.copy(alpha = chromatic * 0.30f * (0.4f + 0.6f * refraction)),
            0.74f to Color.Transparent,
            1f to if (isDark) {
                Color.White.copy(alpha = 0.10f)
            } else {
                Color.Black.copy(alpha = 0.06f)
            }
        ),
        start = Offset.Zero,
        end = Offset(size.width, size.height)
    )
    // stroke 以轮廓为中心，其中一半被卡片 clip 裁掉，只留下约 1dp 的内侧亮边
    drawOutline(
        outline = outline,
        brush = rimBrush,
        style = Stroke(width = 2.dp.toPx())
    )

    // 内侧斜面高光，增加玻璃厚度感
    drawOutline(
        outline = outline,
        brush = Brush.verticalGradient(
            0f to Color.White.copy(alpha = specular * (if (isDark) 0.12f else 0.22f)),
            0.34f to Color.Transparent
        ),
        style = Stroke(width = 3.dp.toPx())
    )
}

/**
 * 没有壁纸时的柔和演示背景。
 * 用几团低饱和度径向光斑代替线性渐变，玻璃才有可采样的颜色层次。
 */
@Composable
fun GlassDemoBackdrop(modifier: Modifier = Modifier) {
    val isDark = LocalIsDarkTheme.current
    Canvas(modifier = modifier.fillMaxSize()) {
        if (isDark) {
            drawRect(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF10121A),
                        Color(0xFF1F1830),
                        Color(0xFF0E1A20)
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
            )
            drawSoftBlob(Color(0xFF7654B8), 0.82f, 0.15f, 0.72f, 0.58f)
            drawSoftBlob(Color(0xFFC44E8C), 0.12f, 0.62f, 0.80f, 0.44f)
            drawSoftBlob(Color(0xFF2F8B96), 0.70f, 0.92f, 0.76f, 0.48f)
            drawSoftBlob(Color(0xFF4A62C4), 0.40f, 0.42f, 0.90f, 0.38f)
        } else {
            drawRect(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFF6F1FB),
                        Color(0xFFFFF7EF),
                        Color(0xFFEDF5FA)
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width, size.height)
                )
            )
            drawSoftBlob(Color(0xFFFFB59F), 0.82f, 0.16f, 0.70f, 0.55f)
            drawSoftBlob(Color(0xFF9FC4FF), 0.12f, 0.55f, 0.80f, 0.45f)
            drawSoftBlob(Color(0xFFCDB2FF), 0.70f, 0.90f, 0.75f, 0.50f)
            drawSoftBlob(Color(0xFFB7EBD8), 0.25f, 0.08f, 0.52f, 0.35f)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSoftBlob(
    color: Color,
    centerX: Float,
    centerY: Float,
    radiusFraction: Float,
    alpha: Float
) {
    val center = Offset(size.width * centerX, size.height * centerY)
    val radius = size.width * radiusFraction
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(color.copy(alpha = alpha), color.copy(alpha = 0f)),
            center = center,
            radius = radius
        ),
        radius = radius,
        center = center
    )
}