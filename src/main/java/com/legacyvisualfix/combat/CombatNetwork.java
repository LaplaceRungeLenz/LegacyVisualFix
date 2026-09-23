package com.legacyvisualfix.combat;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.NetworkManager;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class CombatNetwork {

    public static final String CHANNEL_NAME = "lvf_combat_v1";
    private static final Set<NetworkManager> PEERS = ConcurrentHashMap.newKeySet();
    private static final AtomicLong SEQUENCE = new AtomicLong();
    private static SimpleNetworkWrapper channel;

    private CombatNetwork() {}

    public static void register() {
        channel = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL_NAME);
        channel.registerMessage(Handler.class, HitFeedbackMessage.class, 0, Side.CLIENT);
        FMLCommonHandler.instance()
            .bus()
            .register(new CombatNetwork());
    }

    @SubscribeEvent
    public void channels(FMLNetworkEvent.CustomPacketRegistrationEvent<?> event) {
        if (event.side != Side.SERVER || !event.registrations.contains(CHANNEL_NAME)) return;
        if ("REGISTER".equals(event.operation)) PEERS.add(event.manager);
        else if ("UNREGISTER".equals(event.operation)) PEERS.remove(event.manager);
    }

    @SubscribeEvent
    public void disconnected(FMLNetworkEvent.ServerDisconnectionFromClientEvent event) {
        PEERS.remove(event.manager);
    }

    public static void clearPeers() {
        PEERS.clear();
    }

    public static void send(EntityPlayerMP attacker, EntityLivingBase target, DamageTransactions.Result result) {
        if (channel == null || attacker.playerNetServerHandler == null) return;
        NetworkManager connection = attacker.playerNetServerHandler.netManager;
        if (!PEERS.contains(connection) || !connection.isChannelOpen()) return;
        channel.sendTo(
            new HitFeedbackMessage(
                SEQUENCE.incrementAndGet(),
                target.dimension,
                target.getEntityId(),
                result.health,
                result.absorbed),
            attacker);
    }

    public static final class Handler implements IMessageHandler<HitFeedbackMessage, IMessage> {

        @Override
        public IMessage onMessage(HitFeedbackMessage message, MessageContext context) {
            CombatInbox.offer(message, context.netHandler);
            return null;
        }
    }
}
