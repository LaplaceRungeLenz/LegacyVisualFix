package com.legacyvisualfix.mixin.compat;

import net.minecraft.client.gui.inventory.GuiContainer;

import org.spongepowered.asm.mixin.Mixin;

/** Adapt RiftFlux's merged callbacks after its default-priority mixin has applied. */
@Mixin(value = GuiContainer.class, priority = 900)
public abstract class MixinRiftFluxPickupStars {
}
