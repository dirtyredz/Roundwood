package io.github.dirtyredz.roundwood.client.arch;

import com.mojang.blaze3d.platform.NativeImage;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceCell;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceDesign;
import io.github.dirtyredz.roundwood.block.arch.geometry.KeyholeShoulderProfile.Side;
import io.github.dirtyredz.roundwood.block.arch.trim.ArchPieceTrimGeometry;
import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;
import java.util.EnumSet;
import static org.junit.jupiter.api.Assertions.*;

class ArchPieceModelsTest {
    static { MinecraftBootstrap.ensure(); }
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("test", "donor");
    private static final class Sprite extends TextureAtlasSprite {
        Sprite(SpriteContents contents) { super(TEXTURE, contents, 256, 256, 32, 64); }
    }

    @Test void fittedRebatesBakeWithBoundedDonorUvsAndOutwardWinding() {
        try(var contents=new SpriteContents(TEXTURE,new FrameSize(16,16),new NativeImage(16,16,false),AnimationMetadataSection.EMPTY)) {
            var sprite=new Sprite(contents);
            for(var door:io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign.values())
                for(var thickness:io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness.values())
                    for(var depth:io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth.values()) {
                        if(!io.github.dirtyredz.roundwood.block.door.frame.DoorArchClearance.accepts(door,thickness,depth)) continue;
                        var top=io.github.dirtyredz.roundwood.block.door.frame.DoorArchFitPlan.topFor(door);
                        for(var layer:io.github.dirtyredz.roundwood.block.door.frame.DoorArchGeometry.Layer.values()) for(var cell:ArchPieceCell.of(top))
                            for(var facing:Direction.Plane.HORIZONTAL) for(boolean masonry:new boolean[]{false,true}) {
                                var quads=ArchPieceModels.bakeSurfaces(top,cell,
                                        io.github.dirtyredz.roundwood.block.door.frame.DoorArchGeometry.surfaces(door,thickness,depth,layer,cell.column,cell.row),facing,sprite,masonry);
                                for(var quad:quads) {if(masonry) checkUvDeltas(quad,sprite);else checkQuad(quad,sprite);}
                                area(quads);
                            }
                    }
        }
    }

    @Test void thinBodiesStayExactAndCasingFacesSeparateWithoutTextureBleed() {
        try (var contents = new SpriteContents(TEXTURE, new FrameSize(16, 16), new NativeImage(16, 16, false), AnimationMetadataSection.EMPTY)) {
            var sprite = new Sprite(contents);
            for (var design : ArchPieceDesign.values()) for (var side : Side.values()) for (var cell : ArchPieceCell.of(design))
                for (var depth : io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth.values())
                    for (var facing : Direction.Plane.HORIZONTAL) for (boolean masonry : new boolean[]{false, true}) {
                        var body = ArchPieceModels.quads(design, side, cell, depth.back(), depth.back() + 3, facing, sprite, masonry, ArchPieceModels.Layer.BODY);
                        for (var quad : body) { if (masonry) checkUvDeltas(quad, sprite); else checkQuad(quad, sprite); }
                        for (var face : AttachmentFace.values()) {
                            var band = ArchPieceTrimGeometry.depth(face, depth.back(), depth.back() + 3);
                            var casing = ArchPieceModels.quads(design, side, cell, band[0], band[1], facing, sprite, masonry, ArchPieceModels.Layer.CASING);
                            for (var quad : casing) { if (masonry) checkUvDeltas(quad, sprite); else checkQuad(quad, sprite); }
                            // A contact surface must move along its normal, including jamb/reveal faces.
                            var unseparated = ArchPieceModels.quads(design, side, cell, band[0], band[1], facing, sprite, masonry, ArchPieceModels.Layer.BODY);
                            if (masonry) continue;
                            assertEquals(unseparated.size(), casing.size());
                            for (int i = 0; i < casing.size(); i++) {
                                assertFalse(java.util.Arrays.equals(unseparated.get(i).getVertices(), casing.get(i).getVertices()), "casing face remained coplanar");
                            }
                        }
                    }
        }
    }

    @Test void bodyAndCasingsTileTheDonorAtBlockScaleInEveryFacingAndSide() {
        try (var contents = new SpriteContents(TEXTURE, new FrameSize(16, 16), new NativeImage(16, 16, false), AnimationMetadataSection.EMPTY)) {
            var sprite = new Sprite(contents);
            var seen = EnumSet.noneOf(Direction.class);
            for (var design : ArchPieceDesign.values()) for (var side : Side.values()) for (var cell : ArchPieceCell.of(design))
                for (var facing : Direction.Plane.HORIZONTAL) {
                    for (double[] depth : new double[][]{{0,16}, ArchPieceTrimGeometry.depth(AttachmentFace.FRONT), ArchPieceTrimGeometry.depth(AttachmentFace.BACK)}) {
                        var quads = ArchPieceModels.quads(design, side, cell, depth[0], depth[1], facing, sprite, false, depth[1] - depth[0] == 1 ? ArchPieceModels.Layer.CASING : ArchPieceModels.Layer.BODY);
                        boolean empty = design == ArchPieceDesign.TOP_2X3 && cell.column == 1 && cell.row == 0;
                        assertEquals(empty, quads.isEmpty());
                        for (var quad : quads) { seen.add(quad.getDirection()); checkQuad(quad, sprite); }
                    }
                }
            assertEquals(EnumSet.allOf(Direction.class), seen);
        }
    }

