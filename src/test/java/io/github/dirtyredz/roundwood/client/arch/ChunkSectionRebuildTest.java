package io.github.dirtyredz.roundwood.client.arch;

import io.github.dirtyredz.roundwood.client.render.world.StaticQuadTransform;
import io.github.dirtyredz.roundwood.client.render.world.StaticWorldModel;
import com.google.gson.Gson;
import com.mojang.blaze3d.platform.NativeImage;
import io.github.dirtyredz.roundwood.block.arch.assembly.*;
import io.github.dirtyredz.roundwood.block.arch.geometry.*;
import io.github.dirtyredz.roundwood.block.arch.trim.ArchPieceTrimGeometry;
import io.github.dirtyredz.roundwood.block.attachment.*;
import io.github.dirtyredz.roundwood.block.door.assembly.*;
import io.github.dirtyredz.roundwood.block.door.camouflage.*;
import io.github.dirtyredz.roundwood.block.door.frame.*;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.block.window.geometry.*;
import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowProfile;
import io.github.dirtyredz.roundwood.block.window.geometry.trim.WindowTrimSurfaces;
import io.github.dirtyredz.roundwood.client.door.arch.DoorArchModels;
import io.github.dirtyredz.roundwood.client.render.*;
import io.github.dirtyredz.roundwood.testsupport.QuadModel;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.client.resources.metadata.animation.*;
import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import org.junit.jupiter.api.Test;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

/** Captured census positions, production pre-lighting bakers, synthetic sprites. No tessellation. */
class ChunkSectionRebuildTest {
    static { io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap.ensure(); }
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("test", "donor");
    private static final class Sprite extends TextureAtlasSprite {
        Sprite(SpriteContents contents) { super(TEXTURE, contents, 256, 256, 32, 64); }
    }
    private record Sample(String type, int[] pos, String state, String nbt) {
        BlockPos position() { return new BlockPos(pos[0], pos[1], pos[2]); }
        String id() { return state.substring(state.indexOf(':') + 1, state.indexOf('}')); }
        String property(String name, String fallback) {
            var match = java.util.regex.Pattern.compile("(?:\\[|,)" + name + "=([^,\\]]+)").matcher(state);
            return match.find() ? match.group(1) : fallback;
        }
    }
    private record Draw(String group, BlockPos cell, Supplier<List<BakedQuad>> bake) {}

