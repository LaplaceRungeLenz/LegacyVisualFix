package com.legacyvisualfix.core;

import java.util.List;
import java.util.Set;

import org.spongepowered.asm.lib.Opcodes;
import org.spongepowered.asm.lib.tree.AbstractInsnNode;
import org.spongepowered.asm.lib.tree.ClassNode;
import org.spongepowered.asm.lib.tree.FieldInsnNode;
import org.spongepowered.asm.lib.tree.InsnList;
import org.spongepowered.asm.lib.tree.InsnNode;
import org.spongepowered.asm.lib.tree.JumpInsnNode;
import org.spongepowered.asm.lib.tree.LabelNode;
import org.spongepowered.asm.lib.tree.MethodInsnNode;
import org.spongepowered.asm.lib.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/** MyCTMLib excludes its entire package from transformation; guard its merged vanilla callback. */
public final class StartupMixinPlugin implements IMixinConfigPlugin {

    @Override
    public void postApply(String targetName, ClassNode target, String mixinName, IMixinInfo info) {
        if (!mixinName.equals("com.legacyvisualfix.mixin.compat.MixinMyCtmStartup")) return;
        for (MethodNode method : target.methods) {
            if (!method.name.endsWith("$MyCTMLib$onClearResources")
                || !method.desc.equals("(Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;)V")) continue;
            boolean touchesTextures = false;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction instanceof FieldInsnNode) {
                    FieldInsnNode field = (FieldInsnNode) instruction;
                    touchesTextures |= field.getOpcode() == Opcodes.GETSTATIC
                        && field.owner.equals("com/github/wohaopa/MyCTMLib/Textures");
                }
            }
            if (!touchesTextures) continue;
            // Preserve the callback after discovery, including its static GregTech detection.
            LabelNode ready = new LabelNode();
            InsnList guard = new InsnList();
            guard.add(
                new MethodInsnNode(
                    Opcodes.INVOKESTATIC,
                    "com/legacyvisualfix/compat/StartupCompatibility",
                    "modDiscoveryComplete",
                    "()Z",
                    false));
            guard.add(new JumpInsnNode(Opcodes.IFNE, ready));
            guard.add(new InsnNode(Opcodes.RETURN));
            guard.add(ready);
            method.instructions.insert(guard);
        }
    }

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetName, ClassNode target, String mixinName, IMixinInfo info) {}
}
