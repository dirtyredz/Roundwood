package io.github.dirtyredz.roundwood.appearance;


import io.github.dirtyredz.roundwood.fixture.TestRegistries;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.material.SurfaceMaterials;
import io.github.dirtyredz.roundwood.fixture.AppearanceFixtures;
import io.github.dirtyredz.roundwood.fixture.door.DoorRays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.neoforged.neoforge.gametest.*;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.*;

@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SurfaceFinishGameTests {
    @GameTest(template=TEMPLATE)
    public static void donorChangesArePaidOnceAndUnsupportedPaintIsFree(GameTestHelper helper) {
        floor(helper);
        var door=placeOakDoor(helper,new BlockPos(3,1,3),Direction.NORTH,DoorHingeSide.LEFT,"surfaces");
        var entity=anchorEntity(helper,door);
        var player=player(helper,"surface_payer",GameType.SURVIVAL,Direction.NORTH);
        for(var item:new Item[]{Items.IRON_BLOCK,Items.GOLD_BLOCK,Items.DIAMOND_BLOCK,Items.EMERALD_BLOCK,Items.NETHERITE_BLOCK,
                Items.COPPER_BLOCK,Items.EXPOSED_COPPER,Items.WEATHERED_COPPER,Items.OXIDIZED_COPPER,
                Items.WAXED_COPPER_BLOCK,Items.WAXED_EXPOSED_COPPER,Items.WAXED_WEATHERED_COPPER,Items.WAXED_OXIDIZED_COPPER,
                Items.QUARTZ_BLOCK,Items.SMOOTH_QUARTZ,Items.CHISELED_QUARTZ_BLOCK,Items.QUARTZ_BRICKS,Items.QUARTZ_PILLAR}) {
            var stack=new ItemStack(item,3);
            var look=Appearance.surface(SurfaceMaterials.fromStack(stack).orElseThrow());
            boolean same=look.equals(entity.appearance());
            hold(player,stack,ItemStack.EMPTY);
            var hit=DoorRays.through(helper,player,door,.7,.4,true);
            AppearanceFixtures.use(helper,player,stack,ItemStack.EMPTY,hit,true);
            check(entity.appearance().equals(look) && stack.getCount()==(same?3:2),"donor change/payment "+item);
            AppearanceFixtures.use(helper,player,stack,ItemStack.EMPTY,hit,true);
            check(stack.getCount()==(same?3:2),"unchanged donor charged");
            var dye=new ItemStack(Items.RED_DYE,2);
            AppearanceFixtures.use(helper,player,dye,ItemStack.EMPTY,hit,false);
            check(dye.getCount()==2 && entity.appearance().equals(look),"unpaintable finish consumed dye");
            var saved=entity.saveWithFullMetadata(TestRegistries.get());entity.loadWithComponents(saved, TestRegistries.get());
            check(entity.appearance().equals(look),"finish reload");
        }
        var payload=new ItemStack(Items.IRON_BLOCK);
        net.minecraft.world.item.BlockItem.setBlockEntityData(payload,net.minecraft.world.level.block.entity.BlockEntityType.CHEST,io.github.dirtyredz.roundwood.fixture.PlacedPayloads.contents());
        check(SurfaceMaterials.fromStack(payload).isEmpty(),"payload donor accepted");
        for(var item:new Item[]{Items.CUT_COPPER,Items.WAXED_CUT_COPPER,Items.QUARTZ_SLAB,Items.SMOOTH_QUARTZ_STAIRS})
            check(SurfaceMaterials.fromStack(new ItemStack(item)).isEmpty(),"unsupported shape accepted");
        player.setGameMode(GameType.CREATIVE);
        var stack=new ItemStack(Items.IRON_BLOCK,3);hold(player,stack,ItemStack.EMPTY);
        AppearanceFixtures.use(helper,player,stack,ItemStack.EMPTY,DoorRays.through(helper,player,door,.7,.4,true),true);
        check(stack.getCount()==3 && entity.appearance().material().getPath().equals("iron_block"),"creative donor payment");
        helper.succeed();
    }
}
