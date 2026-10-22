package io.github.dirtyredz.roundwood.migration;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimPlacement;
import io.github.dirtyredz.roundwood.migration.conversion.DoorTrimConversion;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.BRICKS;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.migration.LegacyMigrationFixtures.loadAndTick;

/**
 * Negative guard (0.11.0): the 0.7.0 fitted-frame conversion belongs to the round door only. A
 * circular legacy frame standing exactly where a fitted frame would stand in front of an arched
 * door is never converted onto it, through the chunk-load path or a direct call: the frame stays
 * whole and the arched door gets no trim.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArchedDoorMigrationGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);

    private ArchedDoorMigrationGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void aLegacyFrameNeverConvertsOntoAnArchedDoor(GameTestHelper helper) {
        floor(helper);
        ServerPlayer placer = player(helper, "arched_migration", GameType.SURVIVAL, Direction.NORTH);
        check(placeDoor(helper, placer, new ItemStack(ModBlocks.archedDoor().asItem()), ANCHOR, DoorHingeSide.LEFT).consumesAction(),
                "arched door");
        DoorLayout.Assembly door = assembly(helper, ANCHOR, Direction.NORTH, DoorHingeSide.LEFT, DoorDesign.ARCHED);
        TrimLayout.Assembly frame = new TrimLayout.Assembly(door.anchor().relative(door.facing().getOpposite()), door.facing());
        check(TrimPlacement.placeAt(helper.getLevel(), ModBlocks.roundTrim(), frame, BRICKS), "frame");
        check(!DoorTrimConversion.candidate(helper.getLevel().getBlockState(door.anchor())), "the arched door is a conversion candidate");
        check(DoorTrimConversion.convert(helper.getLevel(), frame.anchor()) == DoorTrimConversion.Result.RETAINED,
                "the frame's conversion was not refused");
        List<BlockPos> area = new ArrayList<>();
        BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 0, 0)), helper.absolutePos(new BlockPos(8, 4, 8)))
                .forEach(pos -> area.add(pos.immutable()));
        loadAndTick(helper, area);
        helper.runAfterDelay(2, () -> {
            check(frame.complete(helper.getLevel(), ModBlocks.roundTrim()), "the legacy frame was taken");
            check(door.complete(helper.getLevel(), ModBlocks.archedDoor()), "the arched door was changed");
            check(!((RoundDoorBlockEntity) helper.getLevel().getBlockEntity(door.anchor())).trims().any(),
                    "the arched door got a trim from a legacy frame");
            helper.succeed();
        });
    }
}
