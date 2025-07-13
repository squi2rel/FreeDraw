package com.github.squi2rel.freedraw.network;

import com.github.squi2rel.freedraw.FreeDraw;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DrawPayload(byte[] data) implements CustomPayload {
    public static final Identifier CONFIG_PAYLOAD_ID = Identifier.of(FreeDraw.MOD_ID, "payload");
    public static final Id<DrawPayload> ID = new Id<>(CONFIG_PAYLOAD_ID);
    public static final PacketCodec<PacketByteBuf, DrawPayload> CODEC = PacketCodec.of((p, buf) -> buf.writeBytes(p.data), buf -> {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return new DrawPayload(data);
    });

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        PayloadTypeRegistry.playS2C().register(ID, CODEC);
        PayloadTypeRegistry.playC2S().register(ID, CODEC);
    }
}
