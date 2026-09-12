package com.modernnh.smoke;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

import net.minecraft.client.Minecraft;

import com.modernnh.reload.ReloadScreen;

import cpw.mods.fml.common.Loader;

/** Uses the actual optional mod through reflection so the Java 8 baseline needs no Angelica. */
public final class ShaderSmoke {

    private ShaderSmoke() {}

    public static void run(Minecraft mc, File results) throws Exception {
        if (!Loader.isModLoaded("angelica")) return;
        Class<?> iris = Class.forName("net.coderbot.iris.Iris");
        Object config = iris.getMethod("getIrisConfig")
            .invoke(null);
        Class<?> configClass = config.getClass();
        Optional<?> previousPack = (Optional<?>) configClass.getMethod("getShaderPackName")
            .invoke(config);
        boolean previousEnabled = (Boolean) configClass.getMethod("areShadersEnabled")
            .invoke(config);
        Field counter = ReloadScreen.class.getDeclaredField("shaderSessions");
        counter.setAccessible(true);
        int before = counter.getInt(null);
        for (String name : new String[] { "ModernNH-Smoke-A", "ModernNH-Smoke-B" }) {
            File shaders = new File(mc.mcDataDir, "shaderpacks/" + name + "/shaders");
            shaders.mkdirs();
            Files.write(
                new File(shaders, "gbuffers_basic.vsh").toPath(),
                "#version 120\nvarying vec4 tint;\nvoid main(){gl_Position=ftransform();tint=gl_Color;}\n"
                    .getBytes(StandardCharsets.UTF_8));
            Files.write(
                new File(shaders, "gbuffers_basic.fsh").toPath(),
                "#version 120\nvarying vec4 tint;\nvoid main(){gl_FragData[0]=tint;}\n"
                    .getBytes(StandardCharsets.UTF_8));
        }
        try {
            configClass.getMethod("setShaderPackName", String.class)
                .invoke(config, "ModernNH-Smoke-A");
            configClass.getMethod("save")
                .invoke(config);
            iris.getMethod("toggleShaders", Minecraft.class, boolean.class)
                .invoke(null, mc, true);
            assertEnabled(iris);
            ClientSmoke.screenshot(new File(results, "shader-enable.png"));
            Object manager = iris.getMethod("getPipelineManager")
                .invoke(null);
            Object pipeline = manager.getClass()
                .getMethod("preparePipeline", String.class)
                .invoke(manager, "Overworld");
            if (!pipeline.getClass()
                .getName()
                .contains("DeferredWorldRenderingPipeline"))
                throw new AssertionError("Shader pipeline fell back: " + pipeline.getClass());
            ClientSmoke.screenshot(new File(results, "shader-pipeline.png"));
            assertHealthy();
            configClass.getMethod("setShaderPackName", String.class)
                .invoke(config, "ModernNH-Smoke-B");
            configClass.getMethod("save")
                .invoke(config);
            iris.getMethod("reload")
                .invoke(null);
            assertEnabled(iris);
            iris.getMethod("toggleShaders", Minecraft.class, boolean.class)
                .invoke(null, mc, false);
            assertHealthy();
            int disabledCount = counter.getInt(null);
            Object disabledManager = iris.getMethod("getPipelineManager")
                .invoke(null);
            disabledManager.getClass()
                .getMethod("preparePipeline", String.class)
                .invoke(disabledManager, "Overworld");
            if (counter.getInt(null) != disabledCount)
                throw new AssertionError("Disabled shaders triggered pipeline screen");
            if (((Optional<?>) iris.getMethod("getCurrentPack")
                .invoke(null)).isPresent()) throw new AssertionError("Shaders still enabled");
            ClientSmoke.screenshot(new File(results, "shader-disable.png"));
            if (counter.getInt(null) - before < 4) throw new AssertionError("Shader reload hooks not executed");
            Field failed = ReloadScreen.class.getDeclaredField("failed");
            failed.setAccessible(true);
            if (failed.getBoolean(null)) throw new AssertionError("Shader loading renderer failed");
            Files.write(
                new File(results, "shader-result.txt").toPath(),
                "PASS: real Angelica enable, pack switch, pipeline compilation and disable\n"
                    .getBytes(StandardCharsets.UTF_8));
        } finally {
            configClass.getMethod("setShaderPackName", String.class)
                .invoke(
                    config,
                    previousPack.isPresent() ? previousPack.get()
                        .toString() : null);
            configClass.getMethod("setShadersEnabled", boolean.class)
                .invoke(config, previousEnabled);
            configClass.getMethod("save")
                .invoke(config);
            iris.getMethod("reload")
                .invoke(null);
        }
    }

    private static void assertEnabled(Class<?> iris) throws Exception {
        assertHealthy();
        if (!((Optional<?>) iris.getMethod("getCurrentPack")
            .invoke(null)).isPresent()) throw new AssertionError("Shader pack not loaded");
        if ((Boolean) iris.getMethod("isFallback")
            .invoke(null)) throw new AssertionError("Shader pack failed");
    }

    private static void assertHealthy() throws Exception {
        Field failed = ReloadScreen.class.getDeclaredField("failed");
        failed.setAccessible(true);
        if (failed.getBoolean(null)) throw new AssertionError("Shader loading renderer failed during this operation");
    }
}
