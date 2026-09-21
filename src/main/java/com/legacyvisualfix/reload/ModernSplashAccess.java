package com.legacyvisualfix.reload;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/** Optional API boundary: neither Minecraft nor ModernSplash is needed to load this class. */
public final class ModernSplashAccess {

    private final Method begin;
    private final Method render;
    private final Method end;

    ModernSplashAccess(Class<?> api) throws ReflectiveOperationException {
        if (!Integer.valueOf(1)
            .equals(
                api.getMethod("apiVersion")
                    .invoke(null))) {
            throw new NoSuchMethodException("Unsupported ModernSplash runtime API");
        }
        begin = api.getMethod("begin");
        render = api.getMethod("render", String.class, String.class, int.class, int.class);
        end = api.getMethod("end");
    }

    public static ModernSplashAccess find(boolean loaded) {
        if (!loaded) return null;
        try {
            return new ModernSplashAccess(Class.forName("gkappa.modernsplash.RuntimeSplash"));
        } catch (ReflectiveOperationException | RuntimeException | LinkageError e) {
            return null;
        }
    }

    public boolean begin() {
        return Boolean.TRUE.equals(invoke(begin));
    }

    public void render(String title, String detail, int completed, int total) {
        invoke(render, title, detail, completed, total);
    }

    public void end() {
        invoke(end);
    }

    private static Object invoke(Method method, Object... arguments) {
        try {
            return method.invoke(null, arguments);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Error && !(cause instanceof LinkageError)) throw (Error) cause;
            throw new IllegalStateException("ModernSplash runtime API failed", cause);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot invoke ModernSplash runtime API", e);
        }
    }
}
