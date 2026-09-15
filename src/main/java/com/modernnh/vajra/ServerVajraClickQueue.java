/*
 * SPDX-License-Identifier: GPL-3.0-only
 * Adapted from PinkYuDeer/GTNH-Qol-Improvements. See META-INF/NOTICE-Vajra.md.
 */
package com.modernnh.vajra;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Moves precise Vajra clicks from Netty onto the authoritative server thread. */
public final class ServerVajraClickQueue {

    private static final Queue<Request> REQUESTS = new ArrayBlockingQueue<Request>(1024);

    public static void enqueue(EntityPlayerMP player, int x, int y, int z, int face, float hitX, float hitY,
        float hitZ) {
        REQUESTS.offer(new Request(player, x, y, z, face, hitX, hitY, hitZ));
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        Request request;
        for (int processed = 0; processed < 128 && (request = REQUESTS.poll()) != null; processed++) {
            if (request.player.playerNetServerHandler != null && !request.player.isDead
                && request.player.dimension == request.dimension
                && request.player.playerNetServerHandler.netManager.isChannelOpen()) {
                VajraEventHandler.handlePreciseToolClick(
                    request.player,
                    request.x,
                    request.y,
                    request.z,
                    request.face,
                    request.hitX,
                    request.hitY,
                    request.hitZ);
            }
        }
    }

    private static final class Request {

        private final EntityPlayerMP player;
        private final int dimension;
        private final int x;
        private final int y;
        private final int z;
        private final int face;
        private final float hitX;
        private final float hitY;
        private final float hitZ;

        private Request(EntityPlayerMP player, int x, int y, int z, int face, float hitX, float hitY, float hitZ) {
            this.player = player;
            this.dimension = player.dimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.face = face;
            this.hitX = hitX;
            this.hitY = hitY;
            this.hitZ = hitZ;
        }
    }
}
