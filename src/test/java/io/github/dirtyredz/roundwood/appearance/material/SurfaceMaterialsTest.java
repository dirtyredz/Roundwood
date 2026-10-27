package io.github.dirtyredz.roundwood.appearance.material;

import io.github.dirtyredz.roundwood.appearance.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;

class SurfaceMaterialsTest {
    static { io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap.ensure(); }
    @Test void allowlistUsesExactFullBlocksAndNormalizesWaxedCopper() {
        String[] metals={"iron_block","gold_block","diamond_block","emerald_block","netherite_block"};
        String[] copper={"copper_block","exposed_copper","weathered_copper","oxidized_copper"};
        String[] quartz={"quartz_block","smooth_quartz","chiseled_quartz_block","quartz_bricks","quartz_pillar"};
        for(var group:new String[][]{metals,copper,quartz}) for(var name:group) {
            var id=ResourceLocation.fromNamespaceAndPath("minecraft",name);
            assertEquals(Optional.of(id),SurfaceMaterials.normalize(id));
        }
        for(var name:copper) assertEquals(SurfaceMaterials.normalize(ResourceLocation.fromNamespaceAndPath("minecraft",name)),
                SurfaceMaterials.normalize(ResourceLocation.fromNamespaceAndPath("minecraft","waxed_"+name)));
        for(var name:new String[]{"cut_copper","waxed_cut_copper","exposed_cut_copper","copper_ore","raw_copper_block",
                "quartz_slab","smooth_quartz_stairs","iron_ore","waxed_gold_block","gold_ingot","quartz"})
            assertTrue(SurfaceMaterials.normalize(ResourceLocation.fromNamespaceAndPath("minecraft",name)).isEmpty(),name);
        assertTrue(SurfaceMaterials.normalize(ResourceLocation.fromNamespaceAndPath("other","iron_block")).isEmpty());
    }
    @Test void oldKindNamesAndPaintRulesAreUnchanged() {
        for(var look:new Appearance[]{Appearance.DEFAULT,Appearance.DEFAULT.painted(DyeColor.BLUE),
                Appearance.masonry(ResourceLocation.fromNamespaceAndPath("minecraft","bricks"))})
            assertEquals(look,AppearanceNbt.decode(AppearanceNbt.encode(look)).orElseThrow());
        assertEquals("wood",Appearance.Kind.WOOD.id());
        assertEquals("masonry",Appearance.Kind.MASONRY.id());
        var look=Appearance.surface(ResourceLocation.fromNamespaceAndPath("minecraft","gold_block"));
        assertEquals(look,look.painted(DyeColor.BLUE));
        assertEquals(AppearanceChanges.Outcome.UNCHANGED,AppearanceChanges.surface(look,look.material()).outcome());
    }
    @Test void blockSurfacesAreAcceptedByEveryFamilyAndCannotBePainted() {
        var kind=Appearance.Kind.byId("block_surface").orElseThrow(()->new AssertionError("Missing block-surface finish kind"));
        var look=new Appearance(kind,ResourceLocation.fromNamespaceAndPath("minecraft","iron_block"),Optional.empty());
        for(var family:AppearanceFamily.values()) assertTrue(family.accepts(look));
        assertEquals(AppearanceChanges.Outcome.REFUSED,AppearanceChanges.paint(look,DyeColor.RED).outcome());
        assertEquals(look,AppearanceNbt.decode(AppearanceNbt.encode(look)).orElseThrow());
    }
}
