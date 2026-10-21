package io.github.dirtyredz.roundwood.block.window;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.placement.WindowTargetZones;
import io.github.dirtyredz.roundwood.fixture.AppearanceFixtures;
import io.github.dirtyredz.roundwood.fixture.WindowFixtures;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.SPRUCE;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.windowLook;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.clearDrops;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.dropped;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.styled;

/** The two arch pieces: independent placement, pairing, handedness, transforms, drops, picks, water and looks. */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WindowArchGameTests {
    private WindowArchGameTests() {
    }

    private static RoundWindowArchBlock left() {
        return ModBlocks.archLeft();
    }

    private static RoundWindowArchBlock right() {
        return ModBlocks.archRight();
    }

    /** A pair placed by a player facing north: left at x, right east of it; both upright, front south, centred. */
    @GameTest(template = TEMPLATE)
    public static void aPairPlacedSideBySideFormsTheUpperHalfOfARoundWindow(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper, "arch_pair", GameType.CREATIVE, Direction.NORTH);
        BlockPos l = new BlockPos(3, 1, 4);
        BlockPos r = l.east();
        check(WindowFixtures.place(player, new ItemStack(left()), WindowFixtures.zone(helper, l.below(), Direction.UP,
                WindowTargetZones.Zone.CENTER)).consumesAction(), "left piece not placed");
        check(WindowFixtures.place(player, new ItemStack(right()), WindowFixtures.zone(helper, r.below(), Direction.UP,
                WindowTargetZones.Zone.CENTER)).consumesAction(), "right piece not placed");
        WindowPose expected = WindowPose.upright(Direction.SOUTH, WindowDepth.CENTER);
        check(helper.getBlockState(l).is(left()) && WindowFixtures.pose(helper.getBlockState(l)).equals(expected), "left: " + helper.getBlockState(l));
        check(helper.getBlockState(r).is(right()) && WindowFixtures.pose(helper.getBlockState(r)).equals(expected), "right: " + helper.getBlockState(r));
        check(expected.right() == Direction.EAST, "the player's right is east");
        // Breaking one leaves the other: no partner teardown.
        helper.destroyBlock(l);
        check(helper.getBlockState(r).is(right()), "breaking the left piece removed the right one");
        helper.succeed();
    }

    /** Edge zones give flush pieces facing the eye; floor and ceiling pieces take the top the player sees as up. */
    @GameTest(template = TEMPLATE)
    public static void flushFloorAndCeilingPiecesKeepTheirHandedness(GameTestHelper helper) {
        BlockPos wall = new BlockPos(4, 2, 4);
        helper.setBlock(wall, Blocks.STONE);
        // Standing south of the wall, eyes level with it, facing north.
        ServerPlayer player = WindowFixtures.playerAt(helper, "arch_edges", GameType.CREATIVE, new Vec3(4.5, 1.0, 7.5), Direction.NORTH, 0);
        BlockPos cell = wall.south();
        // Left zone of the wall's south face: flush west in the cell south of the wall, front towards the eye (east).
        place(helper, player, left(), WindowFixtures.zone(helper, wall, Direction.SOUTH, WindowTargetZones.Zone.U_MIN));
        check(WindowFixtures.pose(helper.getBlockState(cell)).equals(WindowPose.upright(Direction.EAST, WindowDepth.NEGATIVE)),
                "wall-flush piece: " + helper.getBlockState(cell));
        helper.setBlock(cell, Blocks.AIR);
        // Bottom zone: a floor piece in that cell; the eye is above it, so it faces up with its top away from the player.
        place(helper, player, left(), WindowFixtures.zone(helper, wall, Direction.SOUTH, WindowTargetZones.Zone.V_MIN));
        WindowPose floor = WindowFixtures.pose(helper.getBlockState(cell));
        check(floor.equals(new WindowPose(Direction.UP, Direction.NORTH, WindowDepth.NEGATIVE)), "floor piece: " + floor);
        check(floor.right() == Direction.EAST, "floor piece: left is not on the player's left");
        helper.setBlock(cell, Blocks.AIR);
        // Top zone: a ceiling piece; the eye is below it, so it faces down with its top towards the player.
        place(helper, player, left(), WindowFixtures.zone(helper, wall, Direction.SOUTH, WindowTargetZones.Zone.V_MAX));
        WindowPose ceiling = WindowFixtures.pose(helper.getBlockState(cell));
        check(ceiling.equals(new WindowPose(Direction.DOWN, Direction.SOUTH, WindowDepth.POSITIVE)), "ceiling piece: " + ceiling);
        check(ceiling.right() == Direction.EAST, "ceiling piece: left is not on the player's left");
        helper.succeed();
    }

    private static void place(GameTestHelper helper, ServerPlayer player, RoundWindowArchBlock piece, BlockHitResult hit) {
        BlockState predicted = WindowFixtures.predict(player, new ItemStack(piece), hit);
        check(WindowFixtures.place(player, new ItemStack(piece), hit).consumesAction(), piece + " not placed");
        BlockPos at = hit.getBlockPos().relative(hit.getDirection());
        check(helper.getLevel().getBlockState(at).equals(predicted), "placed " + helper.getLevel().getBlockState(at) + ", predicted " + predicted);
    }

    /** Rotation and mirroring move the geometry and keep the piece; four turns or two mirrors are identity. */
    @GameTest(template = TEMPLATE)
    public static void transformsRoundTripAndKeepThePiece(GameTestHelper helper) {
        for (RoundWindowArchBlock piece : ModBlocks.arches()) {
            for (BlockState state : piece.getStateDefinition().getPossibleStates()) {
                BlockState turned = state;
                for (int i = 0; i < 4; i++) {
                    turned = turned.rotate(Rotation.CLOCKWISE_90);
                    check(turned.is(piece), "rotation changed the piece");
                }
                check(turned.equals(state), "four quarter turns changed " + state + " to " + turned);
                for (Mirror mirror : new Mirror[] {Mirror.LEFT_RIGHT, Mirror.FRONT_BACK}) {
                    BlockState mirrored = state.mirror(mirror);
                    check(mirrored.is(piece) && mirrored.mirror(mirror).equals(state), mirror + " twice changed " + state);
                }
            }
        }
        // A left piece facing south, mirrored north-south, is a left piece facing south seen from behind: front unchanged.
        BlockState south = RoundWindowArchBlock.withPose(left().defaultBlockState(), WindowPose.upright(Direction.SOUTH, WindowDepth.NEGATIVE));
        WindowPose mirrored = left().pose(south.mirror(Mirror.LEFT_RIGHT));
        check(mirrored.equals(WindowPose.upright(Direction.SOUTH, WindowDepth.POSITIVE)), "mirrored pose " + mirrored);
        helper.succeed();
    }

    /** Each piece drops and picks itself with its look, and placing the drop again restores the look. */
    @GameTest(template = TEMPLATE)
    public static void piecesDropAndPickThemselvesWithTheirLook(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper, "arch_drop", GameType.SURVIVAL, Direction.NORTH);
        Appearance look = AppearanceFixtures.oakPainted(net.minecraft.world.item.DyeColor.LIME);
        BlockPos l = new BlockPos(2, 1, 2);
        BlockPos r = new BlockPos(5, 1, 2);
        WindowFixtures.place(player, styled(left(), look), WindowFixtures.zone(helper, l.below(), Direction.UP, WindowTargetZones.Zone.CENTER));
        WindowFixtures.place(player, styled(right(), look), WindowFixtures.zone(helper, r.below(), Direction.UP, WindowTargetZones.Zone.U_MIN));
        check(look.equals(windowLook(helper, helper.absolutePos(l))) && look.equals(windowLook(helper, helper.absolutePos(r))),
                "placed pieces lost the stack's look");
        ItemStack pickL = AppearanceFixtures.pick(helper, helper.absolutePos(l));
        ItemStack pickR = AppearanceFixtures.pick(helper, helper.absolutePos(r));
        check(pickL.is(ModItems.ROUND_WINDOW_ARCH_LEFT.get()) && AppearanceStacks.read(pickL).equals(look), "left pick " + pickL);
        check(pickR.is(ModItems.ROUND_WINDOW_ARCH_RIGHT.get()) && AppearanceStacks.read(pickR).equals(look), "right pick " + pickR);
        clearDrops(helper);
        player.gameMode.destroyBlock(helper.absolutePos(l));
        player.gameMode.destroyBlock(helper.absolutePos(r));
        check(dropped(helper, ModItems.ROUND_WINDOW_ARCH_LEFT.get(), look) == 1, "left piece did not drop itself in its look");
        check(dropped(helper, ModItems.ROUND_WINDOW_ARCH_RIGHT.get(), look) == 1, "right piece did not drop itself in its look");
        check(dropped(helper, ModItems.ROUND_WINDOW.get(), look) == 0, "an arch dropped a round window");
        // Break and replace: the drop places the same piece in its look, posed by the new target.
        WindowFixtures.place(player, styled(left(), look), WindowFixtures.zone(helper, l.below(), Direction.UP, WindowTargetZones.Zone.V_MAX));
        check(helper.getBlockState(l).is(left()) && look.equals(windowLook(helper, helper.absolutePos(l)))
                && WindowFixtures.pose(helper.getBlockState(l)).depth() == WindowDepth.POSITIVE, "re-placed piece " + helper.getBlockState(l));
        clearDrops(helper);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void piecesWaterlogAndKeepWaterWhenBroken(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper, "arch_water", GameType.SURVIVAL, Direction.NORTH);
        BlockPos at = new BlockPos(4, 1, 4);
        helper.setBlock(at, Blocks.WATER);
        WindowFixtures.place(player, new ItemStack(right()), WindowFixtures.zone(helper, at.below(), Direction.UP, WindowTargetZones.Zone.V_MIN));
        BlockState state = helper.getBlockState(at);
        check(state.is(right()) && state.getValue(BlockStateProperties.WATERLOGGED), "arch not waterlogged: " + state);
        BlockPos abs = helper.absolutePos(at);
        check(right().pickupBlock(helper.getLevel(), abs, state).is(Items.WATER_BUCKET), "bucket did not drain the arch");
        check(right().placeLiquid(helper.getLevel(), abs, helper.getBlockState(at), Fluids.WATER.getSource(false)), "bucket did not fill");
        player.gameMode.destroyBlock(abs);
        check(helper.getLevel().getFluidState(abs).isSource(), "water not left behind");
        helper.setBlock(at, Blocks.AIR);
        clearDrops(helper);
        helper.succeed();
    }

    /** Restyling with sneak + planks changes only the look; the pose, connections and water stay. */
    @GameTest(template = TEMPLATE)
    public static void restylingKeepsThePose(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper, "arch_style", GameType.SURVIVAL, Direction.NORTH);
        BlockPos at = new BlockPos(4, 1, 4);
        WindowFixtures.place(player, new ItemStack(left()), WindowFixtures.zone(helper, at.below(), Direction.UP, WindowTargetZones.Zone.U_MAX));
        helper.setBlock(at.north(), Blocks.GLASS_PANE);
        BlockState before = helper.getBlockState(at);
        BlockHitResult face = WindowFixtures.hit(helper, at, Direction.SOUTH, 0.5, 0.5);
        check(AppearanceFixtures.use(helper, player, new ItemStack(Items.SPRUCE_PLANKS), ItemStack.EMPTY, face, true).consumesAction(),
                "sneak + planks did not restyle the arch");
        check(SPRUCE.equals(windowLook(helper, helper.absolutePos(at))), "arch look " + windowLook(helper, helper.absolutePos(at)));
        check(helper.getBlockState(at).equals(before), "restyling changed the state: " + before + " -> " + helper.getBlockState(at));
        // A plain click with planks and an empty hand do nothing to the pose.
        AppearanceFixtures.use(helper, player, new ItemStack(Items.SPRUCE_PLANKS), ItemStack.EMPTY, face, false);
        AppearanceFixtures.use(helper, player, ItemStack.EMPTY, ItemStack.EMPTY, face, false);
        check(helper.getBlockState(at).is(left()) && WindowFixtures.pose(helper.getBlockState(at)).equals(WindowFixtures.pose(before)),
                "a plain click moved the arch: " + helper.getBlockState(at));
        helper.succeed();
    }
}
