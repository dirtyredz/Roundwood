package io.github.dirtyredz.roundwood.showcase;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlockEntity;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * Read-back helpers shared by the per-section showcase checks: the sample groups the showcase lays out,
 * the look a placed door, trim or window shows, the first line of a label sign, and whether a pane
 * state has an arm towards a direction.
 */
public final class ShowcaseChecks {
    private ShowcaseChecks() {
    }

    public static List<Appearance> woods() {
        return ShowcaseSamples.woods(ShowcaseSamples.all());
    }

    public static List<Appearance> paints() {
        return ShowcaseSamples.paints(ShowcaseSamples.all());
    }

    public static List<Appearance> masonry() {
        return ShowcaseSamples.masonry(ShowcaseSamples.all());
    }

    public static Appearance doorLook(ServerLevel level, BlockPos anchor) {
        return level.getBlockEntity(anchor) instanceof RoundDoorBlockEntity entity ? entity.appearance() : null;
    }

    public static Appearance trimLook(ServerLevel level, BlockPos anchor) {
        return level.getBlockEntity(anchor) instanceof RoundTrimBlockEntity entity ? entity.appearance() : null;
    }

    public static Appearance windowLook(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof RoundWindowBlockEntity entity ? entity.appearance() : null;
    }

    public static boolean signSays(ServerLevel level, BlockPos pos, String first) {
        return level.getBlockEntity(pos) instanceof SignBlockEntity sign
                && sign.getFrontText().getMessage(0, false).getString().startsWith(first);
    }

    public static boolean connects(BlockState state, Direction direction) {
        return state.getValue(switch (direction) {
            case NORTH -> IronBarsBlock.NORTH;
            case SOUTH -> IronBarsBlock.SOUTH;
            case EAST -> IronBarsBlock.EAST;
            case WEST -> IronBarsBlock.WEST;
            default -> throw new IllegalArgumentException(direction.toString());
        });
    }
}
