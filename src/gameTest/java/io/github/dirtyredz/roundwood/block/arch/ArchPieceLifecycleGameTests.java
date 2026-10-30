package io.github.dirtyredz.roundwood.block.arch;

import io.github.dirtyredz.roundwood.Roundwood;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlock;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceCell;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.fixture.assembly.AssemblyFamilies;
import io.github.dirtyredz.roundwood.fixture.assembly.SingleChunk;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import static io.github.dirtyredz.roundwood.fixture.door.DoorFixtures.*;

/** Variable-size assembly membership and shared lifecycle; full drop/trim guards join with resources. */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArchPieceLifecycleGameTests {
    private ArchPieceLifecycleGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void chunkBoundaryIntegrityDefersWithoutLoadingPartners(GameTestHelper helper) {
        var level = helper.getLevel();
        int y = helper.absolutePos(new BlockPos(0, 1, 0)).getY();
        try (SingleChunk chunk = SingleChunk.open(level, 2600, 2600)) {
            for (var family : AssemblyFamilies.archPieces()) {
                var probe = family.at(new BlockPos(chunk.minX(), y, chunk.midZ()), Direction.NORTH);
                if (probe.columns() < 2) continue;
                for (boolean east : new boolean[]{true, false}) {
                    int index = east ? 0 : probe.size() - 1;
                    BlockPos cell = new BlockPos(east ? chunk.maxX() : chunk.minX(), y, chunk.midZ());
                    var piece = probe.atCell(index, cell);
                    var state = piece.state(index);
                    AssemblyFamilies.writeRaw(level, cell, state);
                    try {
                        check(!piece.loaded(level) && !piece.complete(level) && piece.present(level) == 1, "unloaded membership read");
                        state.tick(level, cell, level.random);
                        check(level.getBlockState(cell).equals(state), "unloaded partner removed piece");
                        chunk.assertIsolated("modular integrity " + family);
                    } finally { AssemblyFamilies.writeRaw(level, cell, Blocks.AIR.defaultBlockState()); }
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void removingAnyCellLeavesTouchingPiecesAlone(GameTestHelper helper) {
        var level = helper.getLevel();
        BlockPos anchor = helper.absolutePos(new BlockPos(1, 1, 3));
        for (var family : AssemblyFamilies.archPieces()) {
            var piece = family.at(anchor, Direction.NORTH);
            var neighbour = piece.rightNeighbour();
            for (int index = 0; index < piece.size(); index++) {
                check(piece.place(level, AssemblyFamilies.LOOK), "placement failed " + family);
                check(neighbour.place(level, AssemblyFamilies.OTHER_LOOK), "neighbour placement failed " + family);
                for (int cell = 0; cell < piece.size(); cell++) {
                    check(piece.anchorLook(level, piece.pos(cell), piece.state(cell)).orElseThrow().equals(AssemblyFamilies.LOOK), "wrong anchor look");
                    for (var foreign : piece.foreignLayouts(cell)) check(!piece.isPart(foreign, cell), "foreign layout was a member");
                    check(piece.atCell(cell, piece.pos(cell)).anchor().equals(anchor), "wrong anchor resolution");
                }
                level.setBlockAndUpdate(piece.pos(index), Blocks.AIR.defaultBlockState());
                check(piece.allAir(level), "part removal left cells: " + family + "/" + index);
                check(neighbour.complete(level), "neighbour removed: " + family + "/" + index);
                neighbour.clear(level);
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void rotationsAndMirrorsPreserveProductAndReflectCellAndShoulder(GameTestHelper helper) {
        for (var family : AssemblyFamilies.archPieces()) for (Direction facing : Direction.Plane.HORIZONTAL) {
            var piece = family.at(BlockPos.ZERO, facing);
            var block = (ArchPieceBlock) family.block();
            for (int i = 0; i < piece.size(); i++) {
                var state = piece.state(i);
                for (Rotation rotation : Rotation.values()) {
                    var rotated = state.rotate(rotation);
                    check(rotated.is(block), "rotation changed product");
                    check(rotated.getValue(ArchPieceBlock.FACING) == rotation.rotate(facing), "wrong facing rotation");
                    check(rotated.getValue(ArchPieceBlock.CELL) == state.getValue(ArchPieceBlock.CELL), "rotation changed authored cell");
                }
                for (Mirror mirror : new Mirror[]{Mirror.LEFT_RIGHT, Mirror.FRONT_BACK}) {
                    var mirrored = state.mirror(mirror);
                    check(mirrored.mirror(mirror).equals(state), "mirror is not involutive");
                    check(mirrored.getValue(ArchPieceBlock.CELL) == state.getValue(ArchPieceBlock.CELL).mirrored(block.design()), "wrong reflected cell");
                    check(mirrored.getValue(ArchPieceBlock.SIDE) == (block.design().shoulder()
                            ? state.getValue(ArchPieceBlock.SIDE).mirrored() : state.getValue(ArchPieceBlock.SIDE)), "wrong shoulder reflection");
                }
            }
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void invalidSmallProductCellCannotRemoveAValidNeighbour(GameTestHelper helper) {
        var level = helper.getLevel();
        var block = ModBlocks.archPiece(ArchPieceDesign.TOP_1X1);
        var family = AssemblyFamilies.archPieces().get(0);
        var piece = family.at(helper.absolutePos(new BlockPos(2, 1, 3)), Direction.NORTH);
        check(piece.place(level, AssemblyFamilies.LOOK), "small piece refused");
        BlockPos invalid = piece.anchor().east(2);
        level.setBlockAndUpdate(invalid, block.defaultBlockState().setValue(ArchPieceBlock.CELL, ArchPieceCell.LOWER_RIGHT));
        helper.runAfterDelay(3, () -> {
            check(level.getBlockState(invalid).isAir(), "invalid cell survived");
            check(piece.complete(level), "invalid cell destroyed real neighbour");
            helper.succeed();
        });
    }

}
