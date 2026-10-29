package io.github.dirtyredz.roundwood.block.arch;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlockEntity;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPiecePlacement;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.block.arch.trim.ArchPieceTrimNbt;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import java.util.Optional;
import static io.github.dirtyredz.roundwood.fixture.AppearanceFixtures.*;
import static io.github.dirtyredz.roundwood.fixture.ArchPieceFixtures.*;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.*;

@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArchPieceTrimGameTests {
    private static final BlockPos ANCHOR = new BlockPos(3, 1, 3);
    private ArchPieceTrimGameTests() {}

    private static ArchPieceBlockEntity entity(GameTestHelper helper, ArchPieceLayout.Assembly piece) {
        return (ArchPieceBlockEntity) helper.getLevel().getBlockEntity(piece.anchor());
    }

    /** Surface point transformed independently, with the owning cell on the inward side. */
    private static BlockHitResult hit(ArchPieceLayout.Assembly piece, Direction surface) {
        double x = piece.side() == Side.RIGHT && piece.design().shoulder() ? 15.5 : .5;
        double y = piece.design().shoulder() ? 8.5 : piece.design().height() - .5;
        double z = surface == Direction.SOUTH ? piece.front() : surface == Direction.NORTH ? piece.back() : (piece.back() + piece.front()) / 2;
        if (surface == Direction.WEST) x = 0;
        if (surface == Direction.EAST) x = 16;
        int turns = VoxelShapeFactory.quarterTurns(piece.facing());
        for (int i = 0; i < turns; i++) { double nx = 16 - z; z = x; x = nx; }
        Direction normal = VoxelShapeFactory.turn(surface, turns);
        Vec3 point = Vec3.atLowerCornerOf(piece.anchor()).add(x / 16, y / 16, z / 16);
        BlockPos owner = BlockPos.containing(point.subtract(Vec3.atLowerCornerOf(normal.getNormal()).scale(.0001)));
        return new BlockHitResult(point, normal, owner, false);
    }

    @GameTest(template = TEMPLATE)
    public static void independentFacesRoutingRefusalsAndShearsAcrossEveryPose(GameTestHelper helper) {
        floor(helper);
        for (var design : ArchPieceDesign.values()) for (var facing : Direction.Plane.HORIZONTAL)
            for (var side : design.shoulder() ? Side.values() : new Side[]{Side.LEFT})
            for (var thickness : io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness.values())
            for (var depth : thickness == io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness.FULL
                    ? new io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth[]{io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth.FRONT}
                    : io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth.values()) {
                var piece = new ArchPieceLayout.Assembly(helper.absolutePos(ANCHOR), facing, design, side, thickness, depth);
                check(ArchPiecePlacement.placeAt(helper.getLevel(), ModBlocks.archPiece(design, thickness), piece, Appearance.DEFAULT), "place");
                var player = player(helper, "arch_trim_" + design + facing + side + thickness + depth, GameType.SURVIVAL, facing);
                var front = hit(piece, Direction.SOUTH);
                var back = hit(piece, Direction.NORTH);
                var reveal = hit(piece, side == Side.RIGHT && design.shoulder() ? Direction.EAST : Direction.WEST);
                var trims = styled(ModItems.ROUND_TRIM.get(), SPRUCE); trims.setCount(3);
                use(helper, player, trims, ItemStack.EMPTY, front, false);
                check(trims.getCount() == 2 && entity(helper, piece).trims().front().equals(Optional.of(SPRUCE)), "front fit/payment");
                use(helper, player, trims, ItemStack.EMPTY, front, true);
                check(trims.getCount() == 2, "occupied face charged");
                var rear = styled(ModItems.ROUND_TRIM.get(), BRICKS);
                use(helper, player, ItemStack.EMPTY, rear, back, true);
                check(rear.isEmpty() && entity(helper, piece).trims().back().equals(Optional.of(BRICKS)), "offhand back fit");
                var birch = new ItemStack(Items.BIRCH_PLANKS, 2);
                use(helper, player, birch, ItemStack.EMPTY, front, false);
                check(birch.getCount() == 1 && entity(helper, piece).trims().front().equals(Optional.of(BIRCH)), "casing donor routing");
                var warped = new ItemStack(Items.WARPED_PLANKS, 2);
                use(helper, player, warped, ItemStack.EMPTY, reveal, true);
                check(warped.getCount() == 1 && entity(helper, piece).appearance().equals(WARPED), "body donor routing");
                check(entity(helper, piece).trims().back().equals(Optional.of(BRICKS)), "back changed with body");
                for (var item : new net.minecraft.world.item.Item[]{ModItems.ARCH_SILL_TRIM.get(), ModItems.CROSS_BEAMS.get()}) {
                    var refused = new ItemStack(item, 2);
                    use(helper, player, refused, ItemStack.EMPTY, front, true);
                    check(refused.getCount() == 2 && entity(helper, piece).trims().count() == 2, "unsupported attachment changed host/payment");
                }
                var shears = new ItemStack(Items.SHEARS);
                use(helper, player, shears, ItemStack.EMPTY, front, false);
                check(shears.getDamageValue() == 1 && !entity(helper, piece).trims().has(AttachmentFace.FRONT), "shears did not remove front");
                int refunds = 0;
                for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                    var stack = player.getInventory().getItem(slot);
                    if (stack.is(ModItems.ROUND_TRIM.get()) && AppearanceStacks.read(stack).equals(BIRCH)) refunds += stack.getCount();
                }
                check(refunds == 1 && entity(helper, piece).trims().back().equals(Optional.of(BRICKS)), "shears refund/back");
                use(helper, player, shears, ItemStack.EMPTY, front, false);
                check(shears.getDamageValue() == 1, "empty face wore shears");
                helper.setBlock(ANCHOR, Blocks.AIR);
                clearDrops(helper);
            }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void anyCellHarvestKeepsBodyAndBothTrimLooks(GameTestHelper helper) {
        floor(helper);
        for (var design : ArchPieceDesign.values()) {
            var piece = assembly(helper, ANCHOR, Direction.NORTH, design, Side.LEFT);
            for (var cell : piece.parts()) {
                check(ArchPiecePlacement.placeAt(helper.getLevel(), ModBlocks.archPiece(design), piece, WARPED), "place");
                check(entity(helper, piece).setTrims(new FaceTrims(Optional.of(BIRCH), Optional.of(BRICKS))), "fit");
                var picked = pick(helper, piece.pos(cell));
                check(picked.is(ModItems.archPiece(design)) && AppearanceStacks.read(picked).equals(WARPED), "pick body look");
                var player = player(helper, "arch_drop_" + design + cell, GameType.SURVIVAL, Direction.NORTH);
                check(player.gameMode.destroyBlock(piece.pos(cell)), "harvest");
                assertAir(helper, piece);
                check(dropped(helper, ModItems.archPiece(design), WARPED) == 1, "body drop");
                check(dropped(helper, ModItems.ROUND_TRIM.get(), BIRCH) == 1, "front drop");
                check(dropped(helper, ModItems.ROUND_TRIM.get(), BRICKS) == 1, "back drop");
                check(dropped(helper, ModItems.ROUND_TRIM.get(), WARPED) == 0, "body look leaked to trim");
                clearDrops(helper);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void trimPayloadRoundTripsAndUnknownVersionRefusesEdits(GameTestHelper helper) {
        var piece = assembly(helper, ANCHOR, Direction.NORTH, ArchPieceDesign.TOP_2X3, Side.LEFT);
        check(ArchPiecePlacement.placeAt(helper.getLevel(), ModBlocks.archPiece(piece.design()), piece, WARPED), "place");
        var anchor = entity(helper, piece);
        var looks = new FaceTrims(Optional.of(BIRCH), Optional.of(BRICKS));
        check(anchor.setTrims(looks), "fit");
        var saved = anchor.saveWithFullMetadata();
        check(saved.getCompound(ArchPieceTrimNbt.TAG).getInt("Version") == 1, "payload version");
        anchor.setTrims(FaceTrims.NONE);
        anchor.load(saved);
        check(anchor.trims().equals(looks) && anchor.appearance().equals(WARPED), "round trip");
        saved.getCompound(ArchPieceTrimNbt.TAG).putInt("Version", 99);
        anchor.load(saved);
        check(!anchor.trimsEditable() && !anchor.setTrims(looks), "future payload editable");
        check(anchor.saveWithFullMetadata().get(ArchPieceTrimNbt.TAG).equals(saved.get(ArchPieceTrimNbt.TAG)), "future payload lost");
        helper.succeed();
    }
}
