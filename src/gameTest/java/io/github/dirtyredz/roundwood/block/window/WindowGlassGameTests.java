package io.github.dirtyredz.roundwood.block.window;

import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.glass.WindowGlassStacks;
import io.github.dirtyredz.roundwood.fixture.AppearanceFixtures;
import io.github.dirtyredz.roundwood.fixture.WindowFixtures;
import io.github.dirtyredz.roundwood.fixture.WindowRays;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;

import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.SPRUCE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.clearDrops;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.dropped;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;

/**
 * Glass colour (0.8.0): dye on the glazing tints the whole window's glass and keeps the frame, dye on
 * the frame or a cross beam paints the frame and keeps the glass, plain planks restyle the frame
 * and keep the glass; one dye per change, repeats free, creative free; the tint survives break, pick,
 * placement, saving and update packets, and water and connections are untouched.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WindowGlassGameTests {
    private static final BlockPos AT = new BlockPos(4, 2, 4);
    private static final WindowPose SOUTH = WindowPose.upright(Direction.SOUTH, WindowDepth.CENTER);

    private WindowGlassGameTests() {
    }

    private static RoundWindowBlockEntity entity(GameTestHelper helper) {
        return WindowTrimGameTests.entity(helper, AT);
    }

    @GameTest(template = TEMPLATE)
    public static void dyeFollowsTheAimedRegion(GameTestHelper helper) {
        ServerPlayer player = player(helper, "glass_regions", GameType.SURVIVAL, Direction.NORTH);
        WindowDesign cross = new WindowDesign(WindowProfile.ROUND, WindowPattern.CROSS);
        helper.setBlock(AT, WindowTrimGameTests.window(cross, SOUTH).setValue(BlockStateProperties.WATERLOGGED, true));
        ItemStack red = new ItemStack(Items.RED_DYE, 3);
        AppearanceFixtures.use(helper, player, red, ItemStack.EMPTY, WindowRays.face(helper, player, AT, 5.5, 5.5, Direction.SOUTH), false);
        check(entity(helper).glass().equals(Optional.of(DyeColor.RED)) && red.getCount() == 2, "glass: " + entity(helper).glass());
        check(entity(helper).appearance().equals(Appearance.DEFAULT), "dye on glass painted the frame");
        AppearanceFixtures.use(helper, player, red, ItemStack.EMPTY, WindowRays.face(helper, player, AT, 10.5, 5.5, Direction.NORTH), false);
        check(red.getCount() == 2, "the same colour again was paid for");
        ItemStack blue = new ItemStack(Items.BLUE_DYE, 2);
        AppearanceFixtures.use(helper, player, blue, ItemStack.EMPTY, WindowRays.face(helper, player, AT, 8.25, 5.5, Direction.SOUTH), false);
        check(entity(helper).appearance().equals(AppearanceFixtures.oakPainted(DyeColor.BLUE)) && entity(helper).glass().equals(Optional.of(DyeColor.RED)),
                "dye on a beam: frame " + entity(helper).appearance() + ", glass " + entity(helper).glass());
        ItemStack green = new ItemStack(Items.GREEN_DYE, 1);
        AppearanceFixtures.use(helper, player, green, ItemStack.EMPTY, WindowRays.face(helper, player, AT, 1.5, 1.5, Direction.SOUTH), false);
        check(entity(helper).appearance().equals(AppearanceFixtures.oakPainted(DyeColor.GREEN)) && entity(helper).glass().equals(Optional.of(DyeColor.RED)),
                "dye on the frame");
        ItemStack planks = new ItemStack(Items.SPRUCE_PLANKS, 2);
        AppearanceFixtures.use(helper, player, planks, ItemStack.EMPTY, WindowRays.face(helper, player, AT, 5.5, 5.5, Direction.SOUTH), false);
        check(entity(helper).appearance().equals(SPRUCE) && entity(helper).glass().equals(Optional.of(DyeColor.RED)), "plain planks on the glass restyle the frame");
        check(helper.getBlockState(AT).getValue(BlockStateProperties.WATERLOGGED), "the water went");
        ServerPlayer creative = player(helper, "glass_creative", GameType.CREATIVE, Direction.NORTH);
        ItemStack lime = new ItemStack(Items.LIME_DYE);
        AppearanceFixtures.use(helper, creative, lime, ItemStack.EMPTY, WindowRays.face(helper, creative, AT, 5.5, 5.5, Direction.SOUTH), false);
        check(entity(helper).glass().equals(Optional.of(DyeColor.LIME)) && lime.getCount() == 1, "creative tint");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void theTintTravelsWithTheWindow(GameTestHelper helper) {
        ServerPlayer player = player(helper, "glass_travel", GameType.SURVIVAL, Direction.NORTH);
        WindowDesign arch = WindowDesign.plain(WindowProfile.ARCH_RIGHT);
        helper.setBlock(AT, WindowTrimGameTests.window(arch, SOUTH));
        Appearance frame = AppearanceFixtures.oakPainted(DyeColor.BROWN);
        entity(helper).setAppearance(frame);
        entity(helper).setGlass(Optional.of(DyeColor.PURPLE));
        ItemStack pick = AppearanceFixtures.pick(helper, helper.absolutePos(AT));
        check(WindowGlassStacks.read(pick).equals(Optional.of(DyeColor.PURPLE)) && AppearanceStacks.read(pick).equals(frame), "pick " + pick);
        CompoundTag update = entity(helper).getUpdateTag();
        RoundWindowBlockEntity copy = new RoundWindowBlockEntity(entity(helper).getType(), helper.absolutePos(AT), helper.getBlockState(AT));
        copy.handleUpdateTag(update);
        check(copy.glass().equals(Optional.of(DyeColor.PURPLE)), "update tag");
        copy.load(entity(helper).saveWithFullMetadata());
        check(copy.glass().equals(Optional.of(DyeColor.PURPLE)), "save");
        clearDrops(helper);
        player.gameMode.destroyBlock(helper.absolutePos(AT));
        check(dropped(helper, ModItems.window(arch), frame) == 1, "drop");
        ItemStack drop = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(helper.absolutePos(AT)).inflate(3), e -> e.getItem().is(ModItems.window(arch)))
                .get(0).getItem().copy();
        check(WindowGlassStacks.read(drop).equals(Optional.of(DyeColor.PURPLE)), "the dropped window lost its tint");
        clearDrops(helper);
        // Placing the dropped stack installs both looks.
        helper.setBlock(AT, Blocks.AIR);
        helper.setBlock(AT.below(), Blocks.STONE);
        ServerPlayer placer = WindowFixtures.playerAt(helper, "glass_place", GameType.SURVIVAL, net.minecraft.world.phys.Vec3.atCenterOf(
                AT.south(3)), Direction.NORTH, 0);
        WindowFixtures.place(placer, drop, WindowFixtures.zone(helper, AT.below(), Direction.UP,
                io.github.dirtyredz.roundwood.block.window.placement.WindowTargetZones.Zone.CENTER));
        check(entity(helper).glass().equals(Optional.of(DyeColor.PURPLE)) && entity(helper).appearance().equals(frame), "placed looks");
        // A clear window carries no glass tag and stacks with a crafted one.
        ItemStack clear = AppearanceStacks.create(ModItems.window(arch), Appearance.DEFAULT);
        WindowGlassStacks.write(clear, Optional.empty());
        check(clear.getTag() == null && ItemStack.isSameItemSameTags(clear, new ItemStack(ModItems.window(arch))), "clear stack " + clear.getTag());
        helper.succeed();
    }
}
