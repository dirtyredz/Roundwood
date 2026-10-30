package io.github.dirtyredz.roundwood.block.door.camouflage;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorColumn;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.STRIP;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.anchorEntity;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.assembly;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.assertOneAnchorEntity;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.oakDoor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.placeDoor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.player;

/** Camouflage persistence, client sync and structure copying on the anchor block entity. */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CamouflageLifecycleGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);

    private CamouflageLifecycleGameTests() {
    }

    private static CamouflageData paidStone() {
        return CamouflageData.paid(Blocks.STONE.defaultBlockState(), new ItemStack(Items.STONE));
    }

    /** Places a door on a fresh floor and returns its anchor entity. */
    private static RoundDoorBlockEntity placed(GameTestHelper helper, String name) {
        floor(helper);
        ServerPlayer player = player(helper, name, GameType.SURVIVAL, Direction.NORTH);
        check(placeDoor(helper, player, oakDoor().asItem(), ANCHOR, DoorHingeSide.LEFT).consumesAction(), "placement failed");
        return anchorEntity(helper, assembly(helper, ANCHOR, Direction.NORTH));
    }

    /** A fresh entity standing in for a client's copy of the anchor. */
    private static RoundDoorBlockEntity clientCopy(RoundDoorBlockEntity entity) {
        return new RoundDoorBlockEntity(ModBlockEntities.ROUND_DOOR.get(), entity.getBlockPos(), entity.getBlockState());
    }

    /** The entity's saved tag with its Camouflage compound replaced. */
    private static CompoundTag withCamouflage(RoundDoorBlockEntity entity, CompoundTag camouflage) {
        CompoundTag tag = entity.saveWithFullMetadata();
        tag.put(CamouflageData.TAG, camouflage);
        return tag;
    }

    private static CompoundTag camouflageTag(String material, ItemStack refund) {
        CompoundTag state = new CompoundTag();
        state.putString("Name", material);
        CompoundTag tag = new CompoundTag();
        tag.put("Material", state);
        if (!refund.isEmpty()) {
            tag.put("Refund", refund.save(new CompoundTag()));
        }
        return tag;
    }

    private static void sendUpdate(RoundDoorBlockEntity server, RoundDoorBlockEntity client) {
        client.onDataPacket(null, ClientboundBlockEntityDataPacket.create(server));
    }

    @GameTest(template = TEMPLATE)
    public static void camouflageSurvivesASaveLoadRoundTrip(GameTestHelper helper) {
        RoundDoorBlockEntity entity = placed(helper, "camo_roundtrip");
        entity.setCamouflage(paidStone());
        BlockEntity copy = BlockEntity.loadStatic(entity.getBlockPos(), entity.getBlockState(), entity.saveWithFullMetadata());
        check(copy instanceof RoundDoorBlockEntity, "saved entity did not load back: " + copy);
        CamouflageData data = ((RoundDoorBlockEntity) copy).camouflage().orElse(null);
        check(data != null, "camouflage lost across the round trip");
        check(data.material() == Blocks.STONE.defaultBlockState(), "material changed: " + data.material());
        check(data.paid() && data.refund().is(Items.STONE) && data.refund().getCount() == 1, "refund changed: " + data.refund());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void aTagWithoutCamouflageLoadsAsNone(GameTestHelper helper) {
        RoundDoorBlockEntity entity = placed(helper, "camo_absent");
        entity.setCamouflage(paidStone());
        CompoundTag tag = entity.saveWithFullMetadata();
        tag.remove(CamouflageData.TAG);
        entity.load(tag);
        check(entity.camouflage().isEmpty(), "a tag without Camouflage kept the old camouflage");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void unknownOrInvalidMaterialsLoadAsNone(GameTestHelper helper) {
        RoundDoorBlockEntity entity = placed(helper, "camo_badmaterial");
        for (String material : new String[] {"minecraft:nope", "minecraft:glass"}) {
            entity.load(withCamouflage(entity, camouflageTag(material, ItemStack.EMPTY)));
            check(entity.camouflage().isEmpty(), material + " loaded as a camouflage");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void aRefundThatDoesNotMatchTheMaterialIsDropped(GameTestHelper helper) {
        RoundDoorBlockEntity entity = placed(helper, "camo_badrefund");
        entity.load(withCamouflage(entity, camouflageTag("minecraft:stone", new ItemStack(Items.DIAMOND))));
        CamouflageData data = entity.camouflage().orElse(null);
        check(data != null && data.material() == Blocks.STONE.defaultBlockState(), "valid material was not loaded");
        check(!data.paid(), "a mismatched refund was kept: " + data.refund());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void updateTagCarriesTheMaterialOnly(GameTestHelper helper) {
        RoundDoorBlockEntity entity = placed(helper, "camo_updatetag");
        CompoundTag empty = entity.getUpdateTag();
        check(empty.contains(CamouflageData.TAG) && empty.getCompound(CamouflageData.TAG).isEmpty(),
                "an uncamouflaged update tag must carry an empty Camouflage compound: " + empty);
        entity.setCamouflage(paidStone());
        CompoundTag camouflage = entity.getUpdateTag().getCompound(CamouflageData.TAG);
        check(camouflage.contains("Material"), "update tag has no material");
        check(!camouflage.contains("Refund"), "update tag leaks the refund: " + camouflage);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void updatePacketGivesAClientTheMaterialButNoRefund(GameTestHelper helper) {
        RoundDoorBlockEntity entity = placed(helper, "camo_packet");
        entity.setCamouflage(paidStone());
        RoundDoorBlockEntity client = clientCopy(entity);
        sendUpdate(entity, client);
        CamouflageData data = client.camouflage().orElse(null);
        check(data != null && data.material() == Blocks.STONE.defaultBlockState(), "client did not receive the material");
        check(!data.paid(), "client received the refund");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void clearingCamouflagePropagatesToAClientCopy(GameTestHelper helper) {
        RoundDoorBlockEntity entity = placed(helper, "camo_clear");
        entity.setCamouflage(paidStone());
        RoundDoorBlockEntity client = clientCopy(entity);
        sendUpdate(entity, client);
        check(client.camouflage().isPresent(), "client copy never got the material");
        entity.setCamouflage(null);
        sendUpdate(entity, client);
        check(client.camouflage().isEmpty(), "client copy kept the camouflage after a clear");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void replacingTheMaterialNeverShowsTheOldOne(GameTestHelper helper) {
        RoundDoorBlockEntity entity = placed(helper, "camo_replace");
        entity.setCamouflage(paidStone());
        BlockState bricks = Blocks.BRICKS.defaultBlockState();
        entity.setCamouflage(CamouflageData.paid(bricks, new ItemStack(Items.BRICKS)));
        entity.load(entity.saveWithFullMetadata());
        check(entity.camouflage().map(CamouflageData::material).orElse(null) == bricks, "reload did not show the new material");
        RoundDoorBlockEntity client = clientCopy(entity);
        sendUpdate(entity, client);
        check(client.camouflage().map(CamouflageData::material).orElse(null) == bricks, "client did not show the new material");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void loadingDoesNotResetTheSwing(GameTestHelper helper) {
        RoundDoorBlockEntity placed = placed(helper, "camo_swing");
        BlockPos anchor = placed.getBlockPos();
        oakDoor().setOpen(null, helper.getLevel(), helper.getLevel().getBlockState(anchor), anchor, true);
        RoundDoorBlockEntity open = anchorEntity(helper, assembly(helper, ANCHOR, Direction.NORTH));
        double now = 1000.0;
        double before = open.swing().progress(now);
        open.setCamouflage(paidStone());
        open.load(open.saveWithFullMetadata());
        check(open.swing().progress(now) == before, "load changed the swing: " + before + " -> " + open.swing().progress(now));
        RoundDoorBlockEntity client = clientCopy(open);
        double fresh = client.swing().progress(now);
        sendUpdate(open, client);
        check(client.swing().progress(now) == fresh, "a camouflage packet changed the client's swing");
        helper.succeed();
    }

    @GameTest(template = STRIP)
    public static void everyPartResolvesTheSameAnchorAcrossAChunkBoundary(GameTestHelper helper) {
        floor(helper, 17, 4);
        int x = 0;
        while (((helper.absolutePos(new BlockPos(x, 0, 0)).getX()) & 15) != 15) {
            x++;
        }
        BlockPos anchor = new BlockPos(x, 1, 1);
        DoorLayout.Assembly assembly = assembly(helper, anchor, Direction.NORTH);
        check((assembly.pos(DoorLayout.Part.LOWER_LEFT).getX() >> 4) != (assembly.pos(DoorLayout.Part.LOWER_RIGHT).getX() >> 4),
                "test setup does not straddle chunks");
        ServerPlayer player = player(helper, "camo_chunks", GameType.SURVIVAL, Direction.NORTH);
        check(placeDoor(helper, player, oakDoor().asItem(), anchor).consumesAction(), "placement failed");
        anchorEntity(helper, assembly).setCamouflage(paidStone());
        for (DoorLayout.Part part : DoorLayout.Part.of(io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign.ROUND)) {
            BlockPos pos = assembly.pos(part);
            BlockPos resolved = DoorLayout.of(pos, helper.getLevel().getBlockState(pos)).anchor();
            check(resolved.equals(assembly.anchor()), part + " resolves anchor " + resolved + ", expected " + assembly.anchor());
            check(helper.getLevel().getBlockEntity(resolved) instanceof RoundDoorBlockEntity found && found.camouflage().isPresent(),
                    part + ": the resolved anchor does not hold the camouflage");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void rotatedCopyKeepsTheCamouflage(GameTestHelper helper) {
        copied(helper, Rotation.CLOCKWISE_90, Mirror.NONE, true);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void mirroredCopyStartsWithoutCamouflage(GameTestHelper helper) {
        copied(helper, Rotation.NONE, Mirror.LEFT_RIGHT, false);
    }

    /** Copies a camouflaged door through a structure template under the transform and checks the new anchor. */
    private static void copied(GameTestHelper helper, Rotation rotation, Mirror mirror, boolean expectCamouflage) {
        floor(helper);
        String name = rotation + "/" + mirror;
        ServerPlayer player = player(helper, "camo_structure", GameType.SURVIVAL, Direction.NORTH);
        check(placeDoor(helper, player, oakDoor().asItem(), new BlockPos(1, 1, 1), DoorHingeSide.LEFT).consumesAction(),
                "source placement failed");
        anchorEntity(helper, assembly(helper, new BlockPos(1, 1, 1), Direction.NORTH)).setCamouflage(paidStone());
        ServerLevel level = helper.getLevel();
        BlockPos dest = helper.absolutePos(new BlockPos(5, 1, 5));
        StructureTemplate template = new StructureTemplate();
        template.fillFromWorld(level, helper.absolutePos(new BlockPos(0, 1, 0)), new Vec3i(3, 2, 3), false, Blocks.STRUCTURE_VOID);
        template.placeInWorld(level, dest, dest, new StructurePlaceSettings().setRotation(rotation).setMirror(mirror),
                level.getRandom(), Block.UPDATE_CLIENTS);
        DoorLayout.Assembly placed = findPlacedAssembly(helper, dest);
        check(placed != null, name + ": no placed door found");
        check(placed.complete(level, oakDoor()), name + ": placed assembly is incomplete");
        assertOneAnchorEntity(helper, placed);
        CamouflageData data = anchorEntity(helper, placed).camouflage().orElse(null);
        check((data != null) == expectCamouflage, name + ": camouflage present=" + (data != null) + ", expected " + expectCamouflage);
        check(data == null || data.material() == Blocks.STONE.defaultBlockState(), name + ": wrong material");
        helper.succeed();
    }

    /** The assembly whose left-column lower part lies in the 5 x 5 box around {@code dest}; the anchor may have moved. */
    private static DoorLayout.Assembly findPlacedAssembly(GameTestHelper helper, BlockPos dest) {
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                BlockPos pos = dest.offset(x, 0, z);
                BlockState state = helper.getLevel().getBlockState(pos);
                if (state.is(oakDoor()) && state.getValue(RoundDoorBlock.COLUMN) == DoorColumn.LEFT
                        && state.getValue(DoorBlock.HALF) == DoubleBlockHalf.LOWER) {
                    return DoorLayout.of(pos, state);
                }
            }
        }
        return null;
    }
}
