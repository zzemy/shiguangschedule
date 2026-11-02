package com.xingheyuzhuan.shiguangschedule.ui.theme

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
        assertEquals(24f, style.glassBlurRadiusDp)
        assertEquals(0.18f, style.glassTintAlpha)
        assertEquals(0.8f, style.glassRefractionStrength)
        assertEquals(0.6f, style.glassSpecularIntensity)
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
}