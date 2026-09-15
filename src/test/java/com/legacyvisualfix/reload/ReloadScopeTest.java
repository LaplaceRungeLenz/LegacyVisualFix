package com.legacyvisualfix.reload;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReloadScopeTest {

    @Test
    public void nestedReloadDoesNotOwnOrFinishOuterSession() {
        ReloadScope scope = new ReloadScope();
        assertTrue(scope.enter());
        assertTrue(scope.isOutermost());
        assertFalse(scope.enter());
        assertFalse(scope.isOutermost());
        assertFalse(scope.exit());
        assertTrue(scope.isOutermost());
        assertTrue(scope.exit());
        assertFalse(scope.isOutermost());
        assertTrue(scope.enter());
    }
}
