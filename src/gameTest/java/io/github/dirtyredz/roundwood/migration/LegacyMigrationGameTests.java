package io.github.dirtyredz.roundwood.migration;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.AppearanceFamily;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.door.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageData;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlockEntity;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.block.window.RoundWindowBlock;
import io.github.dirtyredz.roundwood.block.window.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.pick;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.STRIP;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.clearDrops;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.dropReport;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.dropped;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.useItemOn;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.placeFreestanding;
import static io.github.dirtyredz.roundwood.fixture.TrimFixtures.trimAssembly;
import static io.github.dirtyredz.roundwood.migration.LegacyMigrationFixtures.isCanonicalDoor;
import static io.github.dirtyredz.roundwood.migration.LegacyMigrationFixtures.isCanonicalTrim;
import static io.github.dirtyredz.roundwood.migration.LegacyMigrationFixtures.legacy;
import static io.github.dirtyredz.roundwood.migration.LegacyMigrationFixtures.loadAndTick;
import static io.github.dirtyredz.roundwood.migration.LegacyMigrationFixtures.raw;
import static io.github.dirtyredz.roundwood.migration.LegacyMigrationFixtures.rawDoor;
import static io.github.dirtyredz.roundwood.migration.LegacyMigrationFixtures.rawEntity;
import static io.github.dirtyredz.roundwood.migration.LegacyMigrationFixtures.rawTrim;
import static io.github.dirtyredz.roundwood.migration.LegacyMigrationFixtures.wipe;

