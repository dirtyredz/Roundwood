package io.github.dirtyredz.roundwood.client.door;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.door.hardware.*;
import io.github.dirtyredz.roundwood.client.material.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import java.util.function.Function;

/** Slot markers are read before material substitution, then removed so panel tint cannot color hardware. */
public final class DoorHardwareModels {
    private DoorHardwareModels() {}
    public static BakedQuad retexture(BakedQuad quad,DoorHardware hardware){
        return retexture(quad,hardware,look->DonorSprites.atlas(AppearanceLooks.planks(look)));
    }
    static BakedQuad retexture(BakedQuad quad,DoorHardware hardware,Function<Appearance,TextureAtlasSprite> sprites){
        for(var slot:DoorHardwareSlot.values()) if(quad.getTintIndex()==slot.marker()) {
            var sprite=hardware.get(slot).map(sprites).orElse(quad.getSprite());
            return ModelRetexturing.retexture(quad,sprite,-1);
        }
        return quad;
    }
}
