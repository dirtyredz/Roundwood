package io.github.dirtyredz.roundwood.block.door.frame;


import io.github.dirtyredz.roundwood.fixture.TestRegistries;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.arch.assembly.*;
import io.github.dirtyredz.roundwood.block.arch.geometry.*;
import io.github.dirtyredz.roundwood.block.attachment.*;
import io.github.dirtyredz.roundwood.block.door.assembly.*;
import io.github.dirtyredz.roundwood.block.door.adapter.*;
import io.github.dirtyredz.roundwood.fixture.AppearanceFixtures;
import io.github.dirtyredz.roundwood.registry.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.neoforged.neoforge.gametest.*;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.*;

@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorArchLifecycleGameTests {
    private static DoorLayout.Assembly fitted(GameTestHelper helper,AssemblyDoorBlock block,ArchPieceThickness thickness,ArchPieceDepth depth) {
        var door=new DoorLayout.Assembly(helper.absolutePos(new BlockPos(3,1,3)),Direction.NORTH,DoorHingeSide.LEFT,block.design(),DoorDepth.BACK);
        check(DoorPlacement.placeAt(helper.getLevel(),block,door,false,AppearanceFixtures.WARPED),"door fixture");
        var player=player(helper,"lifecycle_fit",GameType.SURVIVAL,Direction.NORTH);
        var arch=ModBlocks.archPiece(DoorArchFitPlan.topFor(block.design()),thickness);
        var stack=styled(arch.asItem(),AppearanceFixtures.BRICKS);
        AppearanceFixtures.use(helper,player,stack,ItemStack.EMPTY,frontHit(door,.5,depth==ArchPieceDepth.MIDDLE?.5:.875),false);
        check(stack.isEmpty(),"production fitting refused");
        var result=DoorLayout.of(door.anchor(),helper.getLevel().getBlockState(door.anchor()));
        var entity=anchorEntity(helper,result);
        var frame=entity.arch().orElseThrow();
        check(entity.setArch(frame.withCasings(FaceTrims.NONE.with(AttachmentFace.FRONT,AppearanceFixtures.BIRCH).with(AttachmentFace.BACK,AppearanceFixtures.SPRUCE))),"frame trim setup");
        check(entity.setTrims(FaceTrims.both(AppearanceFixtures.WARPED)),"door trim setup");
        clearDrops(helper); return result;
    }
    @GameTest(template=TEMPLATE,timeoutTicks=200)
    public static void everyOwnedCellDropsComponentsOnceWithIndependentStyles(GameTestHelper helper) {
        floor(helper);
        for(var block:ModBlocks.doors()) for(var thickness:ArchPieceThickness.values()) for(var depth:ArchPieceDepth.values()) {
            if(!DoorArchClearance.accepts(thickness,depth,io.github.dirtyredz.roundwood.block.door.assembly.DoorDepth.BACK)) continue;
            for(var part:DoorLayout.Part.of(block.design())) {
                var door=fitted(helper,block,thickness,depth);
                var entity=anchorEntity(helper,door);
                var loaded=net.minecraft.world.level.block.entity.BlockEntity.loadStatic(door.anchor(),entity.getBlockState(),entity.saveWithFullMetadata(TestRegistries.get()), TestRegistries.get());
                check(loaded instanceof RoundDoorBlockEntity copy && copy.arch().equals(entity.arch()) && copy.trims().equals(entity.trims()),"reload custody");
                var player=player(helper,"frame_break",GameType.SURVIVAL,Direction.NORTH);
                check(player.gameMode.destroyBlock(door.pos(part)),"harvest refused"); assertNoDoor(helper,door);
                check(dropped(helper,block.asItem(),AppearanceFixtures.WARPED)==1,"one styled door");
                check(dropped(helper,ModBlocks.archPiece(DoorArchFitPlan.topFor(block.design()),thickness).asItem(),AppearanceFixtures.BRICKS)==1,"one styled arch");
                check(dropped(helper,ModItems.ROUND_TRIM.get(),AppearanceFixtures.BIRCH)==1 && dropped(helper,ModItems.ROUND_TRIM.get(),AppearanceFixtures.SPRUCE)==1
                        && dropped(helper,ModItems.ROUND_TRIM.get(),AppearanceFixtures.WARPED)==2,"independent casing refunds");
                clearDrops(helper);
            }
        }
        helper.succeed();
    }
    @GameTest(template=TEMPLATE)
    public static void creativeDisabledDropsSupportAndExplosionUseOrdinaryLootRules(GameTestHelper helper) {
        var level=helper.getLevel(); var block=ModBlocks.archedDoor(); var arch=ModBlocks.archPiece(ArchPieceDesign.TOP_1X1).asItem();
        for(int mode=0;mode<4;mode++) {
            floor(helper);var door=fitted(helper,block,ArchPieceThickness.FULL,ArchPieceDepth.FRONT);
            var rule=level.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS); boolean previous=rule.get();
            try {
                if(mode==0) player(helper,"creative_frame",GameType.CREATIVE,Direction.NORTH).gameMode.destroyBlock(door.anchor().above());
                if(mode==1) { rule.set(false,level.getServer());player(helper,"nodrops_frame",GameType.SURVIVAL,Direction.NORTH).gameMode.destroyBlock(door.anchor()); }
                if(mode==2) level.setBlockAndUpdate(door.anchor().below(),Blocks.AIR.defaultBlockState());
                if(mode==3) level.explode(null,door.anchor().getX()+.5,door.anchor().getY()+1,door.anchor().getZ()+.5,3,Level.ExplosionInteraction.BLOCK);
                assertNoDoor(helper,door);
                int expected=mode<2?0:1;
                if(mode<3) check(dropped(helper,arch)==expected && dropped(helper,block.asItem())==expected,"ordinary loot rule "+mode);
                else check(dropped(helper,arch)<=1 && dropped(helper,block.asItem())<=1 && dropped(helper,ModItems.ROUND_TRIM.get())<=4,"explosion duplicated components");
            } finally { rule.set(previous,level.getServer()); }
            clearDrops(helper);
        }
        helper.succeed();
    }

    @GameTest(template=TEMPLATE)
    public static void frameHitsStylePickAndSelectFrameWhileLeafStillToggles(GameTestHelper helper) {
        floor(helper);var level=helper.getLevel();var door=fitted(helper,ModBlocks.archedDoor(),ArchPieceThickness.FULL,ArchPieceDepth.FRONT);
        var entity=anchorEntity(helper,door);var player=player(helper,"frame_use",GameType.SURVIVAL,Direction.NORTH);
        var origin=net.minecraft.world.phys.Vec3.atLowerCornerOf(door.anchor());
        var front=ray(helper,player,origin.add(.03125,1.96875,2),origin.add(.03125,1.96875,-1));
        check(DoorArchInteraction.hit(level,front.getBlockPos(),level.getBlockState(front.getBlockPos()),front).isPresent(),"frame ray classified as leaf");
        var picked=ModBlocks.archedDoor().getCloneItemStack(level.getBlockState(front.getBlockPos()),front,level,front.getBlockPos(),player);
        check(picked.is(ModBlocks.archPiece(ArchPieceDesign.TOP_1X1).asItem()) && io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks.read(picked).equals(AppearanceFixtures.BRICKS),"frame pick");
        var dye=new ItemStack(Items.RED_DYE,2);AppearanceFixtures.use(helper,player,dye,ItemStack.EMPTY,front,false);
        check(dye.getCount()==1 && entity.arch().orElseThrow().casings().front().orElseThrow().equals(AppearanceFixtures.BIRCH.painted(DyeColor.RED)),"front casing paint");
        check(entity.arch().orElseThrow().casings().back().orElseThrow().equals(AppearanceFixtures.SPRUCE) && entity.appearance().equals(AppearanceFixtures.WARPED),"restyle leaked to another component");
        hold(player,ItemStack.EMPTY,ItemStack.EMPTY);
        var top=ray(helper,player,origin.add(.03125,3,.5),origin.add(.03125,1,.5));
        var wood=new ItemStack(Items.SPRUCE_PLANKS,2);AppearanceFixtures.use(helper,player,wood,ItemStack.EMPTY,top,false);
        check(wood.getCount()==1 && entity.arch().orElseThrow().appearance().equals(AppearanceFixtures.SPRUCE),"body restyle");
        var leaf=ray(helper,player,origin.add(.5,.5,2),origin.add(.5,.5,-1));
        var leafPick=ModBlocks.archedDoor().getCloneItemStack(level.getBlockState(leaf.getBlockPos()),leaf,level,leaf.getBlockPos(),player);
        check(leafPick.is(ModBlocks.archedDoor().asItem()),"leaf pick");
        AppearanceFixtures.use(helper,player,ItemStack.EMPTY,ItemStack.EMPTY,leaf,false);
        check(level.getBlockState(door.anchor()).getValue(net.minecraft.world.level.block.DoorBlock.OPEN),"leaf did not toggle");
        check(!io.github.dirtyredz.roundwood.block.door.selection.DoorHitResolver.projected(level,front),"frame gained projected transport");
        check(io.github.dirtyredz.roundwood.block.door.interaction.ProjectedDoorUse.use(player,front)==net.minecraft.world.InteractionResult.FAIL,"frame accepted projected use");
        helper.succeed();
    }
    private static net.minecraft.world.phys.BlockHitResult ray(GameTestHelper helper,net.minecraft.server.level.ServerPlayer player,
            net.minecraft.world.phys.Vec3 from,net.minecraft.world.phys.Vec3 to) {
        player.setPos(from.x,from.y-player.getEyeHeight(),from.z);
        var hit=helper.getLevel().clip(new ClipContext(from,to,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,player));
        check(hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK,"ray missed");
        return io.github.dirtyredz.roundwood.block.door.selection.DoorHitResolver.resolve(helper.getLevel(),hit).orElseThrow();
    }

    @GameTest(template=TEMPLATE)
    public static void unknownFrameSurvivesAsOpaqueDoorItemAndCannotBePlacedLossily(GameTestHelper helper) {
        floor(helper);var level=helper.getLevel();
        var door=fitted(helper,ModBlocks.archedDoor(),ArchPieceThickness.FULL,ArchPieceDepth.FRONT);
        var entity=anchorEntity(helper,door);var saved=entity.saveWithFullMetadata(TestRegistries.get());
        var future=saved.getCompound(DoorArchStore.TAG).copy();future.putInt("Version",99);future.putString("Mystery","keep");saved.put(DoorArchStore.TAG,future);entity.loadWithComponents(saved, TestRegistries.get());
        var player=player(helper,"opaque_frame",GameType.SURVIVAL,Direction.NORTH);player.gameMode.destroyBlock(door.anchor());
        var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(door.anchor()).inflate(5));
        var returned=drops.stream().map(net.minecraft.world.entity.item.ItemEntity::getItem).filter(stack->stack.is(ModBlocks.archedDoor().asItem())).findFirst().orElseThrow();
        check(io.github.dirtyredz.roundwood.appearance.item.StackPayloads.custom(returned, DoorArchStore.TAG).equals(future),"opaque custody lost on harvest");
        check(dropped(helper,ModBlocks.archPiece(ArchPieceDesign.TOP_1X1).asItem())==0,"unknown frame decoded into refund");
        check(!placeDoor(helper,player,returned,new BlockPos(3,1,3),DoorHingeSide.LEFT).consumesAction(),"unknown item placed lossily");
        check(returned.getCount()==1 && io.github.dirtyredz.roundwood.appearance.item.StackPayloads.custom(returned, DoorArchStore.TAG).equals(future),"refusal consumed opaque item");
        helper.succeed();
    }
}
