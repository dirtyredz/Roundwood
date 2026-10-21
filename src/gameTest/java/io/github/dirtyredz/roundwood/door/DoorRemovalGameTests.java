package io.github.dirtyredz.roundwood.door;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.door.assembly.DoorLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assertAssembly;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assertNoDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.blockEntities;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.clearDrops;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.dropped;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.oakDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.rel;

/** Breaking any part removes the assembly and yields exactly the loot of one door. */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorRemovalGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);

    private DoorRemovalGameTests() {
    }

    private static DoorLayout.Assembly place(GameTestHelper helper, String name) {
        return place(helper, name, DoorHingeSide.LEFT, false);
    }

    /** Places a door with the given hinge, optionally opened, and clears the drops. */
    private static DoorLayout.Assembly place(GameTestHelper helper, String name, DoorHingeSide hinge, boolean open) {
        ServerPlayer player = player(helper, name + "_placer", GameType.SURVIVAL, Direction.NORTH);
        check(placeDoor(helper, player, oakDoor().asItem(), ANCHOR, hinge).consumesAction(), "placement failed");
        if (open) {
            oakDoor().setOpen(null, helper.getLevel(), helper.getLevel().getBlockState(helper.absolutePos(ANCHOR)),
                    helper.absolutePos(ANCHOR), true);
        }
        clearDrops(helper);
        return assembly(helper, ANCHOR, Direction.NORTH, hinge);
    }

    private static Item item() {
        return oakDoor().asItem();
    }

    @GameTest(template = TEMPLATE)
    public static void survivalBreakOfAnyPartDropsOneDoor(GameTestHelper helper) {
        floor(helper);
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            for (boolean open : new boolean[] {false, true}) {
                for (DoorLayout.Part part : DoorLayout.Part.values()) {
                    String name = hinge + "/open=" + open + "/" + part;
                    DoorLayout.Assembly assembly = place(helper, "survival", hinge, open);
                    ServerPlayer player = player(helper, "survival_breaker", GameType.SURVIVAL, Direction.NORTH);
                    check(player.gameMode.destroyBlock(assembly.pos(part)), name + ": break refused");
                    assertNoDoor(helper, assembly);
                    check(blockEntities(helper, assembly) == 0, name + ": block entity left behind");
                    check(dropped(helper, item()) == 1, name + ": expected one door, got " + dropped(helper, item()));
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void creativeBreakOfAnyPartDropsNothing(GameTestHelper helper) {
        floor(helper);
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            for (boolean open : new boolean[] {false, true}) {
                for (DoorLayout.Part part : DoorLayout.Part.values()) {
                    String name = hinge + "/open=" + open + "/" + part;
                    DoorLayout.Assembly assembly = place(helper, "creative", hinge, open);
                    ServerPlayer player = player(helper, "creative_breaker", GameType.CREATIVE, Direction.NORTH);
                    check(player.gameMode.destroyBlock(assembly.pos(part)), name + ": break refused");
                    assertNoDoor(helper, assembly);
                    check(blockEntities(helper, assembly) == 0, name + ": block entity left behind");
                    check(dropped(helper, item()) == 0, name + ": creative break dropped " + dropped(helper, item()));
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void losingSupportUnderEitherColumnDropsOneDoor(GameTestHelper helper) {
        for (BlockPos support : new BlockPos[] {ANCHOR.below(), ANCHOR.below().east()}) {
            floor(helper);
            DoorLayout.Assembly assembly = place(helper, "support");
            helper.setBlock(support, Blocks.AIR);
            assertNoDoor(helper, assembly);
            check(dropped(helper, item()) == 1, support + ": expected one door, got " + dropped(helper, item()));
            clearDrops(helper);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void explosionYieldsAtMostOneDoor(GameTestHelper helper) {
        floor(helper);
        DoorLayout.Assembly assembly = place(helper, "explosion");
        BlockPos centre = assembly.pos(DoorLayout.Part.UPPER_RIGHT);
        helper.getLevel().explode(null, centre.getX(), centre.getY(), centre.getZ() - 0.5, 3.0F, Level.ExplosionInteraction.TNT);
        assertNoDoor(helper, assembly);
        check(dropped(helper, item()) <= 1, "explosion dropped " + dropped(helper, item()));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void replacementAndFireCleanupDropNothing(GameTestHelper helper) {
        floor(helper);
        DoorLayout.Assembly assembly = place(helper, "replace");
        helper.setBlock(rel(helper, assembly.pos(DoorLayout.Part.UPPER_LEFT)), Blocks.STONE);
        assertNoDoor(helper, assembly);
        check(helper.getBlockState(rel(helper, assembly.pos(DoorLayout.Part.UPPER_LEFT))).is(Blocks.STONE),
                "the replacing block was removed");
        check(dropped(helper, item()) == 0, "replacement dropped " + dropped(helper, item()));
        helper.setBlock(rel(helper, assembly.pos(DoorLayout.Part.UPPER_LEFT)), Blocks.AIR);
        // Fire burning a block away uses removeBlock, which never drops.
        assembly = place(helper, "replace_fire");
        helper.getLevel().removeBlock(assembly.pos(DoorLayout.Part.LOWER_RIGHT), false);
        assertNoDoor(helper, assembly);
        check(dropped(helper, item()) == 0, "removeBlock dropped " + dropped(helper, item()));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, batch = "roundwood_gamerules")
    public static void doTileDropsFalseDropsNothing(GameTestHelper helper) {
        floor(helper);
        GameRules.BooleanValue rule = helper.getLevel().getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS);
        boolean before = rule.get();
        rule.set(false, helper.getLevel().getServer());
        try {
            DoorLayout.Assembly assembly = place(helper, "tiledrops");
            ServerPlayer player = player(helper, "tiledrops_breaker", GameType.SURVIVAL, Direction.NORTH);
            player.gameMode.destroyBlock(assembly.pos(DoorLayout.Part.UPPER_RIGHT));
            assertNoDoor(helper, assembly);
            check(dropped(helper, item()) == 0, "doTileDrops=false still dropped");
        } finally {
            rule.set(before, helper.getLevel().getServer());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void pistonsCannotMoveOrBreakTheAssembly(GameTestHelper helper) {
        floor(helper);
        DoorLayout.Assembly assembly = place(helper, "piston");
        // A piston behind the lower-left cell pushing south into it.
        BlockPos piston = rel(helper, assembly.pos(DoorLayout.Part.LOWER_LEFT)).north();
        helper.setBlock(piston, Blocks.PISTON.defaultBlockState().setValue(DirectionalBlock.FACING, Direction.SOUTH));
        helper.setBlock(piston.north(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(4, () -> {
            assertAssembly(helper, assembly, oakDoor(), false);
            check(dropped(helper, item()) == 0, "piston caused a drop");
            check(!helper.getBlockState(piston).getValue(net.minecraft.world.level.block.piston.PistonBaseBlock.EXTENDED),
                    "piston extended into the door");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void incompleteStructureIsCleanedUpWithoutCreatingParts(GameTestHelper helper) {
        floor(helper);
        // A lone left column, as a command or a structure tool could leave it.
        DoorLayout.Assembly assembly = assembly(helper, ANCHOR, Direction.NORTH);
        helper.setBlock(ANCHOR, assembly.stateFor(DoorLayout.Part.LOWER_LEFT, oakDoor(), false, false));
        helper.setBlock(ANCHOR.above(), assembly.stateFor(DoorLayout.Part.UPPER_LEFT, oakDoor(), false, false));
        clearDrops(helper);
        helper.runAfterDelay(3, () -> {
            assertNoDoor(helper, assembly);
            check(helper.getBlockState(ANCHOR).isAir() && helper.getBlockState(ANCHOR.above()).isAir(), "the lone column was not removed");
            check(helper.getBlockState(ANCHOR.east()).isAir() && helper.getBlockState(ANCHOR.east().above()).isAir(),
                    "a missing part was created at the right column");
            check(dropped(helper, item()) <= 1, "incomplete cleanup dropped " + dropped(helper, item()));
            helper.succeed();
        });
    }
}
