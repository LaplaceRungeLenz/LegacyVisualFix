package com.legacyvisualfix.vajra;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockCocoa;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockEndPortalFrame;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockFurnace;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockHugeMushroom;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockLog;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockPistonExtension;
import net.minecraft.block.BlockPumpkin;
import net.minecraft.block.BlockRail;
import net.minecraft.block.BlockRailDetector;
import net.minecraft.block.BlockRailPowered;
import net.minecraft.block.BlockRedstoneComparator;
import net.minecraft.block.BlockRedstoneRepeater;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockTripWireHook;
import net.minecraft.block.BlockVine;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

/** Side-effect-free target checks; Forge's default rotation axes also include air and stone. */
public final class VajraBlockTarget {

    private static final ClassValue<Boolean> CUSTOM_ROTATION = new ClassValue<Boolean>() {

        @Override
        protected Boolean computeValue(Class<?> type) {
            try {
                // Forge-added method: its name is unchanged in production jars.
                return type.getMethod("rotateBlock", World.class, int.class, int.class, int.class, ForgeDirection.class)
                    .getDeclaringClass() != Block.class;
            } catch (NoSuchMethodException ignored) {
                return false;
            }
        }
    };

    private VajraBlockTarget() {}

    public static boolean isBlockHit(MovingObjectPosition hit) {
        return hit != null && hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
            && hit.sideHit >= 0
            && hit.sideHit < 6
            && hit.hitVec != null;
    }

    public static boolean canRotate(Block block, World world, int x, int y, int z) {
        if (block == null || block.isAir(world, x, y, z)) return false;
        if (!hasVanillaRotation(block) && !CUSTOM_ROTATION.get(block.getClass())) return false;
        ForgeDirection[] axes = block.getValidRotations(world, x, y, z);
        return axes != null && axes.length > 0;
    }

    private static boolean hasVanillaRotation(Block block) {
        // The block families handled by Forge 1.7.10 RotationHelper.rotateVanillaBlock.
        // Checking getValidRotations alone is insufficient: its default is all six axes.
        return block instanceof BlockAnvil || block instanceof BlockBed
            || block instanceof BlockButton
            || block instanceof BlockChest
            || block instanceof BlockCocoa
            || block instanceof BlockDispenser
            || block instanceof BlockDoor
            || block instanceof BlockEnderChest
            || block instanceof BlockEndPortalFrame
            || block instanceof BlockFenceGate
            || block instanceof BlockFurnace
            || block instanceof BlockHopper
            || block instanceof BlockHugeMushroom
            || block instanceof BlockLadder
            || block instanceof BlockLever
            || block instanceof BlockLog
            || block instanceof BlockPistonBase
            || block instanceof BlockPistonExtension
            || block instanceof BlockPumpkin
            || block instanceof BlockRail
            || block instanceof BlockRailDetector
            || block instanceof BlockRailPowered
            || block instanceof BlockRedstoneComparator
            || block instanceof BlockRedstoneRepeater
            || block instanceof BlockSkull
            || block instanceof BlockStairs
            || block instanceof BlockTorch
            || block instanceof BlockTrapDoor
            || block instanceof BlockTripWireHook
            || block instanceof BlockVine
            || block == Blocks.wall_sign
            || block == Blocks.standing_sign;
    }
}
