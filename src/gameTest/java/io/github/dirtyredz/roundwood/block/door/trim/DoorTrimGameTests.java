package io.github.dirtyredz.roundwood.block.door.trim;


import io.github.dirtyredz.roundwood.fixture.TestRegistries;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.selection.DoorSelectionShapes;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock;
import io.github.dirtyredz.roundwood.fixture.AppearanceFixtures;
import io.github.dirtyredz.roundwood.fixture.door.DoorFixtures;
import io.github.dirtyredz.roundwood.fixture.door.DoorRays;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.BRICKS;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.SPRUCE;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.use;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.anchorEntity;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.clearDrops;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.dropped;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.oakDoor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.placeOakDoor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.styled;

/**
 * Door trims (0.8.0) through the production interaction path with real rays ({@link DoorRays}):
 * the door trim item attaches to the clicked face of a closed door and to an aimed static ring of an
 * open door without moving it; an occupied face is a free no-op; it never places a frame, toggles or
 * camouflages; planks, masonry and dye restyle an installed ring and shears remove only that face;
 * breaking any quarter drops the door once plus one trim per face in its look; the rings are static,
 * inside the render bounds, and change no collision. Since 0.9.0 the door's router does the fitting:
 * every Universal Trim id fits, plain or sneaking, the main hand first and the offhand only with an
 * empty main hand; a protected door refuses for free; looks save and reload per face.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorTrimGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    /** A pixel on the ring's bottom (across 16.5 px, up 1.5 px). */
    private static final double RING_A = 16.5 / 16;
    private static final double RING_H = 1.5 / 16;

    private DoorTrimGameTests() {
    }

    private static DoorLayout.Assembly place(GameTestHelper helper, Direction facing, DoorHingeSide hinge, String name) {
        floor(helper);
        return placeOakDoor(helper, ANCHOR, facing, hinge, name + "_placer");
    }

    private static boolean open(GameTestHelper helper, DoorLayout.Assembly door) {
        return helper.getLevel().getBlockState(door.anchor()).getValue(DoorBlock.OPEN);
    }

    private static FaceTrims trims(GameTestHelper helper, DoorLayout.Assembly door) {
        return anchorEntity(helper, door).trims();
    }

    private static ItemStack doorTrims(Appearance look, int count) {
        ItemStack stack = styled(ModItems.ROUND_TRIM.get(), look);
        stack.setCount(count);
        return stack;
    }

    @GameTest(template = TEMPLATE)
    public static void theDoorTrimFitsTheClickedFaceOfAClosedDoor(GameTestHelper helper) {
        for (Direction facing : List.of(Direction.NORTH, Direction.EAST)) {
            for (DoorHingeSide hinge : DoorHingeSide.values()) {
                String what = facing + " " + hinge;
                DoorLayout.Assembly door = place(helper, facing, hinge, "fit_" + facing.getName() + "_" + hinge.name().toLowerCase());
                ServerPlayer player = player(helper, "fit", GameType.SURVIVAL, facing);
                List<VoxelShape> collision = collision(helper, door);
                ItemStack stack = doorTrims(SPRUCE, 3);
                DoorFixtures.hold(player, stack, ItemStack.EMPTY);
                BlockHitResult front = DoorRays.through(helper, player, door, 1.0, 1.0, true);
                use(helper, player, stack, ItemStack.EMPTY, front, false);
                check(trims(helper, door).equals(FaceTrims.NONE.with(AttachmentFace.FRONT, SPRUCE)), what + ": front " + trims(helper, door));
                check(stack.getCount() == 2 && !open(helper, door), what + ": cost or toggle");
                use(helper, player, stack, ItemStack.EMPTY, DoorRays.through(helper, player, door, 1.0, 1.0, true), false);
                check(stack.getCount() == 2 && !open(helper, door), what + ": occupied face not a free no-op");
                ItemStack bricks = doorTrims(BRICKS, 1);
                use(helper, player, bricks, ItemStack.EMPTY, DoorRays.through(helper, player, door, 1.0, 1.0, false), false);
                check(trims(helper, door).equals(new FaceTrims(Optional.of(SPRUCE), Optional.of(BRICKS))), what + ": back " + trims(helper, door));
                check(anchorEntity(helper, door).camouflage().isEmpty(), what + ": camouflaged");
                check(collision.equals(collision(helper, door)), what + ": collision changed");
                // The ring stays inside the render bounds on both sides.
                AABB bounds = anchorEntity(helper, door).renderBoundingBox();
                check(bounds.contains(DoorFixtures.point(door, RING_A, RING_H, -0.9 / 16))
                        && bounds.contains(DoorFixtures.point(door, RING_A, RING_H, 3.9 / 16)), what + ": rings outside the render bounds " + bounds);
                for (BlockPos cell : door.positions()) {
                    helper.getLevel().destroyBlock(cell, false);
                }
            }
        }
        helper.succeed();
    }

    /**
     * Every Universal Trim id (canonical, the 0.8.0 window trim, 0.4.0 aliases in their historical
     * looks) fits the clicked face of a closed door for one item, plainly or sneaking, without
     * toggling or camouflaging; shears give back one canonical {@code round_trim} in the same look.
     */
    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void everyTrimIdFitsADoorPlainOrSneaking(GameTestHelper helper) {
        for (Item id : AppearanceFixtures.universalTrimIds()) {
            for (boolean sneaking : new boolean[] {false, true}) {
                String what = id + (sneaking ? " sneaking" : "");
                DoorLayout.Assembly door = place(helper, Direction.NORTH, DoorHingeSide.LEFT, "ids_" + (sneaking ? "sneak" : "plain"));
                ServerPlayer player = player(helper, "ids", GameType.SURVIVAL, Direction.NORTH);
                ItemStack stack = new ItemStack(id, 2);
                Appearance look = AppearanceStacks.read(stack);
                DoorFixtures.hold(player, stack, ItemStack.EMPTY);
                check(use(helper, player, stack, ItemStack.EMPTY, DoorRays.through(helper, player, door, 1.0, 1.0, true), sneaking)
                        .consumesAction(), what + ": not handled");
                check(trims(helper, door).equals(FaceTrims.NONE.with(AttachmentFace.FRONT, look)) && stack.getCount() == 1,
                        what + ": fitted " + trims(helper, door) + ", holding " + stack);
                check(!open(helper, door) && anchorEntity(helper, door).camouflage().isEmpty(), what + ": toggled or camouflaged");
                player.getInventory().clearContent();
                ItemStack shears = new ItemStack(Items.SHEARS);
                use(helper, player, shears, ItemStack.EMPTY, DoorRays.through(helper, player, door, RING_A, RING_H, true), sneaking);
                check(!trims(helper, door).any() && returned(player, look) == 1 && shears.getDamageValue() == 1,
                        what + ": shears did not give back one Universal Trim in " + look.key());
                for (BlockPos cell : door.positions()) {
                    helper.getLevel().destroyBlock(cell, false);
                }
            }
        }
        helper.succeed();
    }

    /** The main hand wins; an offhand trim fits only while the main hand is empty; a protected door refuses for free. */
    @GameTest(template = TEMPLATE)
    public static void theOffhandFitsOnlyWithAnEmptyMainHandAndProtectionRefuses(GameTestHelper helper) {
        DoorLayout.Assembly door = place(helper, Direction.NORTH, DoorHingeSide.LEFT, "door_offhand");
        ServerPlayer visitor = player(helper, "door_visitor", GameType.ADVENTURE, Direction.NORTH);
        ItemStack refused = doorTrims(SPRUCE, 2);
        use(helper, visitor, refused, ItemStack.EMPTY, DoorRays.through(helper, visitor, door, 1.0, 1.0, true), false);
        check(!trims(helper, door).any() && refused.getCount() == 2 && !open(helper, door), "a player who may not build fitted or toggled");
        ServerPlayer player = player(helper, "door_offhand", GameType.SURVIVAL, Direction.NORTH);
        ItemStack offTrim = doorTrims(BRICKS, 2);
        use(helper, player, ItemStack.EMPTY, offTrim, DoorRays.through(helper, player, door, 1.0, 1.0, true), false);
        check(trims(helper, door).equals(FaceTrims.NONE.with(AttachmentFace.FRONT, BRICKS)) && offTrim.getCount() == 1 && !open(helper, door),
                "offhand trim with an empty main hand: " + trims(helper, door));
        use(helper, player, new ItemStack(Items.STICK), offTrim, DoorRays.through(helper, player, door, 1.0, 1.0, false), false);
        check(!trims(helper, door).has(AttachmentFace.BACK) && offTrim.getCount() == 1, "an offhand trim fitted with a full main hand");
        helper.succeed();
    }

    /** Each face's look saves and reloads; the update tag always carries the payload so a removal reaches clients. */
    @GameTest(template = TEMPLATE)
    public static void doorFaceTrimsSaveReloadAndSync(GameTestHelper helper) {
        DoorLayout.Assembly door = place(helper, Direction.NORTH, DoorHingeSide.LEFT, "door_save");
        Appearance back = AppearanceFixtures.oakPainted(DyeColor.BLUE);
        check(DoorTrimActions.install(helper.getLevel(), door.anchor(), AttachmentFace.FRONT, BRICKS)
                && DoorTrimActions.install(helper.getLevel(), door.anchor(), AttachmentFace.BACK, back), "install");
        RoundDoorBlockEntity anchor = anchorEntity(helper, door);
        CompoundTag saved = anchor.saveWithFullMetadata(TestRegistries.get());
        check(saved.getCompound("DoorTrim").getInt("Version") == 1, "saved " + saved);
        RoundDoorBlockEntity copy = new RoundDoorBlockEntity(anchor.getType(), door.anchor(), helper.getLevel().getBlockState(door.anchor()));
        copy.loadWithComponents(saved, TestRegistries.get());
        check(copy.trims().equals(new FaceTrims(Optional.of(BRICKS), Optional.of(back))), "reloaded " + copy.trims());
        check(anchor.setTrims(FaceTrims.NONE), "clear");
        check(!anchor.saveWithFullMetadata(TestRegistries.get()).contains("DoorTrim"), "an untrimmed door wrote a payload");
        CompoundTag update = anchor.getUpdateTag(TestRegistries.get());
        check(update.contains("DoorTrim"), "a removal must be sent: " + update);
        copy.loadWithComponents(update, TestRegistries.get());
        check(!copy.trims().any(), "client kept a removed trim");
        helper.succeed();
    }

    /** The item never places a freestanding frame, and is refused on corners. */
    @GameTest(template = TEMPLATE)
    public static void theDoorTrimNeverPlacesAFrame(GameTestHelper helper) {
        DoorLayout.Assembly door = place(helper, Direction.NORTH, DoorHingeSide.LEFT, "no_frame");
        ServerPlayer player = player(helper, "no_frame", GameType.SURVIVAL, Direction.NORTH);
        ItemStack stack = doorTrims(SPRUCE, 2);
        DoorFixtures.useItemOn(helper, player, stack, new BlockPos(6, 1, 1), Direction.UP);
        use(helper, player, stack, ItemStack.EMPTY, AppearanceFixtures.corner(door), false);
        check(stack.getCount() == 2 && !trims(helper, door).any() && !open(helper, door), "corner or floor use did something");
        BlockPos.betweenClosed(helper.absolutePos(new BlockPos(0, 1, 0)), helper.absolutePos(new BlockPos(8, 4, 8))).forEach(pos ->
                check(!(helper.getLevel().getBlockState(pos).getBlock() instanceof RoundTrimBlock), "a frame was placed at " + pos));
        helper.succeed();
    }

    /** An open door's static rings are still targets for the door trim; the door does not move. */
    @GameTest(template = TEMPLATE)
    public static void anOpenDoorTakesTheTrimOnItsStaticRing(GameTestHelper helper) {
        DoorLayout.Assembly door = place(helper, Direction.NORTH, DoorHingeSide.LEFT, "open_fit");
        ServerPlayer player = player(helper, "open_fit", GameType.SURVIVAL, Direction.NORTH);
        use(helper, player, ItemStack.EMPTY, ItemStack.EMPTY, AppearanceFixtures.leaf(door, false), false);
        check(open(helper, door), "door did not open");
        ItemStack stack = doorTrims(SPRUCE, 2);
        DoorFixtures.hold(player, stack, ItemStack.EMPTY);
        use(helper, player, stack, ItemStack.EMPTY, DoorRays.through(helper, player, door, RING_A, RING_H, true), false);
        check(trims(helper, door).has(AttachmentFace.FRONT) && open(helper, door), "open front: " + trims(helper, door));
        use(helper, player, stack, ItemStack.EMPTY, DoorRays.through(helper, player, door, RING_A, RING_H, false), false);
        check(trims(helper, door).count() == 2 && open(helper, door) && stack.isEmpty(), "open back: " + trims(helper, door));
        // Installed rings stay outlined and targeted while open, with an empty hand too.
        DoorFixtures.hold(player, ItemStack.EMPTY, ItemStack.EMPTY);
        AABB visible = DoorSelectionShapes.visible(helper.getLevel().getBlockState(door.anchor()), helper.getLevel(), door.anchor())
                .bounds().move(door.anchor());
        check(visible.inflate(1e-6).contains(DoorFixtures.point(door, RING_A, RING_H, -0.5 / 16)), "front ring not outlined while open " + visible);
        helper.succeed();
    }

    /** Planks, masonry and dye on an installed ring restyle that face; shears remove only it; the leaf never changes. */
    @GameTest(template = TEMPLATE)
    public static void ringsRestyleAndShearPerFace(GameTestHelper helper) {
        DoorLayout.Assembly door = place(helper, Direction.NORTH, DoorHingeSide.RIGHT, "ring_style");
        check(DoorTrimActions.install(helper.getLevel(), door.anchor(), AttachmentFace.FRONT, Appearance.DEFAULT), "install front");
        check(DoorTrimActions.install(helper.getLevel(), door.anchor(), AttachmentFace.BACK, Appearance.DEFAULT), "install back");
        ServerPlayer player = player(helper, "ring_style", GameType.SURVIVAL, Direction.NORTH);
        ItemStack planks = new ItemStack(Items.SPRUCE_PLANKS, 2);
        use(helper, player, planks, ItemStack.EMPTY, DoorRays.through(helper, player, door, RING_A, RING_H, true), false);
        check(trims(helper, door).get(AttachmentFace.FRONT).equals(Optional.of(SPRUCE)) && planks.getCount() == 1, "planks: " + trims(helper, door));
        check(anchorEntity(helper, door).appearance().equals(Appearance.DEFAULT) && !open(helper, door)
                && anchorEntity(helper, door).camouflage().isEmpty(), "planks on the ring touched the door");
        ItemStack dye = new ItemStack(Items.CYAN_DYE, 2);
        use(helper, player, dye, ItemStack.EMPTY, DoorRays.through(helper, player, door, RING_A, RING_H, false), false);
        check(trims(helper, door).get(AttachmentFace.BACK).equals(Optional.of(AppearanceFixtures.oakPainted(DyeColor.CYAN))), "dye back");
        ItemStack bricks = new ItemStack(Items.BRICKS, 2);
        use(helper, player, bricks, ItemStack.EMPTY, DoorRays.through(helper, player, door, RING_A, RING_H, true), true);
        check(trims(helper, door).get(AttachmentFace.FRONT).equals(Optional.of(BRICKS)) && bricks.getCount() == 1, "sneak bricks");
        player.getInventory().clearContent();
        ItemStack shears = new ItemStack(Items.SHEARS);
        use(helper, player, shears, ItemStack.EMPTY, DoorRays.through(helper, player, door, RING_A, RING_H, true), false);
        check(!trims(helper, door).has(AttachmentFace.FRONT) && trims(helper, door).has(AttachmentFace.BACK), "shears: " + trims(helper, door));
        check(shears.getDamageValue() == 1 && returned(player, BRICKS) == 1 && !open(helper, door), "shears return/wear/toggle");
        use(helper, player, shears, ItemStack.EMPTY, DoorRays.through(helper, player, door, RING_A, RING_H, true), false);
        check(shears.getDamageValue() == 1 && !open(helper, door), "shears on an empty ring were not a free no-op");
        helper.succeed();
    }

    /** Breaking any quarter drops the door once and one trim per face, in its own look. */
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void everyQuarterDropsTheDoorAndBothTrims(GameTestHelper helper) {
        Appearance front = AppearanceFixtures.oakPainted(DyeColor.PURPLE);
        Appearance back = AppearanceFixtures.oakPainted(DyeColor.ORANGE);
        for (DoorLayout.Part part : DoorLayout.Part.of(io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign.ROUND)) {
            DoorLayout.Assembly door = place(helper, Direction.NORTH, DoorHingeSide.LEFT, "drops_" + part.name().toLowerCase());
            DoorTrimActions.install(helper.getLevel(), door.anchor(), AttachmentFace.FRONT, front);
            DoorTrimActions.install(helper.getLevel(), door.anchor(), AttachmentFace.BACK, back);
            clearDrops(helper);
            ServerPlayer player = player(helper, "drops", GameType.SURVIVAL, Direction.NORTH);
            player.gameMode.destroyBlock(door.pos(part));
            String what = part.name();
            check(dropped(helper, oakDoor().asItem()) == 1, what + ": doors " + dropped(helper, oakDoor().asItem()));
            check(dropped(helper, ModItems.ROUND_TRIM.get(), front) == 1 && dropped(helper, ModItems.ROUND_TRIM.get(), back) == 1
                    && dropped(helper, ModItems.ROUND_TRIM.get()) == 2, what + ": trims " + dropped(helper, ModItems.ROUND_TRIM.get()));
            clearDrops(helper);
        }
        // Creative: nothing.
        DoorLayout.Assembly door = place(helper, Direction.NORTH, DoorHingeSide.LEFT, "drops_creative");
        DoorTrimActions.install(helper.getLevel(), door.anchor(), AttachmentFace.FRONT, front);
        player(helper, "drops_creative", GameType.CREATIVE, Direction.NORTH).gameMode.destroyBlock(door.pos(DoorLayout.Part.UPPER_RIGHT));
        check(dropped(helper, ModItems.ROUND_TRIM.get()) == 0, "creative break dropped a trim");
        helper.succeed();
    }

    private static List<VoxelShape> collision(GameTestHelper helper, DoorLayout.Assembly door) {
        List<VoxelShape> out = new ArrayList<>();
        for (BlockPos cell : door.positions()) {
            out.add(helper.getLevel().getBlockState(cell).getCollisionShape(helper.getLevel(), cell));
        }
        return out;
    }

    private static int returned(ServerPlayer player, Appearance look) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (stack.is(ModItems.ROUND_TRIM.get()) && AppearanceStacks.read(stack).equals(look)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}
