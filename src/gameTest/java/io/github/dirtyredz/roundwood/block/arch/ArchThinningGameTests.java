package io.github.dirtyredz.roundwood.block.arch;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlockEntity;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPiecePlacement;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.Optional;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.*;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.*;

@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArchThinningGameTests {
    @GameTest(template = TEMPLATE)
    public static void oneWayCraftingPreservesMetadataAndRefusesUnknownData(GameTestHelper helper) {
        var level = helper.getLevel();
        for (var design : ArchPieceDesign.values()) for (int slot = 0; slot < 9; slot++) {
            var input = AppearanceStacks.create(ModItems.archPiece(design), WARPED);
            input.setCount(4);
            input.setHoverName(Component.literal("Named arch"));
            input.getOrCreateTag().putInt("RepairCost", 7);
            var before = input.serializeNBT();
            var grid = grid(); grid.setItem(slot, input);
            var recipe = level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).orElseThrow();
            var output = recipe.assemble(grid, level.registryAccess());
            check(output.is(ModItems.archPiece(design, ArchPieceThickness.THIN)) && output.getCount() == 1, "wrong thinning output");
            check(output.getTag().equals(input.getTag()) && AppearanceStacks.read(output).equals(WARPED), "metadata lost");
            check(input.serializeNBT().equals(before), "recipe mutated input");
            grid.setItem(slot, output);
            check(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, grid, level).isEmpty(), "reverse/thin conversion exists");
            grid.setItem(slot, input);
            input.getOrCreateTag().putString("FuturePayload", "opaque");
            check(!recipe.matches(grid, level) && recipe.assemble(grid, level.registryAccess()).isEmpty(), "unknown payload converted");
            input.getOrCreateTag().remove("FuturePayload");
            input.getOrCreateTag().getCompound(AppearanceNbt.TAG).putInt("Version", 99);
            check(!recipe.matches(grid, level), "future appearance converted");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void thinCasingsSelectInsideCellsAndDropIndependentLooks(GameTestHelper helper) {
        floor(helper);
        var level = helper.getLevel();
        for (var design : ArchPieceDesign.values()) for (var depth : ArchPieceDepth.values()) {
            var block = ModBlocks.archPiece(design, ArchPieceThickness.THIN);
            var piece = new ArchPieceLayout.Assembly(helper.absolutePos(new BlockPos(3, 1, 3)), Direction.NORTH,
                    design, Side.LEFT, ArchPieceThickness.THIN, depth);
            for (var cell : piece.parts()) {
                check(ArchPiecePlacement.placeAt(level, block, piece, WARPED), "thin fixture refused");
                var anchor = (ArchPieceBlockEntity) level.getBlockEntity(piece.anchor());
                check(anchor.setTrims(new FaceTrims(Optional.of(BIRCH), Optional.of(BRICKS))), "thin casings refused");
                var state = level.getBlockState(piece.anchor());
                var shape = state.getShape(level, piece.anchor());
                check(Math.abs(shape.min(Direction.Axis.Z) - Math.max(0, depth.back() - 1) / 16) < 1e-9, "rear proxy wrong");
                check(Math.abs(shape.max(Direction.Axis.Z) - Math.min(16, depth.back() + 4) / 16) < 1e-9, "front proxy wrong");
                var player = player(helper, "thin_drop", GameType.SURVIVAL, Direction.NORTH);
                clearDrops(helper);
                check(player.gameMode.destroyBlock(piece.pos(cell)), "thin harvest refused");
                check(dropped(helper, block.asItem(), WARPED) == 1, "thin body drop");
                check(dropped(helper, ModItems.ROUND_TRIM.get(), BIRCH) == 1, "thin front casing drop");
                check(dropped(helper, ModItems.ROUND_TRIM.get(), BRICKS) == 1, "thin back casing drop");
                for (var pos : piece.positions()) check(level.getBlockState(pos).isAir(), "thin partner left behind");
                clearDrops(helper);
            }
        }
        helper.succeed();
    }

    private static TransientCraftingContainer grid() {
        return new TransientCraftingContainer(new AbstractContainerMenu(null, -1) {
            @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(Player player) { return true; }
        }, 3, 3);
    }
}
