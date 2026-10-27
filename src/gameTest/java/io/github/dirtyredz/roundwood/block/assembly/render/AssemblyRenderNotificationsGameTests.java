package io.github.dirtyredz.roundwood.block.assembly.render;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.*;
import io.github.dirtyredz.roundwood.block.door.frame.DoorArchAttachment;
import io.github.dirtyredz.roundwood.block.arch.geometry.*;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth;
import io.github.dirtyredz.roundwood.fixture.AppearanceFixtures;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraftforge.gametest.*;
import java.util.*;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;

@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AssemblyRenderNotificationsGameTests {
    @GameTest(template = "empty")
    public static void editFitClearAndBreakNotifyEveryCellAcrossSectionBorders(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(BlockPos.ZERO);
        var pos = origin.offset(15 - (origin.getX() & 15), 15 - (origin.getY() & 15), 3);
        var block = ModBlocks.tallDoubleArchedDoor();
        var door = new DoorLayout.Assembly(pos, Direction.NORTH, DoorHingeSide.LEFT, block.design(), DoorDepth.BACK);
        for (var cell : door.positions()) level.setBlockAndUpdate(cell, Blocks.AIR.defaultBlockState());
        for (int x = 0; x < 3; x++) level.setBlockAndUpdate(pos.east(x).below(), Blocks.STONE.defaultBlockState());
        check(DoorPlacement.placeAt(level, block, door, false, AppearanceFixtures.BIRCH), "notification fixture");
        var entity = (RoundDoorBlockEntity) level.getBlockEntity(pos);
        var expected = Set.copyOf(door.positions());
        check(expected.stream().map(SectionPos::of).distinct().count() == 4, "fixture crosses horizontal and vertical section borders");
        var notified = new HashSet<BlockPos>();
        var previous = AssemblyRenderUpdates.notificationProbe;
        AssemblyRenderUpdates.notificationProbe = (source, cell) -> {
            if (source == entity) {
                check(source.staticRenderPublication().current() != null, "notification follows publication");
                notified.add(cell);
            }
        };
        try {
            entity.setTrims(FaceTrims.both(AppearanceFixtures.SPRUCE));
            check(notified.equals(expected), "edit sends every owned cell update"); notified.clear();
            var frame = new DoorArchAttachment(ArchPieceDesign.TOP_2X3, ArchPieceThickness.FULL,
                    ArchPieceDepth.FRONT, AppearanceFixtures.BRICKS, FaceTrims.NONE);
            check(entity.setArch(frame), "fit notification payload");
            check(notified.equals(expected), "fit sends every owned cell update"); notified.clear();
            check(entity.setArch(null), "clear notification payload");
            check(notified.equals(expected), "clear sends every owned cell update"); notified.clear();
            level.destroyBlock(pos.above(3), false);
            check(notified.equals(expected), "break sends every old owned cell update");
        } finally { AssemblyRenderUpdates.notificationProbe = previous; }
        for (int x = 0; x < 3; x++) level.setBlockAndUpdate(pos.east(x).below(), Blocks.AIR.defaultBlockState());
        helper.succeed();
    }
}
