package io.github.dirtyredz.roundwood.block.window.design;

import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.block.window.pane.OrientedWindowBlock;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.placement.WindowTargetZones;
import io.github.dirtyredz.roundwood.fixture.AppearanceFixtures;
import io.github.dirtyredz.roundwood.fixture.window.WindowFixtures;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.windowLook;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.clearDrops;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.dropped;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.styled;

/**
 * The bottom corners (0.7.0), plain and crossed: placed by real items with the arches' targeting,
 * four independent pieces forming a 2 x 2 arched window whose inner faces hide each other (no seam,
 * no divider), handedness under transforms, water, looks, drops and picks.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WindowCornerGameTests {
    private WindowCornerGameTests() {
    }

    private static WindowPaneBlock block(WindowProfile profile, WindowPattern pattern) {
        return ModBlocks.window(new WindowDesign(profile, pattern));
    }

    /** A player facing north builds a 2 x 2: corners on the floor, arches above; every piece is centred, front south, independent. */
    @GameTest(template = TEMPLATE)
    public static void fourPiecesPlacedByItemsFormTheArchedWindow(GameTestHelper helper) {
        for (WindowPattern pattern : WindowPattern.values()) {
            floor(helper);
            ServerPlayer player = player(helper, "corner_build", GameType.CREATIVE, Direction.NORTH);
            BlockPos bl = new BlockPos(3, 1, 4);
            BlockPos br = bl.east();
            BlockPos al = bl.above();
            BlockPos ar = br.above();
            place(helper, player, block(WindowProfile.BOTTOM_LEFT, pattern), WindowFixtures.zone(helper, bl.below(), Direction.UP, WindowTargetZones.Zone.CENTER));
            place(helper, player, block(WindowProfile.BOTTOM_RIGHT, pattern), WindowFixtures.zone(helper, br.below(), Direction.UP, WindowTargetZones.Zone.CENTER));
            place(helper, player, block(WindowProfile.ARCH_LEFT, pattern), WindowFixtures.zone(helper, bl, Direction.UP, WindowTargetZones.Zone.CENTER));
            place(helper, player, block(WindowProfile.ARCH_RIGHT, pattern), WindowFixtures.zone(helper, br, Direction.UP, WindowTargetZones.Zone.CENTER));
            WindowPose expected = WindowPose.upright(Direction.SOUTH, WindowDepth.CENTER);
            for (BlockPos pos : List.of(bl, br, al, ar)) {
                check(WindowFixtures.pose(helper.getBlockState(pos)).equals(expected), pattern + " " + pos + ": " + helper.getBlockState(pos));
            }
            // Every inner boundary: both faces hide each other, so neither draws a divider there.
            hideEachOther(helper, bl, br, Direction.EAST);
            hideEachOther(helper, al, ar, Direction.EAST);
            hideEachOther(helper, bl, al, Direction.UP);
            hideEachOther(helper, br, ar, Direction.UP);
            // Independent pieces: breaking one leaves the others.
            helper.destroyBlock(bl);
            check(helper.getBlockState(br).is(block(WindowProfile.BOTTOM_RIGHT, WindowPattern.PLAIN))
                    && helper.getBlockState(al).is(block(WindowProfile.ARCH_LEFT, WindowPattern.PLAIN)), "breaking a corner removed a neighbour");
            for (BlockPos pos : List.of(br, al, ar)) {
                helper.setBlock(pos, Blocks.AIR);
            }
            clearDrops(helper);
        }
        helper.succeed();
    }

    private static void hideEachOther(GameTestHelper helper, BlockPos a, BlockPos b, Direction aToB) {
        BlockState sa = helper.getBlockState(a);
        BlockState sb = helper.getBlockState(b);
        check(sa.skipRendering(sb, aToB) && sb.skipRendering(sa, aToB.getOpposite()),
                "faces between " + a + " and " + b + " are not hidden: a seam would show");
    }

    private static void place(GameTestHelper helper, ServerPlayer player, WindowPaneBlock piece, net.minecraft.world.phys.BlockHitResult hit) {
        BlockState predicted = WindowFixtures.predict(player, new ItemStack(piece), hit);
        check(WindowFixtures.place(player, new ItemStack(piece), hit).consumesAction(), piece + " not placed");
        BlockPos at = hit.getBlockPos().relative(hit.getDirection());
        check(helper.getLevel().getBlockState(at).equals(predicted), "placed " + helper.getLevel().getBlockState(at) + ", predicted " + predicted);
    }

    /** Rotation and mirroring keep the piece; four turns or two mirrors are identity. */
    @GameTest(template = TEMPLATE)
    public static void transformsRoundTripAndKeepThePiece(GameTestHelper helper) {
        for (WindowPaneBlock piece : ModBlocks.windows()) {
            if (!(piece instanceof OrientedWindowBlock)) {
                continue;
            }
            for (BlockState state : piece.getStateDefinition().getPossibleStates()) {
                if (state.getValue(WindowPaneBlock.TRIMMED) || state.getValue(BlockStateProperties.WATERLOGGED)) {
                    continue;
                }
                BlockState turned = state;
                for (int i = 0; i < 4; i++) {
                    turned = turned.rotate(Rotation.CLOCKWISE_90);
                }
                check(turned.equals(state), piece + ": four quarter turns changed " + state);
                for (Mirror mirror : new Mirror[] {Mirror.LEFT_RIGHT, Mirror.FRONT_BACK}) {
                    check(state.mirror(mirror).is(piece) && state.mirror(mirror).mirror(mirror).equals(state), piece + ": " + mirror);
                }
            }
        }
        helper.succeed();
    }

    /** Each corner drops and picks itself in its look; water stays; restyling keeps the pose. */
    @GameTest(template = TEMPLATE)
    public static void cornersDropPickWaterlogAndRestyle(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper, "corner_drop", GameType.SURVIVAL, Direction.NORTH);
        Appearance look = AppearanceFixtures.oakPainted(DyeColor.BROWN);
        WindowPaneBlock left = block(WindowProfile.BOTTOM_LEFT, WindowPattern.PLAIN);
        BlockPos at = new BlockPos(3, 1, 3);
        helper.setBlock(at, Blocks.WATER);
        WindowFixtures.place(player, styled(left, look), WindowFixtures.zone(helper, at.below(), Direction.UP, WindowTargetZones.Zone.CENTER));
        BlockState state = helper.getBlockState(at);
        check(state.is(left) && state.getValue(BlockStateProperties.WATERLOGGED), "corner not waterlogged: " + state);
        check(look.equals(windowLook(helper, helper.absolutePos(at))), "placed corner lost its look");
        check(io.github.dirtyredz.roundwood.block.window.trim.WindowTrimActions.install(helper.getLevel(), helper.absolutePos(at),
                io.github.dirtyredz.roundwood.block.attachment.TrimSlot.CROSS_BEAMS,
                io.github.dirtyredz.roundwood.block.attachment.AttachmentFace.FRONT, look), "corner beams not installed");
        ItemStack pick = AppearanceFixtures.pick(helper, helper.absolutePos(at));
        check(pick.is(left.asItem()) && AppearanceStacks.read(pick).equals(look), "pick " + pick);
        BlockState before = helper.getBlockState(at);
        AppearanceFixtures.use(helper, player, new ItemStack(Items.SPRUCE_PLANKS), ItemStack.EMPTY,
                io.github.dirtyredz.roundwood.fixture.window.WindowRays.face(helper, player, at, 1.5, 8, Direction.SOUTH), true);
        check(AppearanceFixtures.SPRUCE.equals(windowLook(helper, helper.absolutePos(at))) && helper.getBlockState(at).equals(before),
                "restyling a corner: " + helper.getBlockState(at));
        clearDrops(helper);
        player.gameMode.destroyBlock(helper.absolutePos(at));
        check(dropped(helper, left.asItem(), AppearanceFixtures.SPRUCE) == 1, "corner did not drop itself in its look");
        check(dropped(helper, ModItems.CROSS_BEAMS.get(), look) == 1, "crossed corner lost its independent beams");
        check(helper.getLevel().getFluidState(helper.absolutePos(at)).isSource(), "water not left behind");
        helper.setBlock(at, Blocks.AIR);
        clearDrops(helper);
        helper.succeed();
    }
}
