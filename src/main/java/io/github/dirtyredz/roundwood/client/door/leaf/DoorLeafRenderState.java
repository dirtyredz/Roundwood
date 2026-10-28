package io.github.dirtyredz.roundwood.client.door.leaf;

import io.github.dirtyredz.roundwood.client.door.DoorAppearanceModels;
import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.hardware.DoorHardware;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.AABB;
import java.util.function.Function;

/** Per-anchor cached leaf inputs; no entity/world reference, so weak renderer keys can be collected. */
public final class DoorLeafRenderState {
    @FunctionalInterface public interface Models {
        BakedModel resolve(DoorAppearanceModels.Look look, DoorDesign design, DoorHingeSide hinge, DoorHardware hardware);
    }
    private final DoorLayout.Assembly assembly;
    private final AABB bounds;
    private final BlockPos[] cells;
    private final DoorLayout.Part[] parts;
    private final DoorHingeSide[] hinges;
    private final BakedModel[] models;
    private Appearance appearance;
    private DoorHardware hardware;
    private long generation = Long.MIN_VALUE;
    private float red = 1, green = 1, blue = 1;

    public DoorLeafRenderState(DoorLayout.Assembly assembly) {
        this.assembly = assembly;
        bounds = assembly.sweepBounds();
        parts = assembly.parts().toArray(DoorLayout.Part[]::new);
        cells = assembly.positions().toArray(BlockPos[]::new);
        hinges = assembly.design().doubleLeaf() ? new DoorHingeSide[]{DoorHingeSide.LEFT, DoorHingeSide.RIGHT}
                : new DoorHingeSide[]{assembly.hinge()};
        models = new BakedModel[hinges.length];
    }
    public DoorLayout.Assembly assembly() { return assembly; }
    public AABB bounds() { return bounds; }
    public int leaves() { return hinges.length; }
    public DoorHingeSide hinge(int index) { return hinges[index]; }
    public BakedModel model(int index) { return models[index]; }
    public float red() { return red; }
    public float green() { return green; }
    public float blue() { return blue; }
    public boolean complete(Level level, Block block) {
        for (int i = 0; i < cells.length; i++) {
            if (!level.isLoaded(cells[i]) || !assembly.isPart(level.getBlockState(cells[i]), block, parts[i])) return false;
        }
        return true;
    }
    public void material(Appearance next, DoorHardware hardware, long generation,
                         Function<Appearance, DoorAppearanceModels.Look> looks, Models resolver) {
        if (generation == this.generation && next.equals(appearance) && hardware.equals(this.hardware)) return;
        var look = looks.apply(next);
        for (int i = 0; i < hinges.length; i++) models[i] = resolver.resolve(look, assembly.design(), hinges[i], hardware);
        red = look.tint() < 0 ? 1 : ((look.tint() >> 16) & 255) / 255F;
        green = look.tint() < 0 ? 1 : ((look.tint() >> 8) & 255) / 255F;
        blue = look.tint() < 0 ? 1 : (look.tint() & 255) / 255F;
        this.appearance = next;
        this.hardware = hardware;
        this.generation = generation;
    }
}
