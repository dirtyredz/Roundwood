package io.github.dirtyredz.roundwood.showcase.section.arch;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlockEntity;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceLayout;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import io.github.dirtyredz.roundwood.showcase.ShowcaseCanvas;
import io.github.dirtyredz.roundwood.showcase.ShowcaseFrame;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import java.util.HashSet;
import java.util.Set;
import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;
import static io.github.dirtyredz.roundwood.showcase.ShowcaseChecks.*;

/** Read-back checks run immediately and after integrity ticks, in every showcase facing. */
public final class ArchSectionsChecks {
    private ArchSectionsChecks() {}
    public static void verify(ServerLevel level, ShowcaseFrame frame) {
        Set<BlockPos> owned = new HashSet<>();
        int f = ArchPiecesSection.F;
        piece(level, frame, owned, ArchPiecesSection.SMALL_R, 0, f, ArchPieceDesign.TOP_1X1, Side.LEFT, Appearance.DEFAULT, FaceTrims.NONE);
        piece(level, frame, owned, ArchPiecesSection.MEDIUM_R, 0, f, ArchPieceDesign.TOP_1X2, Side.LEFT, Appearance.DEFAULT, ArchPiecesSection.CASINGS);
        piece(level, frame, owned, ArchPiecesSection.LARGE_R, 0, f, ArchPieceDesign.TOP_2X3, Side.LEFT, ArchPiecesSection.BRICK, FaceTrims.both(ArchPiecesSection.BRICK));
        piece(level, frame, owned, ArchPiecesSection.SHOULDERS_R, 0, f, ArchPieceDesign.KEYHOLE_SHOULDER, Side.LEFT, Appearance.DEFAULT, FaceTrims.NONE);
        piece(level, frame, owned, ArchPiecesSection.SHOULDERS_R + 1, 0, f, ArchPieceDesign.KEYHOLE_SHOULDER, Side.RIGHT, Appearance.DEFAULT, FaceTrims.NONE);
        label(level, frame, owned, ArchPiecesSection.SMALL_R, f, "Arch top 1x1");
        label(level, frame, owned, ArchPiecesSection.MEDIUM_R, f, "Arch top 1x2");
        label(level, frame, owned, ArchPiecesSection.LARGE_R, f, "Arch top 2x3");
        label(level, frame, owned, ArchPiecesSection.SHOULDERS_R, f, "Shoulder pair");
        for (var design : ArchPieceDesign.values()) {
            int r = ArchPiecesSection.THIN_R + design.ordinal() * io.github.dirtyredz.roundwood.showcase.ShowcaseSpacing.pitch(3);
            label(level, frame, owned, r, f, "Thin " + design.id().replace("arch_", ""));
            for (var depth : io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth.values())
                for (var side : design.shoulder() ? Side.values() : new Side[]{Side.LEFT}) {
                    var thin = new ArchPieceLayout.Assembly(frame.pos(r + (side == Side.RIGHT ? 1 : 0), 2 * depth.ordinal(), f), frame.forward(), design, side,
                            io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness.THIN, depth);
                    check(thin.complete(level, ModBlocks.archPiece(design, thin.thickness())), "missing thin showcase pose");
                    for (var pos : thin.positions()) check(owned.add(pos), "overlapping thin showcase");
                }
        }
        verifyOwnsOnly(level, frame, "arch pieces", 0, ArchPiecesSection.R_MAX, f, f, owned);
        owned.clear();
        f = ArchCompositionsSection.F;
        for (int r : new int[] {ArchCompositionsSection.ORDINARY_R, ArchCompositionsSection.KEYHOLE_R, ArchCompositionsSection.TALL_R}) {
            boolean tall = r == ArchCompositionsSection.TALL_R;
            boolean keyhole = r == ArchCompositionsSection.KEYHOLE_R;
            int width = tall ? 3 : 2, height = tall ? 5 : 2;
            piece(level, frame, owned, r + 1, tall ? 3 : 1, f, tall ? ArchPieceDesign.TOP_2X3 : ArchPieceDesign.TOP_1X2,
                    Side.LEFT, Appearance.DEFAULT, FaceTrims.NONE);
            if (keyhole) {
                piece(level, frame, owned, r + 1, 0, f, ArchPieceDesign.KEYHOLE_SHOULDER, Side.LEFT, Appearance.DEFAULT, FaceTrims.NONE);
                piece(level, frame, owned, r + 2, 0, f, ArchPieceDesign.KEYHOLE_SHOULDER, Side.RIGHT, Appearance.DEFAULT, FaceTrims.NONE);
            }
            for (int y = 0; y < height; y++) for (int column : new int[] {r, r + width + 1}) {
                var pos = frame.pos(column, y, f);
                check(level.getBlockState(pos).is(Blocks.OAK_PLANKS), "composition column missing");
                check(owned.add(pos), "overlapping column");
            }
            label(level, frame, owned, r, f, tall ? "Taller opening" : keyhole ? "Keyhole opening" : "Ordinary opening");
        }
        verifyOwnsOnly(level, frame, "arch compositions", 0, ArchCompositionsSection.R_MAX, f, f, owned);
        verifyFitted(level, frame);
    }
    public static void verifyFitted(ServerLevel level, ShowcaseFrame frame) {
        for (int pose = 0; pose < 3; pose++) {
            var owned = new HashSet<BlockPos>();
            int f = ArchCompositionsSection.FITTED_F + pose * (io.github.dirtyredz.roundwood.showcase.ShowcaseSpacing.GAP + 2);
            var blocks = ModBlocks.doors();
            for (int index = 0; index < blocks.size(); index++) {
                int r = io.github.dirtyredz.roundwood.showcase.ShowcaseSpacing.FIRST_R + index * io.github.dirtyredz.roundwood.showcase.ShowcaseSpacing.pitch(3);
                var block = blocks.get(index); var pos = frame.pos(r, 0, f); var state = level.getBlockState(pos);
                check(state.is(block), "missing fitted showcase door");
                var door = io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout.of(pos, state);
                check(door.depth() == ArchCompositionsSection.leafDepth(pose) && door.complete(level, block), "fitted showcase pose/completeness");
                var entity = (io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity) level.getBlockEntity(pos);
                var attachment = entity.arch().orElseThrow();
                check(attachment.product() == io.github.dirtyredz.roundwood.block.door.frame.DoorArchFitPlan.topFor(block.design())
                        && attachment.thickness() == (pose == 0 ? io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness.FULL : io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness.THIN)
                        && attachment.depth() == (pose == 1 ? io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth.MIDDLE : io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth.FRONT), "fitted showcase attachment");
                check(attachment.appearance().equals(ArchCompositionsSection.FITTED_BODY) && attachment.casings().equals(ArchCompositionsSection.FITTED_CASINGS), "fitted showcase independent looks");
                for (var cell : door.positions()) check(owned.add(cell), "overlapping fitted showcase");
                label(level, frame, owned, r, f, "Fitted " + block.design().columns() + "x" + block.design().height()/16);
            }
            verifyOwnsOnly(level, frame, "fitted arches", 0, 28, f, f, owned);
        }
    }

