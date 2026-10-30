package io.github.dirtyredz.roundwood.migration;


import io.github.dirtyredz.roundwood.fixture.TestRegistries;
import io.github.dirtyredz.roundwood.migration.retired.CrossWindowCompatibility;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.migration.conversion.LegacyWindowConversion;
import io.github.dirtyredz.roundwood.migration.conversion.LegacyCells;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.TEMPLATE;

/** Reproductions of the old decoded-field transfer losing non-perimeter stores. */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WindowCrossMigrationGameTests {
    @GameTest(template = TEMPLATE)
    public static void everyRetiredIdKeepsFullContentsAndConvertsOnlyOnce(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        for (String id : CrossWindowCompatibility.IDS) {
            helper.getLevel().setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
            var block = (io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock) net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("roundwood", id));
            var state = block.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED, true)
                    .setValue(io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock.FACE_REVERSED, true);
            helper.getLevel().setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            var source = (RoundWindowBlockEntity) helper.getLevel().getBlockEntity(pos);
            var look = io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.SPRUCE;
            source.setAppearance(look);
            source.setGlass(java.util.Optional.of(net.minecraft.world.item.DyeColor.CYAN));
            source.setTrims(FaceTrims.both(Appearance.DEFAULT));
            source.setTrims(TrimSlot.SILL, FaceTrims.both(look));
            var before = source.saveWithFullMetadata(TestRegistries.get());
            check(io.github.dirtyredz.roundwood.migration.conversion.WindowCrossConversion.convert(helper.getLevel(), pos)
                    == LegacyCells.Result.CONVERTED, id + " not converted");
            var after = (RoundWindowBlockEntity) helper.getLevel().getBlockEntity(pos);
            check(after.appearance().equals(look) && after.glass().equals(source.glass()) && after.trims().equals(source.trims())
                    && after.trims(TrimSlot.SILL).equals(source.trims(TrimSlot.SILL)), id + " lost contents");
            check(after.trims(TrimSlot.CROSS_BEAMS).front().equals(java.util.Optional.of(look)) && after.trims(TrimSlot.CROSS_BEAMS).count() == 1,
                    id + " wrong beam appearance/count");
            check(block.pose(state).equals(((io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock) after.getBlockState().getBlock()).pose(after.getBlockState()))
                    && after.getBlockState().getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED)
                    && after.getBlockState().getValue(io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock.FACE_REVERSED), id + " pose/water/identity");
            check(io.github.dirtyredz.roundwood.migration.conversion.WindowCrossConversion.convert(helper.getLevel(), pos)
                    == LegacyCells.Result.NOT_LEGACY, id + " converted twice");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void unknownBeamsAndFailedWritesLeaveTheSourceIntact(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        var state = CrossWindowCompatibility.blocks().get(0).defaultBlockState();
        helper.getLevel().setBlock(pos, state, Block.UPDATE_CLIENTS);
        var source = (RoundWindowBlockEntity) helper.getLevel().getBlockEntity(pos);
        var disk = source.saveWithFullMetadata(TestRegistries.get());
        var unknown = new CompoundTag();
        unknown.putInt("Version", 99);
        unknown.putString("Keep", "all contents");
        disk.put("WindowCrossBeams", unknown);
        source.loadWithComponents(disk, TestRegistries.get());
        var before = source.saveWithFullMetadata(TestRegistries.get());
        check(io.github.dirtyredz.roundwood.migration.conversion.WindowCrossConversion.convert(helper.getLevel(), pos)
                == LegacyCells.Result.DEFERRED, "converted unknown beam data");
        check(helper.getLevel().getBlockState(pos) == state && source.saveWithFullMetadata(TestRegistries.get()).equals(before), "refusal changed source");
        check(!io.github.dirtyredz.roundwood.migration.conversion.WindowCellConversion.install(helper.getLevel(), pos, state, before),
                "unchecked same-state write reported success");
        check(!io.github.dirtyredz.roundwood.migration.conversion.WindowCellConversion.install(helper.getLevel(), pos,
                net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), before), "missing destination entity reported success");
        check(helper.getLevel().getBlockState(pos) == state
                && helper.getLevel().getBlockEntity(pos).saveWithFullMetadata(TestRegistries.get()).equals(before), "rollback lost the source");
        var drops = Block.getDrops(state, helper.getLevel(), pos, helper.getLevel().getBlockEntity(pos));
        check(drops.size() == 1 && drops.get(0).is(io.github.dirtyredz.roundwood.registry.ModItems.window(
                ((io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock) state.getBlock()).design())), "pending block refunded attachments separately");
        // 1.21 item block-entity data is the entity's own payload plus its id, without coordinates.
        var custody = before.copy();
        custody.remove("x"); custody.remove("y"); custody.remove("z");
        check(io.github.dirtyredz.roundwood.appearance.item.StackPayloads.blockEntityData(drops.get(0)).equals(custody), "pending harvest lost custody");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void historicalDefaultsAndExistingBeamsKeepUnknownSills(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        var state = CrossWindowCompatibility.blocks().get(0).defaultBlockState()
                .setValue(io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock.TRIMMED, true);
        helper.getLevel().setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        var source = (RoundWindowBlockEntity) helper.getLevel().getBlockEntity(pos);
        var disk = new CompoundTag(); // 0.7 oak: true summary, omitted perimeter payload and frame look.
        var beam = FaceTrims.NONE.with(AttachmentFace.FRONT, io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.SPRUCE);
        io.github.dirtyredz.roundwood.block.window.trim.WindowBeamNbt.write(disk, beam);
        var future = new CompoundTag();
        future.putInt("Version", 99);
        future.putString("UnknownSill", "keep");
        disk.put("WindowSillTrim", future);
        source.loadWithComponents(disk, TestRegistries.get());
        io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.clearDrops(helper);
        check(io.github.dirtyredz.roundwood.migration.conversion.WindowCrossConversion.convert(helper.getLevel(), pos)
                == LegacyCells.Result.CONVERTED, "historical defaults conversion failed");
        var after = (RoundWindowBlockEntity) helper.getLevel().getBlockEntity(pos);
        check(after.appearance().equals(Appearance.DEFAULT) && after.trims().equals(FaceTrims.both(Appearance.DEFAULT)), "0.7 omitted oak default lost");
        check(after.trims(TrimSlot.CROSS_BEAMS).equals(beam) && after.saveWithFullMetadata(TestRegistries.get()).getCompound("WindowSillTrim").equals(future), "existing beam/unknown sill lost");
        check(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2)).isEmpty(), "migration spawned drops");
        var far = pos.offset(100000, 0, 100000);
        check(!helper.getLevel().isLoaded(far), "unloaded candidate fixture already loaded");
        check(io.github.dirtyredz.roundwood.migration.conversion.WindowCrossConversion.convert(helper.getLevel(), far)
                == LegacyCells.Result.DEFERRED && !helper.getLevel().isLoaded(far), "conversion force-loaded a chunk");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void failedTransactionsKeepTheOuterCaptureAndRestoreOnException(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        var state = CrossWindowCompatibility.blocks().get(0).defaultBlockState();
        level.setBlock(pos, state, Block.UPDATE_CLIENTS);
        var before = level.getBlockEntity(pos).saveWithFullMetadata(TestRegistries.get());
        var outer = net.neoforged.neoforge.common.util.BlockSnapshot.create(level.dimension(), level, pos.east());
        int mark = level.capturedBlockSnapshots.size();
        boolean previousCapture = level.captureBlockSnapshots;
        level.captureBlockSnapshots = true;
        level.capturedBlockSnapshots.add(outer);
        try {
            check(!io.github.dirtyredz.roundwood.migration.conversion.WindowCellConversion.install(level, pos,
                    net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), before), "invalid destination accepted");
            check(level.captureBlockSnapshots && level.capturedBlockSnapshots.size() == mark + 1
                    && level.capturedBlockSnapshots.get(mark) == outer, "rollback damaged outer capture");
        } finally {
            level.captureBlockSnapshots = previousCapture;
            level.capturedBlockSnapshots.subList(mark, level.capturedBlockSnapshots.size()).clear();
        }
        boolean threw = false;
        try {
            io.github.dirtyredz.roundwood.migration.conversion.WindowCellConversion.install(level, pos,
                    io.github.dirtyredz.roundwood.registry.ModBlocks.roundWindow().defaultBlockState(), null);
        } catch (NullPointerException expected) {
            threw = true;
        }
        check(threw && level.getBlockState(pos) == state && level.getBlockEntity(pos).saveWithFullMetadata(TestRegistries.get()).equals(before)
                && !level.restoringBlockSnapshots && level.captureBlockSnapshots == previousCapture, "exception did not restore exact source and flags");
        boolean[] accepted = {true};
        nested(level, 4, () -> accepted[0] = io.github.dirtyredz.roundwood.migration.conversion.WindowCellConversion.install(level, pos,
                io.github.dirtyredz.roundwood.registry.ModBlocks.roundWindow().defaultBlockState(), before));
        check(!accepted[0] && level.getBlockState(pos) == state && level.getBlockEntity(pos).saveWithFullMetadata(TestRegistries.get()).equals(before), "refused batch changed source");
        helper.succeed();
    }

    private static void nested(net.minecraft.server.level.ServerLevel level, int depth, Runnable mutation) {
        if (depth == 0) mutation.run();
        else io.github.dirtyredz.roundwood.block.assembly.AssemblyMutationBatch.run(level, java.util.List.of(BlockPos.ZERO),
                () -> nested(level, depth - 1, mutation));
    }

    @GameTest(template = "window_cross_v0120", timeoutTicks = 200)
    public static void queuedDiscoveryConvertsTheHistoricalBuild(GameTestHelper helper) {
        var level = helper.getLevel();
        for (int x = 1; x <= 13; x++) {
            var pos = helper.absolutePos(new BlockPos(x, 2, 1));
            ChunkMigrations.enqueueChunk(level, level.getChunkAt(pos));
        }
        helper.succeedWhen(() -> {
            for (int x = 1; x <= 13; x++) {
                var pos = helper.absolutePos(new BlockPos(x, 2, 1));
                check(!CrossWindowCompatibility.isRetired(level.getBlockState(pos)), "queued cross pending at " + x);
                check(level.getBlockEntity(pos) instanceof RoundWindowBlockEntity window
                        && window.trims(TrimSlot.CROSS_BEAMS).count() == 1, "queued cross missing at " + x);
            }
        });
    }

    @GameTest(template = TEMPLATE)
    public static void aliasTransferPreservesSillsAndUnknownPerimeter(GameTestHelper helper) {
        BlockPos pos = helper.absolutePos(new BlockPos(3, 2, 3));
        var state = LegacyContent.windows().get(0).defaultBlockState();
        helper.getLevel().setBlock(pos, state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        var source = (RoundWindowBlockEntity) helper.getLevel().getBlockEntity(pos);
        source.setTrims(TrimSlot.SILL, FaceTrims.NONE.with(AttachmentFace.BACK, Appearance.DEFAULT));
        CompoundTag disk = source.saveWithFullMetadata(TestRegistries.get());
        CompoundTag unknown = new CompoundTag();
        unknown.putInt("Version", 99);
        unknown.putString("Custody", "keep me");
        disk.put("WindowTrim", unknown);
        source.loadWithComponents(disk, TestRegistries.get());
        check(LegacyWindowConversion.convert(helper.getLevel(), pos, state) == LegacyCells.Result.CONVERTED,
                "alias conversion refused");
        var converted = (RoundWindowBlockEntity) helper.getLevel().getBlockEntity(pos);
        check(converted.trims(TrimSlot.SILL).has(AttachmentFace.BACK), "migration lost the sill");
        check(converted.saveWithFullMetadata(TestRegistries.get()).getCompound("WindowTrim").equals(unknown), "migration lost unknown custody");
        helper.succeed();
    }
}
