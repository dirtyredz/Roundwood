package io.github.dirtyredz.roundwood.window;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.variant.FinishCatalog;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.function.Supplier;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.clearDrops;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.dropped;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.useItemOn;

/** Round windows: orientation, full-block collision, stability next to neighbours and water. */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WindowGameTests {
    private WindowGameTests() {
    }

    private static RoundWindowBlock window() {
        return ModBlocks.window(FinishCatalog.PAINT_BASE);
    }

    /** Places an item on top of the floor cell below {@code pos}, as a player facing {@code facing}. */
    private static void place(GameTestHelper helper, String name, Direction facing, ItemStack stack, BlockPos pos) {
        ServerPlayer player = player(helper, name, GameType.SURVIVAL, facing);
        check(useItemOn(helper, player, stack, pos.below(), Direction.UP).consumesAction(), name + ": placement failed at " + pos);
    }

    private static BlockState at(GameTestHelper helper, BlockPos pos) {
        return helper.getBlockState(pos);
    }

    /** Fresh stacks of a pane, a wall and another window: a placed stack is consumed. */
    @SuppressWarnings("unchecked")
    private static Supplier<ItemStack>[] neighbourStacks() {
        return new Supplier[] {
                (Supplier<ItemStack>) () -> new ItemStack(Items.GLASS_PANE),
                (Supplier<ItemStack>) () -> new ItemStack(Items.COBBLESTONE_WALL),
                (Supplier<ItemStack>) () -> new ItemStack(window())};
    }

    @GameTest(template = TEMPLATE)
    public static void facesThePlacingPlayer(GameTestHelper helper) {
        floor(helper);
        place(helper, "face_n", Direction.NORTH, new ItemStack(window()), new BlockPos(1, 1, 1));
        place(helper, "face_e", Direction.EAST, new ItemStack(window()), new BlockPos(5, 1, 5));
        check(at(helper, new BlockPos(1, 1, 1)).getValue(RoundWindowBlock.AXIS) == Direction.Axis.X, "looking north should give axis x");
        check(at(helper, new BlockPos(5, 1, 5)).getValue(RoundWindowBlock.AXIS) == Direction.Axis.Z, "looking east should give axis z");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void collisionIsASquareBlock(GameTestHelper helper) {
        floor(helper);
        place(helper, "square", Direction.NORTH, new ItemStack(window()), new BlockPos(4, 1, 4));
        BlockPos abs = helper.absolutePos(new BlockPos(4, 1, 4));
        double[][] probes = {{0.45, 0.45}, {0.01, 0.01}, {0.92, 0.01}, {0.01, 0.92}, {0.92, 0.92}};
        for (double[] probe : probes) {
            AABB box = new AABB(abs.getX() + probe[0], abs.getY() + probe[1], abs.getZ() + 0.4,
                    abs.getX() + probe[0] + 0.07, abs.getY() + probe[1] + 0.07, abs.getZ() + 0.6);
            check(!helper.getLevel().noCollision(box), "window should collide at " + probe[0] + "," + probe[1]);
        }
        AABB bounds = helper.getLevel().getBlockState(abs).getShape(helper.getLevel(), abs).bounds();
        check(bounds.equals(new AABB(0, 0, 0, 1, 1, 1)), "outline is not the full unit cube: " + bounds);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void woodCornersHitRays(GameTestHelper helper) {
        floor(helper);
        place(helper, "corner_ray", Direction.NORTH, new ItemStack(window()), new BlockPos(4, 1, 4)); // axis x: opening runs along z
        BlockPos abs = helper.absolutePos(new BlockPos(4, 1, 4));
        for (double[] corner : new double[][] {{0.05, 0.05}, {0.95, 0.05}, {0.05, 0.95}, {0.95, 0.95}}) {
            Vec3 from = new Vec3(abs.getX() + corner[0], abs.getY() + corner[1], abs.getZ() - 1.0);
            Vec3 to = new Vec3(abs.getX() + corner[0], abs.getY() + corner[1], abs.getZ() + 2.0);
            BlockHitResult hit = helper.getLevel().clip(new ClipContext(from, to, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, null));
            check(hit.getType() == HitResult.Type.BLOCK && helper.getLevel().getBlockState(hit.getBlockPos()).is(window()),
                    "ray through the wooden corner " + corner[0] + "," + corner[1] + " missed the window: " + hit);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void noConnectorStatesAndStableNextToPanesWallsAndWindows(GameTestHelper helper) {
        floor(helper);
        Supplier<ItemStack>[] neighbours = neighbourStacks();
        BlockPos[] offsets = {new BlockPos(1, 0, 0), new BlockPos(0, 0, 1), new BlockPos(-1, 0, 0)}; // in plane, behind, in plane
        // Window first, then each neighbour; and neighbours first, then the window.
        BlockPos first = new BlockPos(2, 1, 1);
        place(helper, "stable_a", Direction.NORTH, new ItemStack(window()), first);
        BlockState expected = at(helper, first);
        check(expected.getProperties().size() == 2, "window has connector or extra properties: " + expected.getProperties());
        for (int i = 0; i < neighbours.length; i++) {
            place(helper, "stable_a" + i, Direction.NORTH, neighbours[i].get(), first.offset(offsets[i]));
            check(at(helper, first).equals(expected), "window state changed after neighbour " + i + ": " + at(helper, first));
        }
        BlockPos second = new BlockPos(6, 1, 5);
        for (int i = 0; i < neighbours.length; i++) {
            place(helper, "stable_b" + i, Direction.NORTH, neighbours[i].get(), second.offset(offsets[i]));
        }
        place(helper, "stable_b", Direction.NORTH, new ItemStack(window()), second);
        BlockState placed = at(helper, second);
        check(placed.getValue(RoundWindowBlock.AXIS) == Direction.Axis.X && !placed.getValue(RoundWindowBlock.WATERLOGGED),
                "window placed between neighbours has the wrong state: " + placed);
        check(placed.getProperties().size() == 2, "window has connector or extra properties: " + placed.getProperties());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void waterloggingBucketsAndWaterRestoration(GameTestHelper helper) {
        floor(helper);
        BlockPos pos = new BlockPos(4, 1, 4);
        helper.setBlock(pos, Blocks.WATER);
        // Placing into a source block waterlogs.
        ServerPlayer player = player(helper, "water", GameType.SURVIVAL, Direction.NORTH);
        check(useItemOn(helper, player, new ItemStack(window()), pos.below(), Direction.UP).consumesAction(), "placement into water failed");
        check(at(helper, pos).is(window()) && at(helper, pos).getValue(BlockStateProperties.WATERLOGGED), "not waterlogged in a source");
        // Bucket out, bucket in.
        BlockPos abs = helper.absolutePos(pos);
        ItemStack bucket = window().pickupBlock(helper.getLevel(), abs, at(helper, pos));
        check(bucket.is(Items.WATER_BUCKET) && !at(helper, pos).getValue(BlockStateProperties.WATERLOGGED), "bucket did not drain the window");
        check(window().placeLiquid(helper.getLevel(), abs, at(helper, pos), Fluids.WATER.getSource(false)), "bucket did not fill the window");
        check(at(helper, pos).getValue(BlockStateProperties.WATERLOGGED), "window not waterlogged after filling");
        // Breaking a waterlogged window leaves the water and drops the window (no Silk Touch needed).
        clearDrops(helper);
        player.gameMode.destroyBlock(abs);
        check(helper.getLevel().getFluidState(abs).isSource() && helper.getLevel().getFluidState(abs).is(Fluids.WATER), "water not restored");
        check(dropped(helper, window().asItem()) == 1, "window did not drop itself: " + dropped(helper, window().asItem()));
        helper.setBlock(pos, Blocks.AIR);
        helper.succeed();
    }
}
