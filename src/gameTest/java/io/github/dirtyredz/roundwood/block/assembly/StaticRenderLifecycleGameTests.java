package io.github.dirtyredz.roundwood.block.assembly;

import io.github.dirtyredz.roundwood.block.door.adapter.DoorRenderExtras;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlockEntity;
import io.github.dirtyredz.roundwood.block.arch.assembly.*;
import io.github.dirtyredz.roundwood.block.arch.geometry.*;
import io.github.dirtyredz.roundwood.block.assembly.render.*;
import io.github.dirtyredz.roundwood.block.attachment.*;
import io.github.dirtyredz.roundwood.fixture.AppearanceFixtures;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.*;
import java.util.Set;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;

@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StaticRenderLifecycleGameTests {
    @GameTest(template = "empty")
    public static void invalidAnchorIdentityRetiresOldFootprintAndCanRecover(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(3, 2, 3));
        var block = ModBlocks.archPiece(ArchPieceDesign.TOP_2X3);
        var assembly = new ArchPieceLayout.Assembly(pos, Direction.NORTH, block.design(), KeyholeShoulderProfile.Side.LEFT);
        check(ArchPiecePlacement.placeAt(level, block, assembly, AppearanceFixtures.BIRCH), "identity fixture");
        var entity = (ArchPieceBlockEntity) level.getBlockEntity(pos);
        var state = level.getBlockState(pos);
        int flags = net.minecraft.world.level.block.Block.UPDATE_CLIENTS | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE;
        level.setBlock(pos, assembly.stateFor(ArchPieceCell.UPPER_LEFT, block), flags);
        AssemblyRenderUpdates.refresh(entity, false); // The bounded client membership check uses this same publication path.
        check(!entity.staticRenderPublication().current().available(), "invalid anchor must retire old footprint");
        level.setBlock(pos, state, flags);
        AssemblyRenderUpdates.refresh(entity, false);
        check(entity.staticRenderPublication().current().available(), "restored anchor must recover");
        level.destroyBlock(pos, false);
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void completenessStopsAtTheFirstMissingPart(GameTestHelper helper) {
        var assembly = new Assembly<Integer>() {
            public BlockPos anchor() { return helper.absolutePos(BlockPos.ZERO); }
            public Direction facing() { return Direction.NORTH; }
            public java.util.List<Integer> parts() { return java.util.List.of(0, 1); }
            public BlockPos pos(Integer part) {
                if (part != 0) throw new AssertionError("complete read after its first mismatch");
                return anchor();
            }
            public boolean isPart(net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.block.Block block, Integer part) { return false; }
        };
        check(!assembly.complete(helper.getLevel(), Blocks.STONE), "missing member must be incomplete");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void doorPayloadClearingAndRollbackPublishFinalDataAcrossSections(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(BlockPos.ZERO);
        var pos = origin.offset(15 - (origin.getX() & 15), 15 - (origin.getY() & 15), 3);
        var block = ModBlocks.tallDoubleArchedDoor();
        var door = new io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout.Assembly(pos, Direction.NORTH,
                net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT, block.design(),
                io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth.BACK);
        for (var cell : door.positions()) level.setBlockAndUpdate(cell, Blocks.AIR.defaultBlockState());
        for (int x = 0; x < 3; x++) level.setBlockAndUpdate(pos.east(x).below(), Blocks.STONE.defaultBlockState());
        check(io.github.dirtyredz.roundwood.block.door.assembly.DoorPlacement.placeAt(level, block, door, false, AppearanceFixtures.BIRCH), "crossing door placement");
        var entity = (io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity) level.getBlockEntity(pos);
        var frame = new io.github.dirtyredz.roundwood.block.door.frame.DoorArchAttachment(ArchPieceDesign.TOP_2X3,
                ArchPieceThickness.FULL, ArchPieceDepth.FRONT, AppearanceFixtures.BRICKS, FaceTrims.both(AppearanceFixtures.SPRUCE));
        check(entity.setArch(frame), "fit payload");
        var fitted = entity.staticRenderPublication().current();
        check(fitted.available() && DoorRenderExtras.of(fitted).frame().orElseThrow().equals(frame), "published fitted payload");
        check(fitted.members().size() == 12 && fitted.members().keySet().stream().map(SectionPos::of).distinct().count() == 4, "all twelve cells across sections");
        var states = new java.util.LinkedHashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
        for (var cell : door.positions()) states.put(cell, level.getBlockState(cell));
        boolean installed = io.github.dirtyredz.roundwood.block.assembly.placement.AssemblyWriteTransaction.write(level, states,
                () -> entity.setArch(null), () -> false);
        check(!installed && DoorRenderExtras.of(entity.staticRenderPublication().current()).frame().orElseThrow().equals(frame), "rollback publishes restored unchanged-state NBT");
        check(entity.setArch(null) && DoorRenderExtras.of(entity.staticRenderPublication().current()).frame().isEmpty(), "clear publishes empty frame");
        check(DoorRenderExtras.of(fitted).frame().isPresent(), "retained snapshot remains immutable");
        level.destroyBlock(pos.above(3), false);
        check(!entity.staticRenderPublication().current().available(), "break retires door snapshot");
        for (int x = 0; x < 3; x++) level.setBlockAndUpdate(pos.east(x).below(), Blocks.AIR.defaultBlockState());
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void sectionCrossingEditsClearAndRemovalPublishCompleteFootprints(GameTestHelper helper) {
        var level = helper.getLevel();
        var origin = helper.absolutePos(BlockPos.ZERO);
        for (var facing : java.util.List.of(Direction.NORTH, Direction.EAST)) {
        var pos = origin.offset(facing == Direction.NORTH ? 15 - (origin.getX() & 15) : 3,
                15 - (origin.getY() & 15), facing == Direction.EAST ? 15 - (origin.getZ() & 15) : 3);
        var block = ModBlocks.archPiece(ArchPieceDesign.TOP_2X3);
        var assembly = new ArchPieceLayout.Assembly(pos, facing, block.design(), KeyholeShoulderProfile.Side.LEFT);
        for (var cell : assembly.positions()) level.setBlockAndUpdate(cell, Blocks.AIR.defaultBlockState());
        check(ArchPiecePlacement.placeAt(level, block, assembly, AppearanceFixtures.BIRCH), "place crossing arch");
        var entity = (ArchPieceBlockEntity) level.getBlockEntity(pos);
        var first = entity.staticRenderPublication().current();
        check(first != null && first.available(), "complete snapshot after commit");
        check(first.members().keySet().equals(Set.copyOf(assembly.positions())), "every owned cell published");
        check(first.members().keySet().stream().map(SectionPos::of).distinct().count() == 4, "fixture crosses x and y sections");
        entity.setTrims(FaceTrims.both(AppearanceFixtures.SPRUCE));
        var edited = entity.staticRenderPublication().current();
        check(edited.revision() > first.revision() && edited.trims().any() && !first.trims().any(), "immutable retained revision");
        entity.handleUpdateTag(new net.minecraft.nbt.CompoundTag());
        check(!entity.staticRenderPublication().current().trims().any(), "tagless clearing publishes");
        level.destroyBlock(assembly.pos(ArchPieceCell.UPPER_RIGHT), false);
        check(!entity.staticRenderPublication().current().available(), "removal retires published data");
        check(entity.staticRenderPublication().footprint().equals(Set.copyOf(assembly.positions())), "removal retains invalidation footprint");
        }
        helper.succeed();
    }
}
