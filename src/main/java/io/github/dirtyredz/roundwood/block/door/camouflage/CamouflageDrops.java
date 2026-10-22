package io.github.dirtyredz.roundwood.block.door.camouflage;

import com.mojang.logging.LogUtils;
import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemHandlerHelper;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The material a survival player paid is stored contents, separate from the door's own loot, and
 * comes back exactly once: to the player when they replace or clear it, or as a drop when the
 * assembly is torn down (any part broken, support lost, exploded, replaced or burnt; never decayed by
 * the explosion; nothing with {@code doTileDrops=false}). Whoever withdraws it from the anchor entity
 * first is the only one who can give it back, so partner removal and repeated callbacks find nothing.
 * Creative breaking discards it, through a scope around the player's removal.
 */
public final class CamouflageDrops {
    static final int MAX_DEPTH = 4;
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ThreadLocal<Deque<Level>> DISCARDING = ThreadLocal.withInitial(ArrayDeque::new);

    private CamouflageDrops() {
    }

    /**
     * Runs a creative player's removal of a part with refunds discarded for {@code level}. The scope
     * lives on this thread only for the duration of {@code removal}, never on the entity.
     */
    public static boolean discardingRefunds(Level level, Supplier<Boolean> removal) {
        Deque<Level> scopes = DISCARDING.get();
        if (scopes.size() >= MAX_DEPTH) {
            LOGGER.warn("Roundwood: camouflage discard scopes nested {} deep; running without a new scope", scopes.size());
            return removal.get();
        }
        scopes.push(level);
        try {
            return removal.get();
        } finally {
            scopes.pop();
        }
    }

    /**
     * From the start of a teardown ({@code DoorRemoval.onRemove} of the part at {@code pos}, before its
     * partners go): withdraws the assembly's camouflage and drops the paid item at {@code pos}. The
     * anchor is found from the removed part's old state; an unloaded anchor is left alone (its own
     * integrity cleanup owns the payload later).
     */
    public static void releaseOnTeardown(AssemblyDoorBlock block, BlockState oldState, Level level, BlockPos pos) {
        DoorLayout.Assembly assembly = DoorLayout.of(pos, oldState);
        BlockPos anchor = assembly.anchor();
        if (!level.isLoaded(anchor)) {
            return;
        }
        boolean anchorIsOurs = anchor.equals(pos)
                || assembly.isPart(level.getBlockState(anchor), block, DoorLayout.Part.LOWER_LEFT);
        if (!anchorIsOurs || !(level.getBlockEntity(anchor) instanceof RoundDoorBlockEntity entity)) {
            return;
        }
        Optional<CamouflageData> taken = entity.takeCamouflage();
        if (taken.isEmpty() || !taken.get().paid() || discarding(level)) {
            return;
        }
        Block.popResource(level, pos, taken.get().refundStack()); // honours doTileDrops
    }

    /** Gives a refund back to the player who replaced or cleared it: into the inventory, overflow dropped. */
    public static void giveBack(Player player, CamouflageData previous) {
        if (previous.paid()) {
            ItemHandlerHelper.giveItemToPlayer(player, previous.refundStack());
        }
    }

    private static boolean discarding(Level level) {
        for (Level scoped : DISCARDING.get()) {
            if (scoped == level) {
                return true;
            }
        }
        return false;
    }
}
