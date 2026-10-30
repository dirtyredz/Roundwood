package io.github.dirtyredz.roundwood.block.trim;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimPlacement;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.STRIP;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.assertAssembly;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.oakDoor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.assertNoTrim;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.assertTrim;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.oakTrim;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.placeFreestanding;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.trimAssembly;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.useAt;

/**
 * Frame placement rules. Frames are legacy content from older saves, written here by the fixture
 * writer ({@code TrimPlacement.placeAt}): the support, cell, fluid, limit and unloaded-chunk checks and
 * chunk-boundary placement; and that the Door Trim item never places a frame itself.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TrimPlacementGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);

    private TrimPlacementGameTests() {
    }

    private static ItemStack two(RoundTrimBlock block) {
        return new ItemStack(block.asItem(), 2);
    }

    @GameTest(template = TEMPLATE)
    public static void missingSupportUnderEitherLowerQuarterRefuses(GameTestHelper helper) {
        floor(helper);
        TrimLayout.Assembly assembly = trimAssembly(helper, ANCHOR, Direction.NORTH);
        // Under the left quarter.
        helper.setBlock(ANCHOR.below(), Blocks.AIR);
        check(!TrimPlacement.placeAt(helper.getLevel(), oakTrim(), assembly), "placed with no floor under the left quarter");
        assertNoTrim(helper, assembly);
        helper.setBlock(ANCHOR.below(), Blocks.STONE);
        // Under the right quarter.
        helper.setBlock(ANCHOR.below().east(), Blocks.AIR);
        check(!TrimPlacement.placeAt(helper.getLevel(), oakTrim(), assembly), "placed with no floor under the right quarter");
        assertNoTrim(helper, assembly);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void occupiedWetAndDoorCellsRefuseWithoutChanges(GameTestHelper helper) {
        floor(helper);
        BlockPos upperRight = ANCHOR.above().east();
        Object[][] blockers = {{Blocks.STONE, "stone"}, {Blocks.WATER, "water"}};
        for (Object[] blocker : blockers) {
            helper.setBlock(upperRight, (net.minecraft.world.level.block.Block) blocker[0]);
            if (TrimPlacement.placeAt(helper.getLevel(), oakTrim(), trimAssembly(helper, ANCHOR, Direction.NORTH))) {
                check(false, blocker[1] + ": placed into a blocked cell");
            }
            assertNoTrim(helper, trimAssembly(helper, ANCHOR, Direction.NORTH));
            helper.setBlock(upperRight, Blocks.AIR);
        }
        // A frame never overwrites a door: aiming so the frame would cover the door's cells refuses.
        ServerPlayer placer = player(helper, "door_cells", GameType.SURVIVAL, Direction.NORTH);
        check(placeDoor(helper, placer, oakDoor().asItem(), ANCHOR.east(), DoorHingeSide.LEFT).consumesAction(), "door");
        check(!TrimPlacement.placeAt(helper.getLevel(), oakTrim(), trimAssembly(helper, ANCHOR, Direction.NORTH)),
                "a frame was placed over door cells");
        assertAssembly(helper, assembly(helper, ANCHOR.east(), Direction.NORTH, DoorHingeSide.LEFT), oakDoor(), false);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void limitsAndUnloadedCellsRefuse(GameTestHelper helper) {
        floor(helper);
        // The top of the world: the upper quarters would be outside the build height.
        BlockPos base = helper.absolutePos(ANCHOR);
        int top = helper.getLevel().getMaxBuildHeight() - 1;
        TrimLayout.Assembly ceiling = new TrimLayout.Assembly(new BlockPos(base.getX(), top, base.getZ()), Direction.NORTH);
        check(!TrimPlacement.placeAt(helper.getLevel(), oakTrim(), ceiling), "placed through the build height");
        // Far away: unloaded cells are never written (or loaded).
        TrimLayout.Assembly far = new TrimLayout.Assembly(base.offset(1_000_000, 0, 1_000_000), Direction.NORTH);
        check(!TrimPlacement.placeAt(helper.getLevel(), oakTrim(), far), "placed into unloaded chunks");
        helper.succeed();
    }

    /** The Door Trim item only trims doors: used on a floor it places no frame, sneaking or not, and is not consumed. */
    @GameTest(template = TEMPLATE)
    public static void theDoorTrimItemNeverPlacesAFrame(GameTestHelper helper) {
        floor(helper);
        BlockPos floorAbs = helper.absolutePos(ANCHOR.below());
        for (boolean sneaking : new boolean[]{false, true}) {
            ServerPlayer player = player(helper, "item_" + sneaking, GameType.SURVIVAL, Direction.NORTH);
            ItemStack stack = new ItemStack(ModItems.ROUND_TRIM.get(), 2);
            useAt(helper, player, stack, floorAbs, Direction.UP, Vec3.atCenterOf(floorAbs).add(0, 0.5, 0), sneaking);
            check(stack.getCount() == 2, "the item was consumed (sneaking=" + sneaking + "): " + stack.getCount());
            for (int x = 0; x < 9; x++) {
                for (int y = 0; y < 6; y++) {
                    for (int z = 0; z < 9; z++) {
                        if (helper.getBlockState(new BlockPos(x, y, z)).getBlock() instanceof RoundTrimBlock) {
                            check(false, "a frame quarter appeared at " + x + "," + y + "," + z + " (sneaking=" + sneaking + ")");
                        }
                    }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = STRIP)
    public static void placesAcrossAChunkBoundary(GameTestHelper helper) {
        floor(helper, 17, 4);
        for (int x = 0; x < 16; x++) {
            BlockPos anchor = new BlockPos(x, 1, 1);
            TrimLayout.Assembly assembly = trimAssembly(helper, anchor, Direction.NORTH);
            int left = assembly.anchor().getX();
            if ((left >> 4) != ((left + 1) >> 4)) {
                ServerPlayer player = player(helper, "chunk", GameType.SURVIVAL, Direction.NORTH);
                check(placeFreestanding(helper, player, two(oakTrim()), anchor).consumesAction(), "chunk-boundary placement refused");
                assertTrim(helper, assembly, oakTrim());
                helper.succeed();
                return;
            }
        }
        helper.fail("the strip template does not span a chunk boundary");
    }
}
