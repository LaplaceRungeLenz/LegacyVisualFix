package com.legacyvisualfix.reload;

import static org.junit.Assert.*;

import org.junit.Test;

public class ReloadProgressTest {

    @Test
    public void reportsOnlyCompletedStagesAndResetsOnNextReload() {
        ReloadProgress progress = new ReloadProgress();
        progress.begin(3);
        progress.beforeListener("Textures");
        assertEquals(0, progress.getCompleted());
        progress.afterListener();
        assertEquals(1, progress.getCompleted());
        assertEquals(3, progress.getTotal());
        assertEquals(1.0 / 3, progress.getFraction(), 0.0001);
        progress.finish();
        assertFalse(progress.isActive());
        progress.begin(2);
        assertEquals(0, progress.getCompleted());
        assertEquals("", progress.getStage());
    }

    @Test
    public void emptyAndExtraCallbacksNeverOverflowOrProduceNan() {
        ReloadProgress progress = new ReloadProgress();
        progress.begin(0);
        progress.afterListener();
        assertEquals(0, progress.getCompleted());
        assertEquals(0, progress.getFraction(), 0);
        progress.begin(1);
        progress.afterListener();
        progress.afterListener();
        assertEquals(1, progress.getCompleted());
        progress.finish();
        progress.beforeListener("ignored");
        assertEquals("", progress.getStage());
    }
}
