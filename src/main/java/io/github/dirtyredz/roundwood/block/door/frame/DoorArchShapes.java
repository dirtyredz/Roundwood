package io.github.dirtyredz.roundwood.block.door.frame;

import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.geometry.VoxelShapeFactory;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/** Static whole-frame shapes shared by placement preview, rays and highlights; no leaf-depth shift. */
public final class DoorArchShapes {
    private static final ConcurrentHashMap<List<Object>, VoxelShape> SHAPES = new ConcurrentHashMap<>();
    private DoorArchShapes() {}
    public static VoxelShape of(DoorLayout.Assembly door, DoorArchAttachment frame, boolean target) {
        var key=List.<Object>of(door.design(),door.facing(),frame.thickness(),frame.depth(),
                frame.casings().has(AttachmentFace.FRONT),frame.casings().has(AttachmentFace.BACK),target);
        return SHAPES.computeIfAbsent(key,k -> VoxelShapeFactory.shape(VoxelShapeFactory.rotate(
                DoorArchGeometry.selection(door.design(),frame,target),VoxelShapeFactory.quarterTurns(door.facing()))));
    }
}
