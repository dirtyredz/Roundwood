package io.github.dirtyredz.roundwood.migration;


import io.github.dirtyredz.roundwood.fixture.TestRegistries;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.block.door.trim.DoorTrimActions;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlockEntity;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimLayout;
import io.github.dirtyredz.roundwood.block.trim.assembly.TrimPlacement;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.BRICKS;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.SPRUCE;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.oakDoor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.migration.LegacyMigrationFixtures.loadAndTick;

/**
 * Upgrading 0.7.0 content (0.8.0): the {@code legacy_v070} build (written from 0.7.0 states and NBT)
 * loads with both-faced window trims (the omitted-oak case included) and its fitted frames become
 * door FRONT trims, open doors too, while a freestanding frame stays; built cases check that an
 * occupied FRONT face, another facing or offset keep the frame, that a frame whose door appears later
 * converts then, that repeated passes change nothing, and that nothing ever drops.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorTrimMigrationGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);

    private DoorTrimMigrationGameTests() {
    }

    private static void migrate(GameTestHelper helper) {
        List<BlockPos> area = new ArrayList<>();
        BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 0, 0)), helper.absolutePos(new BlockPos(12, 4, 8)))
                .forEach(pos -> area.add(pos.immutable()));
        loadAndTick(helper, area);
    }

    private static int items(GameTestHelper helper) {
        AABB box = new AABB(net.minecraft.world.phys.Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(-2, -2, -2))), net.minecraft.world.phys.Vec3.atLowerCornerOf(helper.absolutePos(new BlockPos(16, 8, 12))));
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, box).size();
    }

    private static FaceTrims windowTrims(GameTestHelper helper, int x, int z) {
        return ((RoundWindowBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(new BlockPos(x, 2, z)))).trims();
    }

    private static RoundDoorBlockEntity anchor(GameTestHelper helper, DoorLayout.Assembly door) {
        return (RoundDoorBlockEntity) helper.getLevel().getBlockEntity(door.anchor());
    }

    /** The structure is placed one block up: template y 1 is helper y 2. */
    @GameTest(template = "legacy_v070", timeoutTicks = 100)
    public static void aSevenOhBuildUpgrades(GameTestHelper helper) {
        int before = items(helper);
        migrate(helper);
        helper.runAfterDelay(3, () -> {
            check(windowTrims(helper, 1, 1).equals(FaceTrims.both(Appearance.DEFAULT)), "omitted-oak window: " + windowTrims(helper, 1, 1));
            check(windowTrims(helper, 3, 1).equals(FaceTrims.both(BRICKS)), "brick window: " + windowTrims(helper, 3, 1));
            check(!windowTrims(helper, 5, 1).any(), "an untrimmed window gained trims");
            BlockPos flush = helper.absolutePos(new BlockPos(3, 2, 1));
            check(((RoundWindowBlockEntity) helper.getLevel().getBlockEntity(flush)).appearance().equals(SPRUCE)
                    && WindowPaneBlock.trimmed(helper.getLevel().getBlockState(flush)), "brick window frame or summary");

            DoorLayout.Assembly closed = new DoorLayout.Assembly(helper.absolutePos(new BlockPos(1, 2, 3)), Direction.NORTH, DoorHingeSide.LEFT, DoorDesign.ROUND);
            DoorLayout.Assembly open = new DoorLayout.Assembly(helper.absolutePos(new BlockPos(5, 2, 3)), Direction.NORTH, DoorHingeSide.RIGHT, DoorDesign.ROUND);
            check(anchor(helper, closed).trims().equals(FaceTrims.NONE.with(AttachmentFace.FRONT, SPRUCE)), "closed door: " + anchor(helper, closed).trims());
            check(anchor(helper, open).trims().equals(FaceTrims.NONE.with(AttachmentFace.FRONT, Appearance.DEFAULT)), "open door: " + anchor(helper, open).trims());
            check(helper.getLevel().getBlockState(open.anchor()).getValue(DoorBlock.OPEN), "the open door closed");
            for (BlockPos cell : new TrimLayout.Assembly(helper.absolutePos(new BlockPos(1, 2, 4)), Direction.NORTH).positions()) {
                check(helper.getLevel().getBlockState(cell).isAir(), "fitted spruce frame left at " + cell);
            }
            for (BlockPos cell : new TrimLayout.Assembly(helper.absolutePos(new BlockPos(5, 2, 4)), Direction.NORTH).positions()) {
                check(helper.getLevel().getBlockState(cell).isAir(), "fitted oak frame left at " + cell);
            }
            TrimLayout.Assembly free = new TrimLayout.Assembly(helper.absolutePos(new BlockPos(9, 2, 3)), Direction.NORTH);
            check(free.complete(helper.getLevel(), ModBlocks.roundTrim()) && ((RoundTrimBlockEntity) helper.getLevel().getBlockEntity(free.anchor()))
                    .appearance().equals(Appearance.masonry(ResourceLocation.fromNamespaceAndPath("minecraft", "mud_bricks"))), "freestanding frame lost");
            // A second pass changes nothing.
            migrate(helper);
            helper.runAfterDelay(2, () -> {
                check(anchor(helper, closed).trims().count() == 1 && free.complete(helper.getLevel(), ModBlocks.roundTrim()), "second pass changed things");
                check(items(helper) == before, "migration dropped items");
                helper.succeed();
            });
        });
    }

    private static DoorLayout.Assembly door(GameTestHelper helper, String name) {
        floor(helper);
        ServerPlayer placer = player(helper, name, GameType.SURVIVAL, Direction.NORTH);
        check(placeDoor(helper, placer, oakDoor().asItem(), ANCHOR, DoorHingeSide.LEFT).consumesAction(), name + ": door");
        return assembly(helper, ANCHOR, Direction.NORTH, DoorHingeSide.LEFT);
    }

    /** The cells a 0.7.0 fitted frame took: one step in front of the door, same facing. */
    private static TrimLayout.Assembly fitted(DoorLayout.Assembly door) {
        return new TrimLayout.Assembly(door.anchor().relative(door.facing().getOpposite()), door.facing());
    }

    @GameTest(template = TEMPLATE)
    public static void anOccupiedFrontFaceKeepsTheFrame(GameTestHelper helper) {
        DoorLayout.Assembly door = door(helper, "occupied");
        DoorTrimActions.install(helper.getLevel(), door.anchor(), AttachmentFace.FRONT, BRICKS);
        TrimLayout.Assembly frame = fitted(door);
        check(TrimPlacement.placeAt(helper.getLevel(), ModBlocks.roundTrim(), frame, BRICKS), "frame");
        migrate(helper);
        helper.runAfterDelay(2, () -> {
            check(frame.complete(helper.getLevel(), ModBlocks.roundTrim()), "an occupied destination lost its frame");
            check(anchor(helper, door).trims().equals(FaceTrims.NONE.with(AttachmentFace.FRONT, BRICKS)), "door trims " + anchor(helper, door).trims());
            helper.succeed();
        });
    }

    /**
     * A door holding a trim payload of an unknown future version is not editable, so its fitted frame
     * stays (no drop, no conversion) and the unknown payload is saved back unchanged (0.9.0 shared store).
     */
    @GameTest(template = TEMPLATE)
    public static void anUnknownDoorPayloadKeepsTheFrameAndThePayload(GameTestHelper helper) {
        DoorLayout.Assembly door = door(helper, "unknown_payload");
        RoundDoorBlockEntity anchor = anchor(helper, door);
        CompoundTag tag = anchor.saveWithoutMetadata(TestRegistries.get());
        CompoundTag future = new CompoundTag();
        future.putInt("Version", 7);
        future.putString("Shape", "gothic");
        tag.put("DoorTrim", future);
        anchor.loadWithComponents(tag, TestRegistries.get());
        check(!anchor.trimsEditable(), "an unknown payload was understood");
        TrimLayout.Assembly frame = fitted(door);
        check(TrimPlacement.placeAt(helper.getLevel(), ModBlocks.roundTrim(), frame, SPRUCE), "frame");
        int before = items(helper);
        migrate(helper);
        helper.runAfterDelay(2, () -> {
            check(frame.complete(helper.getLevel(), ModBlocks.roundTrim()), "the frame was removed onto an uneditable door");
            check(items(helper) == before, "migration dropped items");
            check(anchor(helper, door).saveWithoutMetadata(TestRegistries.get()).getCompound("DoorTrim").equals(future), "the unknown payload changed");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void otherArrangementsKeepTheFrame(GameTestHelper helper) {
        DoorLayout.Assembly door = door(helper, "other");
        TrimLayout.Assembly turned = new TrimLayout.Assembly(fitted(door).anchor(), Direction.EAST);
        TrimLayout.Assembly offset = new TrimLayout.Assembly(fitted(door).anchor().relative(Direction.SOUTH), Direction.NORTH);
        check(TrimPlacement.placeAt(helper.getLevel(), ModBlocks.roundTrim(), offset, SPRUCE), "offset frame");
        migrate(helper);
        helper.runAfterDelay(2, () -> {
            check(offset.complete(helper.getLevel(), ModBlocks.roundTrim()) && !anchor(helper, door).trims().any(), "an offset frame converted");
            offset.positions().forEach(cell -> helper.getLevel().removeBlock(cell, false));
            check(TrimPlacement.placeAt(helper.getLevel(), ModBlocks.roundTrim(), turned, SPRUCE), "turned frame");
            migrate(helper);
            helper.runAfterDelay(2, () -> {
                check(turned.complete(helper.getLevel(), ModBlocks.roundTrim()) && !anchor(helper, door).trims().any(),
                        "a frame of another facing converted");
                helper.succeed();
            });
        });
    }

    /** A frame whose door appears later (its chunk loads later) converts when the door is found. */
    @GameTest(template = TEMPLATE)
    public static void aLateDoorConvertsItsFrame(GameTestHelper helper) {
        floor(helper);
        DoorLayout.Assembly planned = new DoorLayout.Assembly(helper.absolutePos(ANCHOR), Direction.NORTH, DoorHingeSide.LEFT, DoorDesign.ROUND);
        TrimLayout.Assembly frame = fitted(planned);
        check(TrimPlacement.placeAt(helper.getLevel(), ModBlocks.roundTrim(), frame, SPRUCE), "frame");
        migrate(helper);
        helper.runAfterDelay(2, () -> {
            check(frame.complete(helper.getLevel(), ModBlocks.roundTrim()), "a frame without its door converted");
            DoorLayout.Assembly door = door(helper, "late");
            migrate(helper);
            helper.runAfterDelay(2, () -> {
                check(anchor(helper, door).trims().equals(FaceTrims.NONE.with(AttachmentFace.FRONT, SPRUCE)), "late door: " + anchor(helper, door).trims());
                check(!frame.complete(helper.getLevel(), ModBlocks.roundTrim()), "frame kept after converting");
                helper.succeed();
            });
        });
    }
}
