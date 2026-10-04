package com.legacyvisualfix.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;

import net.minecraftforge.common.config.Configuration;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import cpw.mods.fml.relauncher.FMLInjectionData;

public class UiEffectsConfigTest {

    @Rule
    public final TemporaryFolder temporary = new TemporaryFolder();

    private Field minecraftHome;
    private Object previousHome;

    @Before
    public void initializeForgeConfigDirectory() throws Exception {
        minecraftHome = FMLInjectionData.class.getDeclaredField("minecraftHome");
        minecraftHome.setAccessible(true);
        previousHome = minecraftHome.get(null);
        minecraftHome.set(null, temporary.getRoot());
    }

    @After
    public void restoreForgeConfigDirectory() throws Exception {
        minecraftHome.set(null, previousHome);
    }

    @Test
    public void freshConfigDisablesTheOverlayAndUsesTheLargerHoverScale() throws Exception {
        File directory = temporary.newFolder();
        UiEffectsConfig.load(directory);
        Configuration saved = read(directory);
        assertTrue(saved.getBoolean("hideHoverOverlay", "effects", false, ""));
        assertEquals(1.25f, UiEffectsConfig.hoverScale, 0);
        assertEquals(1.2f, UiEffectsConfig.carriedScale, 0);
    }

    @Test
    public void legacyDefaultUpgradesAndPersistsOnlyOnce() throws Exception {
        File directory = temporary.newFolder();
        Configuration legacy = read(directory);
        legacy.get("effects", "hoverScale", 1.2f)
            .set(1.2f);
        legacy.save();

        UiEffectsConfig.load(directory);
        assertEquals(1.25f, UiEffectsConfig.hoverScale, 0);
        Configuration upgraded = read(directory);
        assertEquals(1.25f, upgraded.getFloat("hoverScale", "effects", 0, 1, 1.6f, ""), 0);

        upgraded.get("effects", "hoverScale", 1.25f)
            .set(1.2f);
        upgraded.get("effects", "hideHoverOverlay", true)
            .set(false);
        upgraded.save();
        UiEffectsConfig.load(directory);
        assertEquals(1.2f, UiEffectsConfig.hoverScale, 0);
        assertFalse(UiEffectsConfig.hideHoverOverlay);
        assertFalse(read(directory).getBoolean("hideHoverOverlay", "effects", true, ""));
    }

    @Test
    public void customScalesAndOtherSettingsSurviveTheUpgrade() throws Exception {
        File directory = temporary.newFolder();
        Configuration legacy = read(directory);
        legacy.get("effects", "hoverScale", 1.4f)
            .set(1.4f);
        legacy.get("effects", "carriedScale", 1.35f)
            .set(1.35f);
        legacy.get("effects", "hover", true)
            .set(false);
        legacy.save();

        UiEffectsConfig.load(directory);
        assertEquals(1.4f, UiEffectsConfig.hoverScale, 0);
        assertEquals(1.35f, UiEffectsConfig.carriedScale, 0);
        assertFalse(UiEffectsConfig.hover);
    }

    private static Configuration read(File directory) {
        Configuration configuration = new Configuration(new File(directory, "legacyvisualfix/ui.cfg"));
        configuration.load();
        return configuration;
    }
}
