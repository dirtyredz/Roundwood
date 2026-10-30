package io.github.dirtyredz.roundwood.client.render;

import io.github.dirtyredz.roundwood.geometry.Box;
import io.github.dirtyredz.roundwood.geometry.FaceUv;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.core.Direction;
import org.joml.Vector3f;

/** Position-projected, tile-bounded surface baking shared by pane composition and projecting trims. */
public final class SurfaceQuadBaker {
    private static final FaceBakery BAKERY = new FaceBakery();
    private SurfaceQuadBaker() { }

    public static BakedQuad bake(Box box, Direction side, TextureAtlasSprite sprite) {
        var element = new BlockElementFace(null, -1, "", new BlockFaceUV(FaceUv.tiled(box, side), 0));
        return BAKERY.bakeQuad(new Vector3f((float) box.minX(), (float) box.minY(), (float) box.minZ()),
                new Vector3f((float) box.maxX(), (float) box.maxY(), (float) box.maxZ()), element, sprite, side,
                BlockModelRotation.X0_Y0, null, true);
    }
}
