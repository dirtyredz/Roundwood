package io.github.dirtyredz.roundwood.showcase;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

import java.util.List;

/**
 * Builds the labelled test layout: refuses unless the whole area is inside the world, then clears
 * it to air, lays a stone floor, and runs every registered section. Clearing and the floor use
 * client-only updates, so nothing outside the area is notified or changed.
 */
public final class ShowcaseBuilder {
    private static final int CLEAR_FLAGS = Block.UPDATE_CLIENTS;

    private ShowcaseBuilder() {
    }

    /** @param bounds the rewritten area; @param problems what could not be placed (empty on success) */
    public record Result(boolean built, String message, BoundingBox bounds, List<String> problems) {
    }

    public static Result build(ServerLevel level, BlockPos feet, Direction facing) {
        ShowcaseFrame frame = ShowcaseFrame.at(feet, facing);
        BoundingBox bounds = frame.bounds();
        String refusal = refusal(level, bounds);
        if (refusal != null) {
            return new Result(false, refusal, bounds, List.of());
        }
        for (BlockPos pos : BlockPos.betweenClosed(bounds.minX(), bounds.minY(), bounds.minZ(),
                bounds.maxX(), bounds.maxY(), bounds.maxZ())) {
            level.setBlock(pos, pos.getY() == frame.floorY() ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), CLEAR_FLAGS);
        }
        ShowcaseCanvas canvas = new ShowcaseCanvas(level, frame);
        for (ShowcaseSection section : ShowcaseSections.ALL) {
            section.build(canvas);
        }
        String message = "Built the showcase (" + ShowcaseSections.ALL.size() + " sections) over "
                + size(bounds) + " blocks ahead of you.";
        return new Result(true, message, bounds, List.copyOf(canvas.problems()));
    }

    private static String refusal(ServerLevel level, BoundingBox bounds) {
        if (bounds.minY() < level.getMinBuildHeight() || bounds.maxY() >= level.getMaxBuildHeight()) {
            return "The showcase needs " + (bounds.getYSpan()) + " blocks of height here; it would leave the build limits.";
        }
        BlockPos min = new BlockPos(bounds.minX(), bounds.minY(), bounds.minZ());
        BlockPos max = new BlockPos(bounds.maxX(), bounds.maxY(), bounds.maxZ());
        for (BlockPos corner : new BlockPos[] {min, max, new BlockPos(min.getX(), min.getY(), max.getZ()),
                new BlockPos(max.getX(), min.getY(), min.getZ())}) {
            if (!level.getWorldBorder().isWithinBounds(corner)) {
                return "The showcase would leave the world border.";
            }
        }
        if (!level.hasChunksAt(min, max)) {
            return "The showcase area is not fully loaded; move to the middle of loaded terrain.";
        }
        return null;
    }

    private static String size(BoundingBox bounds) {
        return bounds.getXSpan() + "x" + bounds.getYSpan() + "x" + bounds.getZSpan();
    }
}
