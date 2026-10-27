package io.github.dirtyredz.roundwood.block.window.design;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.TrimSlot;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.glass.WindowGlassStacks;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.window.trim.WindowTrimActions;
import io.github.dirtyredz.roundwood.fixture.AppearanceFixtures;
import io.github.dirtyredz.roundwood.fixture.WindowFixtures;
import io.github.dirtyredz.roundwood.fixture.WindowRays;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.Optional;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.*;

@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FramelessWindowGameTests {
    @GameTest(template = TEMPLATE)
    public static void placementTintCustodyAndFreeRefusals(GameTestHelper helper) {
        floor(helper);
        var player = player(helper, "blank_pane", GameType.SURVIVAL, Direction.NORTH);
        var at = new BlockPos(3, 1, 3);
        var abs = helper.absolutePos(at);
        var design = WindowDesign.plain(WindowProfile.BLANK);
        var pane = ModBlocks.window(design);
        helper.setBlock(at, Blocks.WATER);
        var floorPos = abs.below();
        var hit = helper.getLevel().getBlockState(floorPos).getShape(helper.getLevel(), floorPos)
                .clip(Vec3.atCenterOf(floorPos).add(0, 2, 0), Vec3.atCenterOf(floorPos).add(0, -1, 0), floorPos);
        check(hit != null, "placement ray missed floor");
        var stack = new ItemStack(pane);
        var predicted = WindowFixtures.predict(player, stack, hit);
        check(WindowFixtures.place(player, stack, hit).consumesAction(), "placement refused");
        check(helper.getBlockState(at).equals(predicted), "preview differs from placement");
        check(helper.getBlockState(at).getValue(BlockStateProperties.WATERLOGGED), "water lost");
        var entity = (RoundWindowBlockEntity) helper.getLevel().getBlockEntity(abs);
        for (var item : java.util.List.of(Items.SPRUCE_PLANKS, ModItems.ROUND_TRIM.get(), ModItems.ARCH_SILL_TRIM.get())) {
            var donor = new ItemStack(item, 2);
            AppearanceFixtures.use(helper, player, donor, ItemStack.EMPTY,
                    WindowRays.face(helper, player, at, 4.5, 5.5, Direction.SOUTH), false);
            check(donor.getCount() == 2 && entity.appearance().equals(Appearance.DEFAULT), "invisible frame action paid");
        }
        check(!WindowTrimActions.install(helper.getLevel(), abs, AttachmentFace.FRONT, Appearance.DEFAULT), "blank trim installed");
        var dye = new ItemStack(Items.PURPLE_DYE, 2);
        AppearanceFixtures.use(helper, player, dye, ItemStack.EMPTY,
                WindowRays.face(helper, player, at, 4.5, 5.5, Direction.SOUTH), false);
        check(dye.getCount() == 1 && entity.glass().equals(Optional.of(DyeColor.PURPLE)), "tint/payment");
        var pick = AppearanceFixtures.pick(helper, abs);
        check(pick.is(pane.asItem()) && WindowGlassStacks.read(pick).equals(Optional.of(DyeColor.PURPLE)), "pick lost tint");
        check(WindowTrimActions.install(helper.getLevel(), abs, TrimSlot.CROSS_BEAMS, AttachmentFace.FRONT, Appearance.DEFAULT), "beams refused");
        clearDrops(helper);
        player.gameMode.destroyBlock(abs);
        var drops = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(abs).inflate(2), e -> e.getItem().is(pane.asItem()));
        check(drops.size() == 1 && WindowGlassStacks.read(drops.get(0).getItem()).equals(Optional.of(DyeColor.PURPLE)), "drop lost tint");
        clearDrops(helper);
        helper.succeed();
    }
}
