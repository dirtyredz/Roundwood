package io.github.dirtyredz.roundwood.block.door.selection;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.interaction.ProjectedDoorUse;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.*;

/** Real world rays, projected transport validation and the normal server interaction router. */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorFreeEdgeGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);

    private DoorFreeEdgeGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void projectedUseRejectsObstructionsDistanceAndUnrelatedPoints(GameTestHelper helper) {
        floor(helper);
        var assembly = placeOakDoor(helper, ANCHOR, Direction.NORTH, DoorHingeSide.LEFT, "projection_guard");
        var level = helper.getLevel();
        var anchor = assembly.anchor();
        oakDoor().setOpen(null, level, level.getBlockState(anchor), anchor, true);
        ServerPlayer player = player(helper, "projection_guard_user", GameType.SURVIVAL, Direction.NORTH);
        Vec3 eye = point(assembly, 1.5, 0.9, 0.5);
        BlockHitResult hit = aim(helper, player, eye, point(assembly, 3.0 / 16, 0.9, 1.875));
        player.setPos(eye.x + 20, eye.y, eye.z);
        check(ProjectedDoorUse.use(player, hit) == InteractionResult.FAIL, "out-of-reach projection accepted");
        player.setPos(eye.x, eye.y - player.getEyeHeight(), eye.z);
        BlockHitResult forged = new BlockHitResult(hit.getLocation().add(0, 3, 0), hit.getDirection(), hit.getBlockPos(), false);
        check(ProjectedDoorUse.use(player, forged) == InteractionResult.FAIL, "unrelated point accepted");
        BlockPos obstacle = BlockPos.containing(eye.add(hit.getLocation()).scale(0.5));
        check(!assembly.positions().contains(obstacle), "obstacle would replace door");
        level.setBlockAndUpdate(obstacle, Blocks.STONE.defaultBlockState());
        check(ProjectedDoorUse.use(player, hit) == InteractionResult.FAIL, "solid neighbour did not block projected use");
        check(level.getBlockState(anchor).getValue(DoorBlock.OPEN), "rejected request toggled door");
        level.setBlockAndUpdate(obstacle, Blocks.AIR.defaultBlockState());
        check(ProjectedDoorUse.use(player, hit).consumesAction(), "unobstructed projection refused");
        check(!level.getBlockState(anchor).getValue(DoorBlock.OPEN), "valid request did not close door");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void freeEdgeClosesBothDesignsAndClosedLeafReopens(GameTestHelper helper) {
        floor(helper);
        for (AssemblyDoorBlock door : new AssemblyDoorBlock[] {ModBlocks.roundDoor(), ModBlocks.archedDoor(), ModBlocks.doubleArchedDoor(), ModBlocks.tallDoubleArchedDoor()}) {
            for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
                for (var depth : door.defaultBlockState().hasProperty(io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth.PROPERTY)
                        ? io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth.values()
                        : new io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth[]{io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth.FRONT})
                for (DoorHingeSide hinge : DoorHingeSide.values()) {
                    String label = door.design() + " " + facing + " " + hinge + " " + depth;
                    ServerPlayer player = player(helper, "free_edge_" + label, GameType.SURVIVAL, facing);
                    check(io.github.dirtyredz.roundwood.block.door.assembly.DoorPlacement.placeAt(helper.getLevel(), door,
                            new DoorLayout.Assembly(helper.absolutePos(ANCHOR), facing, hinge, door.design(), depth), false), label + ": placement failed");
                    hold(player, ItemStack.EMPTY, ItemStack.EMPTY);
                    var level = helper.getLevel();
                    BlockPos anchor = helper.absolutePos(ANCHOR);
                    var assembly = DoorLayout.of(anchor, level.getBlockState(anchor));
                    door.setOpen(null, level, level.getBlockState(anchor), anchor, true);
                    double width = door.design().width() / 16.0;
                    double leafWidth = door.design().doubleLeaf() ? width / 2 : width;
                    boolean left = hinge == DoorHingeSide.LEFT;
                    Vec3 eye = point(assembly, left ? width - 0.5 : 0.5, 0.9, 0.5);
                    Vec3 target = point(assembly, left ? 3.0 / 16 : width - 3.0 / 16, 0.9, leafWidth - 0.125 - depth.offset() / 16);
                    BlockHitResult hit = aim(helper, player, eye, target);
                    check(DoorHitRegion.classify(level.getBlockState(hit.getBlockPos()), hit.getBlockPos(), hit)
                            == DoorHitRegion.Region.LEAF, label + ": free edge classified as corner");
                    boolean projected = DoorHitResolver.projected(level, hit);
                    check(projected == (leafWidth - .125 - depth.offset() / 16 > 1), label + ": unexpected projection");
                    InteractionResult result = projected ? ProjectedDoorUse.use(player, hit) : use(helper, player, hit);
                    check(result.consumesAction(), label + ": free-edge use refused");
                    check(assembly.positions().stream().noneMatch(pos -> level.getBlockState(pos).getValue(DoorBlock.OPEN)),
                            label + ": free-edge click did not close the whole door");
                    if (projected) {
                        check(ProjectedDoorUse.use(player, hit) == InteractionResult.FAIL, label + ": stale projection accepted");
                    }
                    BlockHitResult closed = aim(helper, player, point(assembly, width / 2, 0.9, -1),
                            point(assembly, width / 2, 0.9, 0.25 - depth.offset() / 16));
                    check(use(helper, player, closed).consumesAction(), label + ": closed use refused");
                    check(assembly.positions().stream().allMatch(pos -> level.getBlockState(pos).getValue(DoorBlock.OPEN)),
                            label + ": closed leaf did not reopen");
                    helper.setBlock(ANCHOR, Blocks.AIR);
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void fittedFrameOccludesProjectedLeafUse(GameTestHelper helper) {
        floor(helper);
        var door = ModBlocks.roundDoor();
        var level = helper.getLevel();
        var original = placeOakDoor(helper, ANCHOR, Direction.NORTH, DoorHingeSide.LEFT, "frame_projection");
        check(io.github.dirtyredz.roundwood.block.door.frame.DoorArchFitting.fitAt(level, original.anchor(),
                io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness.FULL,
                io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth.FRONT,
                io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.BRICKS), "frame fixture");
        var assembly = DoorLayout.of(original.anchor(), level.getBlockState(original.anchor()));
        door.setOpen(null, level, level.getBlockState(assembly.anchor()), assembly.anchor(), true);
        var player = player(helper, "frame_projection_user", GameType.SURVIVAL, Direction.NORTH);
        var requested = aim(helper, player, point(assembly, 1.5, .9, .5),
                point(assembly, 3.0 / 16, .9, 1.875 - assembly.depth().offset() / 16));
        check(DoorHitResolver.projected(level, requested), "fixture is not a projected leaf point");
        boolean witnessed = false;
        for (double height = 2.2; height <= 3.01; height += .1) {
            var eye = point(assembly, 3.0 / 16, height, -.5);
            player.setPos(eye.x, eye.y - player.getEyeHeight(), eye.z);
            var ray = level.clip(new ClipContext(eye, requested.getLocation(), ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
            var resolved = DoorHitResolver.resolve(level, ray);
            if (ray.getType() != HitResult.Type.BLOCK || resolved.isEmpty()) continue;
            var hit = resolved.get();
            if (io.github.dirtyredz.roundwood.block.door.frame.DoorArchInteraction.hit(level, hit.getBlockPos(), level.getBlockState(hit.getBlockPos()), hit).isEmpty()) continue;
            check(!DoorHitResolver.projected(level, hit), "static frame acquired projected ownership");
            check(ProjectedDoorUse.use(player, requested) == InteractionResult.FAIL, "frame-occluded leaf request accepted");
            check(level.getBlockState(assembly.anchor()).getValue(DoorBlock.OPEN), "occluded request toggled door");
            witnessed = true;
            break;
        }
        check(witnessed, "no actual frame-occluded ray witnessed");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void fittedLeafMiddleAndFreeEdgeToggleEverySize(GameTestHelper helper) {
        floor(helper);
        var level = helper.getLevel();
        for (var block : ModBlocks.doors()) for (var facing : Direction.Plane.HORIZONTAL)
            for (var hinge : DoorHingeSide.values())
                for (var depth : io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth.values()) {
            var anchor = helper.absolutePos(ANCHOR);
            var assembly = new DoorLayout.Assembly(anchor, facing, hinge, block.design(), depth);
            check(io.github.dirtyredz.roundwood.block.door.assembly.DoorPlacement.placeAt(level, block, assembly, false), "leaf fixture");
            check(io.github.dirtyredz.roundwood.block.door.frame.DoorArchFitting.fitAt(level, anchor,
                    io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness.FULL,
                    io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth.FRONT,
                    io.github.dirtyredz.roundwood.appearance.Appearance.DEFAULT), "fit fixture");
            var player = player(helper, "fitted_leaf", GameType.SURVIVAL, facing);
            hold(player, ItemStack.EMPTY, ItemStack.EMPTY);
            double width = block.design().width() / 16.0;
            double leaf = block.design().doubleLeaf() ? width / 2 : width;
            for (boolean open : new boolean[]{true, false}) for (double fraction : new double[]{.5, .85}) {
                block.setOpen(null, level, level.getBlockState(anchor), anchor, open);
                Vec3 eye = open ? point(assembly, hinge == DoorHingeSide.LEFT ? width + .5 : -.5, .9, leaf * fraction - depth.offset() / 16)
                        : point(assembly, leaf * fraction, .9, -1);
                Vec3 target = open ? point(assembly, hinge == DoorHingeSide.LEFT ? .09375 : width - .09375, .9, leaf * fraction - depth.offset() / 16)
                        : point(assembly, leaf * fraction, .9, 1.25);
                var hit = aim(helper, player, eye, target);
                var result = DoorHitResolver.projected(level, hit) ? ProjectedDoorUse.use(player, hit) : use(helper, player, hit);
                check(result.consumesAction() && level.getBlockState(anchor).getValue(DoorBlock.OPEN) != open,
                        block.design() + " " + facing + " " + hinge + " " + depth + " open=" + open + " fraction=" + fraction + ": visible leaf did not toggle");
            }
            level.removeBlock(anchor, false); clearDrops(helper);
        }
        helper.succeed();
    }

    private static BlockHitResult aim(GameTestHelper helper, ServerPlayer player, Vec3 eye, Vec3 target) {
        player.setPos(eye.x, eye.y - player.getEyeHeight(), eye.z);
        Vec3 end = target.add(target.subtract(eye).scale(0.1));
        BlockHitResult raw = DoorRaycast.clip(helper.getLevel(), player, eye, end);
        check(raw.getType() == HitResult.Type.BLOCK, "ray missed leaf");
        return DoorHitResolver.resolve(helper.getLevel(), raw).orElseThrow();
    }
}