    @Test void censusStaticGeometryIsCollectedByItsActualOwningSection() throws Exception {
        try (var input = getClass().getResourceAsStream("/fps/static-census.json");
             var contents = new SpriteContents(TEXTURE, new FrameSize(16,16), new NativeImage(16,16,false), net.minecraft.server.packs.resources.ResourceMetadata.EMPTY)) {
            assertNotNull(input);
            var samples = new Gson().fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), Sample[].class);
            var sprite = new Sprite(contents);
            var draws = new ArrayList<Draw>();
            for (var sample : samples) {
                var nbt = TagParser.parseTag(sample.nbt());
                switch (sample.type()) {
                    case "roundwood:arch_piece" -> arch(sample, nbt, sprite, draws);
                    case "roundwood:round_door" -> door(sample, nbt, sprite, draws);
                    case "roundwood:round_window" -> window(sample, nbt, sprite, draws);
                    default -> fail(sample.type());
                }
            }
            assertEquals(215, draws.size(), "captured migrated static-piece census");
            assertEquals(135, draws.stream().filter(draw -> draw.group().equals("fitted")).count());
            var sections = new TreeMap<String, List<Draw>>();
            for (var draw : draws) sections.computeIfAbsent(SectionPos.of(draw.cell()).toString(), key -> new ArrayList<>()).add(draw);
            int total = 0, fitted = 0;
            for (var entry : sections.entrySet()) {
                var quads = new ArrayList<BakedQuad>();
                var models = new ArrayList<net.minecraft.client.resources.model.BakedModel>();
                long coldStart = System.nanoTime();
                for (var draw : entry.getValue()) {
                    var built = draw.bake().get(); quads.addAll(built); models.add(new QuadModel(built, sprite));
                    if (draw.group().equals("fitted")) fitted += built.size();
                }
                double cold = (System.nanoTime() - coldStart) / 1e6;
                assertEquals(quads, StaticWorldModel.collect(models, null, RandomSource.create(0), null));
                long[] warm = new long[5];
                for (int run = 0; run < warm.length; run++) {
                    long start = System.nanoTime(); int count = 0;
                    for (var draw : entry.getValue()) count += draw.bake().get().size();
                    warm[run] = System.nanoTime() - start; assertEquals(quads.size(), count);
                }
                Arrays.sort(warm); total += quads.size();
                System.out.printf("Census section %s: models=%d quads=%d cold=%.3f ms warm-median=%.3f ms%n",
                        entry.getKey(), models.size(), quads.size(), cold, warm[2]/1e6);
            }
            assertEquals(13410, fitted);
            System.out.printf("Census total: %d sections, %d static submissions -> 0 in chunk mode, %d pre-lighting quads%n", sections.size(), draws.size(), total);
        }
    }
    private static void arch(Sample sample, CompoundTag nbt, Sprite sprite, List<Draw> draws) {
        var design = Arrays.stream(ArchPieceDesign.values()).filter(value -> sample.id().startsWith(value.id())).findFirst().orElseThrow();
        var side = KeyholeShoulderProfile.Side.valueOf(sample.property("side", "left").toUpperCase(Locale.ROOT));
        var thickness = sample.id().endsWith("_thin") ? ArchPieceThickness.THIN : ArchPieceThickness.FULL;
        var depth = ArchPieceDepth.valueOf(sample.property("depth", "front").toUpperCase(Locale.ROOT));
        var facing = Direction.byName(sample.property("facing", "south"));
        var assembly = new ArchPieceLayout.Assembly(sample.position(), facing, design, side, thickness, depth);
        for (int layer = 0; layer < 3; layer++) {
            var face = layer == 1 ? AttachmentFace.FRONT : AttachmentFace.BACK;
            String address = layer == 1 ? "Front" : "Back";
            if (layer > 0 && !nbt.getCompound("ArchPieceTrim").contains(address)) continue;
            var material = layer == 0 ? nbt.getCompound("RoundwoodAppearance") : nbt.getCompound("ArchPieceTrim").getCompound(address).getCompound("RoundwoodAppearance");
            boolean masonry = material.getString("Kind").equals("masonry");
            var band = layer == 0 ? new double[]{assembly.back(), assembly.front()} : ArchPieceTrimGeometry.depth(face, assembly.back(), assembly.front());
            var kind = layer == 0 ? ArchPieceModels.Layer.BODY : ArchPieceModels.Layer.CASING;
            for (var cell : assembly.parts()) draws.add(new Draw("arch", assembly.pos(cell), () ->
                    ArchPieceModels.quads(design, side, cell, band[0], band[1], facing, sprite, masonry, kind)));
        }
    }
    private static void door(Sample sample, CompoundTag nbt, Sprite sprite, List<Draw> draws) {
        var design = Arrays.stream(DoorDesign.values()).filter(value -> sample.id().equals(value.id())).findFirst().orElseThrow();
        var facing = Direction.byName(sample.property("facing", "south"));
        var assembly = new DoorLayout.Assembly(sample.position(), facing, DoorHingeSide.LEFT, design,
                DoorDepth.valueOf(sample.property("depth", "front").toUpperCase(Locale.ROOT)));
        var frame = nbt.getCompound("DoorArch");
        if (!frame.isEmpty()) {
            var product = ArchPieceDesign.valueOf(frame.getString("Product"));
            var thickness = ArchPieceThickness.valueOf(frame.getString("Thickness"));
            var depth = ArchPieceDepth.valueOf(frame.getString("Depth"));
            for (var layer : DoorArchGeometry.Layer.values()) {
                String address = layer == DoorArchGeometry.Layer.FRONT_CASING ? "Front" : "Back";
                if (layer != DoorArchGeometry.Layer.BODY && !frame.getCompound("Casings").contains(address)) continue;
                var mat = layer == DoorArchGeometry.Layer.BODY ? frame.getCompound("Appearance") : frame.getCompound("Casings").getCompound(address).getCompound("RoundwoodAppearance");
                boolean masonry = mat.getString("Kind").equals("masonry");
                for (var cell : ArchPieceCell.of(product)) draws.add(new Draw("fitted", sample.position().above(DoorArchFitPlan.topOffset(design)+cell.row).relative(facing.getClockWise(), cell.column),
                        () -> DoorArchModels.quads(design,thickness,depth,layer,cell,facing,sprite,masonry)));
            }
        }
        if (nbt.contains("Camouflage")) for (var part : assembly.parts()) draws.add(new Draw("camouflage", assembly.pos(part), () -> {
            double shift = assembly.depth().offset()/16;
            return io.github.dirtyredz.roundwood.client.door.CamouflageModels.bake(design, facing, part,
                    CamouflageCoverage.of(design, !frame.isEmpty()), side -> new io.github.dirtyredz.roundwood.client.material.DonorSprites.FaceMaterial(sprite, -1))
                    .stream().map(quad -> StaticQuadTransform.translate(quad, -facing.getStepX()*shift, 0, -facing.getStepZ()*shift, -1)).toList();
        }));
    }
    private static void window(Sample sample, CompoundTag nbt, Sprite sprite, List<Draw> draws) {
        var profile = Arrays.stream(WindowProfile.values()).filter(value -> sample.id().equals(value.id())).findFirst().orElseThrow();
        var pose = WindowPose.upright(sample.property("axis", "z").equals("x") ? Direction.EAST : Direction.NORTH, WindowDepth.CENTER);
        for (var slot : List.of(TrimSlot.PERIMETER, TrimSlot.SILL)) for (var face : AttachmentFace.values()) {
            String tag = slot == TrimSlot.PERIMETER ? "WindowTrim" : "WindowSillTrim";
            if (!nbt.getCompound(tag).contains(face == AttachmentFace.FRONT ? "Front" : "Back")) continue;
            var side = WindowFaceIdentity.side(pose, false, face);
            draws.add(new Draw("window", sample.position(), () -> WindowTrimSurfaces.surfaces(slot, profile, pose, side).stream()
                    .map(surface -> SurfaceQuadBaker.bake(surface.backing(), surface.face(), sprite)).toList()));
        }
    }
}
