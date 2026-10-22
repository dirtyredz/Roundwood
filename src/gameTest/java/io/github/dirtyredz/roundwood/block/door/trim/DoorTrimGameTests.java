package io.github.dirtyredz.roundwood.block.door.trim;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.selection.DoorSelectionShapes;
import io.github.dirtyredz.roundwood.block.trim.RoundTrimBlock;
import io.github.dirtyredz.roundwood.fixture.AppearanceFixtures;
import io.github.dirtyredz.roundwood.fixture.DoorFixtures;
import io.github.dirtyredz.roundwood.fixture.DoorRays;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.BRICKS;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.SPRUCE;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.use;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.anchorEntity;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.clearDrops;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.dropped;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.oakDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.styled;

/**
 * Door trims (0.8.0) through the production interaction path with real rays ({@link DoorRays}):
 * the door trim item attaches to the clicked face of a closed door and to an aimed static ring of an
 * open door without moving it; an occupied face is a free no-op; it never places a frame, toggles or
 * camouflages; planks, masonry and dye restyle an installed ring and shears remove only that face;
 * breaking any quarter drops the door once plus one trim per face in its look; the rings are static,
 * inside the render bounds, and change no collision.
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
        ServerPlayer placer = player(helper, name + "_placer", GameType.SURVIVAL, facing);
        check(placeDoor(helper, placer, oakDoor().asItem(), ANCHOR, hinge).consumesAction(), name + ": placement failed");
        return assembly(helper, ANCHOR, facing, hinge);
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
                AABB bounds = anchorEntity(helper, door).getRenderBoundingBox();
                check(bounds.contains(DoorFixtures.point(door, RING_A, RING_H, -0.9 / 16))
                        && bounds.contains(DoorFixtures.point(door, RING_A, RING_H, 3.9 / 16)), what + ": rings outside the render bounds " + bounds);
                for (BlockPos cell : door.positions()) {
                    helper.getLevel().destroyBlock(cell, false);
                }
            }
        }
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
        for (DoorLayout.Part part : DoorLayout.Part.values()) {
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