    @Test void donorSubstitutionPaintAndMasonryPolicyAreExplicit() {
        var cherry = Appearance.wood(ResourceLocation.fromNamespaceAndPath("minecraft", "cherry_planks"));
        var masonry = Appearance.masonry(ResourceLocation.fromNamespaceAndPath("minecraft", "bricks"));
        for (var appearance : new Appearance[]{Appearance.DEFAULT, cherry, cherry.painted(DyeColor.BLUE), masonry}) {
            var look = ArchPieceModels.look(appearance, ignored -> TEXTURE);
            assertEquals(TEXTURE, look.texture());
            assertEquals(appearance == masonry, look.masonry());
        }
    }

    @Test void masonryBodyCasingsAndItemsShareTheSamePartitionAndUvScale() {
        try (var contents = new SpriteContents(TEXTURE, new FrameSize(16, 16), new NativeImage(16, 16, false), AnimationMetadataSection.EMPTY)) {
            var sprite = new Sprite(contents);
            int mortarQuads = 0;
            for (var design : ArchPieceDesign.values()) for (var side : Side.values()) for (var cell : ArchPieceCell.of(design))
                for (var facing : Direction.Plane.HORIZONTAL) {
                    var plain = ArchPieceModels.quads(design, side, cell, 0, 16, facing, sprite);
                    var masonry = ArchPieceModels.quads(design, side, cell, 0, 16, facing, sprite, true);
                    assertTrue(plain.stream().noneMatch(q -> q.getVertices()[3] == ArchMasonryModels.MORTAR), "wood has no mortar");
                    mortarQuads += masonry.stream().filter(q -> q.getVertices()[3] == ArchMasonryModels.MORTAR).count();
                    assertEquals(area(plain), area(masonry), 1e-4, "partition must preserve total drawn area");
                    var body = planarSignature(masonry, facing);
                    for (double[] depth : new double[][]{{16,17},{-1,0}})
                        assertEquals(body, planarSignature(ArchPieceModels.quads(design, side, cell, depth[0], depth[1], facing, sprite, true, ArchPieceModels.Layer.CASING), facing));
                    for (var quad : masonry) checkUvDeltas(quad, sprite);
                }
            assertTrue(mortarQuads > 0, "masonry quads carry the darkened donor colour");
            for (var design : ArchPieceDesign.values()) for (boolean masonry : new boolean[]{false,true}) {
                var item = ArchPieceModels.itemQuads(design, sprite, masonry);
                int index = 0;
                for (var cell : ArchPieceCell.of(design)) for (var body : ArchPieceModels.quads(design, Side.LEFT, cell, 0, 16, Direction.NORTH, sprite, masonry)) {
                    var whole = item.get(index++);
                    int[] before = body.getVertices(), after = whole.getVertices();
                    int stride = before.length / 4;
                    for (int i = 0; i < before.length; i += stride) {
                        double expectedX = Float.intBitsToFloat(before[i]) + cell.column - (design == ArchPieceDesign.TOP_2X3 ? 1 : 0);
                        assertEquals(expectedX, Float.intBitsToFloat(after[i]), 1e-6);
                        assertEquals(Float.intBitsToFloat(before[i + 1]) + cell.row, Float.intBitsToFloat(after[i + 1]), 1e-6);
                        for (int j = 2; j < stride; j++) assertEquals(before[i + j], after[i + j], "only XY positions may move");
                        for (int j = 0; j < 3; j++) assertTrue(Float.intBitsToFloat(after[i + j]) >= -1 && Float.intBitsToFloat(after[i + j]) <= 2);
                    }
                }
                assertEquals(item.size(), index);
            }
        }
    }

    private static java.util.List<String> planarSignature(java.util.List<BakedQuad> quads, Direction facing) {
        var out = new java.util.ArrayList<String>();
        for (var quad : quads) if (quad.getDirection().getAxis() == facing.getAxis()) {
            var vertices = quad.getVertices().clone();
            int stride = vertices.length / 4;
            for (int i = 0; i < vertices.length; i += stride) vertices[i + (facing.getAxis() == Direction.Axis.X ? 0 : 2)] = 0;
            out.add(quad.getDirection() + java.util.Arrays.toString(vertices));
        }
        return out;
    }

