/*
 * SPDX-License-Identifier: GPL-3.0-only
 * Adapted from PinkYuDeer/GTNH-Qol-Improvements. See META-INF/NOTICE-Vajra.md.
 */
package com.modernnh.vajra;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import appeng.api.util.IOrientable;
import cpw.mods.fml.common.eventhandler.Event;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.SoundResource;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaPipeEntity;
import gregtech.api.metatileentity.implementations.MTECable;
import gregtech.api.objects.GTItemStack;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTUtility;
import ic2.api.tile.IWrenchable;

public final class VajraEventHandler {

    private static final int WRENCH_ENERGY_COST = 1_000;

    /** Applies one validated tool action using the exact client-side GT 3x3 hit position. */
    public static void handlePreciseToolClick(EntityPlayer player, int x, int y, int z, int face, float hitX,
        float hitY, float hitZ) {
        if (player == null || player.worldObj == null
            || player.worldObj.isRemote
            || !VajraConfig.enabled
            || !isVajra(player.getHeldItem())
            || face < 0
            || face > 5
            || player.getDistanceSq(x + 0.5D, y + 0.5D, z + 0.5D) > 64.0D
            || !player.worldObj.blockExists(x, y, z)) {
            return;
        }

        World world = player.worldObj;
        if (!(player instanceof EntityPlayerMP)) return;
        EntityPlayerMP serverPlayer = (EntityPlayerMP) player;
        double dx = player.posX - (x + (double) hitX);
        double dy = player.posY + player.getEyeHeight() - (y + (double) hitY);
        double dz = player.posZ - (z + (double) hitZ);
        if (!VajraClickValidation.valid(
            face,
            hitX,
            hitY,
            hitZ,
            dx * dx + dy * dy + dz * dz,
            serverPlayer.theItemInWorldManager.getBlockReachDistance())
            || !player.canPlayerEdit(x, y, z, face, player.getHeldItem())
            || !world.canMineBlock(player, x, y, z)) return;
        PlayerInteractEvent interaction = ForgeEventFactory
            .onPlayerInteract(player, PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK, x, y, z, face, world);
        if (interaction.isCanceled() || interaction.useBlock == Event.Result.DENY
            || interaction.useItem == Event.Result.DENY) return;
        TileEntity tile = world.getTileEntity(x, y, z);
        Block block = world.getBlock(x, y, z);
        if (!isWrenchTarget(tile, block, world, x, y, z)) {
            return;
        }

        ItemStack vajra = player.getHeldItem();
        ForgeDirection clickedSide = ForgeDirection.getOrientation(face);
        ForgeDirection wrenchingSide = GTUtility.determineWrenchingSide(clickedSide, hitX, hitY, hitZ);
        if (tile instanceof IGregTechTileEntity) {
            IGregTechTileEntity gtTile = (IGregTechTileEntity) tile;
            if (!gtTile.isUseableByPlayer(player)) return;
            IMetaTileEntity meta = gtTile.getMetaTileEntity();
            if (meta == null) {
                return;
            }
            if (meta instanceof MTECable) {
                // A combined tool cannot be placed in both GT tool lists: BaseMetaPipeEntity
                // checks the wrench list first. Route cables through the original cutter hook.
                // Temporarily ignore sneaking so this combined tool always toggles precisely
                // the selected grid side instead of invoking GT's multi-cable BFS mode.
                boolean wasSneaking = player.isSneaking();
                boolean handled;
                if (wasSneaking) player.setSneaking(false);
                try {
                    handled = meta.onWireCutterRightClick(clickedSide, wrenchingSide, player, hitX, hitY, hitZ, vajra);
                } finally {
                    if (wasSneaking) player.setSneaking(true);
                }
                if (handled) {
                    synchronizeCablePair(world, x, y, z, wrenchingSide, (MTECable) meta);
                    GTUtility.sendSoundToPlayers(
                        world,
                        SoundResource.GTCEU_OP_WIRECUTTER,
                        1.0F,
                        1.0F,
                        x + 0.5D,
                        y + 0.5D,
                        z + 0.5D);
                }
                return;
            }

            // Let the original GT base-tile implementation handle machines and other pipes.
            // Temporarily advertising the stack as a wrench preserves special machine-facing
            // logic, native charge costs, sounds, covers and the GT 3x3 side calculation.
            GTItemStack wrenchKey = new GTItemStack(vajra, true);
            boolean alreadyRegistered = GregTechAPI.sWrenchList.contains(wrenchKey);
            if (!alreadyRegistered) {
                GregTechAPI.sWrenchList.add(wrenchKey);
            }
            try {
                gtTile.onRightclick(player, clickedSide, hitX, hitY, hitZ);
            } finally {
                if (!alreadyRegistered) {
                    GregTechAPI.sWrenchList.remove(wrenchKey);
                }
            }
            return;
        }

        boolean handled = handleGenericWrench(tile, block, world, x, y, z, wrenchingSide, player, vajra);
        if (handled) {
            if (tile != null) tile.markDirty();
            world.markBlockForUpdate(x, y, z);
            GTUtility
                .sendSoundToPlayers(world, SoundResource.GTCEU_OP_WRENCH, 1.0F, 1.0F, x + 0.5D, y + 0.5D, z + 0.5D);
        }
    }

