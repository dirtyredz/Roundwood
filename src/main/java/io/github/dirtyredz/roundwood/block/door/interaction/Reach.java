package io.github.dirtyredz.roundwood.block.door.interaction;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** The point-reach rule Forge 1.20.1's {@code canReach(Vec3, 0)} applied: the eye within block interaction range. */
final class Reach {
    private Reach() {
    }

    static boolean within(Player player, Vec3 point) {
        double reach = player.blockInteractionRange();
        return player.getEyePosition().distanceToSqr(point) < reach * reach;
    }
}
