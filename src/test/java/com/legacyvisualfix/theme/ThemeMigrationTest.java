package com.legacyvisualfix.theme;

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.Test;

public class ThemeMigrationTest {

    @Test
    public void upgradesOnlyExactUnmodifiedDefaults() {
        Properties old = new Properties();
        old.setProperty("color.fill", "green");
        Properties next = new Properties();
        next.setProperty("color.fill", "gold");
        Properties current = new Properties();
        current.putAll(old);
        assertTrue(ThemeMigration.upgrade(current, old, next));
        assertEquals("gold", current.getProperty("color.fill"));
        current.setProperty("color.fill", "custom");
        assertFalse(ThemeMigration.upgrade(current, old, next));
        assertEquals("custom", current.getProperty("color.fill"));
        current.putAll(old);
        current.setProperty("extra", "keep");
        assertFalse(ThemeMigration.upgrade(current, old, next));
    }
}
