package com.legacyvisualfix.smoke;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import com.legacyvisualfix.fov.FovConfig;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Opt-in test of the transformed renderer and real sprint attribute; excluded from normal jars. */
@Mod(
    modid = "legacyvisualfixfovsmoke",
    name = "LegacyVisualFix FOV smoke",
    version = "1",
    dependencies = "required-after:legacyvisualfix")
public final class FovSmoke {

    private int ticks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END || ++ticks != 40) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityLivingBase previousView = mc.renderViewEntity;
        boolean enabled = FovConfig.enabled;
        int duration = FovConfig.transitionMs;
        File result = new File(mc.mcDataDir, "legacyvisualfix-fov-smoke.txt");
        try {
            WorldClient world = new WorldClient(
                new NetHandlerPlayClient(mc, null, null),
                new WorldSettings(0, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT),
                0,
                EnumDifficulty.PEACEFUL,
                mc.mcProfiler);
            EntityPlayerSP player = new EntityPlayerSP(mc, world, mc.getSession(), 0);
            mc.renderViewEntity = player;
            EntityRenderer renderer = mc.entityRenderer;
            Method update = EntityRenderer.class.getDeclaredMethod("updateFovModifierHand");
            update.setAccessible(true);
            Field current = field("fovModifierHand");
            Field previous = field("fovModifierHandPrev");
            FovConfig.enabled = true;
            FovConfig.transitionMs = 300;
            current.setFloat(renderer, 1);
            player.setSprinting(true);
            check(player.getFOVMultiplier(), 1.15F);
            update.invoke(renderer);
            float first = current.getFloat(renderer);
            if (!(first > 1 && first < 1.075F)) throw new AssertionError("sprint was not smoothed");
            check(previous.getFloat(renderer), 1);
            for (int i = 1; i < 6; i++) update.invoke(renderer);
            check(current.getFloat(renderer), 1.1425F);
            player.setSprinting(false);
            update.invoke(renderer);
            if (!(current.getFloat(renderer) > 1 && current.getFloat(renderer) < 1.1425F)) {
                throw new AssertionError("stop sprinting was discontinuous");
            }
            FovConfig.enabled = false;
            current.setFloat(renderer, 1);
            player.setSprinting(true);
            update.invoke(renderer);
            check(current.getFloat(renderer), 1.075F);
            FovConfig.enabled = true;
            FovConfig.transitionMs = 0;
            current.setFloat(renderer, 1);
            update.invoke(renderer);
            check(current.getFloat(renderer), 1.075F);
            Files.write(
                result.toPath(),
                "PASS: transformed renderer, sprint, reversal, previous tick, disabled, zero duration"
                    .getBytes(StandardCharsets.UTF_8));
        } catch (Throwable failure) {
            Files.write(result.toPath(), ("FAIL: " + failure).getBytes(StandardCharsets.UTF_8));
            throw new RuntimeException(failure);
        } finally {
            mc.renderViewEntity = previousView;
            FovConfig.enabled = enabled;
            FovConfig.transitionMs = duration;
            mc.shutdown();
        }
    }

    private static Field field(String name) throws Exception {
        Field field = EntityRenderer.class.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void check(float actual, float expected) {
        if (Math.abs(actual - expected) > 0.00001F) throw new AssertionError(actual + " != " + expected);
    }
}
