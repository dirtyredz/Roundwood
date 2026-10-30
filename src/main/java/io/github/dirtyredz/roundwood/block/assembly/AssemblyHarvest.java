package io.github.dirtyredz.roundwood.block.assembly;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * When a survival player harvests one part of a door or trim, the loot must still see the anchor
 * (it holds the appearance). Forge removes the block before the harvest callback, and removing a
 * part tears the whole assembly down, anchor included. So for a harvestable server-side player
 * break the physical removal is deferred: {@code onDestroyedByPlayer} only announces it, the loot
 * is evaluated in {@code playerDestroy} with the assembly still present, and the part is then
 * removed normally (teardown, camouflage refund and neighbour updates as before) in a
 * {@code finally}. A deferred part whose harvest callback never came (another mod's break path) is
 * removed at the end of the level tick. Creative, client and non-harvesting removals are immediate.
 * Single responsibility: this lifetime rule; the loot itself is only decorated elsewhere.
 */
public final class AssemblyHarvest {
    private record Pending(Level level, BlockPos pos) {
    }

    /** Server thread only; at most a handful of entries, each gone within the tick. */
    private static final List<Pending> PENDING = new ArrayList<>();

    private AssemblyHarvest() {
    }

    /**
     * From {@code onDestroyedByPlayer}: defers the removal of a harvesting survival break, otherwise
     * runs {@code removeNow} (the ordinary removal). Returns whether the block counts as removed.
     * NeoForge 21.1 calls {@code playerWillDestroy} before this (Forge 1.20.1 called it from the default
     * {@code onDestroyedByPlayer}), so it is never repeated here.
     */
    public static boolean destroyedByPlayer(Level level, BlockPos pos, Player player, boolean willHarvest, BooleanSupplier removeNow) {
        if (level.isClientSide || !willHarvest || player.getAbilities().instabuild) {
            return removeNow.getAsBoolean();
        }
        PENDING.add(new Pending(level, pos.immutable()));
        return true;
    }

    /** From {@code playerDestroy}: evaluates the loot ({@code harvest}) with the part present, then removes it. */
    public static void playerDestroy(Level level, BlockPos pos, Runnable harvest) {
        boolean deferred = PENDING.remove(new Pending(level, pos.immutable()));
        try {
            harvest.run();
        } finally {
            if (deferred) {
                remove(level, pos);
            }
        }
    }

    /** End of a server level tick: removes any deferred part whose harvest never ran. */
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || PENDING.isEmpty()) {
            return;
        }
        for (Iterator<Pending> it = PENDING.iterator(); it.hasNext(); ) {
            Pending pending = it.next();
            if (pending.level() == level) {
                it.remove();
                remove(level, pending.pos());
            }
        }
    }

    private static void remove(Level level, BlockPos pos) {
        FluidState fluid = level.getFluidState(pos);
        level.setBlock(pos, fluid.createLegacyBlock(), Block.UPDATE_ALL);
    }
}
