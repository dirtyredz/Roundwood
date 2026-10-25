package io.github.dirtyredz.roundwood.block.door.adapter;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.interaction.ProjectedDoorUse;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** One serverbound use request for real leaf hits beyond vanilla's per-cell packet limit. */
public final class DoorUseChannel {
    private static final String VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, "door_use"), () -> VERSION, VERSION::equals, VERSION::equals);

    private DoorUseChannel() {
    }

    public static void register() {
        CHANNEL.messageBuilder(Use.class, 0, NetworkDirection.PLAY_TO_SERVER)
                .encoder((message, buffer) -> buffer.writeBlockHitResult(message.hit()))
                .decoder(buffer -> new Use(buffer.readBlockHitResult()))
                .consumerMainThread((message, context) -> {
                    var player = context.get().getSender();
                    if (player != null) {
                        ProjectedDoorUse.use(player, message.hit());
                    }
                    context.get().setPacketHandled(true);
                }).add();
    }

    public static void send(BlockHitResult hit) {
        CHANNEL.sendToServer(new Use(hit));
    }

    private record Use(BlockHitResult hit) {
    }
}
