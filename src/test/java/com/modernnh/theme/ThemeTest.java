package com.modernnh.theme;

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.Test;

public class ThemeTest {

    @Test
    public void malformedValuesUseSafeDefaults() {
        Properties p = new Properties();
        p.setProperty("bar.x", "NaN");
        p.setProperty("bar.y", "Infinity");
        p.setProperty("bar.width", "-50");
        p.setProperty("bar.height", "0");
        p.setProperty("color.fill", "not-a-color");
        p.setProperty("background.fit", "invalid");
        Theme theme = Theme.parse(p);
        assertEquals(0.5, theme.barX, 0);
        assertEquals(0.75, theme.barY, 0);
        assertEquals(240, theme.barWidth);
        assertEquals(12, theme.barHeight);
        assertEquals(0xFF59C9A5, theme.fillColor);
        assertEquals(Theme.Fit.COVER, theme.fit);
    }

    @Test
    public void customSettingsAndLocalTexturesAreAccepted() {
        Properties p = new Properties();
        p.setProperty("bar.width", "320");
        p.setProperty("color.fill", "#AAFFCC00");
        p.setProperty("background.fit", "contain");
        p.setProperty("texture.background", "file:background.png");
        p.setProperty("showText", "false");
        Theme theme = Theme.parse(p);
        assertEquals(320, theme.barWidth);
        assertEquals(0xAAFFCC00, theme.fillColor);
        assertEquals(Theme.Fit.CONTAIN, theme.fit);
        assertEquals("file:background.png", theme.background);
        assertFalse(theme.showText);
    }

    @Test
    public void traversalAndAbsolutePathsFallBack() {
        for (String value : new String[] { "file:../secret.png", "file:C:/secret.png", "file:/secret.png",
            "file:a\\..\\secret.png", "other:../secret.png" }) {
            Properties p = new Properties();
            p.setProperty("texture.background", value);
            assertEquals("modernnh:textures/gui/background.png", Theme.parse(p).background);
        }
    }
}
