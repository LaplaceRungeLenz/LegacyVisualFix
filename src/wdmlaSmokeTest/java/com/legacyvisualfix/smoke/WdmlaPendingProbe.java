package com.legacyvisualfix.smoke;

import java.lang.reflect.Field;
import java.util.function.Function;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import com.gtnewhorizons.wdmla.api.accessor.Accessor;
import com.gtnewhorizons.wdmla.api.accessor.AccessorClientHandler;
import com.gtnewhorizons.wdmla.api.provider.IWDMlaProvider;
import com.gtnewhorizons.wdmla.api.ui.ITooltip;
import com.gtnewhorizons.wdmla.impl.ObjectDataCenter;
import com.gtnewhorizons.wdmla.impl.WDMlaClientRegistration;
import com.gtnewhorizons.wdmla.impl.ui.component.RootComponent;
import com.gtnewhorizons.wdmla.impl.ui.component.TextComponent;
import com.gtnewhorizons.wdmla.overlay.WDMlaTickHandler;
import com.legacyvisualfix.waila.WailaAnimationConfig;

import cpw.mods.fml.common.gameevent.TickEvent;

/** Exercise the real target-data gate, rather than injecting a null HUD after it. */
final class WdmlaPendingProbe {

    interface Drawer {

        void draw(RootComponent root) throws Exception;
    }

