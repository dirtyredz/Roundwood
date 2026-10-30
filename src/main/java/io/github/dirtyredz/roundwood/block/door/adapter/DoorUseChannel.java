package io.github.dirtyredz.roundwood.block.door.adapter;

import io.github.dirtyredz.roundwood.block.door.interaction.ProjectedDoorUse;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Registration and transport of {@link DoorUsePayload}, the one serverbound use request for real leaf hits beyond
 * vanilla's per-cell packet limit. Protocol {@value #PROTOCOL} is the NeoForge wire format; Roundwood is required
 * on both sides, so the payload is not optional. Handling runs on the server thread and re-validates everything
 * ({@link ProjectedDoorUse}).
 */
public final class DoorUseChannel {
    public static final String PROTOCOL = "2";

    private DoorUseChannel() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(RegisterPayloadHandlersEvent.class, DoorUseChannel::onRegister);
    }

    private static void onRegister(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(PROTOCOL).executesOn(HandlerThread.MAIN);
        registrar.playToServer(DoorUsePayload.TYPE, DoorUsePayload.STREAM_CODEC, (payload, context) -> {
            if (context.player() instanceof ServerPlayer player) {
                ProjectedDoorUse.use(player, payload.hit());
            }
        });
    }

    /** Client: asks the server to use the door at the real leaf hit. */
    public static void send(BlockHitResult hit) {
        PacketDistributor.sendToServer(new DoorUsePayload(hit));
    }
}
