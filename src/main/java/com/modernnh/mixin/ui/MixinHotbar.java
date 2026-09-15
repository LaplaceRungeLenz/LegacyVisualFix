package com.modernnh.mixin.ui;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.GuiIngameForge;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.modernnh.ui.UiEffectsConfig;
import com.modernnh.ui.UiMotion;

@Mixin(value = GuiIngameForge.class, remap = false)
public abstract class MixinHotbar {

    @Unique
    private long modernnh$last;
    @Unique
    private double modernnh$position;
    @Unique
    private Object modernnh$world;

    @ModifyArg(
        method = "renderHotbar",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/client/GuiIngameForge;drawTexturedModalRect(IIIIII)V",
            ordinal = 1,
            remap = true),
        index = 0)
    private int modernnh$selector(int x) {
        Minecraft mc = Minecraft.getMinecraft();
        int selected = mc.thePlayer.inventory.currentItem;
        long now = System.nanoTime();
        if (!UiEffectsConfig.enabled || !UiEffectsConfig.hotbar || modernnh$last == 0 || modernnh$world != mc.theWorld)
            modernnh$position = selected;
        else modernnh$position = UiMotion
            .approach(modernnh$position, selected, UiEffectsConfig.speed, (now - modernnh$last) * 1e-9);
        modernnh$last = now;
        modernnh$world = mc.theWorld;
        return x + (int) Math.round((modernnh$position - selected) * 20);
    }
}
