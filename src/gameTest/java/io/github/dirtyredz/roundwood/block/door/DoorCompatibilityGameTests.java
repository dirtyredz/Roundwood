package io.github.dirtyredz.roundwood.block.door;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Set;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assertAssembly;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.click;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.oakDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;

/** Other mods and commands that write OPEN on one part directly: the rest of the assembly must follow at once. */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorCompatibilityGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    private static final List<Integer> FLAG_SETS = List.of(Block.UPDATE_CLIENTS, Block.UPDATE_ALL);

    private DoorCompatibilityGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void externalOpenWriteOnAnySinglePartMovesAllFour(GameTestHelper helper) {
        floor(helper);
        for (DoorLayout.Part part : DoorLayout.Part.values()) {
            for (int flags : FLAG_SETS) {
                String name = part + "/flags=" + flags;
                ServerPlayer player = player(helper, "compat", GameType.SURVIVAL, Direction.NORTH);
                check(placeDoor(helper, player, oakDoor().asItem(), ANCHOR).consumesAction(), name + ": placement failed");
                DoorLayout.Assembly assembly = assembly(helper, ANCHOR, Direction.NORTH);
                write(helper, assembly.pos(part), true, flags);
                assertAssembly(helper, assembly, oakDoor(), true);
                assertUnpowered(helper, assembly, name + " after opening");
                write(helper, assembly.pos(part), false, flags);
                assertAssembly(helper, assembly, oakDoor(), false);
                assertUnpowered(helper, assembly, name + " after closing");
                helper.setBlock(ANCHOR, Blocks.AIR);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void extraInjectedPropertiesSurviveAToggle(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper, "compat_extra", GameType.SURVIVAL, Direction.NORTH);
        check(placeDoor(helper, player, oakDoor().asItem(), ANCHOR).consumesAction(), "placement failed");
        BlockState before = helper.getBlockState(ANCHOR);
        Set<String> known = Set.of("facing", "half", "hinge", "open", "powered", "column");
        for (Property<?> property : before.getProperties()) {
            if (known.contains(property.getName())) {
                continue;
            }
            Object value = before.getValue(property);
            check(click(helper, player, ANCHOR).consumesAction(), "click failed");
            check(value.equals(helper.getBlockState(ANCHOR).getValue(property)),
                    "injected property " + property.getName() + " changed on open: " + helper.getBlockState(ANCHOR));
            check(click(helper, player, ANCHOR).consumesAction(), "second click failed");
            check(value.equals(helper.getBlockState(ANCHOR).getValue(property)),
                    "injected property " + property.getName() + " changed on close: " + helper.getBlockState(ANCHOR));
        }
        helper.succeed();
    }

    /** What an outside mod does: rewrite one part's blockstate with a different OPEN value. */
    private static void write(GameTestHelper helper, BlockPos pos, boolean open, int flags) {
        BlockState state = helper.getLevel().getBlockState(pos);
        helper.getLevel().setBlock(pos, state.setValue(DoorBlock.OPEN, open), flags);
    }

    private static void assertUnpowered(GameTestHelper helper, DoorLayout.Assembly assembly, String when) {
        for (BlockPos pos : assembly.positions()) {
            check(!helper.getLevel().getBlockState(pos).getValue(DoorBlock.POWERED), "POWERED changed " + when + " at " + pos);
        }
    }
}
