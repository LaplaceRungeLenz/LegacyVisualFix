package com.legacyvisualfix.combat;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import io.netty.buffer.ByteBuf;

/** Server-to-attacker protocol v1. HP units, not hearts. */
public final class HitFeedbackMessage implements IMessage {

    public long sequence;
    public int dimension, targetId;
    public float health, absorbed;

    public HitFeedbackMessage() {}

    public HitFeedbackMessage(long sequence, int dimension, int targetId, float health, float absorbed) {
        this.sequence = sequence;
        this.dimension = dimension;
        this.targetId = targetId;
        this.health = health;
        this.absorbed = absorbed;
    }

    public boolean valid() {
        return sequence > 0 && Float.isFinite(health)
            && Float.isFinite(absorbed)
            && health >= 0
            && absorbed >= 0
            && (health > 0 || absorbed > 0);
    }

    @Override
    public void fromBytes(ByteBuf buffer) {
        sequence = buffer.readLong();
        dimension = buffer.readInt();
        targetId = buffer.readInt();
        health = buffer.readFloat();
        absorbed = buffer.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buffer) {
        buffer.writeLong(sequence);
        buffer.writeInt(dimension);
        buffer.writeInt(targetId);
        buffer.writeFloat(health);
        buffer.writeFloat(absorbed);
    }
}
