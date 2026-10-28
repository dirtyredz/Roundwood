package io.github.dirtyredz.roundwood.block.assembly.placement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.Nullable;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * A player-visible multi-block placement on top of {@link AssemblyWriteTransaction}: it owns its Forge
 * block snapshots, asks the multi-place event for approval, discards only the captures it made and,
 * once installed, runs {@code installed} and then notifies every written cell as vanilla placement does.
 * Refuses to run inside an outer capture, so an item-use capture never receives a duplicate event.
 */
public final class AssemblyCapturedPlacement {
    private AssemblyCapturedPlacement() {}

    public static boolean place(Level level, Map<BlockPos, BlockState> states, BooleanSupplier initialize,
                                @Nullable Player player, Direction face, Runnable installed) {
        int mark = level.capturedBlockSnapshots.size(); boolean capturing = level.captureBlockSnapshots;
        if (mark != 0) return false;
        var before = states.keySet().stream().map(p -> BlockSnapshot.create(level.dimension(), level, p, Block.UPDATE_ALL)).toList();
        boolean success;
        level.captureBlockSnapshots = true;
        try {
            success = AssemblyWriteTransaction.write(level, states, initialize,
                    () -> player == null || !ForgeEventFactory.onMultiBlockPlace(player, before, face));
        } finally {
            level.captureBlockSnapshots = capturing;
            if (level.capturedBlockSnapshots.size() > mark) level.capturedBlockSnapshots.subList(mark, level.capturedBlockSnapshots.size()).clear();
        }
        if (!success) return false;
        installed.run();
        for (var snapshot : before) level.markAndNotifyBlock(snapshot.getPos(), level.getChunkAt(snapshot.getPos()),
                snapshot.getReplacedBlock(), level.getBlockState(snapshot.getPos()), Block.UPDATE_ALL, Block.UPDATE_LIMIT);
        return true;
    }
}
