package com.modernnh.mixin.angelica;

import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.modernnh.reload.ReloadScreen;

@Pseudo
@Mixin(targets = "net.coderbot.iris.pipeline.PipelineManager", remap = false)
public abstract class MixinPipelineManager {

    @WrapOperation(
        method = "preparePipeline",
        at = @At(value = "INVOKE", target = "Ljava/util/function/Function;apply(Ljava/lang/Object;)Ljava/lang/Object;"),
        remap = false)
    private Object modernnh$pipeline(Function<?, ?> factory, Object dimension, Operation<Object> original) {
        // The same factory creates vanilla fixed-function pipelines while shaders are off.
        try {
            Class<?> iris = Class.forName("net.coderbot.iris.Iris", false, getClass().getClassLoader());
            if (!((java.util.Optional<?>) iris.getMethod("getCurrentPack")
                .invoke(null)).isPresent()) {
                return original.call(factory, dimension);
            }
        } catch (ReflectiveOperationException e) {
            return original.call(factory, dimension);
        }
        Object[] result = new Object[1];
        ReloadScreen.runShaderPipeline(() -> result[0] = original.call(factory, dimension));
        return result[0];
    }
}
