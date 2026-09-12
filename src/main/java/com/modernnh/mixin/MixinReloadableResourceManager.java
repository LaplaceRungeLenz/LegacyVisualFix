package com.modernnh.mixin;

import java.util.List;

import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.IResourcePack;
import net.minecraft.client.resources.SimpleReloadableResourceManager;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.modernnh.reload.ReloadScreen;

@Mixin(SimpleReloadableResourceManager.class)
public abstract class MixinReloadableResourceManager {

    @Shadow
    @Final
    private List<IResourceManagerReloadListener> reloadListeners;

    @WrapMethod(method = "reloadResources")
    private void modernnh$reload(List<IResourcePack> packs, Operation<Void> original) {
        ReloadScreen.runReload(reloadListeners.size(), () -> original.call(packs));
    }

    @WrapOperation(
        method = "notifyReloadListeners",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/IResourceManagerReloadListener;onResourceManagerReload(Lnet/minecraft/client/resources/IResourceManager;)V"))
    private void modernnh$listener(IResourceManagerReloadListener listener, IResourceManager manager,
        Operation<Void> original) {
        ReloadScreen.beforeListener(listener);
        original.call(listener, manager);
        ReloadScreen.afterListener();
    }
}
