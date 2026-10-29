package io.github.dirtyredz.roundwood.migration;

import io.github.dirtyredz.roundwood.migration.retired.CrossWindowCompatibility;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneBlock;
import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import io.github.dirtyredz.roundwood.block.window.placement.WindowTargetZones;
import io.github.dirtyredz.roundwood.fixture.AppearanceFixtures;
import io.github.dirtyredz.roundwood.fixture.window.WindowFixtures;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.windowLook;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.TEMPLATE;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.clearDrops;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.dropped;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.floor;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.player;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.styled;

/**
 * Cross-beam variants (0.7.0): each crossed item places its own crossed block with exactly the plain
 * piece's targeting (centred and flush edge zones agree with the preview), keeps its pattern through
 * restyling, picks and drops, and a flush crossed window still grows no glass bridge.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CrossWindowItemCompatibilityGameTests {
    private CrossWindowItemCompatibilityGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void hiddenStacksRoundTripInventoriesAndChestsWithoutRewriting(GameTestHelper helper) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var owner = player(helper, "cross_storage", GameType.SURVIVAL, Direction.NORTH);
        owner.getInventory().clearContent();
        BlockPos chestPos = new BlockPos(2, 1, 2);
        helper.setBlock(chestPos, Blocks.CHEST);
        var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(chestPos));
        var expected = new java.util.ArrayList<ItemStack>();
        for (int i = 0; i < CrossWindowCompatibility.IDS.size(); i++) {
            var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(net.minecraft.resources.ResourceLocation
                    .fromNamespaceAndPath("roundwood", CrossWindowCompatibility.IDS.get(i)));
            var stack = new ItemStack(item, i + 2);
            stack.getOrCreateTag().putString("Arbitrary", "kept " + i);
            stack.getOrCreateTag().put("BlockEntityTag", net.minecraft.nbt.TagParser.parseTag("{WindowCrossBeams:{Version:99,Opaque:[I;1,2,3]}}"));
            expected.add(stack.copy());
            owner.getInventory().items.set(i, stack.copy());
            chest.setItem(i, stack.copy());
        }
        var inventory = owner.getInventory().save(new net.minecraft.nbt.ListTag());
        var savedChest = chest.saveWithoutMetadata();
        owner.getInventory().clearContent();
        chest.clearContent();
        owner.getInventory().load(inventory);
        chest.load(savedChest);
        for (int i = 0; i < expected.size(); i++) {
            check(ItemStack.matches(expected.get(i), owner.getInventory().items.get(i)), "inventory changed hidden stack " + i);
            check(ItemStack.matches(expected.get(i), chest.getItem(i)), "chest changed hidden stack " + i);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void rejectedAndCancelledPlacementKeepsTheStackAndDestination(GameTestHelper helper) {
        floor(helper);
        BlockPos at = new BlockPos(4, 1, 4);
        var owner = player(helper, "cross_refused", GameType.SURVIVAL, Direction.NORTH);
        var item = CrossWindowCompatibility.items().get(0);
        var stack = new ItemStack(item, 5);
        var disk = new net.minecraft.nbt.CompoundTag();
        var future = new net.minecraft.nbt.CompoundTag();
        future.putInt("Version", 99);
        disk.put("WindowCrossBeams", future);
        stack.getOrCreateTag().put("BlockEntityTag", disk);
        var before = stack.copy();
        var hit = WindowFixtures.zone(helper, at.below(), Direction.UP, WindowTargetZones.Zone.CENTER);
        check(!WindowFixtures.place(owner, stack, hit).consumesAction(), "accepted unsupported beam payload");
        check(ItemStack.matches(stack, before) && helper.getBlockState(at).isAir(), "refused placement changed contents");
        stack = new ItemStack(item, 5);
        var cancelled = player(helper, io.github.dirtyredz.roundwood.fixture.PlacementCanceller.PLAYER.substring("rw_".length()), GameType.SURVIVAL, Direction.NORTH);
        check(!WindowFixtures.place(cancelled, stack, hit).consumesAction(), "cancelled placement succeeded");
        check(stack.getCount() == 5 && helper.getBlockState(at).isAir(), "cancelled placement consumed or left a pane");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void fullDiskContentsMatchThePlacementProposal(GameTestHelper helper) {
        floor(helper);
        var owner = player(helper, "cross_full_disk", GameType.SURVIVAL, Direction.NORTH);
        BlockPos at = new BlockPos(4, 1, 4);
        var item = (io.github.dirtyredz.roundwood.migration.item.RetiredCrossWindowItem) CrossWindowCompatibility.items().get(1);
        var stack = new ItemStack(item, 3);
        var disk = new net.minecraft.nbt.CompoundTag();
        io.github.dirtyredz.roundwood.appearance.AppearanceNbt.write(disk, AppearanceFixtures.SPRUCE);
        io.github.dirtyredz.roundwood.block.window.glass.WindowGlassNbt.write(disk, java.util.Optional.of(DyeColor.CYAN));
        var trims = io.github.dirtyredz.roundwood.block.attachment.FaceTrims.both(Appearance.DEFAULT);
        io.github.dirtyredz.roundwood.block.window.trim.WindowTrimNbt.write(disk, trims);
        io.github.dirtyredz.roundwood.block.window.trim.WindowSillNbt.write(disk, trims);
        stack.getOrCreateTag().put("BlockEntityTag", disk);
        var hit = WindowFixtures.zone(helper, at.below(), Direction.UP, WindowTargetZones.Zone.CENTER);
        owner.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
        var context = new net.minecraft.world.item.context.BlockPlaceContext(owner, net.minecraft.world.InteractionHand.MAIN_HAND, stack, hit);
        var plan = io.github.dirtyredz.roundwood.migration.item.CrossWindowPlacement.plan(item, context).orElseThrow();
        check(WindowFixtures.place(owner, stack, hit).consumesAction() && stack.getCount() == 2, "full contents placement failed/payment");
        var state = helper.getBlockState(at);
        var entity = (io.github.dirtyredz.roundwood.block.window.pane.RoundWindowBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(at));
        check(entity.appearance().equals(AppearanceFixtures.SPRUCE) && entity.glass().equals(java.util.Optional.of(DyeColor.CYAN)), "disk-only frame/tint lost");
        check(entity.trims(io.github.dirtyredz.roundwood.block.attachment.TrimSlot.PERIMETER).equals(trims)
                && entity.trims(io.github.dirtyredz.roundwood.block.attachment.TrimSlot.SILL).equals(trims), "disk face attachments lost");
        check(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(plan.selection(), state.getShape(helper.getLevel(), helper.absolutePos(at)),
                net.minecraft.world.phys.shapes.BooleanOp.NOT_SAME), "legacy placement preview differs from fitted result");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hiddenItemsPlacePlainPanesWithOneCross(GameTestHelper helper) {
        floor(helper);
        ServerPlayer player = player(helper, "cross_place", GameType.SURVIVAL, Direction.NORTH);
        BlockPos at = new BlockPos(4, 1, 4);
        Appearance look = AppearanceFixtures.oakPainted(DyeColor.PURPLE);
        for (WindowDesign design : io.github.dirtyredz.roundwood.registry.ModBlocks.WindowProducts.all()) {
            if (design.pattern() != WindowPattern.CROSS) {
                continue;
            }
            WindowDesign plain = WindowDesign.plain(design.profile());
            for (WindowTargetZones.Zone zone : new WindowTargetZones.Zone[] {WindowTargetZones.Zone.CENTER, WindowTargetZones.Zone.U_MIN}) {
                var hit = WindowFixtures.zone(helper, at.below(), Direction.UP, zone);
                BlockState plainPredicted = WindowFixtures.predict(player, new ItemStack(ModItems.window(plain)), hit);
                ItemStack stack = styled(ModItems.window(design), look);
                check(WindowFixtures.place(player, stack, hit).consumesAction(), design.id() + " not placed");
                BlockState placed = helper.getBlockState(at);
                check(placed.is(ModBlocks.window(plain)), design.id() + ": placed " + placed);
                check(WindowFixtures.pose(placed).equals(WindowFixtures.pose(plainPredicted)), design.id() + " " + zone
                        + ": pose differs from the plain piece's " + placed);
                check(look.equals(windowLook(helper, helper.absolutePos(at))), design.id() + ": look lost");
                if (zone != WindowTargetZones.Zone.CENTER) {
                    check(WindowPaneBlock.connections(placed).isEmpty(), design.id() + ": a flush crossed window connected " + placed);
                }
                ItemStack pick = AppearanceFixtures.pick(helper, helper.absolutePos(at));
                check(pick.is(ModItems.window(plain)) && AppearanceStacks.read(pick).equals(look), design.id() + ": pick " + pick);
                clearDrops(helper);
                player.gameMode.destroyBlock(helper.absolutePos(at));
                check(dropped(helper, ModItems.window(plain), look) == 1 && dropped(helper, ModItems.CROSS_BEAMS.get(), look) == 1,
                        design.id() + ": drop");
                clearDrops(helper);
                helper.setBlock(at, Blocks.AIR);
            }
        }
        helper.succeed();
    }
}
