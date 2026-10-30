package io.github.dirtyredz.roundwood.block.door.interaction;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.adapter.DoorUseChannel;
import io.github.dirtyredz.roundwood.block.door.adapter.DoorUsePayload;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.frontHit;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.hold;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.oakDoor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.player;

/**
 * The serverbound {@code roundwood:door_use} payload (protocol 2): its wire codec keeps the whole real hit, and
 * the server refuses every request the projected-leaf rules reject without touching the world or the inventory.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorUsePayloadGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);

    private DoorUsePayloadGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void theWireCodecRoundTripsTheRealHit(GameTestHelper helper) {
        check(DoorUsePayload.TYPE.id().toString().equals("roundwood:door_use") && DoorUseChannel.PROTOCOL.equals("2"), "payload identity");
        for (boolean inside : new boolean[]{false, true}) {
            var hit = new BlockHitResult(new Vec3(12.25, 64.0625, -3.875), Direction.WEST, new BlockPos(12, 64, -4), inside);
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            DoorUsePayload.STREAM_CODEC.encode(buffer, new DoorUsePayload(hit));
            var decoded = DoorUsePayload.STREAM_CODEC.decode(buffer).hit();
            check(decoded.getBlockPos().equals(hit.getBlockPos()) && decoded.getDirection() == hit.getDirection()
                    && decoded.isInside() == inside && decoded.getLocation().distanceToSqr(hit.getLocation()) < 1e-10
                    && buffer.readableBytes() == 0, "decoded " + decoded.getLocation() + " " + decoded.getDirection());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void invalidProjectedRequestsChangeNothing(GameTestHelper helper) {
        floor(helper);
        ServerPlayer placer = player(helper, "payload_placer", GameType.SURVIVAL, Direction.NORTH);
        check(placeDoor(helper, placer, oakDoor().asItem(), ANCHOR, DoorHingeSide.LEFT).consumesAction(), "placement failed");
        DoorLayout.Assembly door = assembly(helper, ANCHOR, Direction.NORTH);
        BlockHitResult real = frontHit(door, 1.0, 1.0);
        helper.setBlock(new BlockPos(7, 1, 3), Blocks.STONE);
        BlockPos stone = helper.absolutePos(new BlockPos(7, 1, 3));
        List<BlockHitResult> requests = new ArrayList<>();
        requests.add(new BlockHitResult(new Vec3(Double.NaN, real.getLocation().y, real.getLocation().z), real.getDirection(), real.getBlockPos(), false));
        requests.add(new BlockHitResult(Vec3.atCenterOf(stone), Direction.SOUTH, stone, false));
        requests.add(new BlockHitResult(real.getLocation().add(0, 0, -0.75), real.getDirection(), real.getBlockPos(), false));
        List<BlockState> before = door.positions().stream().map(pos -> helper.getLevel().getBlockState(pos)).toList();
        ServerPlayer near = player(helper, "payload_near", GameType.SURVIVAL, Direction.NORTH);
        ServerPlayer far = player(helper, "payload_far", GameType.SURVIVAL, Direction.NORTH);
        far.setPos(far.getX(), far.getY(), far.getZ() + 40);
        for (ServerPlayer player : List.of(near, far)) {
            ItemStack planks = new ItemStack(Items.SPRUCE_PLANKS, 3);
            hold(player, planks, ItemStack.EMPTY);
            List<BlockHitResult> tried = new ArrayList<>(requests);
            if (player == far) tried.add(real); // a real hit out of reach
            for (BlockHitResult request : tried) {
                InteractionResult result = ProjectedDoorUse.use(player, request);
                check(result == InteractionResult.FAIL, player.getGameProfile().getName() + ": " + request.getLocation() + " was accepted as " + result);
            }
            check(planks.getCount() == 3, "a refused request spent the held stack");
        }
        List<BlockState> after = door.positions().stream().map(pos -> helper.getLevel().getBlockState(pos)).toList();
        check(before.equals(after), "a refused request changed the door");
        helper.succeed();
    }
}
