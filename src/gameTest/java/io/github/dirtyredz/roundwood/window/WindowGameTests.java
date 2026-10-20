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
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.clearDrops;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.dropped;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.useItemOn;

/** Round windows: orientation, round collision, pane connections and water. */
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
    public static void collisionIsRound(GameTestHelper helper) {
        floor(helper);
        place(helper, "round", Direction.NORTH, new ItemStack(window()), new BlockPos(4, 1, 4));
        BlockPos abs = helper.absolutePos(new BlockPos(4, 1, 4));
        AABB centre = new AABB(abs.getX() + 0.45, abs.getY() + 0.45, abs.getZ() + 0.4, abs.getX() + 0.55, abs.getY() + 0.55, abs.getZ() + 0.6);
        AABB corner = new AABB(abs.getX() + 0.0, abs.getY() + 0.0, abs.getZ() + 0.4, abs.getX() + 0.08, abs.getY() + 0.08, abs.getZ() + 0.6);
        check(!helper.getLevel().noCollision(centre), "window centre should collide");
        check(helper.getLevel().noCollision(corner), "unconnected corner should be open");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void panesAndWindowsConnectReciprocallyInBothOrders(GameTestHelper helper) {
        floor(helper);
        // Window first, then a pane to its east (in plane).
        place(helper, "order_a1", Direction.NORTH, new ItemStack(window()), new BlockPos(1, 1, 1));
        place(helper, "order_a2", Direction.NORTH, new ItemStack(Items.GLASS_PANE), new BlockPos(2, 1, 1));
        check(at(helper, new BlockPos(1, 1, 1)).getValue(IronBarsBlock.EAST), "window did not connect east to the pane");
        check(at(helper, new BlockPos(2, 1, 1)).getValue(IronBarsBlock.WEST), "pane did not connect west to the window");
        // Pane first, then the window.
        place(helper, "order_b1", Direction.NORTH, new ItemStack(Items.GLASS_PANE), new BlockPos(5, 1, 1));
        place(helper, "order_b2", Direction.NORTH, new ItemStack(window()), new BlockPos(6, 1, 1));
        check(at(helper, new BlockPos(6, 1, 1)).getValue(IronBarsBlock.WEST), "window did not connect west to the pane");
        check(at(helper, new BlockPos(5, 1, 1)).getValue(IronBarsBlock.EAST), "pane did not connect east to the window");
        // Two windows side by side.
        place(helper, "order_c1", Direction.NORTH, new ItemStack(window()), new BlockPos(1, 1, 7));
        place(helper, "order_c2", Direction.NORTH, new ItemStack(window()), new BlockPos(2, 1, 7));
        check(at(helper, new BlockPos(1, 1, 7)).getValue(IronBarsBlock.EAST) && at(helper, new BlockPos(2, 1, 7)).getValue(IronBarsBlock.WEST),
                "adjacent windows did not connect");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void perpendicularPaneFormsAJunctionAndRemovalDisconnects(GameTestHelper helper) {
        floor(helper);
        BlockPos windowPos = new BlockPos(4, 1, 4);
        place(helper, "junction_w", Direction.NORTH, new ItemStack(window()), windowPos); // axis x
        place(helper, "junction_p", Direction.EAST, new ItemStack(Items.GLASS_PANE), windowPos.north());
        check(at(helper, windowPos).getValue(IronBarsBlock.NORTH), "window did not join the perpendicular pane");
        // The arm adds collision between the north edge and the window plane.
        BlockPos abs = helper.absolutePos(windowPos);
        AABB arm = new AABB(abs.getX() + 0.45, abs.getY() + 0.1, abs.getZ() + 0.05, abs.getX() + 0.55, abs.getY() + 0.2, abs.getZ() + 0.3);
        check(!helper.getLevel().noCollision(arm), "junction arm has no collision");
        helper.setBlock(windowPos.north(), Blocks.AIR);
        check(!at(helper, windowPos).getValue(IronBarsBlock.NORTH), "connection survived removing the pane");
        check(helper.getLevel().noCollision(arm), "arm collision survived removing the pane");
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
