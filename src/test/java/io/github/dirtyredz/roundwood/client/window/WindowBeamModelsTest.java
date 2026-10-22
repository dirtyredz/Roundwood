package io.github.dirtyredz.roundwood.client.window;

import io.github.dirtyredz.roundwood.block.window.geometry.*;
import io.github.dirtyredz.roundwood.block.window.geometry.face.*;
import io.github.dirtyredz.roundwood.block.window.pane.WindowPaneState;
import io.github.dirtyredz.roundwood.geometry.FaceUv;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class WindowBeamModelsTest {
    static { io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap.ensure(); }
    @Test
    void compatibilityIconKeepsSourceRenderTypesAndDrawsItsOwnPass() {
        java.util.List<net.minecraft.client.renderer.RenderType> types = new java.util.ArrayList<>();
        var source = new net.minecraft.client.resources.model.BakedModel() {
            @Override public java.util.List<net.minecraft.client.renderer.block.model.BakedQuad> getQuads(
                    net.minecraft.world.level.block.state.BlockState state, Direction side, net.minecraft.util.RandomSource random) { return java.util.List.of(); }
            @Override public boolean useAmbientOcclusion() { return true; }
            @Override public boolean usesBlockLight() { return true; }
            @Override public boolean isGui3d() { return false; }
            @Override public boolean isCustomRenderer() { return false; }
            @Override public net.minecraft.client.renderer.texture.TextureAtlasSprite getParticleIcon() { return null; }
            @Override public net.minecraft.client.renderer.block.model.ItemOverrides getOverrides() { return null; }
            @Override public java.util.List<net.minecraft.client.renderer.RenderType> getRenderTypes(net.minecraft.world.item.ItemStack stack, boolean fabulous) { return types; }
        };
        var icon = WindowBeamModels.item(source, java.util.List.of(), null);
        org.junit.jupiter.api.Assertions.assertSame(types, icon.getRenderTypes(null, false));
        org.junit.jupiter.api.Assertions.assertSame(types, icon.getRenderTypes(null, true));
        org.junit.jupiter.api.Assertions.assertEquals(java.util.List.of(icon), icon.getRenderPasses(null, false));
        org.junit.jupiter.api.Assertions.assertTrue(icon.usesBlockLight());
        org.junit.jupiter.api.Assertions.assertFalse(icon.isGui3d());
    }

    @Test
    void composedSurfacesHaveIndependentMaterialsAndBoundedUvs() {
        for (var profile : WindowProfile.values()) {
            var pane = new WindowPaneState.Snapshot(new WindowDesign(profile, WindowPattern.CROSS),
                    WindowPose.upright(Direction.SOUTH, WindowDepth.CENTER), Set.of(Direction.NORTH, Direction.SOUTH));
            var surfaces = WindowBeamModels.surfaces(pane);
            assertEquals(Set.of("frame", "beam", "glass"), surfaces.stream().map(s -> s.texture()).collect(java.util.stream.Collectors.toSet()));
            for (var surface : surfaces) {
                for (float uv : FaceUv.tiled(surface.backing(), surface.face())) assertTrue(uv >= 0 && uv <= 16);
                if (surface.texture().equals("glass") && surface.face().getAxis() == Direction.Axis.Z
                        && surface.backing().minZ() >= 7 && surface.backing().maxZ() <= 9) {
                    var b = surface.backing();
                    for (double u = b.minX() + .25; u < b.maxX(); u += .5) for (double v = b.minY() + .25; v < b.maxY(); v += .5) {
                        assertFalse(profile.inOpening((int) u, (int) v) && (u >= 7.5 && u < 8.5 || v >= 7.5 && v < 8.5),
                                "glazing beneath a beam");
                    }
                }
            }
        }
    }
}
