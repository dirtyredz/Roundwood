package io.github.dirtyredz.roundwood.migration;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowDepth;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowFaceIdentity;
import io.github.dirtyredz.roundwood.block.window.geometry.WindowPose;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.fixture.AppearanceFixtures;
import io.github.dirtyredz.roundwood.fixture.window.WindowRays;
import io.github.dirtyredz.roundwood.registry.ModItems;
import io.github.dirtyredz.roundwood.showcase.ShowcaseWindow;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;

import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.BRICKS;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.SPRUCE;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.player;

/**
 * Old trim stacks after the 0.9.0 Universal Trim (the migration contract): stacks of every trim id in
 * a chest and in a player's inventory keep their id, count and NBT through saving and loading (no
 * inventory rewrite, no remap), and a reloaded old stack works at once: it fits a window in its own
 * look, and recovering the attachment gives the canonical {@code round_trim} in that look.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TrimItemCompatibilityGameTests {
    private static final BlockPos CHEST = new BlockPos(2, 2, 2);
    private static final BlockPos WINDOW = new BlockPos(5, 2, 4);

    private TrimItemCompatibilityGameTests() {
    }

    /** One stack per trim id, each a different count and look (an alias with no tag keeps its historical look). */
    private static List<ItemStack> oldStacks() {
        List<ItemStack> stacks = new ArrayList<>();
        List<Item> ids = AppearanceFixtures.universalTrimIds();
        for (int i = 0; i < ids.size(); i++) {
            ItemStack stack = new ItemStack(ids.get(i), 3 + i);
            if (i == 1) {
                AppearanceStacks.write(stack, BRICKS); // a styled 0.8.0 window trim
            } else if (i == 0) {
                AppearanceStacks.write(stack, SPRUCE);
            }
            stacks.add(stack);
        }
        return stacks;
    }

    private static void checkSame(ItemStack before, ItemStack after, String where) {
        check(BuiltInRegistries.ITEM.getKey(after.getItem()).equals(BuiltInRegistries.ITEM.getKey(before.getItem()))
                && after.getCount() == before.getCount() && ItemStack.isSameItemSameTags(before, after),
                where + ": " + before + " " + before.getTag() + " came back as " + after + " " + after.getTag());
    }

    @GameTest(template = TEMPLATE)
    public static void oldTrimStacksKeepIdCountAndNbtInChestsAndInventories(GameTestHelper helper) {
        List<ItemStack> stacks = oldStacks();
        helper.setBlock(CHEST, Blocks.CHEST);
        ChestBlockEntity chest = (ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(CHEST));
        for (int i = 0; i < stacks.size(); i++) {
            chest.setItem(i, stacks.get(i).copy());
        }
        CompoundTag saved = chest.saveWithFullMetadata();
        helper.setBlock(CHEST, Blocks.AIR);
        helper.setBlock(CHEST, Blocks.CHEST);
        ChestBlockEntity reloaded = (ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(CHEST));
        reloaded.load(saved);
        for (int i = 0; i < stacks.size(); i++) {
            checkSame(stacks.get(i), reloaded.getItem(i), "chest slot " + i);
        }
        ServerPlayer player = player(helper, "trim_inventory", GameType.SURVIVAL, Direction.NORTH);
        for (int i = 0; i < stacks.size(); i++) {
            player.getInventory().setItem(i, stacks.get(i).copy());
        }
        ListTag list = player.getInventory().save(new ListTag());
        Inventory inventory = new Inventory(player);
        inventory.load(list);
        for (int i = 0; i < stacks.size(); i++) {
            checkSame(stacks.get(i), inventory.getItem(i), "inventory slot " + i);
        }
        helper.succeed();
    }

    /** A reloaded 0.8.0 window trim fits in its look; shears recover it as one canonical Universal Trim in that look. */
    @GameTest(template = TEMPLATE)
    public static void aReloadedOldStackFitsAndRecoversAsTheUniversalTrim(GameTestHelper helper) {
        ItemStack old = new ItemStack(LegacyContent.windowTrim(), 2);
        AppearanceStacks.write(old, BRICKS);
        ItemStack stack = ItemStack.of(old.save(new CompoundTag()));
        check(stack.is(LegacyContent.windowTrim()) && AppearanceStacks.read(stack).equals(BRICKS), "reloaded " + stack + " " + stack.getTag());
        WindowPose pose = WindowPose.upright(Direction.SOUTH, WindowDepth.CENTER);
        BlockState window = ShowcaseWindow.of(WindowDesign.plain(WindowProfile.ROUND), pose, 0, 0, 0, Appearance.DEFAULT).state();
        helper.setBlock(WINDOW, window);
        ServerPlayer player = player(helper, "trim_recover", GameType.SURVIVAL, Direction.NORTH);
        BlockState placed = helper.getBlockState(WINDOW);
        AttachmentFace south = WindowFaceIdentity.face(((WindowPaneBlock) placed.getBlock()).pose(placed), WindowPaneBlock.faceReversed(placed),
                Direction.SOUTH);
        AppearanceFixtures.use(helper, player, stack, ItemStack.EMPTY, WindowRays.face(helper, player, WINDOW, 8.5, 0.5, Direction.SOUTH), false);
        RoundWindowBlockEntity entity = (RoundWindowBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(WINDOW));
        check(entity.trims().equals(FaceTrims.NONE.with(south, BRICKS)) && stack.getCount() == 1, "fitted " + entity.trims());
        player.getInventory().clearContent();
        ItemStack shears = new ItemStack(Items.SHEARS);
        AppearanceFixtures.use(helper, player, shears, ItemStack.EMPTY, WindowRays.face(helper, player, WINDOW, 8.5, 0.5, Direction.SOUTH), false);
        int canonical = 0;
        int old08 = 0;
        for (ItemStack held : player.getInventory().items) {
            if (held.is(ModItems.ROUND_TRIM.get()) && AppearanceStacks.read(held).equals(BRICKS)) {
                canonical += held.getCount();
            }
            if (held.is(LegacyContent.windowTrim())) {
                old08 += held.getCount();
            }
        }
        check(canonical == 1 && old08 == 0 && !entity.trims().any(), "recovered " + canonical + " round_trim, " + old08 + " window_trim");
        helper.setBlock(WINDOW, Blocks.AIR);
        helper.succeed();
    }
}
