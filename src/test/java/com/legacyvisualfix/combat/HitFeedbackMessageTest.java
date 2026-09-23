package com.legacyvisualfix.combat;

import static org.junit.Assert.*;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class HitFeedbackMessageTest {

    @Test
    public void preservesIdentityAndFractionalDamageAcrossTheWire() {
        ByteBuf wire = Unpooled.buffer();
        try {
            new HitFeedbackMessage(71L, -28, 123, 1.25F, 3.5F).toBytes(wire);
            HitFeedbackMessage result = new HitFeedbackMessage();
            result.fromBytes(wire);
            assertTrue(result.valid());
            assertEquals(71L, result.sequence);
            assertEquals(-28, result.dimension);
            assertEquals(123, result.targetId);
            assertEquals(1.25F, result.health, 0);
            assertEquals(3.5F, result.absorbed, 0);
            assertEquals(0, wire.readableBytes());
        } finally {
            wire.release();
        }
    }

    @Test
    public void rejectsImpossibleOrEmptyFeedback() {
        assertFalse(new HitFeedbackMessage(0, 0, 1, 1, 0).valid());
        assertFalse(new HitFeedbackMessage(1, 0, 1, Float.NaN, 0).valid());
        assertFalse(new HitFeedbackMessage(1, 0, 1, Float.POSITIVE_INFINITY, 0).valid());
        assertFalse(new HitFeedbackMessage(1, 0, 1, -1, 3).valid());
        assertFalse(new HitFeedbackMessage(1, 0, 1, 0, 0).valid());
    }
}
