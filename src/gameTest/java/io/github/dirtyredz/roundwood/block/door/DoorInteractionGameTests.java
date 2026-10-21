package io.github.dirtyredz.roundwood.block.door;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assertAssembly;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.click;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.oakDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.rel;

/** Opening by hand, by explicit request and by redstone always moves the whole assembly. */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorInteractionGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);

    private DoorInteractionGameTests() {
    }

    /** Places an oak assembly facing north at {@link #ANCHOR}. */
    static DoorLayout.Assembly place(GameTestHelper helper, String name) {
        return place(helper, name, DoorHingeSide.LEFT);
    }

    /** Places an oak assembly facing north at {@link #ANCHOR} with the given hinge. */
    static DoorLayout.Assembly place(GameTestHelper helper, String name, DoorHingeSide hinge) {
        floor(helper);
        ServerPlayer player = player(helper, name, GameType.SURVIVAL, Direction.NORTH);
        check(placeDoor(helper, player, oakDoor().asItem(), ANCHOR, hinge).consumesAction(), "placement failed");
        return assembly(helper, ANCHOR, Direction.NORTH, hinge);
    }

    @GameTest(template = TEMPLATE)
    public static void clickingAnyPartTogglesAllFour(GameTestHelper helper) {
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            DoorLayout.Assembly assembly = place(helper, "click_" + hinge, hinge);
            ServerPlayer player = player(helper, "clicker_" + hinge, GameType.SURVIVAL, Direction.NORTH);
            boolean open = false;
            for (DoorLayout.Part part : DoorLayout.Part.values()) {
                InteractionResult result = click(helper, player, rel(helper, assembly.pos(part)));
                check(result.consumesAction(), hinge + ": click on " + part + " was not handled: " + result);
                open = !open;
                assertAssembly(helper, assembly, oakDoor(), open);
            }
            // Crouching with an empty hand still opens a door, exactly once.
            player.setShiftKeyDown(true);
            click(helper, player, rel(helper, assembly.pos(DoorLayout.Part.UPPER_RIGHT)));
            player.setShiftKeyDown(false);
            assertAssembly(helper, assembly, oakDoor(), !open);
            helper.setBlock(ANCHOR, Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void explicitSetOpenIsIdempotent(GameTestHelper helper) {
        DoorLayout.Assembly assembly = place(helper, "setopen");
        BlockPos part = assembly.pos(DoorLayout.Part.LOWER_RIGHT);
        for (int i = 0; i < 2; i++) {
            BlockState state = helper.getLevel().getBlockState(part);
            oakDoor().setOpen(null, helper.getLevel(), state, part, true);
            assertAssembly(helper, assembly, oakDoor(), true);
        }
        for (int i = 0; i < 2; i++) {
            BlockState state = helper.getLevel().getBlockState(assembly.pos(DoorLayout.Part.UPPER_LEFT));
            oakDoor().setOpen(null, helper.getLevel(), state, assembly.pos(DoorLayout.Part.UPPER_LEFT), false);
            assertAssembly(helper, assembly, oakDoor(), false);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void redstoneOnAnySinglePartDrivesTheAssembly(GameTestHelper helper) {
        DoorLayout.Assembly assembly = place(helper, "redstone");
        for (DoorLayout.Part part : DoorLayout.Part.values()) {
            BlockPos behind = rel(helper, assembly.pos(part)).north(); // the free side of the cell
            helper.setBlock(behind, Blocks.REDSTONE_BLOCK);
            assertAssembly(helper, assembly, oakDoor(), true);
            assertPowered(helper, assembly, true);
            helper.setBlock(behind, Blocks.AIR);
            assertAssembly(helper, assembly, oakDoor(), false);
            assertPowered(helper, assembly, false);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void multipleSourcesCloseOnlyWhenTheLastIsRemoved(GameTestHelper helper) {
        DoorLayout.Assembly assembly = place(helper, "sources");
        BlockPos a = rel(helper, assembly.pos(DoorLayout.Part.LOWER_LEFT)).north();
        BlockPos b = rel(helper, assembly.pos(DoorLayout.Part.UPPER_RIGHT)).north();
        helper.setBlock(a, Blocks.REDSTONE_BLOCK);
        helper.setBlock(b, Blocks.REDSTONE_BLOCK);
        assertAssembly(helper, assembly, oakDoor(), true);
        helper.setBlock(a, Blocks.AIR);
        assertAssembly(helper, assembly, oakDoor(), true);
        assertPowered(helper, assembly, true);
        helper.setBlock(b, Blocks.AIR);
        assertAssembly(helper, assembly, oakDoor(), false);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void manualControlHoldsBetweenPowerTransitions(GameTestHelper helper) {
        DoorLayout.Assembly assembly = place(helper, "manual");
        ServerPlayer player = player(helper, "manual_clicker", GameType.SURVIVAL, Direction.NORTH);
        BlockPos source = rel(helper, assembly.pos(DoorLayout.Part.LOWER_LEFT)).north();
        helper.setBlock(source, Blocks.REDSTONE_BLOCK);
        assertAssembly(helper, assembly, oakDoor(), true);
        click(helper, player, rel(helper, assembly.pos(DoorLayout.Part.LOWER_RIGHT)));
        assertAssembly(helper, assembly, oakDoor(), false);
        // An unrelated neighbour update must not reassert the power.
        helper.setBlock(rel(helper, assembly.pos(DoorLayout.Part.UPPER_RIGHT)).north(), Blocks.STONE);
        assertAssembly(helper, assembly, oakDoor(), false);
        assertPowered(helper, assembly, true);
        // The falling edge closes (already closed), the next rising edge opens again.
        helper.setBlock(source, Blocks.AIR);
        assertAssembly(helper, assembly, oakDoor(), false);
        helper.setBlock(source, Blocks.REDSTONE_BLOCK);
        assertAssembly(helper, assembly, oakDoor(), true);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void adjacentAssembliesStayIndependent(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper, "adjacent", GameType.SURVIVAL, Direction.NORTH);
        BlockPos leftAnchor = new BlockPos(1, 1, 3);
        BlockPos rightAnchor = new BlockPos(3, 1, 3); // touching: left assembly's right column is x = 2
        check(placeDoor(helper, player, oakDoor().asItem(), leftAnchor).consumesAction(), "left placement failed");
        check(placeDoor(helper, player, oakDoor().asItem(), rightAnchor).consumesAction(), "right placement failed");
        DoorLayout.Assembly left = assembly(helper, leftAnchor, Direction.NORTH);
        // A round door just outside its left edge makes the new one hinge on the right (vanilla pairing).
        DoorLayout.Assembly right = assembly(helper, rightAnchor, Direction.NORTH, DoorHingeSide.RIGHT);
        click(helper, player, rel(helper, left.pos(DoorLayout.Part.LOWER_RIGHT)));
        assertAssembly(helper, left, oakDoor(), true);
        assertAssembly(helper, right, oakDoor(), false);
        click(helper, player, rel(helper, right.pos(DoorLayout.Part.UPPER_LEFT)));
        assertAssembly(helper, left, oakDoor(), true);
        assertAssembly(helper, right, oakDoor(), true);
        helper.succeed();
    }

    private static void assertPowered(GameTestHelper helper, DoorLayout.Assembly assembly, boolean powered) {
        for (BlockPos pos : assembly.positions()) {
            check(helper.getLevel().getBlockState(pos).getValue(DoorBlock.POWERED) == powered,
                    "powered should be " + powered + " at " + pos);
        }
    }
}