    /** Keep both cable endpoints and both client connection masks in the same state. */
    private static void synchronizeCablePair(World world, int x, int y, int z, ForgeDirection side, MTECable cable) {
        TileEntity clickedTile = world.getTileEntity(x, y, z);
        TileEntity neighbourTile = world.getTileEntity(x + side.offsetX, y + side.offsetY, z + side.offsetZ);
        MTECable neighbourCable = null;
        if (neighbourTile instanceof IGregTechTileEntity neighbourGt
            && neighbourGt.getMetaTileEntity() instanceof MTECable) {
            neighbourCable = (MTECable) neighbourGt.getMetaTileEntity();
            ForgeDirection opposite = side.getOpposite();
            boolean connected = cable.isConnectedAtSide(side);
            if (connected && !neighbourCable.isConnectedAtSide(opposite)) {
                if (neighbourCable.connect(opposite) <= 0 && !neighbourCable.isConnectedAtSide(opposite)) {
                    // Do not leave a visually and electrically invalid half-connection when
                    // the adjacent cable rejects the reciprocal connection.
                    cable.disconnect(side);
                }
            } else if (!connected && neighbourCable.isConnectedAtSide(opposite)) {
                neighbourCable.disconnect(opposite);
            }
        }

        syncCableTile(world, x, y, z, clickedTile, cable);
        if (neighbourCable != null) {
            syncCableTile(world, x + side.offsetX, y + side.offsetY, z + side.offsetZ, neighbourTile, neighbourCable);
        }
    }

    private static void syncCableTile(World world, int x, int y, int z, TileEntity tile, MTECable cable) {
        cable.markDirty();
        tile.markDirty();
        if (tile instanceof BaseMetaPipeEntity pipe) {
            pipe.updateConnections();
            pipe.issueTextureUpdate();
            pipe.doEnetUpdate();
        } else if (tile instanceof IGregTechTileEntity gtTile) {
            gtTile.issueTextureUpdate();
        }
        world.markBlockForUpdate(x, y, z);
    }

    /** Enables GT's full-block pipe ray trace without putting Vajra in wrench-first routing. */
    public static boolean registerAsGtWireCutter(ItemStack stack) {
        if (isVajra(stack)) return GregTechAPI.sWireCutterList.add(new GTItemStack(stack, true));
        return false;
    }

    public static void unregisterAsGtWireCutter(ItemStack stack) {
        if (isVajra(stack)) {
            GregTechAPI.sWireCutterList.remove(new GTItemStack(stack, true));
        }
    }

    private static boolean handleGenericWrench(TileEntity tile, Block block, World world, int x, int y, int z,
        ForgeDirection direction, EntityPlayer player, ItemStack vajra) {
        if (!canSpendEnergy(vajra, player)) {
            return false;
        }

        boolean handled = false;
        if (tile instanceof IOrientable) {
            handled = rotateOrientable((IOrientable) tile, direction, player.isSneaking());
        } else if (tile instanceof IWrenchable) {
            IWrenchable wrenchable = (IWrenchable) tile;
            if (wrenchable.wrenchCanSetFacing(player, direction.ordinal())) {
                wrenchable.setFacing((short) direction.ordinal());
                handled = true;
            }
        } else if (block != null) {
            ForgeDirection[] rotations = block.getValidRotations(world, x, y, z);
            if (rotations != null) {
                for (ForgeDirection rotation : rotations) {
                    if (rotation == direction) {
                        handled = block.rotateBlock(world, x, y, z, direction);
                        break;
                    }
                }
            }
        }

        if (handled) {
            spendEnergy(vajra, player);
        }
        return handled;
    }

    private static boolean rotateOrientable(IOrientable orientable, ForgeDirection direction, boolean sneaking) {
        if (!orientable.canBeRotated()) {
            return false;
        }

        ForgeDirection front = orientable.getForward();
        ForgeDirection up = orientable.getUp();
        if (front == ForgeDirection.UNKNOWN) {
            front = direction.offsetY == 0 ? ForgeDirection.UP : ForgeDirection.NORTH;
        }
        if (up == ForgeDirection.UNKNOWN || up == front || up == front.getOpposite()) {
            up = front.offsetY == 0 ? ForgeDirection.UP : ForgeDirection.NORTH;
        }
        if (sneaking) {
            up = up.getRotation(front);
        } else {
            front = direction;
            if (up == front || up == front.getOpposite()) {
                up = front.offsetY == 0 ? ForgeDirection.UP : ForgeDirection.NORTH;
            }
        }
        orientable.setOrientation(front, up);
        return true;
    }

    private static boolean canSpendEnergy(ItemStack stack, EntityPlayer player) {
        return player.capabilities.isCreativeMode || !GTModHandler.isElectricItem(stack)
            || GTModHandler.canUseElectricItem(stack, WRENCH_ENERGY_COST);
    }

    private static void spendEnergy(ItemStack stack, EntityPlayer player) {
        if (!player.capabilities.isCreativeMode) {
            GTModHandler.damageOrDechargeItem(stack, 1, WRENCH_ENERGY_COST, player);
        }
    }

    public static boolean isWrenchTarget(TileEntity tile, Block block, World world, int x, int y, int z) {
        if (tile instanceof IGregTechTileEntity || tile instanceof IOrientable || tile instanceof IWrenchable) {
            return true;
        }
        if (block == null) {
            return false;
        }
        ForgeDirection[] rotations = block.getValidRotations(world, x, y, z);
        return rotations != null && rotations.length > 0;
    }

    public static boolean isVajra(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }
        Class<?> type = stack.getItem()
            .getClass();
        while (type != null) {
            String name = type.getName();
            if ("gravisuite.ItemVajra".equals(name) || "gregtech.common.tools.ToolVajra".equals(name)) {
                return true;
            }
            type = type.getSuperclass();
        }
        return false;
    }

}
