package com.legacyvisualfix.mixin.waila.chromatic;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import com.legacyvisualfix.waila.chromatic.ChromaticAnimation;
import com.legacyvisualfix.waila.chromatic.TransformedScissor;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.slprime.chromatictooltips.api.TooltipContext;
import com.slprime.chromatictooltips.util.SectionBox;
import com.slprime.chromatictooltips.util.TooltipDecoratorCollection;

@Mixin(value = SectionBox.class, remap = false)
public abstract class MixinSectionBox {

    @ModifyArgs(
        method = "draw",
        at = @At(
            value = "INVOKE",
            target = "Lcom/slprime/chromatictooltips/util/TooltipTransform;pushTransformMatrix(DDDDJ)V"))
    private void legacyvisualfix$transformBounds(Args args) {
        ChromaticAnimation.Frame frame = ChromaticAnimation.forBox(this);
        if (frame != null) {
            args.set(2, Math.max(0, (double) args.get(2) + frame.deltaWidth));
            args.set(3, Math.max(0, (double) args.get(3) + frame.deltaHeight));
        }
    }

    @WrapOperation(
        method = "draw",
        at = @At(
            value = "INVOKE",
            target = "Lcom/slprime/chromatictooltips/util/TooltipDecoratorCollection;draw(IIIILcom/slprime/chromatictooltips/api/TooltipContext;I)V"))
    private void legacyvisualfix$decorators(TooltipDecoratorCollection decorators, int x, int y, int width, int height,
        TooltipContext context, int color, Operation<Void> original) {
        ChromaticAnimation.Frame frame = ChromaticAnimation.forBox(this);
        if (frame != null) {
            width = Math.max(0, width + frame.deltaWidth);
            height = Math.max(0, height + frame.deltaHeight);
            frame.clipX = x;
            frame.clipY = y;
            frame.clipWidth = width;
            frame.clipHeight = height;
            frame.hasClip = frame.deltaWidth < 0 || frame.deltaHeight < 0;
        }
        original.call(decorators, x, y, width, height, context, color);
    }

    @WrapOperation(
        method = "draw",
        at = @At(
            value = "INVOKE",
            target = "Lcom/slprime/chromatictooltips/util/SectionBox;drawContent(IIIILcom/slprime/chromatictooltips/api/TooltipContext;)V"))
    private void legacyvisualfix$content(SectionBox box, int x, int y, int width, int height, TooltipContext context,
        Operation<Void> original) {
        ChromaticAnimation.Frame frame = ChromaticAnimation.forBox(this);
        if (frame != null && frame.hasClip) {
            try (TransformedScissor clip = new TransformedScissor(
                frame.clipX,
                frame.clipY,
                frame.clipWidth,
                frame.clipHeight)) {
                original.call(box, x, y, width, height, context);
            }
        } else {
            original.call(box, x, y, width, height, context);
        }
    }
}
