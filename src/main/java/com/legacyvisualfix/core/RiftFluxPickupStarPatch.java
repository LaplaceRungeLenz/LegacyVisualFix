package com.legacyvisualfix.core;

import java.util.ArrayList;

import org.spongepowered.asm.lib.Handle;
import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.Type;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.AnnotationNode;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.InsnNode;
import org.spongepowered.asm.lib.tree.InvokeDynamicInsnNode;
import org.spongepowered.asm.lib.tree.JumpInsnNode;
import org.spongepowered.asm.lib.tree.LabelNode;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.lib.tree.VarInsnNode;

/** Optional compatibility with RiftFlux's screen-space pickup stars; no RiftFlux classes are loaded. */
final class RiftFluxPickupStarPatch {

    private static final String SOURCE = "com.voidsrift.riftflux.mixin.early.MixinItemPickupStar";
    private static final String CALLBACK = "(IIFLorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V";
    private static final String ANIMATIONS = "com/legacyvisualfix/inventory/InventoryAnimations";

    private RiftFluxPickupStarPatch() {}

    static void apply(ClassNode target) {
        // A regex injector skips MixinMerged methods. Inspect the actual source annotation instead.
        for (MethodNode method : new ArrayList<>(target.methods)) {
            if (!CALLBACK.equals(method.desc) || !fromRiftFlux(method)) continue;
            if (method.name.endsWith("$rf$drawStarsAfterPop") && !calls(method, "draw")) {
                wrapRender(target, method);
            } else if (method.name.endsWith("$rf$sendClearIfHovered") && !calls(method, "entering")) {
                LabelNode ready = new LabelNode();
                InsnList guard = new InsnList();
                guard.add(new VarInsnNode(Opcodes.ALOAD, 0));
                guard.add(
                    new MethodInsnNode(Opcodes.INVOKESTATIC, ANIMATIONS, "entering", "(Ljava/lang/Object;)Z", false));
                guard.add(new JumpInsnNode(Opcodes.IFEQ, ready));
                guard.add(new InsnNode(Opcodes.RETURN));
                guard.add(ready);
                method.instructions.insert(guard);
            }
        }
    }

    private static boolean fromRiftFlux(MethodNode method) {
        if (method.visibleAnnotations == null) return false;
        for (AnnotationNode annotation : method.visibleAnnotations) {
            if (!"Lorg/spongepowered/asm/mixin/transformer/meta/MixinMerged;".equals(annotation.desc)
                || annotation.values == null) continue;
            for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
                if ("mixin".equals(annotation.values.get(i)) && SOURCE.equals(annotation.values.get(i + 1)))
                    return true;
            }
        }
        return false;
    }

    private static boolean calls(MethodNode method, String name) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode) {
                MethodInsnNode call = (MethodInsnNode) instruction;
                if (ANIMATIONS.equals(call.owner) && name.equals(call.name)) return true;
            }
        }
        return false;
    }

    private static void wrapRender(ClassNode target, MethodNode method) {
        String helperName = method.name + "$legacyvisualfix";
        while (hasMethod(target, helperName)) helperName += "$";
        MethodNode body = new MethodNode(
            Opcodes.ACC_PRIVATE | Opcodes.ACC_SYNTHETIC,
            helperName,
            method.desc,
            null,
            null);
        body.instructions = method.instructions;
        body.tryCatchBlocks = method.tryCatchBlocks;
        body.localVariables = method.localVariables;
        body.visibleLocalVariableAnnotations = method.visibleLocalVariableAnnotations;
        body.invisibleLocalVariableAnnotations = method.invisibleLocalVariableAnnotations;
        body.maxLocals = method.maxLocals;
        body.maxStack = method.maxStack;
        target.methods.add(body);

        // Keep the merged handler's identity and annotations. Only its body becomes a scoped call.
        method.instructions = new InsnList();
        method.tryCatchBlocks = new ArrayList<>();
        method.localVariables = null;
        method.visibleLocalVariableAnnotations = null;
        method.invisibleLocalVariableAnnotations = null;
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
        method.instructions.add(new VarInsnNode(Opcodes.FLOAD, 3));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 4));
        method.instructions.add(
            new InvokeDynamicInsnNode(
                "run",
                "(L" + target.name + ";" + CALLBACK.substring(1, CALLBACK.length() - 1) + "Ljava/lang/Runnable;",
                new Handle(
                    Opcodes.H_INVOKESTATIC,
                    "java/lang/invoke/LambdaMetafactory",
                    "metafactory",
                    "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                        + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)"
                        + "Ljava/lang/invoke/CallSite;",
                    false),
                Type.getType("()V"),
                new Handle(Opcodes.H_INVOKESPECIAL, target.name, helperName, CALLBACK, false),
                Type.getType("()V")));
        method.instructions.add(
            new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                ANIMATIONS,
                "draw",
                "(Ljava/lang/Object;Ljava/lang/Runnable;)V",
                false));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.maxLocals = 5;
        method.maxStack = 6;
        target.version = Math.max(target.version, Opcodes.V1_8);
    }

    private static boolean hasMethod(ClassNode target, String name) {
        for (MethodNode method : target.methods)
            if (name.equals(method.name) && CALLBACK.equals(method.desc)) return true;
        return false;
    }
}