    static void run(Drawer drawer) throws Exception {
        TestHandler provider = new TestHandler();
        WDMlaClientRegistration.instance()
            .registerAccessorHandler(TestAccessor.class, provider);
        WDMlaTickHandler handler = new WDMlaTickHandler();
        TestAccessor first = new TestAccessor(1), next = new TestAccessor(2);
        try {
            ObjectDataCenter.set(first);
            ObjectDataCenter.setServerData(new NBTTagCompound());
            RootComponent complete = handler.handle(first);
            require(complete != null, "initial complete tooltip");
            drawer.draw(complete);
            ObjectDataCenter.set(next);
            for (int i = 0; i < 4; i++) {
                RootComponent waiting = handler.handle(next);
                require(waiting == complete, "valid target waiting for data must retain a drawable HUD");
                int before = provider.draws;
                drawer.draw(waiting);
                require(provider.draws > before, "waiting frame must actually draw content");
            }
            require(provider.requests > 0, "pending target still requests fresh data");
            ObjectDataCenter.setServerData(new NBTTagCompound());
            RootComponent replacement = handler.handle(next);
            require(replacement != null && replacement != complete, "fresh server response replaces old HUD");
            drawer.draw(replacement);

            for (TestAccessor moved : new TestAccessor[] { new TestAccessor(3, 64, 0), new TestAccessor(1, 64, 0),
                new TestAccessor(1, 65, 0), new TestAccessor(1, 63, 0), new TestAccessor(1, 63, 1),
                new TestAccessor(1, 63, -1) }) {
                ObjectDataCenter.set(moved);
                RootComponent waiting = handler.handle(moved);
                require(waiting == replacement, "target movement on every axis must keep a drawable HUD");
                drawer.draw(waiting);
                ObjectDataCenter.setServerData(new NBTTagCompound());
                replacement = handler.handle(moved);
                require(replacement != null && replacement != waiting, "each new target replaces retained content");
            }

            ObjectDataCenter.set(first);
            Field retained = WDMlaTickHandler.class.getDeclaredField("legacyvisualfix$pending");
            retained.setAccessible(true);
            Object cache = retained.get(handler);
            Field lastComplete = cache.getClass()
                .getDeclaredField("lastComplete");
            lastComplete.setAccessible(true);
            lastComplete.setLong(cache, System.nanoTime() - 600_000_000L);
            require(handler.handle(first) == null, "stalled response must not leave old content indefinitely");
            ObjectDataCenter.setServerData(new NBTTagCompound());
            require(handler.handle(first) != null, "response after timeout can show fresh content");
            ObjectDataCenter.set(next);
            ObjectDataCenter.setServerData(new NBTTagCompound());

            provider.display = false;
            require(handler.handle(next) == null, "provider suppression hides immediately");
            provider.display = true;
            ObjectDataCenter.set(first);
            require(handler.handle(first) == null, "suppressed content cannot reappear during a later wait");

            ObjectDataCenter.setServerData(new NBTTagCompound());
            handler.handle(first);
            next.connected = false;
            require(handler.handle(next) == null, "disconnected server hides immediately");
            next.connected = true;
            ObjectDataCenter.set(next);
            require(handler.handle(next) == null, "disconnected content cannot reappear");

            ObjectDataCenter.setServerData(new NBTTagCompound());
            handler.handle(next);
            handler.tickClient(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
            ObjectDataCenter.set(first);
            require(handler.handle(first) == null, "early-return tick must invalidate retained HUD");

            ObjectDataCenter.setServerData(new NBTTagCompound());
            handler.handle(first);
            ObjectDataCenter.set(next);
            WailaAnimationConfig.enabled = false;
            try {
                require(handler.handle(next) == null, "disabled adapter preserves original pending behavior");
            } finally {
                WailaAnimationConfig.enabled = true;
            }

            ObjectDataCenter.setServerData(new NBTTagCompound());
            handler.handle(next);
            ObjectDataCenter.set(first);
            int duration = WailaAnimationConfig.durationMs;
            WailaAnimationConfig.durationMs = 0;
            try {
                require(handler.handle(first) == null, "zero duration disables retention");
            } finally {
                WailaAnimationConfig.durationMs = duration;
            }

            ObjectDataCenter.set(next);

            ObjectDataCenter.setServerData(new NBTTagCompound());
            handler.handle(next);
            ObjectDataCenter.set(first);
            Minecraft mc = Minecraft.getMinecraft();
            GuiScreen screen = mc.currentScreen;
            mc.currentScreen = new GuiScreen() {};
            try {
                require(handler.handle(first) == null, "screen change must not carry old content");
            } finally {
                mc.currentScreen = screen;
            }
        } finally {
            ObjectDataCenter.set(null);
            WDMlaClientRegistration.instance().accessorHandlers.remove(TestAccessor.class);
        }
    }

    private static final class TestHandler implements AccessorClientHandler<TestAccessor> {

        boolean display = true;
        int requests, draws;

        public boolean shouldDisplay(TestAccessor accessor) {
            return display;
        }

        public boolean shouldRequestData(TestAccessor accessor) {
            return true;
        }

        public void requestData(TestAccessor accessor) {
            requests++;
        }

        public void gatherComponents(TestAccessor accessor, Function<IWDMlaProvider, ITooltip> provider) {
            provider.apply(null)
                .child(new TextComponent("Target " + accessor.id) {

                    @Override
                    public void tick(float x, float y) {
                        draws++;
                        super.tick(x, y);
                    }
                });
        }
    }

    private static final class TestAccessor implements Accessor {

        final int id, y, z;
        final NBTTagCompound data = new NBTTagCompound();
        boolean connected = true;

        TestAccessor(int id) {
            this(id, 64, 0);
        }

        TestAccessor(int id, int y, int z) {
            this.id = id;
            this.y = y;
            this.z = z;
        }

        public World getWorld() {
            return Minecraft.getMinecraft().theWorld;
        }

        public EntityPlayer getPlayer() {
            return null;
        }

        public NBTTagCompound getServerData() {
            return data;
        }

        public MovingObjectPosition getHitResult() {
            return new MovingObjectPosition(id, y, z, 1, Vec3.createVectorHelper(id, y, z));
        }

        public boolean isServerConnected() {
            return connected;
        }

        public boolean showDetails() {
            return false;
        }

        public Object getTarget() {
            return this;
        }

        public Class<? extends Accessor> getAccessorType() {
            return TestAccessor.class;
        }

        public boolean verifyData(NBTTagCompound tag) {
            return tag != null;
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
