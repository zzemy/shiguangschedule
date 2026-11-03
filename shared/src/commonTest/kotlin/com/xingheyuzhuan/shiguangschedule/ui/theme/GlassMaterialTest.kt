package com.xingheyuzhuan.shiguangschedule.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.xingheyuzhuan.shiguangschedule.data.model.ScheduleGridStyle
import com.xingheyuzhuan.shiguangschedule.data.model.toCompose
import com.xingheyuzhuan.shiguangschedule.data.model.toProto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GlassMaterialTest {

    @Test
    fun glassSettingsDefaultToDisabled() {
        val style = ScheduleGridStyle.DEFAULT
        assertFalse(style.glassMaterialEnabled)
        assertEquals(30f, style.glassBlurRadiusDp)
        assertEquals(0.22f, style.glassTintAlpha)
        assertEquals(0.7f, style.glassRefractionStrength)
        assertEquals(0.8f, style.glassSpecularIntensity)
        assertEquals(0.12f, style.glassChromaticAberration)
    }

    @Test
    fun glassSettingsRoundTripThroughProto() {
        val style = ScheduleGridStyle.DEFAULT.copy(
            glassMaterialEnabled = true,
            glassBlurRadiusDp = 32f,
            glassTintAlpha = 0.24f,
            glassRefractionStrength = 0.65f,
            glassSpecularIntensity = 0.9f,
            glassChromaticAberration = 0.3f
        )
        val restored = style.toProto().toCompose()
        assertTrue(restored.glassMaterialEnabled)
        assertEquals(32f, restored.glassBlurRadiusDp)
        assertEquals(0.24f, restored.glassTintAlpha)
        assertEquals(0.65f, restored.glassRefractionStrength)
        assertEquals(0.9f, restored.glassSpecularIntensity)
        assertEquals(0.3f, restored.glassChromaticAberration)
    }

    @Test
    fun glassTintLightensDarkCourseColors() {
        val darkPalette = Color(0xFF663300)
        val tint = glassTintColor(darkPalette, isDark = true)
        assertTrue(tint.luminance() > darkPalette.luminance())
        assertTrue(tint.alpha > 0.99f)
    }

    @Test
    fun glassChromeStaysTranslucentInBothModes() {
        val surface = Color(0xFF141218)
        val dark = glassChromeColor(surface, 0.22f, isDark = true)
        val light = glassChromeColor(surface, 0.22f, isDark = false)
        assertTrue(dark.alpha in 0.40f..0.62f)
        assertTrue(light.alpha in 0.55f..0.75f)
    }

    @Test
    fun glassTextBackdropLiftsDarkSurface() {
        val surface = Color(0xFF141218)
        val backdrop = glassTextBackdrop(
            surface = surface,
            tintColor = glassTintColor(Color(0xFFFFCC99), isDark = true),
            tintAlpha = 0.22f,
            isDark = true
        )
        assertTrue(backdrop.luminance() >= surface.luminance())
    }
}