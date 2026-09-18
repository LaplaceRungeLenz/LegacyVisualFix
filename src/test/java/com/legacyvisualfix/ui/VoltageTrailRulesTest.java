package com.legacyvisualfix.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class VoltageTrailRulesTest {

    @Test
    public void tierFormattingKeepsColorAndIgnoresDecorations() {
        String[] formats = { "c", "2", "6", "e", "8", "9", "d", "b", "2n", "4n", "5n", "1ln", "cln", "4ln", "fln" };
        int[] colors = { 0xff5555, 0x00aa00, 0xffaa00, 0xffff55, 0x555555, 0x5555ff, 0xff55ff, 0x55ffff, 0x00aa00,
            0xaa0000, 0xaa00aa, 0x0000aa, 0xff5555, 0xaa0000, 0xffffff };
        for (int i = 0; i < formats.length; i++) {
            StringBuilder format = new StringBuilder();
            for (char c : formats[i].toCharArray()) format.append('\u00a7')
                .append(c);
            assertEquals(colors[i], VoltageTrailRules.formatColor(format.toString()));
        }
        assertEquals(-1, VoltageTrailRules.formatColor("\u00a7l"));
        assertEquals(-1, VoltageTrailRules.formatColor(null));
    }

    @Test
    public void componentsRequireExactRegistryFamilyAndTier() {
        assertEquals(6, VoltageTrailRules.componentTier("Electric_Motor_LuV"));
        assertEquals(14, VoltageTrailRules.componentTier("Field_Generator_MAX"));
        assertEquals(0, VoltageTrailRules.componentTier("Electric_Pump_ULV"));
        assertEquals(9, VoltageTrailRules.componentTier("Casing_MAX"));
        assertEquals(14, VoltageTrailRules.componentTier("Casing_MAXV"));
        assertEquals(-1, VoltageTrailRules.componentTier("Hull_MAX"));
        assertEquals(-1, VoltageTrailRules.componentTier("Circuit_Board_Elite"));
        assertEquals(-1, VoltageTrailRules.componentTier("Electric_Motor_LV_Fake"));
        assertEquals(-1, VoltageTrailRules.componentTier("Battery_MAX"));
    }

    @Test
    public void allComponentFamiliesCoverLvThroughMax() {
        String[] families = { "Electric_Motor", "Electric_Pump", "Conveyor_Module", "Electric_Piston", "Robot_Arm",
            "Emitter", "Sensor", "Field_Generator" };
        String[] tiers = { "LV", "MV", "HV", "EV", "IV", "LuV", "ZPM", "UV", "UHV", "UEV", "UIV", "UMV", "UXV", "MAX" };
        for (String family : families) {
            for (int i = 0; i < tiers.length; i++)
                assertEquals(i + 1, VoltageTrailRules.componentTier(family + "_" + tiers[i]));
        }
    }

    @Test
    public void emissionPreservesDarkTierColorsAndExistingParticlesWhenSwitchingItems() {
        TrailParticles trail = new TrailParticles();
        trail.emit(0, 0, 60, 0, 0x0000aa, .05, 128, 60);
        int previous = trail.particles()
            .size();
        org.junit.Assert.assertTrue(previous > 0);
        trail.emit(60, 0, 120, 0, 0x555555, .05, 128, 60);
        org.junit.Assert.assertTrue(
            trail.particles()
                .size() > previous);
        for (int i = 0; i < trail.particles()
            .size(); i++) {
            assertEquals(
                i < previous ? 0x0000aa : 0x555555,
                trail.particles()
                    .get(i).rgb);
        }
    }

    @Test
    public void circuitsUseActualOreNamesRatherThanChipTechnology() {
        String[] names = { "Primitive", "Basic", "Good", "Advanced", "Data", "Elite", "Master", "Ultimate",
            "Superconductor", "Infinite", "Bio", "Optical", "Exotic", "Cosmic", "Transcendent" };
        for (int i = 0; i < names.length; i++) assertEquals(i, VoltageTrailRules.circuitTier("circuit" + names[i]));
        assertEquals(-1, VoltageTrailRules.circuitTier("circuitPiko"));
        assertEquals(-1, VoltageTrailRules.circuitTier("dustBasic"));
        assertEquals(-1, VoltageTrailRules.circuitTier("circuitIntegrated"));
    }
}