/**
 * Upgrading 0.4.0 content: a historical build loaded from its own structure file (0.4.0 ids and
 * NBT), every one of the 49 old ids loaded the way a saved chunk holds it, partly converted and
 * cross-chunk doors, incomplete old doors, aliases placed later, and 0.4.0 items still in
 * inventories. Conversions keep looks, states, camouflage and refunds, drop and refund nothing.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LegacyMigrationGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    private static final String CAMOUFLAGED = "{id:\"roundwood:round_door\",Camouflage:{Material:{Name:\"minecraft:polished_granite\"},"
            + "Refund:{id:\"minecraft:polished_granite\",Count:1b}}}";
    private static final String PLAIN = "{id:\"roundwood:round_door\"}";

    private LegacyMigrationGameTests() {
    }

    private static Appearance planks(String wood) {
        return Appearance.wood(ResourceLocation.fromNamespaceAndPath("minecraft", wood + "_planks"));
    }

    private static Appearance oak(DyeColor dye) {
        return Appearance.DEFAULT.painted(dye);
    }

    private static int items(GameTestHelper helper) {
        AABB area = new AABB(helper.absolutePos(BlockPos.ZERO), helper.absolutePos(new BlockPos(17, 6, 9))).inflate(1);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, area).size();
    }

    /**
     * The historical build, loaded from legacy_v040.nbt by the GameTest framework (structure placement path).
     * The framework places a template one block above the helper's origin, so template y 1 is helper y 2.
     */
    @GameTest(template = "legacy_v040", timeoutTicks = 100)
    public static void aHistoricalBuildConvertsLosslessly(GameTestHelper helper) {
        // The framework clears the template's scheduled ticks after placing it, so the build is found the
        // way a saved chunk is: through the chunk-load handler and the end-of-tick queue.
        List<BlockPos> template = new java.util.ArrayList<>();
        BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(12, 4, 3)))
                .forEach(pos -> template.add(pos.immutable()));
        loadAndTick(helper, template);
        helper.runAfterDelay(2, () -> {
            DoorLayout.Assembly spruce = new DoorLayout.Assembly(helper.absolutePos(new BlockPos(1, 2, 1)), Direction.NORTH, DoorHingeSide.LEFT);
            for (BlockPos cell : spruce.positions()) {
                check(isCanonicalDoor(helper.getLevel().getBlockState(cell)), "spruce door cell not converted: " + helper.getLevel().getBlockState(cell));
            }
            check(spruce.complete(helper.getLevel(), ModBlocks.roundDoor()), "spruce door incomplete after conversion");
            BlockState spruceAnchor = helper.getLevel().getBlockState(spruce.anchor());
            check(!spruceAnchor.getValue(DoorBlock.OPEN) && spruceAnchor.getValue(DoorBlock.HINGE) == DoorHingeSide.LEFT, "spruce door state");
            RoundDoorBlockEntity spruceEntity = (RoundDoorBlockEntity) helper.getLevel().getBlockEntity(spruce.anchor());
            check(spruceEntity.appearance().equals(planks("spruce")), "spruce door shows " + spruceEntity.appearance());
            CamouflageData camouflage = spruceEntity.camouflage().orElse(null);
            check(camouflage != null && camouflage.material().is(Blocks.POLISHED_GRANITE) && camouflage.paid(),
                    "camouflage or its refund lost: " + camouflage);

            DoorLayout.Assembly red = new DoorLayout.Assembly(helper.absolutePos(new BlockPos(4, 2, 1)), Direction.NORTH, DoorHingeSide.RIGHT);
            check(red.complete(helper.getLevel(), ModBlocks.roundDoor()), "red door not converted or incomplete");
            check(helper.getLevel().getBlockState(red.anchor()).getValue(DoorBlock.OPEN), "the open red door closed");
            check(((RoundDoorBlockEntity) helper.getLevel().getBlockEntity(red.anchor())).appearance().equals(oak(DyeColor.RED)), "red door look");

            TrimLayout.Assembly green = new TrimLayout.Assembly(helper.absolutePos(new BlockPos(1, 2, 2)), Direction.NORTH);
            TrimLayout.Assembly mud = new TrimLayout.Assembly(helper.absolutePos(new BlockPos(10, 2, 1)), Direction.NORTH);
            check(green.complete(helper.getLevel(), ModBlocks.roundTrim()), "fitted green trim not converted");
            check(mud.complete(helper.getLevel(), ModBlocks.roundTrim()), "mud-brick trim not converted");
            check(((RoundTrimBlockEntity) helper.getLevel().getBlockEntity(green.anchor())).appearance().equals(oak(DyeColor.GREEN)), "green trim look");
            check(((RoundTrimBlockEntity) helper.getLevel().getBlockEntity(mud.anchor())).appearance()
                    .equals(Appearance.masonry(ResourceLocation.fromNamespaceAndPath("minecraft", "mud_bricks"))), "mud-brick trim look");

            BlockPos birch = helper.absolutePos(new BlockPos(7, 2, 1));
            BlockPos blue = helper.absolutePos(new BlockPos(8, 2, 1));
            BlockState birchState = helper.getLevel().getBlockState(birch);
            BlockState blueState = helper.getLevel().getBlockState(blue);
            check(birchState.is(ModBlocks.roundWindow()) && blueState.is(ModBlocks.roundWindow()), "windows not converted");
            check(birchState.getValue(BlockStateProperties.WATERLOGGED) && helper.getLevel().getFluidState(birch).is(Fluids.WATER),
                    "the wet window lost its water");
            check(birchState.getValue(RoundWindowBlock.EAST) && blueState.getValue(RoundWindowBlock.WEST), "window connections lost");
            check(birchState.getValue(RoundWindowBlock.POSITION) == WindowPosition.CENTER
                    && blueState.getValue(RoundWindowBlock.POSITION) == WindowPosition.CENTER, "converted windows are not centred");
            check(((RoundWindowBlockEntity) helper.getLevel().getBlockEntity(birch)).appearance().equals(planks("birch")), "birch window look");
            check(((RoundWindowBlockEntity) helper.getLevel().getBlockEntity(blue)).appearance().equals(oak(DyeColor.BLUE)), "blue window look");
            check(items(helper) == 0, "conversion dropped items");

            // Saved and reloaded, the converted anchor keeps look and paid camouflage.
            BlockEntity reloaded = BlockEntity.loadStatic(spruce.anchor(), spruceAnchor, spruceEntity.saveWithFullMetadata());
            check(reloaded instanceof RoundDoorBlockEntity copy && copy.appearance().equals(planks("spruce"))
                    && copy.camouflage().map(CamouflageData::paid).orElse(false), "save/reload lost the converted door's data");

            // Breaking it: one spruce door and the granite back, once each.
            ServerPlayer breaker = player(helper, "legacy_breaker", GameType.SURVIVAL, Direction.NORTH);
            check(breaker.gameMode.destroyBlock(spruce.pos(DoorLayout.Part.UPPER_RIGHT)), "break refused");
            helper.runAfterDelay(1, () -> {
                check(dropped(helper, ModItems.ROUND_DOOR.get(), planks("spruce")) == 1,
                        "converted door drop: " + dropReport(helper, ModItems.ROUND_DOOR.get(), planks("spruce")));
                check(dropped(helper, Items.POLISHED_GRANITE) == 1, "refund dropped " + dropped(helper, Items.POLISHED_GRANITE));
                helper.succeed();
            });
        });
    }

    /** Every 0.4.0 id, loaded from a chunk: converted on the next level tick into its canonical family in its old look. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void everyOldIdConvertsFromAChunkInItsHistoricalLook(GameTestHelper helper) {
        floor(helper);
        BlockPos anchor = helper.absolutePos(ANCHOR);
        for (LegacyAppearances.Entry entry : LegacyAppearances.ALL) {
            List<BlockPos> cells;
            if (entry.family() == AppearanceFamily.DOOR) {
                cells = rawDoor(helper, anchor, entry.id(), DoorHingeSide.LEFT, false, PLAIN).positions();
            } else if (entry.family() == AppearanceFamily.TRIM) {
                cells = rawTrim(helper, anchor, entry.id()).positions();
            } else {
                raw(helper, anchor, legacy(entry.id()).defaultBlockState().setValue(RoundWindowBlock.AXIS, Direction.Axis.X));
                cells = List.of(anchor);
            }
            loadAndTick(helper, cells);
            BlockEntity entity = helper.getLevel().getBlockEntity(anchor);
            Appearance shown = switch (entry.family()) {
                case DOOR -> entity instanceof RoundDoorBlockEntity door && isCanonicalDoor(helper.getLevel().getBlockState(anchor))
                        ? door.appearance() : null;
                case TRIM -> entity instanceof RoundTrimBlockEntity trim && isCanonicalTrim(helper.getLevel().getBlockState(anchor))
                        ? trim.appearance() : null;
                case WINDOW -> entity instanceof RoundWindowBlockEntity window && helper.getLevel().getBlockState(anchor).is(ModBlocks.roundWindow())
                        ? window.appearance() : null;
            };
            check(entry.appearance().equals(shown), entry.id() + ": converted to " + helper.getLevel().getBlockState(anchor) + " showing " + shown);
            for (BlockPos cell : cells) {
                check(!LegacyChunkMigration.isLegacy(helper.getLevel().getBlockState(cell)), entry.id() + ": a legacy cell remains at " + cell);
            }
            wipe(helper, cells);
        }
        check(items(helper) == 0, "conversions dropped items");
        helper.succeed();
    }

    /** A door saved half converted (canonical anchor with its look, two alias parts) is finished, once, keeping the anchor's look. */
    @GameTest(template = TEMPLATE)
    public static void aPartlyConvertedDoorIsFinishedIdempotently(GameTestHelper helper) {
        floor(helper);
        Block alias = legacy("oak_round_door");
        Block canonical = ModBlocks.roundDoor();
        DoorLayout.Assembly door = new DoorLayout.Assembly(helper.absolutePos(ANCHOR), Direction.NORTH, DoorHingeSide.RIGHT);
        rawDoor(helper, door, new Block[] {canonical, alias, canonical, alias}, true,
                "{id:\"roundwood:round_door\",RoundwoodAppearance:{Version:1,Kind:\"wood\",Material:\"minecraft:warped_planks\",Paint:\"white\"}}");
        loadAndTick(helper, door.positions());
        Appearance expected = planks("warped").painted(DyeColor.WHITE);
        check(door.complete(helper.getLevel(), canonical), "the mixed door was not completed as canonical");
        check(((RoundDoorBlockEntity) helper.getLevel().getBlockEntity(door.anchor())).appearance().equals(expected), "the anchor's look was lost");
        for (BlockPos cell : door.positions()) {
            BlockState state = helper.getLevel().getBlockState(cell);
            check(state.getValue(DoorBlock.OPEN) && state.getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT, "state lost at " + cell);
        }
        // Again: nothing legacy remains, nothing changes or drops.
        loadAndTick(helper, door.positions());
        check(door.complete(helper.getLevel(), canonical) && items(helper) == 0, "a second pass changed or dropped something");
        helper.succeed();
    }

    /** An old door across a chunk boundary converts as one door when both chunks are loaded. */
    @GameTest(template = STRIP)
    public static void aDoorAcrossAChunkBoundaryConvertsWhole(GameTestHelper helper) {
        floor(helper, 17, 4);
        int x = 0;
        while ((helper.absolutePos(new BlockPos(x, 1, 1)).getX() & 15) != 15) {
            x++;
        }
        DoorLayout.Assembly door = rawDoor(helper, helper.absolutePos(new BlockPos(x, 1, 1)), "acacia_round_door",
                DoorHingeSide.LEFT, false, CAMOUFLAGED);
        check(door.anchor().getX() >> 4 != door.pos(DoorLayout.Part.LOWER_RIGHT).getX() >> 4, "the door does not cross a chunk boundary");
        loadAndTick(helper, door.positions());
        check(door.complete(helper.getLevel(), ModBlocks.roundDoor()), "the cross-chunk door was not converted whole");
        RoundDoorBlockEntity entity = (RoundDoorBlockEntity) helper.getLevel().getBlockEntity(door.anchor());
        check(entity.appearance().equals(planks("acacia")) && entity.camouflage().map(CamouflageData::paid).orElse(false),
                "cross-chunk conversion lost look or camouflage");
        check(items(helper) == 0, "cross-chunk conversion dropped items");
        helper.succeed();
    }

    /** A loaded but incomplete old door is cleaned up normally, dropping one canonical door in its old look. */
    @GameTest(template = TEMPLATE)
    public static void anIncompleteOldDoorIsClearedWithAStyledDrop(GameTestHelper helper) {
        floor(helper);
        Block alias = legacy("jungle_round_door");
        DoorLayout.Assembly door = new DoorLayout.Assembly(helper.absolutePos(ANCHOR), Direction.NORTH, DoorHingeSide.LEFT);
        rawDoor(helper, door, new Block[] {alias, alias, alias, null}, false, PLAIN);
        clearDrops(helper);
        loadAndTick(helper, door.positions());
        helper.runAfterDelay(4, () -> {
            for (BlockPos cell : door.positions()) {
                check(!(helper.getLevel().getBlockState(cell).getBlock() instanceof io.github.dirtyredz.roundwood.block.door.RoundDoorBlock),
                        "an incomplete door part survived at " + cell);
            }
            int styled = dropped(helper, ModItems.ROUND_DOOR.get(), planks("jungle"));
            check(styled == 1 && dropped(helper, ModItems.ROUND_DOOR.get()) == 1,
                    "incomplete cleanup dropped " + dropReport(helper, ModItems.ROUND_DOOR.get(), planks("jungle")));
            helper.succeed();
        });
    }

    /** Aliases placed after loading (commands, structures, editors) convert through their own scheduled tick. */
    @GameTest(template = TEMPLATE)
    public static void aliasesPlacedLaterConvertOnTheirOwnTick(GameTestHelper helper) {
        floor(helper);
        BlockPos windowRel = new BlockPos(6, 1, 6);
        helper.setBlock(windowRel, legacy("cherry_round_window").defaultBlockState());
        DoorLayout.Assembly door = new DoorLayout.Assembly(helper.absolutePos(ANCHOR), Direction.NORTH, DoorHingeSide.LEFT);
        Block alias = legacy("yellow_round_door");
        for (DoorLayout.Part part : DoorLayout.Part.values()) {
            helper.getLevel().setBlockAndUpdate(door.pos(part), door.stateFor(part, alias, false, false));
        }
        helper.runAfterDelay(3, () -> {
            BlockPos window = helper.absolutePos(windowRel);
            check(helper.getLevel().getBlockState(window).is(ModBlocks.roundWindow())
                    && ((RoundWindowBlockEntity) helper.getLevel().getBlockEntity(window)).appearance().equals(planks("cherry")),
                    "the placed alias window did not convert: " + helper.getLevel().getBlockState(window));
            check(door.complete(helper.getLevel(), ModBlocks.roundDoor())
                    && ((RoundDoorBlockEntity) helper.getLevel().getBlockEntity(door.anchor())).appearance().equals(oak(DyeColor.YELLOW)),
                    "the placed alias door did not convert");
            check(items(helper) == 0, "conversion dropped items");
            helper.succeed();
        });
    }

    /** 0.4.0 stacks still in inventories show their look and place canonical blocks; picks are canonical too. */
    @GameTest(template = TEMPLATE)
    public static void oldItemsPlaceCanonicalBlocksInTheirLook(GameTestHelper helper) {
        floor(helper);
        Item oldDoor = item("cherry_round_door");
        check(AppearanceStacks.read(new ItemStack(oldDoor)).equals(planks("cherry")), "the old door item lost its look");
        ServerPlayer player = player(helper, "old_items", GameType.SURVIVAL, Direction.NORTH);
        check(placeDoor(helper, player, new ItemStack(oldDoor), ANCHOR, DoorHingeSide.LEFT).consumesAction(), "old door item did not place");
        DoorLayout.Assembly door = new DoorLayout.Assembly(helper.absolutePos(ANCHOR), Direction.NORTH, DoorHingeSide.LEFT);
        check(door.complete(helper.getLevel(), ModBlocks.roundDoor()), "the old item placed something other than the canonical door");
        check(((RoundDoorBlockEntity) helper.getLevel().getBlockEntity(door.anchor())).appearance().equals(planks("cherry")), "placed look");
        ItemStack picked = pick(helper, door.pos(DoorLayout.Part.UPPER_RIGHT));
        check(picked.is(ModItems.ROUND_DOOR.get()) && AppearanceStacks.read(picked).equals(planks("cherry")), "pick: " + picked);

        BlockPos trimAnchor = new BlockPos(3, 1, 6);
        check(placeFreestanding(helper, player, new ItemStack(item("stone_brick_round_trim")), trimAnchor).consumesAction(), "old trim item");
        TrimLayout.Assembly trim = trimAssembly(helper, trimAnchor, Direction.NORTH);
        check(trim.complete(helper.getLevel(), ModBlocks.roundTrim())
                && ((RoundTrimBlockEntity) helper.getLevel().getBlockEntity(trim.anchor())).appearance()
                .equals(Appearance.masonry(ResourceLocation.fromNamespaceAndPath("minecraft", "stone_bricks"))), "old trim item placement");

        useItemOn(helper, player, new ItemStack(item("green_round_window")), new BlockPos(7, 0, 1), Direction.UP);
        BlockPos window = helper.absolutePos(new BlockPos(7, 1, 1));
        check(helper.getLevel().getBlockState(window).is(ModBlocks.roundWindow())
                && ((RoundWindowBlockEntity) helper.getLevel().getBlockEntity(window)).appearance().equals(oak(DyeColor.GREEN)), "old window item");
        helper.succeed();
    }

    private static Item item(String id) {
        return ForgeRegistries.ITEMS.getValue(ResourceLocation.fromNamespaceAndPath(Roundwood.MOD_ID, id));
    }
}
