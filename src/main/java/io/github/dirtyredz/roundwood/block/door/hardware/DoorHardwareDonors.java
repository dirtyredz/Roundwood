package io.github.dirtyredz.roundwood.block.door.hardware;

import io.github.dirtyredz.roundwood.appearance.*;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceGestures;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import java.util.Optional;

/** Hardware uses the door's material donors with no sneak requirement; dye is still a body gesture. */
public final class DoorHardwareDonors {
    private DoorHardwareDonors() {}
    public static Optional<AppearanceGestures.Gesture> of(Player player,InteractionHand hand){
        return AppearanceGestures.donor(AppearanceFamily.DOOR,player,hand).filter(g->g.type()!=AppearanceGestures.Type.PAINT);
    }
    public static boolean held(Player player){return of(player,InteractionHand.MAIN_HAND).isPresent() || of(player,InteractionHand.OFF_HAND).isPresent();}
}
