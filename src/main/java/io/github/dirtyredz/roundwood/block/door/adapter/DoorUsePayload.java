package io.github.dirtyredz.roundwood.block.door.adapter;

import io.github.dirtyredz.roundwood.Roundwood;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;

/** The serverbound {@code roundwood:door_use} wire payload: one real leaf hit (point, face, cell, inside flag). */
public record DoorUsePayload(BlockHitResult hit) implements CustomPacketPayload {
    public static final Type<DoorUsePayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, "door_use"));
    public static final StreamCodec<FriendlyByteBuf, DoorUsePayload> STREAM_CODEC = StreamCodec.of(
            (buffer, payload) -> buffer.writeBlockHitResult(payload.hit()), buffer -> new DoorUsePayload(buffer.readBlockHitResult()));

    @Override
    public Type<DoorUsePayload> type() {
        return TYPE;
    }
}
