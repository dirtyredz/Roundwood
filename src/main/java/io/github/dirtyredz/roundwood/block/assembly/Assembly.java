package io.github.dirtyredz.roundwood.block.assembly;

import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * One multi-cell assembly (a door, an archway, a compatibility frame), as the shared lifecycle sees it:
 * an anchor cell, the direction the placing player faced, its parts in a fixed order (lower-left,
 * upper-left, lower-right, upper-right; a design may own only the first two) and where each part is.
 * Which state is which part ({@link #isPart}) is the family's: exact membership, never anchor identity.
 * How a part's state is built stays with the family too. Everything else is shared here, and every
 * traversal asks {@link Level#isLoaded} before it reads a cell, so an unloaded cell is never read or
 * loaded.
 *
 * @param <P> the family's part identity
 */
public interface Assembly<P> {
    /** The lower-left cell, which carries the anchor entity. */
    BlockPos anchor();

    /** The direction the placing player faced; the right column is its clockwise neighbour. */
    Direction facing();

    /** The parts this assembly owns, anchor first, in the order above. */
    List<P> parts();

    BlockPos pos(P part);

    /** True when {@code state} is exactly this assembly's {@code part} of {@code block}. */
    boolean isPart(BlockState state, Block block, P part);

    /** Every cell, in {@link #parts} order. */
    default List<BlockPos> positions() {
        List<P> parts = parts();
        List<BlockPos> out = new ArrayList<>(parts.size());
        for (P part : parts) {
            out.add(pos(part));
        }
        return out;
    }

    /** True when every cell is in a loaded chunk. */
    default boolean loaded(Level level) {
        for (P part : parts()) {
            if (!level.isLoaded(pos(part))) {
                return false;
            }
        }
        return true;
    }

    /** The parts currently present and matching; never reads an unloaded cell. */
    default List<P> presentParts(Level level, Block block) {
        List<P> parts = parts();
        List<P> present = new ArrayList<>(parts.size());
        for (P part : parts) {
            BlockPos pos = pos(part);
            if (level.isLoaded(pos) && isPart(level.getBlockState(pos), block, part)) {
                present.add(part);
            }
        }
        return present;
    }

    /** True when every cell is loaded and holds this assembly's parts. */
    default boolean complete(Level level, Block block) {
        return presentParts(level, block).size() == parts().size();
    }

    /** True when {@code player} may interact with every cell (the whole-assembly edit permission). */
    default boolean mayEditAll(Level level, Player player) {
        for (BlockPos cell : positions()) {
            if (!level.mayInteract(player, cell)) {
                return false;
            }
        }
        return true;
    }

    /**
     * A world point in the assembly's authored frame, in pixels: x across from the left edge, y up from
     * the anchor's floor, z the depth with the front face (the placing player's side) at 16 and the back
     * at 0.
     */
    default Vec3 toAuthored(Vec3 world) {
        BlockPos anchor = anchor();
        double x = (world.x - anchor.getX()) * 16;
        double y = (world.y - anchor.getY()) * 16;
        double z = (world.z - anchor.getZ()) * 16;
        // Undo the facing's clockwise quarter turns about the anchor cell's centre: (x, z) <- (z, 16 - x).
        for (int i = 0; i < VoxelShapeFactory.quarterTurns(facing()); i++) {
            double turnedX = z;
            z = 16 - x;
            x = turnedX;
        }
        return new Vec3(x, y, z);
    }
}
