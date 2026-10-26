package io.github.dirtyredz.roundwood.block.arch.trim;

import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceGeometry;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.attachment.storage.FaceTrimStore;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ArchPieceTrimGeometryTest {
    @Test void casingCoversExactlyTheBodyAtProudDepths() {
        for (var design : ArchPieceDesign.values()) for (var side : Side.values()) for (var face : AttachmentFace.values()) {
            var casing = ArchPieceTrimGeometry.casing(design, side, face);
            for (int y = 0; y < design.height(); y++) for (int x = 0; x < design.width(); x++) {
                final double px = x + .5, py = y + .5;
                long covering = casing.stream().filter(b -> b.minX() <= px && b.maxX() > px && b.minY() <= py && b.maxY() > py).count();
                assertEquals(design.contains(side, x, y) ? 1 : 0, covering);
            }
            for (var box : casing) {
                assertEquals(face == AttachmentFace.FRONT ? 16 : -1, box.minZ());
                assertEquals(face == AttachmentFace.FRONT ? 17 : 0, box.maxZ());
            }
            if (!design.shoulder()) assertTrue(ArchPieceGeometry.collisionCell(design, side, 0, 0).isEmpty());
        }
    }
}
