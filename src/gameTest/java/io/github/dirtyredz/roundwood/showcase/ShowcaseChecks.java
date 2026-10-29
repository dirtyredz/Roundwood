package io.github.dirtyredz.roundwood.showcase;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;
import java.util.Set;

import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;

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

    public static Appearance windowLook(ServerLevel level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof RoundWindowBlockEntity entity ? entity.appearance() : null;
    }

    public static boolean signSays(ServerLevel level, BlockPos pos, String first) {
        return level.getBlockEntity(pos) instanceof SignBlockEntity sign
                && sign.getFrontText().getMessage(0, false).getString().startsWith(first);
    }

    /**
     * The free-area guard: the rectangle {@code r0..r1} x {@code f0..f1} (every height) holds only the
     * {@code owned} cells and air. A section calls this for its own rectangle, which proves that no
     * other section overlaps it.
     */
    public static void verifyOwnsOnly(ServerLevel level, ShowcaseFrame frame, String section, int r0, int r1, int f0, int f1,
                                      Set<BlockPos> owned) {
        for (int r = r0; r <= r1; r++) {
            for (int f = f0; f <= f1; f++) {
                for (int y = 0; y < ShowcaseFrame.HEIGHT; y++) {
                    BlockPos pos = frame.pos(r, y, f);
                    check(owned.contains(pos) || level.getBlockState(pos).isAir(), "the " + section + " section's free area holds "
                            + level.getBlockState(pos) + " at r " + r + " y " + y + " f " + f);
                }
            }
        }
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