    private static double area(java.util.List<BakedQuad> quads) {
        double result = 0;
        for (var quad : quads) {
            int[] data = quad.getVertices(); int stride = data.length / 4;
            for (int triangle = 1; triangle <= 2; triangle++) {
                var a = new net.minecraft.world.phys.Vec3(Float.intBitsToFloat(data[0]), Float.intBitsToFloat(data[1]), Float.intBitsToFloat(data[2]));
                int b = triangle * stride, c = (triangle + 1) * stride;
                var ab = new net.minecraft.world.phys.Vec3(Float.intBitsToFloat(data[b]), Float.intBitsToFloat(data[b + 1]), Float.intBitsToFloat(data[b + 2])).subtract(a);
                var ac = new net.minecraft.world.phys.Vec3(Float.intBitsToFloat(data[c]), Float.intBitsToFloat(data[c + 1]), Float.intBitsToFloat(data[c + 2])).subtract(a);
                var cross = ab.cross(ac);
                assertTrue(cross.dot(net.minecraft.world.phys.Vec3.atLowerCornerOf(quad.getDirection().getNormal())) >= -1e-8, "outward winding");
                result += cross.length() / 2;
            }
        }
        return result;
    }

    private static void checkUvDeltas(BakedQuad quad, Sprite sprite) {
        int[] data = quad.getVertices(); int stride = data.length / 4;
        double[] first = null, firstUv = null;
        for (int i = 0; i < data.length; i += stride) {
            double x = Float.intBitsToFloat(data[i]) * 16, y = Float.intBitsToFloat(data[i + 1]) * 16, z = Float.intBitsToFloat(data[i + 2]) * 16;
            double[] projection = switch (quad.getDirection()) {
                case NORTH -> new double[]{-x,-y}; case SOUTH -> new double[]{x,-y};
                case WEST -> new double[]{z,-y}; case EAST -> new double[]{-z,-y};
                case UP -> new double[]{x,z}; case DOWN -> new double[]{x,-z};
            };
            double[] uv = {sprite.getUOffset(Float.intBitsToFloat(data[i + 4])), sprite.getVOffset(Float.intBitsToFloat(data[i + 5]))};
            if (first == null) { first = projection; firstUv = uv; }
            for (int axis = 0; axis < 2; axis++) {
                assertTrue(uv[axis] >= -.0001 && uv[axis] <= 16.0001);
                assertEquals(projection[axis] - first[axis], (uv[axis] - firstUv[axis]) / ((1 - sprite.uvShrinkRatio()) * .998), .0002);
            }
        }
    }

    private static void checkQuad(BakedQuad quad, Sprite sprite) {
        int[] vertices = quad.getVertices();
        int stride = vertices.length / 4;
        double[][] projected = new double[4][];
        double[] min = {Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY};
        double[] max = {Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY};
        for (int vertex = 0; vertex < 4; vertex++) {
            int i = vertex * stride;
            double x = Float.intBitsToFloat(vertices[i]) * 16;
            double y = Float.intBitsToFloat(vertices[i + 1]) * 16;
            double z = Float.intBitsToFloat(vertices[i + 2]) * 16;
            // Independent vanilla projection, including signs: vertical V always decreases with Y.
            projected[vertex] = switch (quad.getDirection()) {
                case DOWN -> new double[] {x, 16 - z};
                case UP -> new double[] {x, z};
                case NORTH -> new double[] {16 - x, 16 - y};
                case SOUTH -> new double[] {x, 16 - y};
                case WEST -> new double[] {z, 16 - y};
                case EAST -> new double[] {16 - z, 16 - y};
            };
            for (int axis = 0; axis < 2; axis++) {
                min[axis] = Math.min(min[axis], projected[vertex][axis]);
                max[axis] = Math.max(max[axis], projected[vertex][axis]);
            }
        }
        for (int axis = 0; axis < 2; axis++) {
            assertTrue(max[axis] > min[axis], "nonzero geometric span");
            double offset = Math.floor(min[axis] / 16) * 16;
            double centre = (min[axis] + max[axis]) / 2 - offset;
            double uvMin = Double.POSITIVE_INFINITY, uvMax = Double.NEGATIVE_INFINITY;
            for (int vertex = 0; vertex < 4; vertex++) {
                double atlasUv = Float.intBitsToFloat(vertices[vertex * stride + 4 + axis]);
                double uv = axis == 0 ? sprite.getUOffset((float) atlasUv) : sprite.getVOffset((float) atlasUv);
                // FaceBakery shrinks toward the centre, then fillVertex mixes 0.999 of this corner
                // with 0.001 of the opposite corner. Undo both vanilla anti-bleed insets only.
                double unshrunk = centre + (uv - centre) / ((1 - sprite.uvShrinkRatio()) * 0.998);
                assertEquals(projected[vertex][axis] - offset, unshrunk, 0.0001,
                        quad.getDirection() + " vertex " + vertex + " UV axis " + axis);
                assertTrue(uv >= 0 && uv <= 16, "UV stays inside its sprite");
                uvMin = Math.min(uvMin, unshrunk);
                uvMax = Math.max(uvMax, unshrunk);
            }
            assertEquals(max[axis] - min[axis], uvMax - uvMin, 0.0001, "UV span equals geometric extent");
        }
    }
}
