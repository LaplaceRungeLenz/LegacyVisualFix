package com.legacyvisualfix.smoke;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.legacyvisualfix.vajra.VajraBlockTarget;

@cpw.mods.fml.common.Mod(
    modid = "legacyvisualfixvajrasmoke",
    name = "LegacyVisualFix Vajra smoke",
    version = "1",
    dependencies = "required-after:legacyvisualfix")
public class VajraBlockTargetSmoke {

    private int ticks;

    @cpw.mods.fml.common.Mod.EventHandler
    public void init(cpw.mods.fml.common.event.FMLInitializationEvent event) {
        cpw.mods.fml.common.FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @cpw.mods.fml.common.eventhandler.SubscribeEvent
    public void tick(cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent event) throws Exception {
        if (event.phase != cpw.mods.fml.common.gameevent.TickEvent.Phase.END || ++ticks != 40) return;
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getMinecraft();
        java.util.List<String> failures = new java.util.ArrayList<String>();
        runCheck("ray hits", this::rejectsMissEntityAndInvalidFaceEvenWithCoordinates, failures);
        runCheck("non-rotatable blocks", this::rejectsAirStoneDirtAndWaterDespiteDefaultRotationAxes, failures);
        runCheck("rotatable blocks", this::acceptsVanillaAndCustomRotationImplementationsWithoutRotatingThem, failures);
        String result = failures.isEmpty()
            ? "PASS: ray hits, air/stone/dirt/water rejection, vanilla/custom rotation and disabled rotation"
            : "FAIL: " + failures;
        java.nio.file.Files.write(
            new java.io.File(mc.mcDataDir, "legacyvisualfix-vajra-smoke.txt").toPath(),
            result.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        mc.shutdown();
        if (!failures.isEmpty()) throw new AssertionError(result);
    }

    private static void runCheck(String name, Runnable check, java.util.List<String> failures) {
        try {
            check.run();
        } catch (Throwable failure) {
            failures.add(name + ": " + failure);
        }
    }

    private static void assertTrue(boolean value) {
        if (!value) throw new AssertionError("Expected true");
    }

    private static void assertFalse(boolean value) {
        if (value) throw new AssertionError("Expected false");
    }

    public void rejectsMissEntityAndInvalidFaceEvenWithCoordinates() {
        MovingObjectPosition hit = new MovingObjectPosition(1, 2, 3, 1, Vec3.createVectorHelper(1, 2, 3));
        assertTrue(VajraBlockTarget.isBlockHit(hit));
        hit.typeOfHit = MovingObjectPosition.MovingObjectType.MISS;
        assertFalse(VajraBlockTarget.isBlockHit(hit));
        hit.typeOfHit = MovingObjectPosition.MovingObjectType.ENTITY;
        assertFalse(VajraBlockTarget.isBlockHit(hit));
        hit.typeOfHit = MovingObjectPosition.MovingObjectType.BLOCK;
        hit.sideHit = -1;
        assertFalse(VajraBlockTarget.isBlockHit(hit));
        assertFalse(VajraBlockTarget.isBlockHit(null));
    }

    public void rejectsAirStoneDirtAndWaterDespiteDefaultRotationAxes() {
        // Forge returns axes even for these blocks; axes alone do not establish support.
        assertTrue(Blocks.air.getValidRotations(null, 0, 0, 0).length > 0);
        for (Block block : new Block[] { Blocks.air, Blocks.stone, Blocks.dirt, Blocks.water }) {
            assertFalse(VajraBlockTarget.canRotate(block, null, 0, 0, 0));
        }
    }

    public void acceptsVanillaAndCustomRotationImplementationsWithoutRotatingThem() {
        assertTrue(VajraBlockTarget.canRotate(Blocks.furnace, null, 0, 0, 0));
        assertTrue(VajraBlockTarget.canRotate(Blocks.log, null, 0, 0, 0));
        assertTrue(VajraBlockTarget.canRotate(Blocks.chest, null, 0, 0, 0));
        assertTrue(VajraBlockTarget.canRotate(new RotatableBlock(), null, 0, 0, 0));
        assertFalse(VajraBlockTarget.canRotate(new DisabledBlock(), null, 0, 0, 0));
    }

    public static class RotatableBlock extends Block {

        public RotatableBlock() {
            super(Material.rock);
        }

        @Override
        public boolean rotateBlock(World world, int x, int y, int z, ForgeDirection axis) {
            throw new AssertionError("Target checks must not mutate the world");
        }
    }

    public static class DisabledBlock extends RotatableBlock {

        @Override
        public ForgeDirection[] getValidRotations(World world, int x, int y, int z) {
            return null;
        }
    }
}
