package com.legacyvisualfix.reload;

import static org.junit.Assert.*;

import org.junit.Test;

public class ModernSplashAccessTest {

    public static class Api {

        static int begins, ends, completed;
        static boolean fail;

        public static int apiVersion() {
            return 1;
        }

        public static boolean begin() {
            begins++;
            return true;
        }

        public static void render(String title, String detail, int done, int total) {
            if (fail) throw new IllegalStateException("broken renderer");
            completed = done;
        }

        public static void end() {
            ends++;
        }
    }

    @Test
    public void optionalModIsNotRequired() {
        assertNull(ModernSplashAccess.find(false));
        assertNull(ModernSplashAccess.find(true));
    }

    @Test
    public void forwardsProgressAndReleasesEverySession() throws Exception {
        Api.begins = Api.ends = 0;
        Api.fail = false;
        ModernSplashAccess access = new ModernSplashAccess(Api.class);
        for (int i = 0; i < 2; i++) {
            assertTrue(access.begin());
            access.render("Reloading", "Textures", 2, 3);
            access.end();
        }
        assertEquals(2, Api.completed);
        assertEquals(2, Api.begins);
        assertEquals(2, Api.ends);
    }

    @Test
    public void propagatesRenderFailureForFallbackAndStillAllowsCleanup() throws Exception {
        ModernSplashAccess access = new ModernSplashAccess(Api.class);
        access.begin();
        Api.fail = true;
        try {
            access.render("Reloading", "", 0, 1);
            fail("Expected renderer error");
        } catch (RuntimeException expected) {
            assertEquals(
                "broken renderer",
                expected.getCause()
                    .getMessage());
        } finally {
            access.end();
            Api.fail = false;
        }
    }

    @Test(expected = NoSuchMethodException.class)
    public void rejectsOldApiBeforeStartingSession() throws Exception {
        new ModernSplashAccess(String.class);
    }
}