    private static void piece(ServerLevel level, ShowcaseFrame frame, Set<BlockPos> owned, int r, int y, int f,
                              ArchPieceDesign design, Side side, Appearance look, FaceTrims trims) {
        var piece = new ArchPieceLayout.Assembly(frame.pos(r, y, f), frame.forward(), design, side);
        check(piece.complete(level, ModBlocks.archPiece(design)), "incomplete showcase " + design);
        for (var cell : piece.positions()) {
            check(owned.add(cell), "overlapping pieces");
            check(ArchPieceLayout.of(cell, level.getBlockState(cell)).equals(piece), "wrong ownership");
            check((level.getBlockEntity(cell) != null) == cell.equals(piece.anchor()), "entity outside anchor");
        }
        var entity = (ArchPieceBlockEntity) level.getBlockEntity(piece.anchor());
        check(entity.appearance().equals(look) && entity.trims().equals(trims), "showcase look/casings changed");
        var state = level.getBlockState(piece.anchor());
        var shape = state.getShape(level, piece.anchor());
        if (design.shoulder()) {
            check(shape.min(net.minecraft.core.Direction.Axis.Y) == 0, "showcase shoulder floats above floor");
            check(!state.getCollisionShape(level, piece.anchor()).isEmpty(), "shoulder lost collision");
        } else {
            check(state.getCollisionShape(level, piece.anchor()).isEmpty(), "showcase top blocks walking");
            if (design == ArchPieceDesign.TOP_1X1)
                check(shape.min(net.minecraft.core.Direction.Axis.Y) >= .5, "small top has lower fill");
        }
    }
    private static void label(ServerLevel level, ShowcaseFrame frame, Set<BlockPos> owned, int r, int f, String text) {
        var pos = frame.pos(ShowcaseCanvas.labelR(r), 0, f);
        check(signSays(level, pos, text), "missing arch label " + text);
        check(owned.add(pos), "overlapping label");
    }
}
