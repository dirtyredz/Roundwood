package io.github.dirtyredz.roundwood.block.door.hardware;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.door.assembly.*;
import io.github.dirtyredz.roundwood.block.door.interaction.*;
import io.github.dirtyredz.roundwood.block.door.selection.*;
import io.github.dirtyredz.roundwood.block.door.leaf.*;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.fixture.AppearanceFixtures;
import net.minecraft.world.item.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.phys.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraftforge.gametest.*;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.*;

@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class DoorHardwareGameTests {
    @GameTest(template=TEMPLATE)
    public static void unknownHardwarePayloadSurvivesReload(GameTestHelper helper) {
        floor(helper);
        var door=placeOakDoor(helper,new BlockPos(3,1,3),Direction.NORTH,DoorHingeSide.LEFT,"opaque_hardware");
        var entity=anchorEntity(helper,door);
        var raw=new CompoundTag();raw.putInt("Version",42);raw.putString("Future","keep");
        var saved=entity.saveWithFullMetadata();saved.put("DoorHardware",raw.copy());entity.load(saved);
        check(raw.equals(entity.saveWithFullMetadata().get("DoorHardware")),"unknown hardware custody lost on reload");
        helper.succeed();
    }

    @GameTest(template=TEMPLATE, timeoutTicks=300)
    public static void endpointRaysPayOnceAndPreserveOtherSlots(GameTestHelper helper) {
        floor(helper); var level=helper.getLevel(); int rays=0,projected=0;
        for(var block:ModBlocks.doors()) for(var facing:Direction.Plane.HORIZONTAL)
            for(var depth:DoorDepth.values()) for(var hinge:DoorHingeSide.values()) for(boolean open:new boolean[]{false,true}) {
            var door=new DoorLayout.Assembly(helper.absolutePos(new BlockPos(4,1,4)),facing,hinge,block.design(),depth);
            check(DoorPlacement.placeAt(level,block,door,open),"hardware fixture");
            var entity=anchorEntity(helper,door);
            var player=player(helper,"hardware_ray",GameType.SURVIVAL,facing);
            for(var side:block.design().doubleLeaf()?DoorHingeSide.values():new DoorHingeSide[]{hinge})
                for(var slot:DoorHardwareSlot.values()) for(boolean front:new boolean[]{true,false}) {
                var piece=DoorGeometry.leaf(block.design(),side).stream().filter(p->p.texture().equals(slot.mesh())
                        && (front?p.box().minZ()>=16:p.box().maxZ()<=13)).findFirst().orElseThrow();
                var b=piece.box(); double x=(b.minX()+b.maxX())/2, y=(b.minY()+b.maxY())/2;
                double z=front?b.maxZ():b.minZ();
                double ex=x,ez=z+(front?32:-32);
                if(open){var a=DoorGeometry.turn(block.design(),x,z,DoorGeometry.openDegrees(side),side);
                    var e=DoorGeometry.turn(block.design(),ex,ez,DoorGeometry.openDegrees(side),side);x=a[0];z=a[1];ex=e[0];ez=e[1];}
                var target=point(door,x/16,y/16,1-(z+depth.offset())/16);
                var eye=point(door,ex/16,y/16,1-(ez+depth.offset())/16);
                player.setPos(eye.x,eye.y-player.getEyeHeight(),eye.z);
                var stack=new ItemStack(slot==DoorHardwareSlot.HINGES?Items.IRON_BLOCK:Items.GOLD_BLOCK,3);
                boolean off=!front; hold(player,off?ItemStack.EMPTY:stack,off?stack:ItemStack.EMPTY);
                var raw=DoorRaycast.clip(level,player,eye,target.add(target.subtract(eye).normalize().scale(.01)));
                check(raw.getType()==HitResult.Type.BLOCK,"hardware ray missed");
                var hit=DoorHitResolver.resolve(level,raw).orElseThrow();
                var proposed=DoorHardwareTarget.of(level,player,off?InteractionHand.OFF_HAND:InteractionHand.MAIN_HAND,hit);
                String context=block.design()+" "+facing+" "+depth+" "+side+" "+slot+" open="+open+" front="+front;
                check(proposed.isPresent(),"no proposal "+context+" authored="+door.toAuthored(hit.getLocation())+" aim="+door.toAuthored(target));
                var proposal=proposed.orElseThrow();
                check(proposal.eligible() && proposal.slot()==slot,"wrong proposal "+context);
                boolean changed=entity.hardware().get(slot).isEmpty();
                var other=entity.hardware().get(slot==DoorHardwareSlot.HINGES?DoorHardwareSlot.KNOBS:DoorHardwareSlot.HINGES);
                player.setShiftKeyDown(!front);
                if(DoorHitResolver.projected(level,hit)){projected++;ProjectedDoorUse.use(player,hit);}else use(helper,player,hit);
                player.setShiftKeyDown(false);
                check(entity.hardware().get(slot).equals(java.util.Optional.of(proposal.after())),"finish not applied "+context);
                check(stack.getCount()==(changed?2:3),"payment "+context);
                check(entity.hardware().get(slot==DoorHardwareSlot.HINGES?DoorHardwareSlot.KNOBS:DoorHardwareSlot.HINGES).equals(other),"other slot changed");
                check(entity.appearance().equals(io.github.dirtyredz.roundwood.appearance.Appearance.DEFAULT),"panel changed");
                check(level.getBlockState(door.anchor()).getValue(DoorBlock.OPEN)==open,"hardware toggled door");
                rays++;
            }
            var saved=entity.saveWithFullMetadata();var looks=entity.hardware();entity.load(saved);
            check(entity.hardware().equals(looks),"hardware reload");
            var item=DoorHardwareStacks.decorate(new ItemStack(block),entity);
            check(DoorHardwareStacks.readable(item) && DoorHardwareStacks.read(item).equals(looks),"item codec");
            level.removeBlock(door.anchor(),false);clearDrops(helper);
        }
        check(rays==1152 && projected>0,"matrix count "+rays+" projected="+projected);
        helper.succeed();
    }

    @GameTest(template=TEMPLATE, timeoutTicks=60)
    public static void movingReversedBlockedAndDeniedRequestsAreFree(GameTestHelper helper) {
        floor(helper);var level=helper.getLevel();
        var door=placeOakDoor(helper,new BlockPos(3,1,3),Direction.NORTH,DoorHingeSide.LEFT,"motion");
        var entity=anchorEntity(helper,door);var player=player(helper,"hardware_motion",GameType.SURVIVAL,Direction.NORTH);
        var stack=new ItemStack(Items.IRON_BLOCK,4);hold(player,stack,ItemStack.EMPTY);
        var eye=point(door,1,.98,-1);player.setPos(eye.x,eye.y-player.getEyeHeight(),eye.z);
        var hit=io.github.dirtyredz.roundwood.fixture.DoorRays.through(helper,player,door,1,.98,true);
        check(DoorHardwareTarget.of(level,player,InteractionHand.MAIN_HAND,hit).orElseThrow().eligible(),"settled proposal");
        var barrier=BlockPos.containing(eye.add(hit.getLocation()).scale(.5));
        level.setBlock(barrier,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
        check(!DoorHardwareTarget.of(level,player,InteractionHand.MAIN_HAND,hit).orElseThrow().eligible(),"world blocker ignored");
        use(helper,player,hit);check(stack.getCount()==4 && entity.hardware().equals(DoorHardware.DEFAULT),"blocked changed hardware");
        level.removeBlock(barrier,false);
        var denied=io.github.dirtyredz.roundwood.fixture.assembly.DenyingPlayer.create(helper,"hardware",Direction.NORTH,java.util.Set.of(door.pos(DoorLayout.Part.UPPER_RIGHT)));
        denied.setPos(eye.x,eye.y-denied.getEyeHeight(),eye.z);hold(denied,stack,ItemStack.EMPTY);
        check(!DoorHardwareTarget.of(level,denied,InteractionHand.MAIN_HAND,hit).orElseThrow().eligible(),"permission ignored");
        use(helper,denied,hit);check(stack.getCount()==4,"denied payment");
        oakDoor().setOpen(null,level,level.getBlockState(door.anchor()),door.anchor(),true);
        check(entity.swing().moving(level.getGameTime()),"server motion missing");
        check(!DoorHardwareTarget.of(level,player,InteractionHand.MAIN_HAND,hit).orElseThrow().eligible(),"stale moving preview");
        use(helper,player,hit);check(stack.getCount()==4 && level.getBlockState(door.anchor()).getValue(DoorBlock.OPEN),"moving action paid or toggled");
        helper.runAfterDelay(2,()->{
            oakDoor().setOpen(null,level,level.getBlockState(door.anchor()),door.anchor(),false);
            check(entity.swing().moving(level.getGameTime()),"reversal motion missing");
            use(helper,player,hit);check(stack.getCount()==4 && !level.getBlockState(door.anchor()).getValue(DoorBlock.OPEN),"reversal paid or toggled");
            helper.runAfterDelay(8,()->{
                check(!entity.swing().moving(level.getGameTime()),"motion never settled");
                use(helper,player,hit);check(stack.getCount()==3 && entity.hardware().knobs().isPresent(),"settled edit refused");
                player.setGameMode(GameType.CREATIVE);var gold=new ItemStack(Items.GOLD_BLOCK,4);hold(player,gold,ItemStack.EMPTY);
                use(helper,player,hit);check(gold.getCount()==4 && entity.hardware().knobs().orElseThrow().material().getPath().equals("gold_block"),"creative charge");
                helper.succeed();
            });
        });
    }

    @GameTest(template=TEMPLATE)
    public static void pickHarvestReplacementAndOpaqueRefusalPreserveCustody(GameTestHelper helper) {
        floor(helper);var level=helper.getLevel();var relative=new BlockPos(3,1,3);
        var door=placeOakDoor(helper,relative,Direction.NORTH,DoorHingeSide.LEFT,"custody");
        var entity=anchorEntity(helper,door);
        entity.setHardware(entity.hardware().with(DoorHardwareSlot.HINGES,java.util.Optional.of(AppearanceFixtures.SPRUCE)));
        entity.setHardware(entity.hardware().with(DoorHardwareSlot.KNOBS,java.util.Optional.of(AppearanceFixtures.BIRCH)));
        var expected=entity.hardware();var player=player(helper,"hardware_custody",GameType.SURVIVAL,Direction.NORTH);
        var hit=frontHit(door,1,.8);
        var picked=oakDoor().getCloneItemStack(level.getBlockState(door.anchor()),hit,level,door.anchor(),player);
        check(DoorHardwareStacks.read(picked).equals(expected),"pick lost slots");
        level.destroyBlock(door.anchor(),true);
        var drops=level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new AABB(door.anchor()).inflate(4));
        var doors=drops.stream().filter(e->e.getItem().is(oakDoor().asItem())).toList();
        check(doors.size()==1 && DoorHardwareStacks.read(doors.get(0).getItem()).equals(expected),"harvest lost/duplicated slots");
        check(drops.stream().noneMatch(e->e.getItem().is(Items.SPRUCE_PLANKS)||e.getItem().is(Items.BIRCH_PLANKS)),"raw donor refund");
        clearDrops(helper);
        check(placeDoor(helper,player,picked,relative,DoorHingeSide.LEFT).consumesAction(),"replacement refused");
        entity=anchorEntity(helper,door);check(entity.hardware().equals(expected),"placement lost slots");
        var raw=new CompoundTag();raw.putInt("Version",42);raw.putString("Future","keep");
        var saved=entity.saveWithFullMetadata();saved.put(DoorHardwareStore.TAG,raw);entity.load(saved);
        var opaque=oakDoor().getCloneItemStack(level.getBlockState(door.anchor()),hit,level,door.anchor(),player);
        check(raw.equals(opaque.getTag().get(DoorHardwareStore.TAG)) && !DoorHardwareStacks.readable(opaque),"opaque item custody");
        level.removeBlock(door.anchor(),false);clearDrops(helper);
        placeDoor(helper,player,opaque,relative,DoorHingeSide.LEFT);
        check(opaque.getCount()==1 && level.getBlockState(door.anchor()).isAir(),"opaque placement was lossy");
        helper.succeed();
    }

    @GameTest(template=TEMPLATE)
    public static void exposedHardwareKeepsFittedFrameCasingAndCamouflageIndependent(GameTestHelper helper) {
        floor(helper);var level=helper.getLevel();
        var door=placeOakDoor(helper,new BlockPos(3,1,3),Direction.NORTH,DoorHingeSide.LEFT,"hardware_frame");
        var entity=anchorEntity(helper,door);
        check(io.github.dirtyredz.roundwood.block.door.frame.DoorArchFitting.fitAt(level,door.anchor(),
                io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness.FULL,
                io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth.FRONT,AppearanceFixtures.BRICKS),"frame fixture");
        check(io.github.dirtyredz.roundwood.block.door.trim.DoorTrimActions.install(level,door.anchor(),
                io.github.dirtyredz.roundwood.block.attachment.AttachmentFace.FRONT,AppearanceFixtures.BIRCH),"casing fixture");
        entity.setCamouflage(io.github.dirtyredz.roundwood.block.door.camouflage.CamouflageData.paid(
                net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),new ItemStack(Items.STONE)));
        var before=entity.saveWithFullMetadata();
        var player=player(helper,"hardware_frame_ray",GameType.SURVIVAL,Direction.NORTH);
        var stack=new ItemStack(Items.GOLD_BLOCK,3);hold(player,stack,ItemStack.EMPTY);
        var eye=point(door,6.5/16,8.5/16,-1);player.setPos(eye.x,eye.y-player.getEyeHeight(),eye.z);
        var hit=io.github.dirtyredz.roundwood.fixture.DoorRays.through(helper,player,door,6.5/16,8.5/16,true);
        var proposal=DoorHardwareTarget.of(level,player,InteractionHand.MAIN_HAND,hit).orElseThrow();
        check(proposal.eligible() && proposal.slot()==DoorHardwareSlot.HINGES,"exposed spear target");
        use(helper,player,hit);
        check(stack.getCount()==2 && entity.hardware().hinges().isPresent(),"exposed spear edit");
        var after=entity.saveWithFullMetadata();after.remove(DoorHardwareStore.TAG);
        check(before.equals(after),"hardware changed body/frame/casing/camouflage custody");
        var update=entity.getUpdateTag();entity.setHardware(DoorHardware.DEFAULT);entity.handleUpdateTag(update);
        check(entity.hardware().hinges().isPresent(),"slot sync");
        entity.setHardware(DoorHardware.DEFAULT);var clear=entity.getUpdateTag();entity.handleUpdateTag(update);entity.handleUpdateTag(clear);
        check(entity.hardware().equals(DoorHardware.DEFAULT),"explicit sync clear");
        helper.succeed();
    }
}
