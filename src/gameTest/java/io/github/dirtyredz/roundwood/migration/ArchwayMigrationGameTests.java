package io.github.dirtyredz.roundwood.migration;

import io.github.dirtyredz.roundwood.migration.retired.ArchwayStackMigration;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlockEntity;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlockEntity;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.migration.conversion.ArchwayConversion;
import io.github.dirtyredz.roundwood.migration.conversion.LegacyCells;
import io.github.dirtyredz.roundwood.migration.item.ArchwayStacks;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.List;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.*;

@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArchwayMigrationGameTests {
    @GameTest(template = "archways_v0133")
    public static void historicalAssembliesConvertThroughChunkDiscoveryExactlyOnce(GameTestHelper helper) {
        var level = helper.getLevel();
        var ordinary = helper.absolutePos(new BlockPos(1, 2, 1));
        var keyhole = helper.absolutePos(new BlockPos(5, 2, 1));
        for (var pos : List.of(ordinary, keyhole)) level.getBlockEntity(pos).getPersistentData().putString("MigrationGuard", "keep");
        ChunkMigrations.enqueueChunk(level, level.getChunkAt(ordinary));
        ChunkMigrations.enqueueChunk(level, level.getChunkAt(keyhole));
        helper.runAfterDelay(4, () -> {
            for (var anchor : List.of(ordinary, keyhole)) {
                var look = anchor.equals(ordinary) ? wood("spruce_planks") : Appearance.masonry(id("stone_bricks"));
                var top = new ArchPieceLayout.Assembly(anchor.above(), Direction.NORTH, ArchPieceDesign.TOP_1X2, Side.LEFT);
                check(top.complete(level, ModBlocks.archPiece(ArchPieceDesign.TOP_1X2)), "missing top cells");
                // Migration deliberately widens the historical radius-14 crown to radius 16.
                for (int x = 0; x < 32; x++) for (int y = 0; y < 16; y++) {
                    int dx = 2 * x + 1 - 32, dy = 2 * y + 1;
                    var cell = top.anchor().east(x / 16);
                    var state = level.getBlockState(cell);
                    double px = (x % 16 + .5) / 16, py = (y + .5) / 16;
                    boolean solid = state.getShape(level, cell).toAabbs().stream().anyMatch(b -> b.contains(px, py, .5));
                    check(solid == (dx * dx + dy * dy > 1024), "migrated crown retained its old margin");
                    check(state.getCollisionShape(level, cell).isEmpty(), "migrated crown blocks passage");
                }
                var entity = (ArchPieceBlockEntity) level.getBlockEntity(top.anchor());
                check(entity.appearance().equals(look), "body look lost");
                check(entity.getPersistentData().getString("MigrationGuard").equals("keep"), "persistent metadata lost");
                check(entity.trims().get(AttachmentFace.FRONT).orElseThrow().equals(Appearance.DEFAULT), "front trim lost");
                check(entity.trims().get(AttachmentFace.BACK).orElseThrow().equals(Appearance.masonry(id("bricks"))), "back trim lost");
                var disk = entity.saveWithFullMetadata();
                for (var side : Side.values()) {
                    var bottom = anchor.relative(Direction.EAST, side == Side.LEFT ? 0 : 1);
                    if (anchor.equals(ordinary)) check(level.getBlockState(bottom).isAir(), "ordinary jamb remains");
                    else {
                        var shoulder = new ArchPieceLayout.Assembly(bottom, Direction.NORTH, ArchPieceDesign.KEYHOLE_SHOULDER, side);
                        check(shoulder.complete(level, ModBlocks.archPiece(ArchPieceDesign.KEYHOLE_SHOULDER)), "shoulder side/owner lost");
                        var lower = (ArchPieceBlockEntity) level.getBlockEntity(bottom);
                        check(lower.appearance().equals(look) && !lower.trims().any(), "shoulder look or duplicated casing");
                        var state = level.getBlockState(bottom);
                        double px = side == Side.LEFT ? 3.5 / 16 : 12.5 / 16;
                        check(state.getCollisionShape(level, bottom).toAabbs().stream()
                                .anyMatch(b -> b.contains(px, .5 / 16, .5)), "migrated shoulder does not reach the floor");
                    }
                    check(ArchwayConversion.convert(level, bottom) == LegacyCells.Result.NOT_LEGACY, "repeat converted bottom");
                }
                check(ArchwayConversion.convert(level, top.anchor()) == LegacyCells.Result.NOT_LEGACY, "repeat converted top");
                check(disk.equals(entity.saveWithFullMetadata()), "repeat changed payload");
                check(level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                        new net.minecraft.world.phys.AABB(anchor).expandTowards(2, 2, 1)).isEmpty(), "conversion dropped duplicate items");
            }
            helper.succeed();
        });
    }

    @GameTest(template = "archways_v0133")
    public static void unknownTrimsAndSnapshotCaptureFailClosed(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var entity = (ArchwayBlockEntity) level.getBlockEntity(pos);
        var disk = entity.saveWithFullMetadata();
        disk.getCompound("ArchwayTrim").putInt("Version", 99);
        entity.load(disk);
        check(ArchwayConversion.convert(level, pos) == LegacyCells.Result.DEFERRED, "unknown trim converted");
        check(disk.equals(entity.saveWithFullMetadata()), "unknown trim changed");
        var keyhole = helper.absolutePos(new BlockPos(5, 2, 1));
        level.captureBlockSnapshots = true;
        try { check(ArchwayConversion.convert(level, keyhole) == LegacyCells.Result.DEFERRED, "converted inside placement capture"); }
        finally { level.captureBlockSnapshots = false; }
        check(level.getBlockState(keyhole).is(ModBlocks.archway(ArchwayDesign.KEYHOLE)), "capture changed source");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void chestAndPlayerStacksConvertWithCountsMetadataAndNoDuplicates(GameTestHelper helper) {
        helper.setBlock(new BlockPos(1, 1, 1), Blocks.CHEST);
        var chest = (ChestBlockEntity) helper.getBlockEntity(new BlockPos(1, 1, 1));
        var ordinary = AppearanceStacks.create(ModItems.archway(ArchwayDesign.ORDINARY), wood("spruce_planks"));
        ordinary.setCount(3);
        ordinary.getOrCreateTag().putString("OwnerData", "preserved");
        chest.setItem(0, ordinary);
        var keyhole = AppearanceStacks.create(ModItems.archway(ArchwayDesign.KEYHOLE), wood("cherry_planks"));
        keyhole.setCount(40);
        chest.setItem(1, keyhole);
        chest.load(chest.saveWithFullMetadata());
        var player = player(helper, "arch_migration", GameType.SURVIVAL, Direction.NORTH);
        player.getInventory().setItem(0, keyhole.copy());
        player.getInventory().load(player.getInventory().save(new net.minecraft.nbt.ListTag()));
        player.tickCount = 0;
        ArchwayStackMigration.onPlayerTick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END, player));
        ArchwayStackMigration.onChunkLoad(new net.minecraftforge.event.level.ChunkEvent.Load(
                helper.getLevel().getChunkAt(chest.getBlockPos()), false));
        helper.runAfterDelay(25, () -> {
            check(chest.countItem(ModItems.archPiece(ArchPieceDesign.TOP_1X2)) == 43, "chest tops");
            check(chest.countItem(ModItems.archPiece(ArchPieceDesign.KEYHOLE_SHOULDER)) == 80, "chest shoulders");
            check(chest.getItem(0).getTag().getString("OwnerData").equals("preserved"), "metadata lost");
            check(AppearanceStacks.read(chest.getItem(0)).equals(wood("spruce_planks")), "ordinary look lost");
            check(player.getInventory().countItem(ModItems.archPiece(ArchPieceDesign.TOP_1X2)) == 40, "inventory tops");
            check(player.getInventory().countItem(ModItems.archPiece(ArchPieceDesign.KEYHOLE_SHOULDER)) == 80, "inventory shoulders");
            check(!ArchwayStacks.convert(chest, stacks -> { throw new AssertionError("duplicate overflow"); }), "repeat changed chest");
            player.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void fullHolderOverflowIsLosslessAndRefusedOverflowLeavesOriginal(GameTestHelper helper) {
        var holder = new SimpleContainer(2);
        var source = AppearanceStacks.create(ModItems.archway(ArchwayDesign.KEYHOLE), wood("birch_planks"));
        source.setCount(64);
        holder.setItem(0, source);
        holder.setItem(1, new ItemStack(Items.STONE, 64));
        check(!ArchwayStacks.convert(holder, stacks -> false), "refused overflow consumed source");
        check(holder.getItem(0) == source && source.getCount() == 64, "source changed on refusal");
        List<ItemStack> drops = new ArrayList<>();
        check(ArchwayStacks.convert(holder, stacks -> { drops.addAll(stacks); return true; }), "full conversion refused");
        check(drops.stream().mapToInt(ItemStack::getCount).sum() == 128, "overflow shoulders lost");
        check(drops.stream().allMatch(stack -> AppearanceStacks.read(stack).equals(wood("birch_planks"))), "overflow look lost");
        check(holder.getItem(0).getCount() == 64 && holder.getItem(0).is(ModItems.archPiece(ArchPieceDesign.TOP_1X2)), "tops lost");
        var player = player(helper, "full_arch_holder", GameType.SURVIVAL, Direction.NORTH);
        for (int slot = 0; slot < player.getInventory().items.size(); slot++) player.getInventory().setItem(slot, new ItemStack(Items.STONE, 64));
        var carried = AppearanceStacks.create(ModItems.archway(ArchwayDesign.KEYHOLE), wood("mangrove_planks"));
        carried.setCount(2);
        player.getInventory().setItem(0, carried);
        var at = net.minecraft.world.phys.Vec3.atCenterOf(helper.absolutePos(new BlockPos(3, 2, 3)));
        check(ArchwayStackMigration.convert(helper.getLevel(), player.getInventory(), at), "full player inventory refused");
        check(player.getInventory().armor.stream().allMatch(ItemStack::isEmpty), "shoulders inserted into armor");
        check(player.getInventory().offhand.stream().allMatch(ItemStack::isEmpty), "shoulders inserted into offhand");
        check(dropped(helper, ModItems.archPiece(ArchPieceDesign.KEYHOLE_SHOULDER), wood("mangrove_planks")) == 4,
                "overflow did not drop exactly four shoulders at holder");
        helper.succeed();
    }

    @GameTest(template = "archways_v0133")
    public static void damagedOrUnloadedSourcesNeverBecomePartialPieces(GameTestHelper helper) {
        var level = helper.getLevel();
        var pos = helper.absolutePos(new BlockPos(1, 2, 1));
        var missing = pos.above().east();
        io.github.dirtyredz.roundwood.block.assembly.AssemblyMutationBatch.run(level, List.of(pos, pos.above(), pos.east(), missing),
                () -> level.setBlock(missing, Blocks.AIR.defaultBlockState(), 2 | 16 | 32));
        check(ArchwayConversion.convert(level, pos) == LegacyCells.Result.INCOMPLETE, "damaged source was split");
        check(level.getBlockState(pos).is(ModBlocks.archway(ArchwayDesign.ORDINARY)), "damaged source changed during preflight");
        var far = pos.offset(100000, 0, 100000);
        check(!level.isLoaded(far), "far fixture already loaded");
        check(ArchwayConversion.convert(level, far) == LegacyCells.Result.DEFERRED && !level.isLoaded(far), "migration force-loaded a chunk");
        ModBlocks.archway(ArchwayDesign.ORDINARY).tick(level.getBlockState(pos), level, pos, level.random);
        for (var cell : List.of(pos, pos.above(), pos.east(), missing)) check(level.getBlockState(cell).isAir(), "damaged cleanup left a cell");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void retiredItemsCannotPlaceTheOriginalAssembly(GameTestHelper helper) {
        floor(helper);
        var player = player(helper, "retired_arch_refused", GameType.SURVIVAL, Direction.NORTH);
        for (var design : ArchwayDesign.values()) {
            var stack = new ItemStack(ModItems.archway(design), 3);
            var context = io.github.dirtyredz.roundwood.fixture.ArchPieceFixtures.context(helper, player, stack,
                    new BlockPos(3, 1, 3), Side.LEFT);
            check(ModItems.archway(design).place(context) == net.minecraft.world.InteractionResult.FAIL, "retired item placed");
            check(stack.getCount() == 3 && helper.getBlockState(new BlockPos(3, 1, 3)).isAir(), "refused item changed world/count");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void loadingAnUnopenedLootChestDoesNotRollItsLoot(GameTestHelper helper) {
        var relative = new BlockPos(1, 1, 1);
        helper.setBlock(relative, Blocks.CHEST);
        var chest = (ChestBlockEntity) helper.getBlockEntity(relative);
        chest.setLootTable(id("chests/simple_dungeon"), 1234);
        var before = chest.saveWithFullMetadata();
        check(!ArchwayStackMigration.convert(helper.getLevel(), chest, net.minecraft.world.phys.Vec3.atCenterOf(chest.getBlockPos())),
                "unopened loot was converted");
        check(before.equals(chest.saveWithFullMetadata()), "migration rolled unopened loot");
        helper.succeed();
    }

    private static ResourceLocation id(String name) { return ResourceLocation.fromNamespaceAndPath("minecraft", name); }
    private static Appearance wood(String name) { return Appearance.wood(id(name)); }
}
