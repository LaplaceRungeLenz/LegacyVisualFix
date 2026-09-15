package com.modernnh.vajra;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

public class VajraToolClickMessageTest {

    @Test
    public void preservesNegativeWorldCoordinatesFaceAndPreciseGridHit() {
        ByteBuf wire = Unpooled.buffer();
        ByteBuf result = Unpooled.buffer();
        try {
            new VajraToolClickMessage(-12345, 255, 23456, 5, 0.125F, 0.875F, 1).toBytes(wire);
            assertEquals(25, wire.readableBytes());
            VajraToolClickMessage received = new VajraToolClickMessage();
            received.fromBytes(wire);
            received.toBytes(result);
            assertEquals(-12345, result.readInt());
            assertEquals(255, result.readInt());
            assertEquals(23456, result.readInt());
            assertEquals(5, result.readUnsignedByte());
            assertEquals(0.125F, result.readFloat(), 0);
            assertEquals(0.875F, result.readFloat(), 0);
            assertEquals(1, result.readFloat(), 0);
            assertEquals(0, result.readableBytes());
        } finally {
            wire.release();
            result.release();
        }
    }
}
