package io.github.dirtyredz.roundwood.block.archway;

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
import static io.github.dirtyredz.roundwood.fixture.ArchwayFixtures.write;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;

/**
 * Walking through an archway (0.13.1, dk-73098f88): a player (0.6 x 1.8) and a mob (a villager, 0.6 x
 * 1.95, the tallest common walker) move through the opening of both designs, from either side and at
 * several offsets from the middle, with the world's own collision ({@code Entity.move}), and arrive
 * where an unobstructed walk would. The 0.12.0 crown left a 1.875-block opening, so the mob stopped at it.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArchwayWalkGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    /** Blocks the walk covers: from 1.2 in front of the front face to 1.8 past the back face. */
    private static final int STEPS = 40;
    private static final double STEP = 0.1;
    /** Offsets across the passage, in blocks from the archway's left edge; the keyhole's lower passage runs 0.375 to 1.625. */
    private static final double[] LATERAL = {0.8, 1.0, 1.2};

    private ArchwayWalkGameTests() {
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
        for (ArchwayDesign design : ArchwayDesign.values()) {
            for (Direction facing : List.of(Direction.NORTH, Direction.EAST)) {
                ArchwayLayout.Assembly archway = write(helper, ANCHOR, facing, design, Appearance.DEFAULT);
                for (double across : LATERAL) {
                    for (boolean fromFront : new boolean[] {true, false}) {
                        String what = who + " " + design + " " + facing + " at " + across + (fromFront ? " from the front" : " from behind");
                        walk(walker, archway, across, fromFront, what);
                    }
                }
                helper.setBlock(ANCHOR, Blocks.AIR);
            }
        }
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
