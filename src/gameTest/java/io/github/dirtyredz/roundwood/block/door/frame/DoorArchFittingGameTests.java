package io.github.dirtyredz.roundwood.block.door.frame;

import io.github.dirtyredz.roundwood.Roundwood;
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

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.*;

/** Reproduction through the ordinary server click path, including block interaction. */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorArchFittingGameTests {
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
                if (!DoorArchClearance.accepts(doorBlock.design(), thickness, depth)) continue;
                for (var facing : Direction.Plane.HORIZONTAL) for (boolean intoArch : new boolean[]{true,false}) {
                    var door = new io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout.Assembly(helper.absolutePos(new BlockPos(4,1,4)), facing,
                            net.minecraft.world.level.block.state.properties.DoorHingeSide.LEFT, doorBlock.design());
                    var fit = DoorArchFitPlan.fromDoor(door, thickness, depth, false).orElseThrow(); var arch = fit.arch();
                    var archBlock = ModBlocks.archPiece(arch.design(), thickness);
                    if (intoArch) check(ArchPiecePlacement.placeAt(level, archBlock, arch, AppearanceFixtures.BRICKS), "arch fixture");
                    else check(io.github.dirtyredz.roundwood.block.door.assembly.DoorPlacement.placeAt(level, doorBlock, door, false, AppearanceFixtures.WARPED), "door fixture");
                    var player = player(helper, "fit_orders", GameType.SURVIVAL, facing);
                    var stack = styled(intoArch ? doorBlock.asItem() : archBlock.asItem(), intoArch ? AppearanceFixtures.WARPED : AppearanceFixtures.BRICKS); stack.setCount(2);
                    var hit = intoArch ? new BlockHitResult(Vec3.atCenterOf(arch.anchor()).relative(facing.getOpposite(), .5).add(0,.45,0), facing.getOpposite(),arch.anchor(),false)
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
            var owner=intoArch?arch.anchor():door.anchor(); var before=level.getBlockEntity(owner).saveWithFullMetadata();
            var states=door.positions().stream().map(level::getBlockState).toList();
            var player=player(helper,"cancelled",GameType.SURVIVAL,Direction.NORTH);
            var stack=new ItemStack(intoArch?ModBlocks.archedDoor().asItem():ModBlocks.archPiece(arch.design()).asItem(),2);
            var hit=new BlockHitResult(Vec3.atLowerCornerOf(owner).add(.03,.95,1),Direction.SOUTH,owner,false);
            int events=io.github.dirtyredz.roundwood.fixture.PlacementCanceller.multiPlaceEvents();
            AppearanceFixtures.use(helper,player,stack,ItemStack.EMPTY,hit,false);
            check(io.github.dirtyredz.roundwood.fixture.PlacementCanceller.multiPlaceEvents()==events+1,"expected one cancellable event: intoArch="+intoArch+" before="+events+" after="+io.github.dirtyredz.roundwood.fixture.PlacementCanceller.multiPlaceEvents());
            check(stack.getCount()==2,"cancelled payment");
            check(level.getBlockEntity(owner).saveWithFullMetadata().equals(before),"full NBT rollback");
            check(door.positions().stream().map(level::getBlockState).toList().equals(states),"state rollback");
            level.removeBlock(owner,false); clearDrops(helper);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void refusedPermissionsUnknownPayloadAndBackDepthLeaveEverythingUntouched(GameTestHelper helper) {
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
        AppearanceFixtures.use(helper,player,thin,ItemStack.EMPTY,frontHit(door,1,.2),false);
        check(thin.getCount()==2 && entity.arch().isEmpty(),"BACK fit accepted");
        var unknown=entity.saveWithFullMetadata(); var payload=new net.minecraft.nbt.CompoundTag();payload.putInt("Version",99);payload.putString("Unknown","kept");unknown.put(DoorArchStore.TAG,payload);entity.load(unknown);
        var full=new ItemStack(ModBlocks.archPiece(ArchPieceDesign.TOP_1X2).asItem(),2);
        AppearanceFixtures.use(helper,player,full,ItemStack.EMPTY,frontHit(door,1,.8),false);
        check(full.getCount()==2 && entity.saveWithFullMetadata().equals(unknown),"unknown attachment overwritten");
        helper.succeed();
    }
}
