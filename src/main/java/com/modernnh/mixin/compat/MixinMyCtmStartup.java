package com.modernnh.mixin.compat;

import net.minecraft.client.resources.SimpleReloadableResourceManager;

import org.spongepowered.asm.mixin.Mixin;

/** Apply after MyCTMLib's priority-1000 mixin, then guard its callback in the plugin. */
@Mixin(value = SimpleReloadableResourceManager.class, priority = 900)
public abstract class MixinMyCtmStartup {
}
