package io.github.dirtyredz.roundwood.migration.conversion;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.assembly.AssemblyMutationBatch;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlockEntity;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.geometry.RingGeometry.Quarter;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Converts one 0.7.0 fitted door frame into the door's FRONT trim attachment (0.8.0), as one guarded
 * operation. 0.7.0 saved no "fitted" marker, so only the exact historical arrangement converts: a
 * complete canonical frame with the same facing as a complete canonical door, the frame's anchor one
 * step in front of the door's anchor ({@code doorAnchor.relative(facing.getOpposite())}), all eight
 * cells loaded and the door's FRONT face empty. An open door is eligible (its frame stayed put).
 *
 * <p>Order: revalidate both and capture the frame's look and states; in one mutation batch over the
 * eight cells remove the four quarters silently (no teardown, no loot, no neighbour cascade), and only
 * when all four are gone install the attachment (a failed removal restores the captured quarters and
 * look, and nothing is installed); then tell the frame cells' neighbours. Anything else is retained
 * untouched: an occupied FRONT face (even with the same look), other facings or offsets, incomplete
 * frames (left to their integrity check), 0.4.0 aliases (converted to canonical first). Nothing is
 * force-loaded, and nothing runs while a block snapshot is being restored.
 */
public final class DoorTrimConversion {
    /** Clients only, no shape cascade, drops suppressed. */
    private static final int SILENT = Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS;

    public enum Result {
        /** The frame is now the door's FRONT trim. */
        CONVERTED,
        /** A required cell is unloaded: try again when it loads. */
        DEFERRED,
        /** Not the fitted arrangement (or nothing to do): left exactly as it is. */
        RETAINED
    }

    /** The frame/door pair a cell of either belongs to, as the historical fitting placed them. */
    record Pair(TrimLayout.Assembly frame, DoorLayout.Assembly door) {
    }

    private DoorTrimConversion() {
    }

    /** Tries to convert the fitted pair {@code pos} (any cell of a frame or a door) belongs to. */
    public static Result convert(ServerLevel level, BlockPos pos) {
        if (level.restoringBlockSnapshots || !level.isLoaded(pos)) {
            return Result.DEFERRED;
        }
        Optional<Pair> pair = pair(level, pos, level.getBlockState(pos));
        if (pair.isEmpty()) {
            return Result.RETAINED;
        }
        TrimLayout.Assembly frame = pair.get().frame();
        DoorLayout.Assembly door = pair.get().door();
        if (!frame.loaded(level) || !door.loaded(level)) {
            return Result.DEFERRED;
        }
        RoundTrimBlock frameBlock = ModBlocks.roundTrim();
        RoundDoorBlock doorBlock = ModBlocks.roundDoor();
        if (!frame.complete(level, frameBlock) || !door.complete(level, doorBlock)
                || !(level.getBlockEntity(door.anchor()) instanceof RoundDoorBlockEntity anchor)
                || !anchor.trimsEditable() || anchor.trims().has(AttachmentFace.FRONT)) {
            return Result.RETAINED;
        }
        Appearance look = level.getBlockEntity(frame.anchor()) instanceof RoundTrimBlockEntity entity
                ? entity.appearance() : frameBlock.defaultAppearance();
        Map<Quarter, BlockState> captured = new EnumMap<>(Quarter.class);
        for (Quarter part : Quarter.values()) {
            captured.put(part, level.getBlockState(frame.pos(part)));
        }
        List<BlockPos> cells = new ArrayList<>(frame.positions());
        cells.addAll(door.positions());
        boolean[] installed = {false};
        boolean ran = AssemblyMutationBatch.run(level, cells, () -> {
            boolean all = true;
            for (Quarter part : Quarter.values()) {
                all &= level.setBlock(frame.pos(part), Blocks.AIR.defaultBlockState(), SILENT);
            }
            if (!all) {
                restore(level, frame, captured, look);
                return;
            }
            installed[0] = anchor.setTrims(anchor.trims().with(AttachmentFace.FRONT, look));
            if (!installed[0]) {
                restore(level, frame, captured, look);
            }
        });
        if (!ran || !installed[0]) {
            return Result.RETAINED;
        }
        for (BlockPos cell : frame.positions()) {
            level.updateNeighborsAt(cell, Blocks.AIR);
            level.getBlockState(cell).updateNeighbourShapes(level, cell, Block.UPDATE_CLIENTS, Block.UPDATE_LIMIT - 1);
        }
        return Result.CONVERTED;
    }

    /** The historical pair around {@code pos}, from a canonical frame or a canonical door; empty for anything else. */
    static Optional<Pair> pair(ServerLevel level, BlockPos pos, BlockState state) {
        if (state.is(ModBlocks.roundTrim())) {
            TrimLayout.Assembly frame = TrimLayout.of(pos, state);
            BlockPos doorAnchor = frame.anchor().relative(frame.facing());
            if (!level.isLoaded(doorAnchor)) {
                return Optional.empty();
            }
            BlockState doorState = level.getBlockState(doorAnchor);
            if (!doorState.is(ModBlocks.roundDoor())) {
                return Optional.empty();
            }
            DoorLayout.Assembly door = DoorLayout.of(doorAnchor, doorState);
            return matches(frame, door) ? Optional.of(new Pair(frame, door)) : Optional.empty();
        }
        if (state.is(ModBlocks.roundDoor())) {
            DoorLayout.Assembly door = DoorLayout.of(pos, state);
            BlockPos frameCell = door.anchor().relative(door.facing().getOpposite());
            if (!level.isLoaded(frameCell)) {
                return Optional.empty();
            }
            BlockState frameState = level.getBlockState(frameCell);
            if (!frameState.is(ModBlocks.roundTrim())) {
                return Optional.empty();
            }
            TrimLayout.Assembly frame = TrimLayout.of(frameCell, frameState);
            return matches(frame, door) ? Optional.of(new Pair(frame, door)) : Optional.empty();
        }
        return Optional.empty();
    }

    /** Same facing, and the frame's anchor exactly one step in front of the door's. */
    static boolean matches(TrimLayout.Assembly frame, DoorLayout.Assembly door) {
        return frame.facing() == door.facing() && frame.anchor().equals(door.anchor().relative(door.facing().getOpposite()));
    }

    private static void restore(ServerLevel level, TrimLayout.Assembly frame, Map<Quarter, BlockState> captured, Appearance look) {
        for (Quarter part : Quarter.values()) {
            level.setBlock(frame.pos(part), captured.get(part), SILENT);
        }
        if (level.getBlockEntity(frame.anchor()) instanceof RoundTrimBlockEntity entity) {
            entity.setAppearance(look);
        }
    }

    /** True when {@code state} may start a fitted pair: a canonical frame quarter or a canonical door part (for discovery). */
    public static boolean candidate(BlockState state) {
        return state.is(ModBlocks.roundTrim()) || state.getBlock() == ModBlocks.roundDoor() && state.hasProperty(DoorBlock.HALF);
    }
}
