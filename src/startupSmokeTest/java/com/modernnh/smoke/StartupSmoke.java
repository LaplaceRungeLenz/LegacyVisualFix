package com.modernnh.smoke;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.SimpleReloadableResourceManager;
import net.minecraft.client.resources.data.IMetadataSerializer;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Paired with fovSmoke, which shuts down the isolated client after both checks. */
@Mod(
    modid = "modernnhstartupsmoke",
    name = "ModernNH startup smoke",
    version = "1",
    dependencies = "required-after:MyCTMLib")
public final class StartupSmoke {

    private int ticks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END || ++ticks != 20) return;
        File report = new File(Minecraft.getMinecraft().mcDataDir, "modernnh-startup-smoke.txt");
        try {
            Class<?> textures = Class.forName("com.github.wohaopa.MyCTMLib.Textures");
            Field gregTech = textures.getDeclaredField("gregTechLoaded");
            gregTech.setAccessible(true);
            if (gregTech.getBoolean(null) != Loader.isModLoaded("gregtech")) {
                throw new AssertionError("GregTech availability was cached before discovery");
            }
            String[] names = { "ctmIconMap", "ctmAltMap", "ctmReplaceMap", "ctmRandomMap" };
            Map[] maps = new Map[names.length];
            Map[] saved = new Map[names.length];
            for (int i = 0; i < names.length; i++) {
                maps[i] = (Map) textures.getField(names[i])
                    .get(null);
                saved[i] = new HashMap(maps[i]);
                maps[i].put("modernnh:startup-regression", new Object());
            }
            try {
                Method clear = SimpleReloadableResourceManager.class.getDeclaredMethod("clearResources");
                clear.setAccessible(true);
                clear.invoke(new SimpleReloadableResourceManager(new IMetadataSerializer()));
                for (Map map : maps) {
                    if (!map.isEmpty()) throw new AssertionError("CTM clear callback stayed disabled after startup");
                }
            } finally {
                for (int i = 0; i < maps.length; i++) {
                    maps[i].clear();
                    maps[i].putAll(saved[i]);
                }
            }
            Files.write(
                report.toPath(),
                "PASS: startup, GregTech detection, all four CTM maps clear after discovery"
                    .getBytes(StandardCharsets.UTF_8));
        } catch (Throwable failure) {
            Files.write(report.toPath(), ("FAIL: " + failure).getBytes(StandardCharsets.UTF_8));
            Minecraft.getMinecraft()
                .shutdown();
            throw new RuntimeException(failure);
        }
    }
}
