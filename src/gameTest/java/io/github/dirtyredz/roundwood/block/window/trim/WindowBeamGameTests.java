package io.github.dirtyredz.roundwood.block.window.trim;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.*;
import io.github.dirtyredz.roundwood.block.window.geometry.*;
import io.github.dirtyredz.roundwood.block.window.geometry.face.*;
import io.github.dirtyredz.roundwood.block.window.interaction.WindowBeamFitPlan;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.fixture.*;
import io.github.dirtyredz.roundwood.fixture.window.WindowFixtures;
import io.github.dirtyredz.roundwood.fixture.window.WindowRays;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.*;
import java.util.List;
import java.util.Optional;

import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.*;

@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WindowBeamGameTests {
    private static final BlockPos AT = new BlockPos(4, 2, 4);

    private static RoundWindowBlockEntity entity(GameTestHelper helper) {
        return (RoundWindowBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(AT));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void everyProfileFitsOneIndependentCrossFromEitherSide(GameTestHelper helper) {
        var player = player(helper, "beams_fit", GameType.SURVIVAL, Direction.NORTH);
        var poses = List.of(WindowPose.upright(Direction.SOUTH, WindowDepth.CENTER),
                WindowPose.upright(Direction.EAST, WindowDepth.NEGATIVE), WindowPose.upright(Direction.WEST, WindowDepth.POSITIVE),
                new WindowPose(Direction.UP, Direction.NORTH, WindowDepth.NEGATIVE),
                new WindowPose(Direction.DOWN, Direction.EAST, WindowDepth.POSITIVE));
        for (var profile : WindowProfile.values()) for (var pose : poses) for (boolean sneak : List.of(false, true)) {
            helper.setBlock(AT, Blocks.AIR);
            WindowFixtures.setWindow(helper, AT, WindowDesign.plain(profile), pose);
            var window = entity(helper);
            window.setGlass(Optional.of(DyeColor.LIME));
            var stack = styled(ModItems.CROSS_BEAMS.get(), AppearanceFixtures.SPRUCE);
            stack.setCount(2);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var hit = WindowRays.face(helper, player, AT, 8, 8, pose.front());
            check(WindowBeamFitPlan.of(helper.getLevel(), helper.absolutePos(AT), player, InteractionHand.MAIN_HAND, hit).isPresent(), "preview refused " + profile + pose);
            check(AppearanceFixtures.use(helper, player, stack, ItemStack.EMPTY, hit, sneak).consumesAction(), "fit unhandled");
            check(stack.getCount() == 1 && window.trims(TrimSlot.CROSS_BEAMS).count() == 1, "fit/payment");
            check(WindowBeamFitPlan.of(helper.getLevel(), helper.absolutePos(AT), player, InteractionHand.MAIN_HAND, hit).isEmpty(), "occupied preview");
            var back = WindowRays.face(helper, player, AT, 8, 8, pose.front().getOpposite());
            AppearanceFixtures.use(helper, player, stack, ItemStack.EMPTY, back, !sneak);
            check(stack.getCount() == 1 && window.trims(TrimSlot.CROSS_BEAMS).count() == 1, "second side duplicated beams");
            var dye = new ItemStack(Items.RED_DYE, 2);
            AppearanceFixtures.use(helper, player, dye, ItemStack.EMPTY, back, sneak);
            check(window.trims(TrimSlot.CROSS_BEAMS).front().equals(Optional.of(AppearanceFixtures.SPRUCE.painted(DyeColor.RED)))
                    && window.appearance().equals(Appearance.DEFAULT) && window.glass().equals(Optional.of(DyeColor.LIME)), "beam dye touched frame/glass");
            check(dye.getCount() == 1, "beam dye payment");
            var shears = new ItemStack(Items.SHEARS);
            player.getInventory().clearContent();
            AppearanceFixtures.use(helper, player, shears, ItemStack.EMPTY, hit, sneak);
            check(!window.trims(TrimSlot.CROSS_BEAMS).any() && shears.getDamageValue() == 1, "whole-pane removal/wear");
            int refunds = player.getInventory().items.stream().filter(s -> s.is(ModItems.CROSS_BEAMS.get())).mapToInt(ItemStack::getCount).sum();
            check(refunds == 1, "expected one beam refund");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void beamPersistenceClearsPacketsAndKeepsUnknownSlotsIndependent(GameTestHelper helper) {
        WindowFixtures.setWindow(helper, AT, WindowDesign.plain(WindowProfile.ROUND), WindowPose.upright(Direction.SOUTH, WindowDepth.CENTER));
        var window = entity(helper);
        WindowTrimActions.install(helper.getLevel(), helper.absolutePos(AT), TrimSlot.CROSS_BEAMS, AttachmentFace.BACK, AppearanceFixtures.SPRUCE);
        var disk = window.saveWithFullMetadata();
        check(disk.getCompound("WindowTrim").getAllKeys().equals(java.util.Set.of("Version")), "beam-only save invents perimeter trims");
        var copy = new RoundWindowBlockEntity(window.getType(), helper.absolutePos(AT), helper.getBlockState(AT));
        copy.load(disk);
        check(copy.trims().count() == 0 && copy.trims(TrimSlot.CROSS_BEAMS).count() == 1, "beam-only reload");
        WindowAttachmentState.store(helper.getLevel(), helper.absolutePos(AT), window, TrimSlot.CROSS_BEAMS, FaceTrims.NONE);
        copy.handleUpdateTag(window.getUpdateTag());
        check(!copy.trims(TrimSlot.CROSS_BEAMS).any(), "removal packet kept beams");
        copy.load(disk);
        copy.onDataPacket(null, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(copy, entity -> new CompoundTag()));
        check(!copy.trims(TrimSlot.CROSS_BEAMS).any() && !copy.trims().any(), "tagless update kept attachments");
        CompoundTag future = new CompoundTag();
        future.putInt("Version", 99);
        future.putString("Data", "custody");
        disk.put(WindowBeamNbt.TAG, future);
        window.load(disk);
        check(!window.setTrims(TrimSlot.CROSS_BEAMS, FaceTrims.NONE), "edited unknown beam data");
        check(window.setTrims(FaceTrims.both(Appearance.DEFAULT)), "unknown beams blocked perimeter");
        check(window.saveWithFullMetadata().getCompound(WindowBeamNbt.TAG).equals(future), "unknown beam data lost");
        check(!window.getUpdateTag().getCompound(WindowBeamNbt.TAG).contains("Data"), "unknown data leaked in update");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void transformsMasonrySillPriorityAndTaggedShearsKeepTheSingleton(GameTestHelper helper) {
        var player = player(helper, "beam_transform", GameType.SURVIVAL, Direction.NORTH);
        WindowFixtures.setWindow(helper, AT, WindowDesign.plain(WindowProfile.ARCH_LEFT), WindowPose.upright(Direction.SOUTH, WindowDepth.CENTER));
        var abs = helper.absolutePos(AT);
        check(WindowTrimActions.install(helper.getLevel(), abs, TrimSlot.CROSS_BEAMS, AttachmentFace.BACK, AppearanceFixtures.SPRUCE), "beam install");
        for (var mirror : net.minecraft.world.level.block.Mirror.values()) {
            for (var rotation : net.minecraft.world.level.block.Rotation.values()) {
                var transformed = helper.getBlockState(AT).mirror(mirror).rotate(rotation);
                helper.setBlock(AT, transformed);
                var pane = (io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock) transformed.getBlock();
                var hit = WindowRays.face(helper, player, AT, 8, 8, pane.pose(transformed).front());
                var wood = new ItemStack(Items.BIRCH_PLANKS, 2);
                AppearanceFixtures.use(helper, player, wood, ItemStack.EMPTY, hit, false);
                check(entity(helper).trims(TrimSlot.CROSS_BEAMS).count() == 1 && !entity(helper).trims(TrimSlot.CROSS_BEAMS).has(AttachmentFace.BACK), "transform duplicated beams");
                for (var masonry : List.of(Items.BRICKS, Items.STONE_BRICKS, Items.DEEPSLATE_BRICKS, Items.MUD_BRICKS)) {
                    var block = new ItemStack(masonry, 2);
                    AppearanceFixtures.use(helper, player, block, ItemStack.EMPTY, hit, true);
                    check(block.getCount() == 1 && entity(helper).trims(TrimSlot.CROSS_BEAMS).front().orElseThrow().kind() == Appearance.Kind.MASONRY, "beam masonry/payment");
                    var dye = new ItemStack(Items.RED_DYE, 2);
                    AppearanceFixtures.use(helper, player, dye, ItemStack.EMPTY, hit, false);
                    check(dye.getCount() == 2, "masonry accepted paint");
                }
            }
        }
        var state = helper.getBlockState(AT);
        var pane = (io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock) state.getBlock();
        var pose = pane.pose(state);
        var face = WindowFaceIdentity.face(pose, io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock.faceReversed(state), pose.front());
        WindowTrimActions.install(helper.getLevel(), abs, TrimSlot.SILL, face, Appearance.DEFAULT);
        var beamBefore = entity(helper).trims(TrimSlot.CROSS_BEAMS);
        var sillHit = WindowRays.face(helper, player, AT, 8, 1, pose.front());
        AppearanceFixtures.use(helper, player, new ItemStack(Items.PINK_DYE), ItemStack.EMPTY, sillHit, false);
        check(entity(helper).trims(TrimSlot.SILL).get(face).orElseThrow().paint().equals(Optional.of(DyeColor.PINK))
                && entity(helper).trims(TrimSlot.CROSS_BEAMS).equals(beamBefore), "sill did not cover the beam end target");
        var shears = new ItemStack(Items.CARROT_ON_A_STICK);
        check(shears.is(net.neoforged.neoforge.common.Tags.Items.TOOLS_SHEAR), "test tagged shears unavailable");
        AppearanceFixtures.use(helper, player, shears, ItemStack.EMPTY, WindowRays.face(helper, player, AT, 8, 8, pose.front().getOpposite()), true);
        check(!entity(helper).trims(TrimSlot.CROSS_BEAMS).any() && shears.getDamageValue() == 1 && entity(helper).trims(TrimSlot.SILL).count() == 1, "tagged shears removed wrong attachment");
        helper.succeed();
    }

    @GameTest(template = "strip")
    public static void boundarySnapshotsTrackFitRemovalUnknownDataAndReload(GameTestHelper helper) {
        int x = 15 - (helper.absolutePos(BlockPos.ZERO).getX() & 15);
        var west = new BlockPos(x, 2, 1);
        var east = west.east();
        var pose = WindowPose.upright(Direction.SOUTH, WindowDepth.CENTER);
        WindowFixtures.setWindow(helper, west, WindowDesign.plain(WindowProfile.EDGE_TOP), pose);
        WindowFixtures.setWindow(helper, east, WindowDesign.plain(WindowProfile.EDGE_TOP), pose);
        var abs = helper.absolutePos(west);
        var neighbourPos = helper.absolutePos(east);
        check((abs.getX() >> 4) != (neighbourPos.getX() >> 4), "fixture does not cross a chunk boundary");
        var neighbour = (RoundWindowBlockEntity) helper.getLevel().getBlockEntity(neighbourPos);
        neighbour.onLoad();
        check(io.github.dirtyredz.roundwood.block.window.pane.WindowPaneState.neighbours(helper.getLevel(), abs).get(Direction.EAST).design().pattern() == WindowPattern.PLAIN, "plain boundary snapshot");
        WindowTrimActions.install(helper.getLevel(), neighbourPos, TrimSlot.CROSS_BEAMS, AttachmentFace.FRONT, Appearance.DEFAULT);
        check(io.github.dirtyredz.roundwood.block.window.pane.WindowPaneState.neighbours(helper.getLevel(), abs).get(Direction.EAST).design().pattern() == WindowPattern.CROSS, "fit did not change boundary snapshot");
        WindowAttachmentState.store(helper.getLevel(), neighbourPos, neighbour, TrimSlot.CROSS_BEAMS, FaceTrims.NONE);
        check(io.github.dirtyredz.roundwood.block.window.pane.WindowPaneState.neighbours(helper.getLevel(), abs).get(Direction.EAST).design().pattern() == WindowPattern.PLAIN, "removal left crossed snapshot");
        neighbour.onChunkUnloaded();
        check(!io.github.dirtyredz.roundwood.block.window.pane.WindowPaneState.neighbours(helper.getLevel(), abs).containsKey(Direction.EAST), "unavailable neighbour culled faces");
        neighbour.onLoad();
        var disk = neighbour.saveWithFullMetadata();
        disk.putString(WindowBeamNbt.TAG, "unsupported");
        neighbour.load(disk);
        check(!io.github.dirtyredz.roundwood.block.window.pane.WindowPaneState.neighbours(helper.getLevel(), abs).containsKey(Direction.EAST), "unknown neighbour culled faces");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void beamPermissionsHandsAndCreativeFollowTheSharedContract(GameTestHelper helper) {
        WindowFixtures.setWindow(helper, AT, WindowDesign.plain(WindowProfile.ROUND), WindowPose.upright(Direction.SOUTH, WindowDepth.CENTER));
        var player = player(helper, "beam_hands", GameType.SURVIVAL, Direction.NORTH);
        var stack = new ItemStack(ModItems.CROSS_BEAMS.get(), 2);
        var hit = WindowRays.face(helper, player, AT, 8, 8, Direction.SOUTH);
        AppearanceFixtures.use(helper, player, new ItemStack(Items.STICK), stack, hit, false);
        check(!entity(helper).trims(TrimSlot.CROSS_BEAMS).any() && stack.getCount() == 2, "offhand bypassed main hand");
        player.getAbilities().mayBuild = false;
        AppearanceFixtures.use(helper, player, stack, ItemStack.EMPTY, hit, false);
        check(!entity(helper).trims(TrimSlot.CROSS_BEAMS).any() && stack.getCount() == 2, "protected fit");
        player.getAbilities().mayBuild = true;
        AppearanceFixtures.use(helper, player, ItemStack.EMPTY, stack, hit, true);
        check(entity(helper).trims(TrimSlot.CROSS_BEAMS).count() == 1 && stack.getCount() == 1, "offhand fit");
        WindowAttachmentState.store(helper.getLevel(), helper.absolutePos(AT), entity(helper), TrimSlot.CROSS_BEAMS, FaceTrims.NONE);
        var creative = player(helper, "beam_creative", GameType.CREATIVE, Direction.NORTH);
        AppearanceFixtures.use(helper, creative, stack, ItemStack.EMPTY, hit, false);
        check(stack.getCount() == 1 && entity(helper).trims(TrimSlot.CROSS_BEAMS).count() == 1, "creative fit charged");
        helper.succeed();
    }
}
