package io.github.dirtyredz.roundwood.block.arch;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayLayout;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static io.github.dirtyredz.roundwood.fixture.ArchwayFixtures.point;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPiecePlacement;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;

/** Real player/villager movement through ordinary/keyhole compositions, for every top size at low and taller placement heights. */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArchPieceWalkGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    /** Blocks the walk covers: from 1.2 in front of the front face to 1.8 past the back face. */
    private static final int STEPS = 40;
    private static final double STEP = 0.1;
    /** Offsets across the passage, in blocks from the archway's left edge; the keyhole's lower passage runs 0.375 to 1.625. */
    private static final double[] LATERAL = {0.8, 1.0, 1.2};

    private ArchPieceWalkGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void aPlayerSizedEntityWalksThroughBothDesigns(GameTestHelper helper) {
        floor(helper);
        Entity walker = player(helper, "archway_walker", GameType.SURVIVAL, Direction.NORTH);
        walkAll(helper, walker, "player");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void aMobWalksThroughBothDesigns(GameTestHelper helper) {
        floor(helper);
        Entity walker = helper.spawnWithNoFreeWill(EntityType.VILLAGER, new BlockPos(0, 4, 0));
        check(walker.getBbHeight() > 1.9F, "the walker is not mob-sized: " + walker.getBbHeight());
        walkAll(helper, walker, "mob");
        helper.succeed();
    }

    private static void walkAll(GameTestHelper helper, Entity walker, String who) {
        for (int height : new int[] {1, 3}) {
            for (ArchPieceDesign top : ArchPieceDesign.values()) {
                if (top.shoulder()) continue;
                for (ArchwayDesign design : ArchwayDesign.values()) {
                    if (top.columns() == 1 && design == ArchwayDesign.KEYHOLE) continue;
                    for (Direction facing : List.of(Direction.NORTH, Direction.EAST)) {
                        ArchwayLayout.Assembly archway = new ArchwayLayout.Assembly(helper.absolutePos(ANCHOR), facing, design);
                        install(helper, archway.anchor().above(height), facing, top, Side.LEFT);
                        if (design == ArchwayDesign.KEYHOLE) {
                            install(helper, archway.anchor().above(height - 1), facing, ArchPieceDesign.KEYHOLE_SHOULDER, Side.LEFT);
                            install(helper, archway.anchor().above(height - 1).relative(facing.getClockWise(), top.columns() - 1), facing, ArchPieceDesign.KEYHOLE_SHOULDER, Side.RIGHT);
                        }
                        for (int y = 0; y < height + top.rows(); y++) {
                            helper.getLevel().setBlockAndUpdate(archway.anchor().relative(facing.getCounterClockWise()).above(y), Blocks.OAK_PLANKS.defaultBlockState());
                            helper.getLevel().setBlockAndUpdate(archway.anchor().relative(facing.getClockWise(), top.columns()).above(y), Blocks.OAK_PLANKS.defaultBlockState());
                        }
                        for (double offset : LATERAL) {
                            double across = top.columns() / 2.0 + (offset - 1.0) * (top.columns() == 1 ? .5 : 1);
                            for (boolean fromFront : new boolean[] {true, false}) {
                                String what = who + " " + top + " " + design + " " + facing + " at " + across + (fromFront ? " from the front" : " from behind");
                                walk(walker, archway, across, fromFront, what);
                            }
                        }
                        for (int y = 0; y < height + top.rows(); y++) for (int x = -1; x <= top.columns(); x++)
                            helper.getLevel().setBlockAndUpdate(archway.anchor().relative(facing.getClockWise(), x).above(y), Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        walker.discard();
    }

    private static void install(GameTestHelper helper, BlockPos pos, Direction facing, ArchPieceDesign design, Side side) {
        var piece = new ArchPieceLayout.Assembly(pos, facing, design, side);
        check(ArchPiecePlacement.placeAt(helper.getLevel(), ModBlocks.archPiece(design), piece, Appearance.DEFAULT), "piece placement refused");
    }

    /** Steps {@code walker} through the opening at {@code across} and requires every step to be free. */
    private static void walk(Entity walker, ArchwayLayout.Assembly archway, double across, boolean fromFront, String what) {
        double from = fromFront ? -1.2 : 2.2;
        Vec3 start = point(archway, across, 0.0, from);
        Vec3 step = point(archway, across, 0.0, from + (fromFront ? STEP : -STEP)).subtract(start);
        walker.setPos(start);
        walker.setDeltaMovement(Vec3.ZERO);
        for (int i = 0; i < STEPS; i++) {
            Vec3 before = walker.position();
            walker.move(MoverType.SELF, step);
            check(walker.position().distanceTo(before.add(step)) < 1e-6, what + ": stopped at step " + i + ", at "
                    + walker.position() + " instead of " + before.add(step));
        }
    }
}
