package io.github.dirtyredz.roundwood.client.door;

import io.github.dirtyredz.roundwood.door.leaf.DoorShapes;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.extensions.common.IClientBlockExtensions;

/**
 * Vanilla spawns break and mining particles over a block's outline. A round door's outline is the
 * whole assembly from every part, so each part would scatter particles over the full door (four
 * times over when it breaks). These use the part's own collision piece instead, with vanilla's
 * spacing, speed and sizes.
 */
public final class DoorParticles implements IClientBlockExtensions {
    public static final DoorParticles INSTANCE = new DoorParticles();

    private DoorParticles() {
    }

    @Override
    public boolean addHitEffects(BlockState state, Level level, HitResult target, ParticleEngine manager) {
        if (!(target instanceof BlockHitResult hit) || !(level instanceof ClientLevel client)) {
            return false;
        }
        VoxelShape shape = DoorShapes.shape(state);
        if (shape.isEmpty()) {
            return true; // the empty free column of an open door: nothing to chip
        }
        BlockPos pos = hit.getBlockPos();
        AABB box = shape.bounds();
        RandomSource random = level.getRandom();
        double x = pos.getX() + random.nextDouble() * (box.maxX - box.minX - 0.2) + 0.1 + box.minX;
        double y = pos.getY() + random.nextDouble() * (box.maxY - box.minY - 0.2) + 0.1 + box.minY;
        double z = pos.getZ() + random.nextDouble() * (box.maxZ - box.minZ - 0.2) + 0.1 + box.minZ;
        switch (hit.getDirection()) {
            case DOWN -> y = pos.getY() + box.minY - 0.1;
            case UP -> y = pos.getY() + box.maxY + 0.1;
            case NORTH -> z = pos.getZ() + box.minZ - 0.1;
            case SOUTH -> z = pos.getZ() + box.maxZ + 0.1;
            case WEST -> x = pos.getX() + box.minX - 0.1;
            case EAST -> x = pos.getX() + box.maxX + 0.1;
        }
        manager.add(new TerrainParticle(client, x, y, z, 0, 0, 0, state, pos).updateSprite(state, pos).setPower(0.2F).scale(0.6F));
        return true;
    }

    @Override
    public boolean addDestroyEffects(BlockState state, Level level, BlockPos pos, ParticleEngine manager) {
        if (!(level instanceof ClientLevel client)) {
            return false;
        }
        DoorShapes.shape(state).forAllBoxes((x0, y0, z0, x1, y1, z1) -> {
            double dx = Math.min(1, x1 - x0);
            double dy = Math.min(1, y1 - y0);
            double dz = Math.min(1, z1 - z0);
            int nx = Math.max(2, Mth.ceil(dx / 0.25));
            int ny = Math.max(2, Mth.ceil(dy / 0.25));
            int nz = Math.max(2, Mth.ceil(dz / 0.25));
            for (int i = 0; i < nx; i++) {
                for (int j = 0; j < ny; j++) {
                    for (int k = 0; k < nz; k++) {
                        double fx = (i + 0.5) / nx;
                        double fy = (j + 0.5) / ny;
                        double fz = (k + 0.5) / nz;
                        manager.add(new TerrainParticle(client, pos.getX() + fx * dx + x0, pos.getY() + fy * dy + y0,
                                pos.getZ() + fz * dz + z0, fx - 0.5, fy - 0.5, fz - 0.5, state, pos).updateSprite(state, pos));
                    }
                }
            }
        });
        return true;
    }
}
