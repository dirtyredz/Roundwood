package io.github.dirtyredz.roundwood.block.door.frame.fitting;

import io.github.dirtyredz.roundwood.block.door.frame.DoorArchStore;
import io.github.dirtyredz.roundwood.block.door.frame.DoorArchFitPlan;
import io.github.dirtyredz.roundwood.block.door.frame.DoorArchClearance;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.hardware.*;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPiecePlacement;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.fixture.AppearanceFixtures;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.*;

/** Reproduction through the ordinary server click path, including block interaction. */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorArchFittingGameTests {
    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void thinGroundFitsRefuseOnlyTheEqualClosedPlaneInBothOrders(GameTestHelper helper) {
        floor(helper);
        var level = helper.getLevel();
        var thickness = io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness.THIN;
        for (var block : ModBlocks.doors()) for (var leafDepth : io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth.values())
            for (var slabDepth : io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth.values())
                for (boolean intoArch : new boolean[]{true, false}) {
            var anchor = helper.absolutePos(new BlockPos(3, 1, 3));
            var door = new io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout.Assembly(anchor, Direction.NORTH,
                    net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT, block.design(), leafDepth);
            var arch = new ArchPieceLayout.Assembly(anchor.above(DoorArchFitPlan.topOffset(block.design())), Direction.NORTH,
                    DoorArchFitPlan.topFor(block.design()), Side.LEFT, thickness, slabDepth);
            var archBlock = ModBlocks.archPiece(arch.design(), thickness);
            if (intoArch) check(ArchPiecePlacement.placeAt(level, archBlock, arch, Appearance.DEFAULT), "thin fixture");
            else check(io.github.dirtyredz.roundwood.block.door.assembly.DoorPlacement.placeAt(level, block, door, false), "door fixture");
            var player = player(helper, "thin_ground", GameType.SURVIVAL, Direction.NORTH);
            var stack = new ItemStack(intoArch ? block.asItem() : archBlock.asItem(), 2);
            int band = intoArch ? leafDepth.ordinal() : slabDepth.ordinal();
            var hit = new BlockHitResult(point(door, .5, 0, .125 + band * .375), Direction.UP, anchor.below(), false);
            hold(player, stack, ItemStack.EMPTY);
            var context = new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            check(DoorArchFitting.claims(context), "thin ground discovery");
            var preview = DoorArchFitting.propose(context);
            boolean allowed = !leafDepth.name().equals(slabDepth.name());
            check(preview.isPresent() == allowed, "thin preview must refuse exactly the equal plane");
            AppearanceFixtures.use(helper, player, stack, ItemStack.EMPTY, hit, false);
            check(stack.getCount() == (allowed ? 1 : 2), "thin payment/refusal");
            if (allowed) {
                check(preview.get().fit().door().depth() == leafDepth, "thin fit moved leaf");
                for (var entry : preview.get().states().entrySet()) check(level.getBlockState(entry.getKey()).equals(entry.getValue()), "thin preview != result");
            } else check(intoArch ? arch.complete(level, archBlock) : door.complete(level, block), "refused thin fit mutated source");
            level.removeBlock(intoArch && !allowed ? arch.anchor() : anchor, false); clearDrops(helper);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void groundClickDiscoversTopAndPreservesPreviewDepth(GameTestHelper helper) {
        floor(helper);
        var level = helper.getLevel();
        for (var block : ModBlocks.doors()) for (var facing : Direction.Plane.HORIZONTAL)
            for (var depth : io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth.values())
                for (boolean intoArch : new boolean[]{true, false}) {
            var anchor = helper.absolutePos(new BlockPos(4, 1, 4));
            var door = new io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout.Assembly(anchor, facing,
                    net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT, block.design(), depth);
            var top = DoorArchFitPlan.fromDoor(door, io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness.FULL,
                    io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth.FRONT, false).orElseThrow().arch();
            if (intoArch) check(ArchPiecePlacement.placeAt(level, ModBlocks.archPiece(top.design()), top, Appearance.DEFAULT), "top fixture");
            else check(io.github.dirtyredz.roundwood.block.door.assembly.DoorPlacement.placeAt(level, block, door, false), "door fixture");
            var player = player(helper, "ground_fit", GameType.SURVIVAL, facing);
            var stack = new ItemStack(intoArch ? block.asItem() : ModBlocks.archPiece(top.design()).asItem(), 2);
            var hit = new BlockHitResult(point(door, .5, 0, .125 + depth.ordinal() * .375), Direction.UP, anchor.below(), false);
            hold(player, stack, ItemStack.EMPTY);
            var context = new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            check(DoorArchFitting.claims(context), block.design() + ": ground fit not claimed");
            var preview = DoorArchFitting.propose(context).orElseThrow();
            check(preview.fit().door().depth() == depth, "ground aim changed");
            AppearanceFixtures.use(helper, player, stack, ItemStack.EMPTY, hit, false);
            for (var entry : preview.states().entrySet()) check(level.getBlockState(entry.getKey()).equals(entry.getValue()), "ground preview != result");
            check(stack.getCount() == 1, "ground payment");
            level.removeBlock(anchor, false); clearDrops(helper);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void doorFitsExistingTopDespiteOccupiedNoncollidingCell(GameTestHelper helper) {
        floor(helper);
        var level = helper.getLevel();
        var anchor = helper.absolutePos(new BlockPos(3, 1, 3));
        var top = new ArchPieceLayout.Assembly(anchor.above(), Direction.NORTH,
                ArchPieceDesign.TOP_1X1, Side.LEFT);
        var block = ModBlocks.archPiece(top.design());
        check(ArchPiecePlacement.placeAt(level, block, top, Appearance.DEFAULT), "top fixture refused");
        check(level.getBlockState(top.anchor()).getCollisionShape(level, top.anchor()).isEmpty(),
                "reproduction requires empty top collision");
        var player = player(helper, "arch_fit", GameType.SURVIVAL, Direction.NORTH);
        var stack = new ItemStack(ModBlocks.archedDoor().asItem(), 2);
        // A solid crown pixel on the front face; the ordinary click must derive the lower door anchor.
        var hit = new BlockHitResult(Vec3.atLowerCornerOf(top.anchor()).add(.03125, .96875, 1),
                Direction.SOUTH, top.anchor(), false);
        AppearanceFixtures.use(helper, player, stack, ItemStack.EMPTY, hit, false);
        check(level.getBlockState(anchor).is(ModBlocks.archedDoor()),
                "matching top with empty collision rejected ordinary door fitting");
        check(stack.getCount() == 1, "fitting must consume exactly one door");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void bothOrdersSharePreviewStatesAndPreserveIndependentCustody(GameTestHelper helper) {
        floor(helper); var level = helper.getLevel();
        for (var doorBlock : ModBlocks.doors()) for (var thickness : io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness.values())
            for (var depth : io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth.values()) {
                if (!DoorArchClearance.accepts(thickness, depth, io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth.BACK)) continue;
                for (var facing : Direction.Plane.HORIZONTAL) for (boolean intoArch : new boolean[]{true,false}) {
                    var door = new io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout.Assembly(helper.absolutePos(new BlockPos(4,1,4)), facing,
                            net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT, doorBlock.design(), io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth.BACK);
                    var fit = DoorArchFitPlan.fromDoor(door, thickness, depth, false).orElseThrow(); var arch = fit.arch();
                    var archBlock = ModBlocks.archPiece(arch.design(), thickness);
                    if (intoArch) check(ArchPiecePlacement.placeAt(level, archBlock, arch, AppearanceFixtures.BRICKS), "arch fixture");
                    else check(io.github.dirtyredz.roundwood.block.door.assembly.DoorPlacement.placeAt(level, doorBlock, door, false, AppearanceFixtures.WARPED), "door fixture");
                    if (!intoArch) anchorEntity(helper,door).setHardware(anchorEntity(helper,door).hardware().with(DoorHardwareSlot.HINGES,java.util.Optional.of(AppearanceFixtures.SPRUCE)));
                    var player = player(helper, "fit_orders", GameType.SURVIVAL, facing);
                    var stack = styled(intoArch ? doorBlock.asItem() : archBlock.asItem(), intoArch ? AppearanceFixtures.WARPED : AppearanceFixtures.BRICKS); stack.setCount(2);
                    if(intoArch) stack.getOrCreateTag().put(DoorHardwareStore.TAG,DoorHardwareStore.encode(DoorHardware.DEFAULT.with(DoorHardwareSlot.HINGES,java.util.Optional.of(AppearanceFixtures.SPRUCE))));
                    var hit = intoArch ? new BlockHitResult(Vec3.atCenterOf(arch.anchor()).relative(facing.getOpposite(), .5).add(0,-.375,0), facing.getOpposite(),arch.anchor(),false)
                            : frontHit(door, .5, depth == io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth.MIDDLE ? .5 : .875);
                    boolean offhand = facing == Direction.SOUTH;
                    var hand = offhand ? net.minecraft.world.InteractionHand.OFF_HAND : net.minecraft.world.InteractionHand.MAIN_HAND;
                    hold(player,offhand ? ItemStack.EMPTY : stack,offhand ? stack : ItemStack.EMPTY);
                    var context = new net.minecraft.world.item.context.UseOnContext(player,hand,hit);
                    var preview = DoorArchFitting.propose(context).orElseThrow();
                    AppearanceFixtures.use(helper,player,offhand ? ItemStack.EMPTY : stack,offhand ? stack : ItemStack.EMPTY,hit,false);
                    check(stack.getCount()==1,"one payment");
                    for(var entry : preview.states().entrySet()) check(level.getBlockState(entry.getKey()).equals(entry.getValue()),"preview != result");
                    var entity=anchorEntity(helper,preview.fit().door());
                    check(entity.hardware().hinges().equals(java.util.Optional.of(AppearanceFixtures.SPRUCE)),"fitting lost hardware");
                    check(entity.appearance().equals(AppearanceFixtures.WARPED) && entity.arch().orElseThrow().appearance().equals(AppearanceFixtures.BRICKS),"independent looks");
                    check(preview.fit().door().depth()==io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth.BACK,"fitted pose");
                    level.removeBlock(door.anchor(),false); clearDrops(helper);
                }
            }
        helper.succeed();
    }
    @GameTest(template = TEMPLATE)
    public static void cancelledFittingRestoresFullNbtInBothOrdersIncludingUnchangedStates(GameTestHelper helper) {
        floor(helper); var level=helper.getLevel(); var anchor=helper.absolutePos(new BlockPos(3,1,3));
        for(boolean intoArch : new boolean[]{true,false}) {
            var door=new io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout.Assembly(anchor,Direction.NORTH,
                    net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT,io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign.ARCHED,
                    io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth.BACK);
            var arch=DoorArchFitPlan.fromDoor(door,io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness.FULL,
                    io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth.FRONT,false).orElseThrow().arch();
            if(intoArch) check(ArchPiecePlacement.placeAt(level,ModBlocks.archPiece(arch.design()),arch,AppearanceFixtures.BRICKS),"fixture");
            else check(io.github.dirtyredz.roundwood.block.door.assembly.DoorPlacement.placeAt(level,ModBlocks.archedDoor(),door,false,AppearanceFixtures.WARPED),"fixture");
            if (!intoArch) anchorEntity(helper, door).setCamouflage(io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageData.paid(
                    net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), new ItemStack(net.minecraft.world.item.Items.STONE)));
            if(!intoArch) anchorEntity(helper,door).setHardware(anchorEntity(helper,door).hardware().with(DoorHardwareSlot.KNOBS,java.util.Optional.of(AppearanceFixtures.BIRCH)));
            var owner=intoArch?arch.anchor():door.anchor(); var before=level.getBlockEntity(owner).saveWithFullMetadata();
            var states=door.positions().stream().map(level::getBlockState).toList();
            var player=player(helper,"cancelled",GameType.SURVIVAL,Direction.NORTH);
            var stack=new ItemStack(intoArch?ModBlocks.archedDoor().asItem():ModBlocks.archPiece(arch.design()).asItem(),2);
            if(intoArch) stack.getOrCreateTag().put(DoorHardwareStore.TAG,DoorHardwareStore.encode(DoorHardware.DEFAULT.with(DoorHardwareSlot.KNOBS,java.util.Optional.of(AppearanceFixtures.BIRCH))));
            var stackBefore=stack.copy();
            var hit=new BlockHitResult(Vec3.atLowerCornerOf(owner).add(.03,.95,1),Direction.SOUTH,owner,false);
            int events=io.github.dirtyredz.roundwood.fixture.PlacementCanceller.multiPlaceEvents();
            AppearanceFixtures.use(helper,player,stack,ItemStack.EMPTY,hit,false);
            check(io.github.dirtyredz.roundwood.fixture.PlacementCanceller.multiPlaceEvents()==events+1,"expected one cancellable event: intoArch="+intoArch+" before="+events+" after="+io.github.dirtyredz.roundwood.fixture.PlacementCanceller.multiPlaceEvents());
            check(ItemStack.matches(stack,stackBefore),"cancelled hardware/payment");
            check(level.getBlockEntity(owner).saveWithFullMetadata().equals(before),"full NBT rollback");
            check(door.positions().stream().map(level::getBlockState).toList().equals(states),"state rollback");
            level.removeBlock(owner,false); clearDrops(helper);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void refusedPermissionsUnknownPayloadAndCoplanarDepthLeaveEverythingUntouched(GameTestHelper helper) {
        floor(helper); var level=helper.getLevel();
        var door=new io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout.Assembly(helper.absolutePos(new BlockPos(3,1,3)),Direction.NORTH,
                net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT,io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign.ROUND);
        check(io.github.dirtyredz.roundwood.block.door.assembly.DoorPlacement.placeAt(level,ModBlocks.roundDoor(),door,false),"fixture");
        var entity=anchorEntity(helper,door);
        for(var denied : door.positions()) {
            var player=io.github.dirtyredz.roundwood.fixture.assembly.DenyingPlayer.create(helper,"fit",Direction.NORTH,java.util.Set.of(denied));
            var stack=new ItemStack(ModBlocks.archPiece(ArchPieceDesign.TOP_1X2).asItem(),2);
            var before=entity.saveWithFullMetadata();
            AppearanceFixtures.use(helper,player,stack,ItemStack.EMPTY,frontHit(door,1,.8),false);
            check(stack.getCount()==2 && entity.saveWithFullMetadata().equals(before) && door.complete(level,ModBlocks.roundDoor()),"denied cell changed assembly");
        }
        var player=player(helper,"fit_refuse",GameType.SURVIVAL,Direction.NORTH);
        var thin=new ItemStack(ModBlocks.archPiece(ArchPieceDesign.TOP_1X2,io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness.THIN).asItem(),2);
        AppearanceFixtures.use(helper,player,thin,ItemStack.EMPTY,frontHit(door,1,.875),false);
        check(thin.getCount()==2 && entity.arch().isEmpty(),"coplanar fit accepted");
        var unknown=entity.saveWithFullMetadata(); var payload=new net.minecraft.nbt.CompoundTag();payload.putInt("Version",99);payload.putString("Unknown","kept");unknown.put(DoorArchStore.TAG,payload);entity.load(unknown);
        var full=new ItemStack(ModBlocks.archPiece(ArchPieceDesign.TOP_1X2).asItem(),2);
        AppearanceFixtures.use(helper,player,full,ItemStack.EMPTY,frontHit(door,1,.8),false);
        check(full.getCount()==2 && entity.saveWithFullMetadata().equals(unknown),"unknown attachment overwritten");
        helper.succeed();
    }
}
