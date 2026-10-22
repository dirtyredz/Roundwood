package io.github.dirtyredz.roundwood.block.archway;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayLayout;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayPart;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.Optional;

import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.pick;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.use;
import static io.github.dirtyredz.roundwood.fixture.ArchwayFixtures.anchorEntity;
import static io.github.dirtyredz.roundwood.fixture.ArchwayFixtures.assertNoArchway;
import static io.github.dirtyredz.roundwood.fixture.ArchwayFixtures.block;
import static io.github.dirtyredz.roundwood.fixture.ArchwayFixtures.leftJambInside;
import static io.github.dirtyredz.roundwood.fixture.ArchwayFixtures.through;
import static io.github.dirtyredz.roundwood.fixture.ArchwayFixtures.write;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.clearDrops;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.dropReport;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.dropped;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.styled;

/**
 * An archway's look and face trims (0.12.0) through the production interaction path with real rays
 * against the cells' shapes: a Universal Trim fits the nearer face for one item (an occupied face is a
 * free no-op, the Arch Sill is refused); shears take one face off with a refund and one point of wear;
 * planks, masonry and dye restyle a trimmed face's casing on that face and the archway everywhere else;
 * sneaking and the offhand behave as on windows; breaking drops the archway in its look and each trim in
 * its own; picking gives the styled archway.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArchwayTrimGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    /** Across the left jamb's face (half a pixel in), and up. */
    private static final double JAMB_A = 0.5 / 16;
    private static final double JAMB_H = 0.5;
    private static final Appearance SPRUCE = wood("spruce_planks");
    private static final Appearance BIRCH = wood("birch_planks");
    private static final Appearance WARPED = wood("warped_planks");
    private static final Appearance BRICKS = Appearance.masonry(ResourceLocation.fromNamespaceAndPath("minecraft", "bricks"));
    private static final Appearance DEEPSLATE = Appearance.masonry(ResourceLocation.fromNamespaceAndPath("minecraft", "deepslate_bricks"));

    private ArchwayTrimGameTests() {
    }

    private static BlockHitResult front(GameTestHelper helper, ServerPlayer player, ArchwayLayout.Assembly archway) {
        return through(helper, player, archway, JAMB_A, JAMB_H, true);
    }

    private static BlockHitResult back(GameTestHelper helper, ServerPlayer player, ArchwayLayout.Assembly archway) {
        return through(helper, player, archway, JAMB_A, JAMB_H, false);
    }

    private static BlockHitResult inside(GameTestHelper helper, ServerPlayer player, ArchwayLayout.Assembly archway) {
        return leftJambInside(helper, player, archway, 0.2, 0.3);
    }

    private static ItemStack stack(Item item, Appearance look, int count) {
        ItemStack stack = styled(item, look);
        stack.setCount(count);
        return stack;
    }

    @GameTest(template = TEMPLATE)
    public static void aUniversalTrimFitsTheNearerFace(GameTestHelper helper) {
        floor(helper);
        for (ArchwayDesign design : ArchwayDesign.values()) {
            for (Direction facing : List.of(Direction.NORTH, Direction.EAST)) {
                String what = design + " " + facing;
                ArchwayLayout.Assembly archway = write(helper, ANCHOR, facing, design, Appearance.DEFAULT);
                ServerPlayer player = player(helper, "fit_" + design + "_" + facing, GameType.SURVIVAL, facing);
                ItemStack trims = stack(ModItems.ROUND_TRIM.get(), SPRUCE, 3);
                use(helper, player, trims, ItemStack.EMPTY, front(helper, player, archway), false);
                check(anchorEntity(helper, archway).trims().equals(FaceTrims.NONE.with(AttachmentFace.FRONT, SPRUCE)),
                        what + ": front " + anchorEntity(helper, archway).trims());
                check(trims.getCount() == 2, what + ": one trim used, have " + trims.getCount());
                use(helper, player, trims, ItemStack.EMPTY, inside(helper, player, archway), false);
                check(trims.getCount() == 2, what + ": the occupied nearer face was not a free no-op");
                ItemStack bricks = stack(ModItems.ROUND_TRIM.get(), BRICKS, 1);
                use(helper, player, bricks, ItemStack.EMPTY, back(helper, player, archway), false);
                check(anchorEntity(helper, archway).trims().equals(new FaceTrims(Optional.of(SPRUCE), Optional.of(BRICKS))),
                        what + ": back " + anchorEntity(helper, archway).trims());
                ItemStack sill = stack(ModItems.ARCH_SILL_TRIM.get(), SPRUCE, 1);
                use(helper, player, sill, ItemStack.EMPTY, front(helper, player, archway), false);
                check(sill.getCount() == 1, what + ": the arch sill was used");
                check(anchorEntity(helper, archway).appearance().equals(Appearance.DEFAULT), what + ": the archway changed");
                helper.setBlock(ANCHOR, Blocks.AIR);
                clearDrops(helper);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void shearsTakeOffOnlyThatFace(GameTestHelper helper) {
        floor(helper);
        ArchwayLayout.Assembly archway = write(helper, ANCHOR, Direction.NORTH, ArchwayDesign.KEYHOLE, Appearance.DEFAULT);
        check(anchorEntity(helper, archway).setTrims(new FaceTrims(Optional.of(WARPED), Optional.of(DEEPSLATE))), "trims refused");
        ServerPlayer player = player(helper, "shears", GameType.SURVIVAL, Direction.NORTH);
        ItemStack shears = new ItemStack(Items.SHEARS);
        use(helper, player, shears, ItemStack.EMPTY, inside(helper, player, archway), false);
        check(anchorEntity(helper, archway).trims().count() == 2 && shears.getDamageValue() == 0, "shears on the passage acted");
        use(helper, player, shears, ItemStack.EMPTY, front(helper, player, archway), false);
        check(anchorEntity(helper, archway).trims().equals(FaceTrims.NONE.with(AttachmentFace.BACK, DEEPSLATE)),
                "after shearing the front " + anchorEntity(helper, archway).trims());
        check(shears.getDamageValue() == 1, "shears wear " + shears.getDamageValue());
        check(held(player, ModItems.ROUND_TRIM.get(), WARPED) == 1, "no warped Universal Trim given back");
        use(helper, player, shears, ItemStack.EMPTY, front(helper, player, archway), false);
        check(shears.getDamageValue() == 1 && held(player, ModItems.ROUND_TRIM.get(), WARPED) == 1, "an empty face was not a free no-op");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void donorsRestyleTheCasingOnATrimmedFaceAndTheArchwayElsewhere(GameTestHelper helper) {
        floor(helper);
        ArchwayLayout.Assembly archway = write(helper, ANCHOR, Direction.NORTH, ArchwayDesign.ORDINARY, Appearance.DEFAULT);
        check(anchorEntity(helper, archway).setTrims(FaceTrims.NONE.with(AttachmentFace.FRONT, Appearance.DEFAULT)), "trim refused");
        ServerPlayer player = player(helper, "restyle", GameType.SURVIVAL, Direction.NORTH);
        ItemStack spruce = new ItemStack(Items.SPRUCE_PLANKS, 4);
        use(helper, player, spruce, ItemStack.EMPTY, front(helper, player, archway), false);
        check(anchorEntity(helper, archway).trims().equals(FaceTrims.NONE.with(AttachmentFace.FRONT, SPRUCE)), "front casing not restyled");
        check(anchorEntity(helper, archway).appearance().equals(Appearance.DEFAULT), "the archway changed with its casing");
        check(spruce.getCount() == 3, "restyle cost " + (4 - spruce.getCount()));
        ItemStack birch = new ItemStack(Items.BIRCH_PLANKS, 2);
        use(helper, player, birch, ItemStack.EMPTY, inside(helper, player, archway), false);
        check(anchorEntity(helper, archway).appearance().equals(BIRCH), "the passage side did not restyle the archway");
        ItemStack dye = new ItemStack(Items.RED_DYE, 2);
        use(helper, player, dye, ItemStack.EMPTY, back(helper, player, archway), false);
        check(anchorEntity(helper, archway).appearance().equals(BIRCH.painted(DyeColor.RED)),
                "dye on the untrimmed back did not paint the archway: " + anchorEntity(helper, archway).appearance());
        check(anchorEntity(helper, archway).trims().get(AttachmentFace.FRONT).equals(Optional.of(SPRUCE)), "the casing changed");
        ItemStack bricks = new ItemStack(Items.BRICKS, 2);
        use(helper, player, bricks, ItemStack.EMPTY, inside(helper, player, archway), false);
        check(anchorEntity(helper, archway).appearance().equals(BRICKS), "masonry did not face the archway");
        check(bricks.getCount() == 1, "masonry cost");
        // Sneaking does the same; the offhand acts while the main hand is empty.
        use(helper, player, new ItemStack(Items.WARPED_PLANKS), ItemStack.EMPTY, inside(helper, player, archway), true);
        check(anchorEntity(helper, archway).appearance().equals(WARPED), "sneaking planks did not restyle");
        ItemStack offTrim = stack(ModItems.ROUND_TRIM.get(), DEEPSLATE, 1);
        use(helper, player, ItemStack.EMPTY, offTrim, back(helper, player, archway), true);
        check(anchorEntity(helper, archway).trims().get(AttachmentFace.BACK).equals(Optional.of(DEEPSLATE)) && offTrim.isEmpty(),
                "the offhand trim, sneaking, did not fit the back");
        helper.succeed();
    }

    /** Breaking any cell drops the archway in its look and each trim in its own (the archway's look never leaks onto a trim). */
    @GameTest(template = TEMPLATE)
    public static void dropsKeepEveryLookAndPickingGivesTheStyledArchway(GameTestHelper helper) {
        floor(helper);
        Appearance blue = Appearance.DEFAULT.painted(DyeColor.BLUE);
        ArchwayLayout.Assembly archway = write(helper, ANCHOR, Direction.NORTH, ArchwayDesign.KEYHOLE, DEEPSLATE);
        check(anchorEntity(helper, archway).setTrims(new FaceTrims(Optional.of(BIRCH), Optional.of(blue))), "trims refused");
        ItemStack picked = pick(helper, archway.pos(ArchwayPart.UPPER_RIGHT));
        check(picked.is(block(ArchwayDesign.KEYHOLE).asItem()) && AppearanceStacks.read(picked).equals(DEEPSLATE), "pick " + picked);
        clearDrops(helper);
        ServerPlayer breaker = player(helper, "drops", GameType.SURVIVAL, Direction.NORTH);
        check(breaker.gameMode.destroyBlock(archway.pos(ArchwayPart.UPPER_RIGHT)), "break refused");
        assertNoArchway(helper, archway);
        Item trim = ModItems.ROUND_TRIM.get();
        check(dropped(helper, block(ArchwayDesign.KEYHOLE).asItem(), DEEPSLATE) == 1,
                "archway drops " + dropReport(helper, block(ArchwayDesign.KEYHOLE).asItem()));
        check(dropped(helper, trim, BIRCH) == 1 && dropped(helper, trim, blue) == 1, "trim drops " + dropReport(helper, trim));
        check(dropped(helper, trim, DEEPSLATE) == 0, "a trim took the archway's look");
        clearDrops(helper);
        helper.succeed();
    }

    private static int held(ServerPlayer player, Item item, Appearance look) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(item) && AppearanceStacks.read(stack).equals(look)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static Appearance wood(String block) {
        return Appearance.wood(ResourceLocation.fromNamespaceAndPath("minecraft", block));
    }
}
