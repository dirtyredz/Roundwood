package io.github.dirtyredz.roundwood.block.door.selection;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.anchorEntity;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.hold;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.oakDoor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.placeOakDoor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.point;

/**
 * Selection through the real ray cast ({@code Level.clip} with the player's context): the round
 * outline, corners that become targets with a block item in hand or once filled, hits credited to
 * the part that contains them, and the open leaf's projection routed for use.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorSelectionGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    private static final List<Direction> FACINGS = List.of(Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST);
    private static final double[][] CORNERS = {{0.1, 0.1}, {1.9, 0.1}, {0.1, 1.9}, {1.9, 1.9}};

    private DoorSelectionGameTests() {
    }

    private static DoorLayout.Assembly place(GameTestHelper helper, Direction facing, DoorHingeSide hinge, String name) {
        return placeOakDoor(helper, ANCHOR, facing, hinge, name);
    }

    private static BlockHitResult clip(GameTestHelper helper, Vec3 from, Vec3 to, Entity entity) {
        return helper.getLevel().clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, entity == null ? CollisionContext.empty() : CollisionContext.of(entity)));
    }

    /** A ray from the player's side straight at the front face point (a, h). */
    private static BlockHitResult front(GameTestHelper helper, DoorLayout.Assembly assembly, double a, double h, Entity entity) {
        return clip(helper, point(assembly, a, h, -1), point(assembly, a, h, 0.5), entity);
    }

    private static boolean onDoor(GameTestHelper helper, BlockHitResult hit) {
        return hit.getType() == HitResult.Type.BLOCK && helper.getLevel().getBlockState(hit.getBlockPos()).getBlock() instanceof RoundDoorBlock;
    }

    /** The server's own acceptance rule for a use packet: the hit lies within one block of its cell's centre. */
    private static boolean serverAccepts(BlockHitResult hit) {
        Vec3 offset = hit.getLocation().subtract(Vec3.atCenterOf(hit.getBlockPos()));
        return Math.abs(offset.x) < 1.0000001 && Math.abs(offset.y) < 1.0000001 && Math.abs(offset.z) < 1.0000001;
    }

    @GameTest(template = TEMPLATE)
    public static void emptyCornersAreTargetsOnlyWithABlockItem(GameTestHelper helper) {
        floor(helper);
        for (Direction facing : FACINGS) {
            DoorLayout.Assembly assembly = place(helper, facing, DoorHingeSide.LEFT, "corners_" + facing);
            ServerPlayer looker = player(helper, "looker_" + facing, GameType.SURVIVAL, facing);
            check(onDoor(helper, front(helper, assembly, 1.0, 1.0, looker)), facing + ": the disc is not selectable");
            for (double[] c : CORNERS) {
                hold(looker, ItemStack.EMPTY, ItemStack.EMPTY);
                check(!onDoor(helper, front(helper, assembly, c[0], c[1], looker)), facing + ": a bare empty corner is selectable");
                hold(looker, new ItemStack(Items.STONE), ItemStack.EMPTY);
                BlockHitResult held = front(helper, assembly, c[0], c[1], looker);
                check(onDoor(helper, held), facing + ": corner " + c[0] + "," + c[1] + " not a target with a block in hand");
                hold(looker, ItemStack.EMPTY, new ItemStack(Items.OAK_PLANKS));
                check(onDoor(helper, front(helper, assembly, c[0], c[1], looker)), facing + ": offhand block item does not target corners");
                hold(looker, new ItemStack(Items.STICK), ItemStack.EMPTY);
                check(!onDoor(helper, front(helper, assembly, c[0], c[1], looker)), facing + ": a non-block item targets corners");
            }
            helper.setBlock(ANCHOR, Blocks.AIR);
        }
        helper.succeed();
    }

    private static boolean contains(VoxelShape shape, BlockPos cell, Vec3 world) {
        Vec3 local = world.subtract(Vec3.atLowerCornerOf(cell));
        return shape.toAabbs().stream().anyMatch(box -> box.contains(local));
    }

    /**
     * Owner report (0.3.1): an open door's highlight traced the closed corner frame (the highlight
     * was the target shape). With a block in either hand the empty corners stay targets, but the
     * visible shape the client highlights shows only what is there: the open leaf, plus the corners
     * once filled and not after they are cleared.
     */
    @GameTest(template = TEMPLATE)
    public static void openDoorHighlightShowsOnlyWhatIsThere(GameTestHelper helper) {
        floor(helper);
        for (Direction facing : FACINGS) {
            for (DoorHingeSide hinge : DoorHingeSide.values()) {
                DoorLayout.Assembly assembly = place(helper, facing, hinge, "highlight_" + facing + "_" + hinge);
                oakDoor().setOpen(null, helper.getLevel(), helper.getLevel().getBlockState(assembly.anchor()), assembly.anchor(), true);
                ServerPlayer looker = player(helper, "highlight_looker", GameType.SURVIVAL, facing);
                // The corners on the free side never overlap the open leaf.
                double freeA = hinge == DoorHingeSide.LEFT ? 1.9 : 0.1;
                for (double h : new double[] {0.1, 1.9}) {
                    Vec3 corner = point(assembly, freeA, h, 0.1);
                    for (ItemStack[] hands : new ItemStack[][] {{new ItemStack(Items.STONE), ItemStack.EMPTY},
                            {ItemStack.EMPTY, new ItemStack(Items.OAK_PLANKS)}}) {
                        hold(looker, hands[0], hands[1]);
                        for (DoorLayout.Part part : DoorLayout.Part.of(io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign.ROUND)) {
                            BlockPos pos = assembly.pos(part);
                            BlockState state = helper.getLevel().getBlockState(pos);
                            check(contains(state.getShape(helper.getLevel(), pos, CollisionContext.of(looker)), pos, corner),
                                    facing + " " + hinge + " " + part + ": the empty corner is no longer a target");
                            check(!contains(DoorSelectionShapes.visible(state, helper.getLevel(), pos), pos, corner),
                                    facing + " " + hinge + " " + part + ": the open door's highlight includes an empty corner");
                            check(contains(DoorSelectionShapes.visible(state, helper.getLevel(), pos), pos,
                                            point(assembly, hinge == DoorHingeSide.LEFT ? 0.1 : 1.9, 1.0, 1.5)),
                                    facing + " " + hinge + " " + part + ": the open leaf is not highlighted");
                        }
                    }
                    anchorEntity(helper, assembly).setCamouflage(CamouflageData.free(Blocks.STONE.defaultBlockState()));
                    for (DoorLayout.Part part : DoorLayout.Part.of(io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign.ROUND)) {
                        BlockPos pos = assembly.pos(part);
                        check(contains(DoorSelectionShapes.visible(helper.getLevel().getBlockState(pos), helper.getLevel(), pos), pos, corner),
                                facing + " " + hinge + " " + part + ": a filled corner is not highlighted");
                    }
                    anchorEntity(helper, assembly).setCamouflage(null);
                    for (DoorLayout.Part part : DoorLayout.Part.of(io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign.ROUND)) {
                        BlockPos pos = assembly.pos(part);
                        check(!contains(DoorSelectionShapes.visible(helper.getLevel().getBlockState(pos), helper.getLevel(), pos), pos, corner),
                                facing + " " + hinge + " " + part + ": a cleared corner is still highlighted");
                    }
                }
                helper.setBlock(ANCHOR, Blocks.AIR);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void filledCornersAreTargetsWithAnything(GameTestHelper helper) {
        floor(helper);
        for (boolean open : new boolean[] {false, true}) {
            DoorLayout.Assembly assembly = place(helper, Direction.NORTH, DoorHingeSide.LEFT, "filled_" + open);
            anchorEntity(helper, assembly).setCamouflage(CamouflageData.free(Blocks.STONE.defaultBlockState()));
            if (open) {
                oakDoor().setOpen(null, helper.getLevel(), helper.getLevel().getBlockState(assembly.anchor()), assembly.anchor(), true);
            }
            ServerPlayer looker = player(helper, "filled_looker", GameType.SURVIVAL, Direction.NORTH);
            hold(looker, new ItemStack(Items.STICK), ItemStack.EMPTY);
            for (double[] c : CORNERS) {
                check(onDoor(helper, front(helper, assembly, c[0], c[1], looker)), "open=" + open + ": filled corner not selectable");
                check(onDoor(helper, front(helper, assembly, c[0], c[1], null)), "open=" + open + ": filled corner missed without a player");
            }
            helper.setBlock(ANCHOR, Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void diagonalHitsAreCreditedToTheirOwnPart(GameTestHelper helper) {
        floor(helper);
        for (Direction facing : FACINGS) {
            DoorLayout.Assembly assembly = place(helper, facing, DoorHingeSide.LEFT, "diagonal_" + facing);
            // From inside the lower-left cell, behind the leaf, up and across to the back of the upper-right part.
            BlockHitResult raw = clip(helper, point(assembly, 0.5, 0.5, 0.9), point(assembly, 1.5, 1.5, 0.1), null);
            check(onDoor(helper, raw), facing + ": the diagonal ray missed the door");
            check(raw.getBlockPos().equals(assembly.anchor()), facing + ": expected the ray to be credited to the first part, got " + raw.getBlockPos());
            Optional<BlockHitResult> resolved = DoorHitResolver.resolve(helper.getLevel(), raw);
            check(resolved.isPresent(), facing + ": a hit on the door was refused");
            check(resolved.get().getBlockPos().equals(assembly.pos(DoorLayout.Part.UPPER_RIGHT)),
                    facing + ": credited to " + resolved.get().getBlockPos() + ", not the upper-right part");
            check(serverAccepts(resolved.get()), facing + ": the resolved hit fails the server's distance check");
            check(resolved.get().getLocation().equals(raw.getLocation()), facing + ": the hit point moved");
            // A hit already in its own part is left alone.
            BlockHitResult straight = front(helper, assembly, 0.5, 0.5, null);
            check(DoorHitResolver.resolve(helper.getLevel(), straight).orElseThrow() == straight, facing + ": a direct hit was rewritten");
            helper.setBlock(ANCHOR, Blocks.AIR);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void theOpenProjectionIsOutlinedAndOwned(GameTestHelper helper) {
        floor(helper);
        for (DoorHingeSide hinge : DoorHingeSide.values()) {
            DoorLayout.Assembly assembly = place(helper, Direction.NORTH, hinge, "projection_" + hinge);
            oakDoor().setOpen(null, helper.getLevel(), helper.getLevel().getBlockState(assembly.anchor()), assembly.anchor(), true);
            boolean left = hinge == DoorHingeSide.LEFT;
            // From the free column's back half across into the projection beside the hinge wall.
            Vec3 from = point(assembly, left ? 1.5 : 0.5, 0.9, 0.5);
            Vec3 to = point(assembly, left ? -1.0 : 3.0, 0.9, 1.5);
            BlockHitResult raw = clip(helper, from, to, null);
            check(onDoor(helper, raw), hinge + ": the projection is not outlined");
            BlockHitResult resolved = DoorHitResolver.resolve(helper.getLevel(), raw).orElseThrow();
            check(assembly.positions().contains(resolved.getBlockPos()), hinge + ": projection lost its assembly");
            check(raw.getLocation().equals(resolved.getLocation()), hinge + ": projection point moved");
            helper.setBlock(ANCHOR, Blocks.AIR);
        }
        helper.succeed();
    }
}
