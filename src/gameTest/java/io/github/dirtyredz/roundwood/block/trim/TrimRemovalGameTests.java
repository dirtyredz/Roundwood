package io.github.dirtyredz.roundwood.block.trim;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageData;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.geometry.RingGeometry.Quarter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.anchorEntity;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assertAssembly;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assertNoDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.clearDrops;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.dropped;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.hold;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.oakDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.rel;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.styled;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.assertNoTrim;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.assertTrim;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.BRICK;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.fitToDoor;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.fittedTo;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.oakTrim;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.placeFreestanding;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.trimAssembly;

/** Breaking any quarter of a legacy frame (placed by the fixture writer) removes the frame and yields at most the loot of one frame; nothing else is disturbed. */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TrimRemovalGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    private static final BlockPos DOOR_ANCHOR = new BlockPos(3, 1, 3);

    private TrimRemovalGameTests() {
    }

    /** A spruce door: other suites drop oak doors, and the drop count reaches into neighbouring test areas. */
    private static final Appearance SPRUCE = Appearance.wood(ResourceLocation.fromNamespaceAndPath("minecraft", "spruce_planks"));

    /** Places a freestanding brick frame with the fixture writer and clears the drops. */
    private static TrimLayout.Assembly placeBrick(GameTestHelper helper, String name) {
        floor(helper);
        ServerPlayer player = player(helper, name, GameType.SURVIVAL, Direction.NORTH);
        check(placeFreestanding(helper, player, styled(oakTrim(), BRICK), ANCHOR).consumesAction(), name + ": placement failed");
        TrimLayout.Assembly assembly = trimAssembly(helper, ANCHOR, Direction.NORTH);
        assertTrim(helper, assembly, oakTrim());
        clearDrops(helper);
        return assembly;
    }

    /** Places a freestanding frame with the fixture writer and clears the drops. */
    private static TrimLayout.Assembly place(GameTestHelper helper, RoundTrimBlock block, String name) {
        ServerPlayer player = player(helper, name + "_placer", GameType.SURVIVAL, Direction.NORTH);
        check(placeFreestanding(helper, player, new ItemStack(block), ANCHOR).consumesAction(), name + ": placement failed");
        TrimLayout.Assembly assembly = trimAssembly(helper, ANCHOR, Direction.NORTH);
        assertTrim(helper, assembly, block);
        clearDrops(helper);
        return assembly;
    }

    @GameTest(template = TEMPLATE)
    public static void survivalBreakOfAnyQuarterDropsOneOakFrame(GameTestHelper helper) {
        floor(helper);
        for (Quarter part : Quarter.values()) {
            TrimLayout.Assembly assembly = place(helper, oakTrim(), "oak_survival_" + part);
            ServerPlayer breaker = player(helper, "oak_survival_breaker", GameType.SURVIVAL, Direction.NORTH);
            check(breaker.gameMode.destroyBlock(assembly.pos(part)), part + ": break refused");
            assertNoTrim(helper, assembly);
            check(dropped(helper, oakTrim().asItem()) == 1, part + ": expected one frame, got " + dropped(helper, oakTrim().asItem()));
            clearDrops(helper);
        }
        helper.succeed();
    }

    /**
     * Appearance is cosmetic (0.5.0): a brick-faced trim is the oak trim's block, so any tool (or none)
     * harvests it, and the one dropped trim keeps its brick look, from whichever quarter.
     */
    @GameTest(template = TEMPLATE)
    public static void masonryLookDropsWithAnyToolKeepingItsLook(GameTestHelper helper) {
        for (Quarter part : Quarter.values()) {
            for (ItemStack tool : new ItemStack[] {new ItemStack(Items.IRON_PICKAXE), ItemStack.EMPTY}) {
                TrimLayout.Assembly assembly = placeBrick(helper, "brick_" + part);
                ServerPlayer breaker = player(helper, "brick_breaker", GameType.SURVIVAL, Direction.NORTH);
                hold(breaker, tool, ItemStack.EMPTY);
                check(breaker.gameMode.destroyBlock(assembly.pos(part)), part + ": break refused");
                assertNoTrim(helper, assembly);
                check(dropped(helper, oakTrim().asItem(), BRICK) == 1,
                        part + " with " + tool + ": expected one brick trim, got " + dropped(helper, oakTrim().asItem(), BRICK));
                clearDrops(helper);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void creativeBreakOfAnyQuarterDropsNothing(GameTestHelper helper) {
        floor(helper);
        for (boolean brick : new boolean[] {false, true}) {
            for (Quarter part : Quarter.values()) {
                TrimLayout.Assembly assembly = brick ? placeBrick(helper, "creative_" + part) : place(helper, oakTrim(), "creative_" + part);
                ServerPlayer breaker = player(helper, "creative_breaker", GameType.CREATIVE, Direction.NORTH);
                check(breaker.gameMode.destroyBlock(assembly.pos(part)), part + ": break refused");
                assertNoTrim(helper, assembly);
                check(dropped(helper, oakTrim().asItem()) == 0, part + ": creative break dropped " + dropped(helper, oakTrim().asItem()));
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void losingSupportUnderEitherLowerQuarterDropsOneFrame(GameTestHelper helper) {
        for (BlockPos support : new BlockPos[] {ANCHOR.below(), ANCHOR.below().east()}) {
            floor(helper);
            TrimLayout.Assembly assembly = place(helper, oakTrim(), "support");
            helper.setBlock(support, Blocks.AIR);
            assertNoTrim(helper, assembly);
            check(dropped(helper, oakTrim().asItem()) == 1, support + ": expected one frame, got " + dropped(helper, oakTrim().asItem()));
            clearDrops(helper);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void explosionYieldsAtMostOneFrame(GameTestHelper helper) {
        floor(helper);
        TrimLayout.Assembly assembly = place(helper, oakTrim(), "explosion");
        BlockPos lower = assembly.pos(Quarter.LOWER_LEFT);
        helper.getLevel().explode(null, lower.getX() + 1.0, lower.getY() + 1.0, lower.getZ() + 0.5, 2F, Level.ExplosionInteraction.TNT);
        assertNoTrim(helper, assembly);
        check(dropped(helper, oakTrim().asItem()) <= 1, "explosion dropped " + dropped(helper, oakTrim().asItem()));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void replacementAndFireCleanupDropNothing(GameTestHelper helper) {
        floor(helper);
        TrimLayout.Assembly assembly = place(helper, oakTrim(), "replace");
        BlockPos upperLeft = rel(helper, assembly.pos(Quarter.UPPER_LEFT));
        helper.setBlock(upperLeft, Blocks.STONE);
        assertNoTrim(helper, assembly);
        check(helper.getBlockState(upperLeft).is(Blocks.STONE), "the replacing block was removed");
        check(dropped(helper, oakTrim().asItem()) == 0, "replacement dropped " + dropped(helper, oakTrim().asItem()));
        helper.setBlock(upperLeft, Blocks.AIR);

        assembly = place(helper, oakTrim(), "fire_burn");
        helper.getLevel().removeBlock(assembly.pos(Quarter.LOWER_RIGHT), false); // what fire burning a block away does
        assertNoTrim(helper, assembly);
        check(dropped(helper, oakTrim().asItem()) == 0, "removeBlock dropped " + dropped(helper, oakTrim().asItem()));

        assembly = place(helper, oakTrim(), "fire_place");
        helper.setBlock(rel(helper, assembly.pos(Quarter.UPPER_RIGHT)), Blocks.FIRE);
        assertNoTrim(helper, assembly);
        check(dropped(helper, oakTrim().asItem()) == 0, "fire replacing a quarter dropped " + dropped(helper, oakTrim().asItem()));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "roundwood_gamerules")
    public static void doTileDropsFalseDropsNothing(GameTestHelper helper) {
        floor(helper);
        GameRules.BooleanValue rule = helper.getLevel().getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS);
        boolean before = rule.get();
        rule.set(false, helper.getLevel().getServer());
        try {
            TrimLayout.Assembly assembly = place(helper, oakTrim(), "tiledrops");
            ServerPlayer breaker = player(helper, "tiledrops_breaker", GameType.SURVIVAL, Direction.NORTH);
            breaker.gameMode.destroyBlock(assembly.pos(Quarter.UPPER_RIGHT));
            assertNoTrim(helper, assembly);
            check(dropped(helper, oakTrim().asItem()) == 0, "doTileDrops=false still dropped");
        } finally {
            rule.set(before, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void pistonsCannotMoveOrBreakTheFrame(GameTestHelper helper) {
        floor(helper);
        TrimLayout.Assembly assembly = place(helper, oakTrim(), "piston");
        // A piston behind the lower-left cell pushing south into it.
        BlockPos piston = rel(helper, assembly.pos(Quarter.LOWER_LEFT)).north();
        helper.setBlock(piston, Blocks.PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.SOUTH));
        helper.setBlock(piston.north(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(4, () -> {
            assertTrim(helper, assembly, oakTrim());
            check(dropped(helper, oakTrim().asItem()) == 0, "piston caused a drop");
            check(!helper.getBlockState(piston).getValue(PistonBaseBlock.EXTENDED), "piston extended into the frame");
            helper.succeed();
        });
    }

    /** A complete door with a camouflaged anchor entity, and the frame fitted in front of it. */
    private static DoorLayout.Assembly doorWithCamouflage(GameTestHelper helper, String name) {
        floor(helper);
        ServerPlayer player = player(helper, name, GameType.SURVIVAL, Direction.NORTH);
        check(placeStyledDoor(helper, player, DOOR_ANCHOR).consumesAction(), "door placement failed");
        DoorLayout.Assembly door = assembly(helper, DOOR_ANCHOR, Direction.NORTH, DoorHingeSide.LEFT);
        anchorEntity(helper, door).setCamouflage(
                CamouflageData.free(Blocks.STONE.defaultBlockState()));
        return door;
    }

    @GameTest(template = TEMPLATE)
    public static void breakingAFittedFrameLeavesTheCamouflagedDoor(GameTestHelper helper) {
        DoorLayout.Assembly door = doorWithCamouflage(helper, "fitted_break");
        TrimLayout.Assembly frame = fittedTo(door);
        for (Quarter part : Quarter.values()) {
            ServerPlayer fitter = player(helper, "fitted_break_fitter", GameType.SURVIVAL, Direction.NORTH);
            check(fitToDoor(helper, fitter, new ItemStack(oakTrim()), door, 0.5, 0.5).consumesAction(), part + ": fitting failed");
            assertTrim(helper, frame, oakTrim());
            clearDrops(helper);
            ServerPlayer breaker = player(helper, "fitted_break_breaker", GameType.SURVIVAL, Direction.NORTH);
            check(breaker.gameMode.destroyBlock(frame.pos(part)), part + ": break refused");
            assertNoTrim(helper, frame);
            check(dropped(helper, oakTrim().asItem()) == 1, part + ": expected one frame, got " + dropped(helper, oakTrim().asItem()));
            assertAssembly(helper, door, oakDoor(), false);
            check(anchorEntity(helper, door).camouflage().isPresent(), part + ": the door lost its camouflage");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void breakingTheDoorLeavesTheFittedFrame(GameTestHelper helper) {
        for (DoorLayout.Part part : DoorLayout.Part.of(io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign.ROUND)) {
            DoorLayout.Assembly door = doorWithCamouflage(helper, "door_break_" + part);
            ServerPlayer fitter = player(helper, "door_break_fitter", GameType.SURVIVAL, Direction.NORTH);
            check(fitToDoor(helper, fitter, new ItemStack(oakTrim()), door, 0.5, 0.5).consumesAction(), part + ": fitting failed");
            TrimLayout.Assembly frame = fittedTo(door);
            assertTrim(helper, frame, oakTrim());
            clearDrops(helper);
            ServerPlayer breaker = player(helper, "door_break_breaker", GameType.SURVIVAL, Direction.NORTH);
            check(breaker.gameMode.destroyBlock(door.pos(part)), part + ": door break refused");
            assertNoDoor(helper, door);
            assertTrim(helper, frame, oakTrim());
            check(dropped(helper, oakTrim().asItem()) == 0, part + ": breaking the door dropped a frame");
            for (BlockPos pos : frame.positions()) {
                helper.setBlock(rel(helper, pos), Blocks.AIR);
            }
            clearDrops(helper);
        }
        helper.succeed();
    }

    /** Places the spruce door through a real item use, its anchor at {@code anchor}, hinge left. */
    private static net.minecraft.world.InteractionResult placeStyledDoor(GameTestHelper helper, ServerPlayer player, BlockPos anchor) {
        return placeDoor(helper, player, styled(oakDoor(), SPRUCE), anchor, DoorHingeSide.LEFT);
    }
}
