package io.github.dirtyredz.roundwood.block.door.placement;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageData;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.fixture.DoorRays;
import io.github.dirtyredz.roundwood.registry.ModBlockEntities;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.BRICKS;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.SPRUCE;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.use;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.anchorEntity;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assertAssembly;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assertNoDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.clearDrops;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.click;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.dropped;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.frontHit;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.rel;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.styled;

/**
 * The placed arched door (0.11.0): both halves open and close together from a click, redstone or
 * an external {@code setOpen} on either half; the open leaf leaves 13/16 of the doorway clear and
 * collides only inside its column; camouflage fills only the two upper corners for one refund; the
 * Universal Trim fits the jamb-and-crown casing and comes off with shears; either half's break
 * drops one door (plus its trims); everything survives a save and reload. Real rays and the real
 * server use path throughout.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArchedDoorGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);

    private ArchedDoorGameTests() {
    }

    private static AssemblyDoorBlock arched() {
        return ModBlocks.archedDoor();
    }

    private static DoorLayout.Assembly place(GameTestHelper helper, String name, DoorHingeSide hinge) {
        return place(helper, name, hinge, ItemStack.EMPTY);
    }

    /** An arched door facing north at {@link #ANCHOR}, from {@code stack} (an oak door when empty), drops cleared. */
    private static DoorLayout.Assembly place(GameTestHelper helper, String name, DoorHingeSide hinge, ItemStack stack) {
        floor(helper);
        ServerPlayer placer = player(helper, name + "_placer", GameType.SURVIVAL, Direction.NORTH);
        ItemStack item = stack.isEmpty() ? new ItemStack(arched().asItem()) : stack;
        check(placeDoor(helper, placer, item, ANCHOR, hinge).consumesAction(), name + ": placement failed");
        clearDrops(helper);
        return assembly(helper, ANCHOR, Direction.NORTH, hinge, DoorDesign.ARCHED);
    }

    private static boolean open(GameTestHelper helper, DoorLayout.Assembly door) {
        return helper.getLevel().getBlockState(door.anchor()).getValue(DoorBlock.OPEN);
    }

    @GameTest(template = TEMPLATE)
    public static void clicksRedstoneAndSetOpenDriveBothHalves(GameTestHelper helper) {
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            DoorLayout.Assembly door = place(helper, "drive_" + hinge, hinge);
            ServerPlayer player = player(helper, "drive_clicker_" + hinge, GameType.SURVIVAL, Direction.NORTH);
            boolean expected = false;
            for (BlockPos half : door.positions()) {
                InteractionResult result = click(helper, player, rel(helper, half));
                check(result.consumesAction(), hinge + ": click on " + half + " not handled: " + result);
                expected = !expected;
                assertAssembly(helper, door, arched(), expected);
            }
            for (BlockPos half : door.positions()) {
                BlockPos behind = rel(helper, half).north(); // the free side of the cell
                helper.setBlock(behind, Blocks.REDSTONE_BLOCK);
                assertAssembly(helper, door, arched(), true);
                for (BlockPos cell : door.positions()) {
                    check(helper.getLevel().getBlockState(cell).getValue(DoorBlock.POWERED), hinge + ": not powered at " + cell);
                }
                helper.setBlock(behind, Blocks.AIR);
                assertAssembly(helper, door, arched(), false);
            }
            BlockPos upper = door.positions().get(1);
            arched().setOpen(null, helper.getLevel(), helper.getLevel().getBlockState(upper), upper, true);
            assertAssembly(helper, door, arched(), true);
            arched().setOpen(null, helper.getLevel(), helper.getLevel().getBlockState(door.anchor()), door.anchor(), false);
            assertAssembly(helper, door, arched(), false);
            helper.setBlock(ANCHOR, Blocks.AIR);
        }
        helper.succeed();
    }

    /** Villagers and pathing treat it as a wooden door, and an open door leaves the player's width clear. */
    @GameTest(template = TEMPLATE)
    public static void theOpenDoorLeavesThirteenSixteenthsClear(GameTestHelper helper) {
        check(arched().defaultBlockState().is(BlockTags.WOODEN_DOORS), "the arched door is not a wooden door");
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            DoorLayout.Assembly door = place(helper, "passage_" + hinge, hinge);
            BlockPos anchor = door.anchor();
            AABB doorway = new AABB(anchor.getX(), anchor.getY(), anchor.getZ(), anchor.getX() + 1, anchor.getY() + 2, anchor.getZ() + 1);
            check(!helper.getLevel().noCollision(doorway.deflate(0.1)), hinge + ": the closed door does not block");
            arched().setOpen(null, helper.getLevel(), helper.getLevel().getBlockState(anchor), anchor, true);
            for (BlockPos cell : door.positions()) {
                VoxelShape shape = helper.getLevel().getBlockState(cell).getCollisionShape(helper.getLevel(), cell);
                check(!shape.isEmpty(), hinge + ": the open leaf stopped colliding at " + cell);
                double width = shape.max(Direction.Axis.X) - shape.min(Direction.Axis.X);
                check(Math.abs(width - 3 / 16.0) < 1e-6, hinge + ": open leaf collision is " + width * 16 + " px wide at " + cell);
            }
            // Facing north, the hinge column is x 0..3 (left) or 13..16 (right) of the cell: the rest is free.
            double from = hinge == DoorHingeSide.LEFT ? 3.01 / 16 : 0;
            AABB player = new AABB(anchor.getX() + from, anchor.getY(), anchor.getZ(),
                    anchor.getX() + from + 12.98 / 16, anchor.getY() + 1.8, anchor.getZ() + 1);
            check(helper.getLevel().noCollision(player), hinge + ": thirteen pixels of the doorway are not clear");
            helper.setBlock(ANCHOR, Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void camouflageFillsOnlyTheUpperCornersForOneRefund(GameTestHelper helper) {
        DoorLayout.Assembly door = place(helper, "camouflage", DoorHingeSide.LEFT);
        ServerPlayer player = player(helper, "camouflage_user", GameType.SURVIVAL, Direction.NORTH);
        // A lower click, even at the very edge, is the leaf: it toggles and applies nothing.
        use(helper, player, new ItemStack(Items.STONE_BRICKS, 2), ItemStack.EMPTY, frontHit(door, 0.03, 0.3), false);
        check(anchorEntity(helper, door).camouflage().isEmpty() && open(helper, door), "a low edge click camouflaged or did not toggle");
        arched().setOpen(null, helper.getLevel(), helper.getLevel().getBlockState(door.anchor()), door.anchor(), false);
        ItemStack bricks = new ItemStack(Items.STONE_BRICKS, 2);
        use(helper, player, bricks, ItemStack.EMPTY, frontHit(door, 0.03, 1.97), false);
        check(anchorEntity(helper, door).camouflage().map(CamouflageData::material).filter(m -> m.is(Blocks.STONE_BRICKS)).isPresent(),
                "the upper-left corner did not take the material");
        check(bricks.getCount() == 1 && !open(helper, door), "the corner charged " + (2 - bricks.getCount()) + " or toggled");
        use(helper, player, bricks, ItemStack.EMPTY, frontHit(door, 0.97, 1.97), false);
        check(bricks.getCount() == 1, "the second upper corner charged again");
        clearDrops(helper);
        player(helper, "camouflage_breaker", GameType.SURVIVAL, Direction.NORTH).gameMode.destroyBlock(door.anchor());
        assertNoDoor(helper, door);
        check(dropped(helper, Items.STONE_BRICKS) == 1, "expected one refund, got " + dropped(helper, Items.STONE_BRICKS));
        check(dropped(helper, ModItems.ARCHED_DOOR.get()) == 1, "expected one door, got " + dropped(helper, ModItems.ARCHED_DOOR.get()));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void theUniversalTrimFitsTheCasingAndShearsTakeItOff(GameTestHelper helper) {
        DoorLayout.Assembly door = place(helper, "casing", DoorHingeSide.LEFT);
        ServerPlayer player = player(helper, "casing_user", GameType.SURVIVAL, Direction.NORTH);
        ItemStack trims = styled(ModItems.ROUND_TRIM.get(), SPRUCE);
        trims.setCount(2);
        use(helper, player, trims, ItemStack.EMPTY, DoorRays.through(helper, player, door, 0.5, 1.0, true), false);
        check(anchorEntity(helper, door).trims().equals(FaceTrims.NONE.with(AttachmentFace.FRONT, SPRUCE)),
                "front casing: " + anchorEntity(helper, door).trims());
        check(trims.getCount() == 1 && !open(helper, door), "fitting charged wrongly or toggled");
        ItemStack bricks = styled(ModItems.ROUND_TRIM.get(), BRICKS);
        use(helper, player, bricks, ItemStack.EMPTY, DoorRays.through(helper, player, door, 0.5, 1.0, false), false);
        check(anchorEntity(helper, door).trims().equals(new FaceTrims(Optional.of(SPRUCE), Optional.of(BRICKS))),
                "back casing: " + anchorEntity(helper, door).trims());
        // Open, the casing (a jamb at 1/16 across) still takes shears; the leaf is not toggled.
        arched().setOpen(null, helper.getLevel(), helper.getLevel().getBlockState(door.anchor()), door.anchor(), true);
        ItemStack shears = new ItemStack(Items.SHEARS);
        use(helper, player, shears, ItemStack.EMPTY, DoorRays.through(helper, player, door, 15.0 / 16, 0.5, true), false);
        check(anchorEntity(helper, door).trims().equals(FaceTrims.NONE.with(AttachmentFace.BACK, BRICKS)),
                "shears on the front jamb left " + anchorEntity(helper, door).trims());
        check(open(helper, door), "shearing toggled the door");
        clearDrops(helper);
        player(helper, "casing_breaker", GameType.SURVIVAL, Direction.NORTH).gameMode.destroyBlock(door.positions().get(1));
        check(dropped(helper, ModItems.ARCHED_DOOR.get()) == 1, "expected one door from the upper half");
        check(dropped(helper, ModItems.ROUND_TRIM.get(), BRICKS) == 1, "expected the back casing's trim");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void eitherHalfDropsOneStyledDoor(GameTestHelper helper) {
        Appearance look = Appearance.DEFAULT.painted(net.minecraft.world.item.DyeColor.CYAN);
        for (int half = 0; half < 2; half++) {
            for (boolean open : new boolean[] {false, true}) {
                String name = "half " + half + " open=" + open;
                DoorLayout.Assembly door = place(helper, "drops_" + half + "_" + open, DoorHingeSide.LEFT,
                        styled(arched(), look));
                if (open) {
                    arched().setOpen(null, helper.getLevel(), helper.getLevel().getBlockState(door.anchor()), door.anchor(), true);
                }
                player(helper, "drops_breaker", GameType.SURVIVAL, Direction.NORTH).gameMode.destroyBlock(door.positions().get(half));
                assertNoDoor(helper, door);
                check(dropped(helper, ModItems.ARCHED_DOOR.get(), look) == 1,
                        name + ": expected one cyan door, got " + dropped(helper, ModItems.ARCHED_DOOR.get()));
                check(dropped(helper, ModItems.ROUND_DOOR.get()) == 0, name + ": dropped a round door");
                clearDrops(helper);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void lookTrimsAndCamouflageSurviveASaveAndReload(GameTestHelper helper) {
        DoorLayout.Assembly door = place(helper, "reload", DoorHingeSide.RIGHT, styled(arched(), SPRUCE));
        RoundDoorBlockEntity anchor = anchorEntity(helper, door);
        anchor.setTrims(new FaceTrims(Optional.of(BRICKS), Optional.of(SPRUCE)));
        anchor.setCamouflage(CamouflageData.free(Blocks.STONE_BRICKS.defaultBlockState()));
        arched().setOpen(null, helper.getLevel(), helper.getLevel().getBlockState(door.anchor()), door.anchor(), true);
        BlockState state = helper.getLevel().getBlockState(door.anchor());
        CompoundTag tag = anchor.saveWithFullMetadata();
        BlockEntity copy = BlockEntity.loadStatic(door.anchor(), state, tag);
        check(copy instanceof RoundDoorBlockEntity && copy.getType() == ModBlockEntities.ROUND_DOOR.get(), "did not load back: " + copy);
        RoundDoorBlockEntity loaded = (RoundDoorBlockEntity) copy;
        check(loaded.appearance().equals(SPRUCE), "look " + loaded.appearance());
        check(loaded.trims().equals(new FaceTrims(Optional.of(BRICKS), Optional.of(SPRUCE))), "trims " + loaded.trims());
        check(loaded.camouflage().map(c -> c.material().is(Blocks.STONE_BRICKS)).orElse(false), "camouflage");
        check(loaded.getBlockState().getValue(DoorBlock.OPEN) && loaded.getBlockState().getValue(DoorBlock.HINGE) == DoorHingeSide.RIGHT,
                "state " + loaded.getBlockState());
        helper.succeed();
    }

    /** The two door designs never mix: a round door's cells never count as an arched door's, and back. */
    @GameTest(template = TEMPLATE)
    public static void designsNeverClaimEachOthersCells(GameTestHelper helper) {
        DoorLayout.Assembly door = place(helper, "designs", DoorHingeSide.LEFT);
        DoorLayout.Assembly asRound = assembly(helper, ANCHOR, Direction.NORTH, DoorHingeSide.LEFT, DoorDesign.ROUND);
        check(!asRound.complete(helper.getLevel(), ModBlocks.roundDoor()) && asRound.presentParts(helper.getLevel(), arched()).isEmpty(),
                "an arched door's cells were read as a round door's");
        check(door.complete(helper.getLevel(), arched()), "the arched door is not complete");
        check(List.of(DoorLayout.Part.LOWER_LEFT, DoorLayout.Part.UPPER_LEFT).equals(door.parts()), "parts " + door.parts());
        helper.succeed();
    }
}
