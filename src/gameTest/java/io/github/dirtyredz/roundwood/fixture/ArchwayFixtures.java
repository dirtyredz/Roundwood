package io.github.dirtyredz.roundwood.fixture;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlock;
import io.github.dirtyredz.roundwood.block.archway.ArchwayBlockEntity;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayLayout;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayPart;
import io.github.dirtyredz.roundwood.block.archway.assembly.ArchwayPlacement;
import io.github.dirtyredz.roundwood.block.archway.geometry.ArchwayDesign;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.leaf.DoorDesign;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoorHingeSide;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;

/**
 * Archway helpers for the GameTests (0.12.0): the assembly at a relative anchor, placement through the
 * real item (an ordinary click on the floor under the anchor) or the player-less writer, real rays
 * clipped against every cell's target shape (so a hit is what a player looking there sends), and
 * assertions. Points are in the assembly's own frame as {@link DoorFixtures#point}: {@code a} blocks
 * across from the left edge, {@code h} up, {@code d} deep from the front face.
 */
public final class ArchwayFixtures {
    private ArchwayFixtures() {
    }

    public static ArchwayBlock block(ArchwayDesign design) {
        return ModBlocks.archway(design);
    }

    /** The archway anchored at a relative position, in absolute coordinates. */
    public static ArchwayLayout.Assembly assembly(GameTestHelper helper, BlockPos anchor, Direction facing, ArchwayDesign design) {
        return new ArchwayLayout.Assembly(helper.absolutePos(anchor), facing, design);
    }

    /** Uses {@code stack} on the floor under {@code anchor} (relative), the click that puts the anchor there. */
    public static InteractionResult place(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos anchor) {
        return DoorFixtures.useItemOn(helper, player, stack, anchor.below(), Direction.UP);
    }

    /** Writes a complete archway in {@code look} without a player; fails the test when refused. */
    public static ArchwayLayout.Assembly write(GameTestHelper helper, BlockPos anchor, Direction facing, ArchwayDesign design,
                                              Appearance look) {
        ArchwayLayout.Assembly assembly = assembly(helper, anchor, facing, design);
        check(ArchwayPlacement.placeAt(helper.getLevel(), block(design), assembly, look), "archway not written at " + anchor);
        return assembly;
    }

    /** A world point in the archway's frame ({@code a} across, {@code h} up, {@code d} deep from the front, in blocks). */
    public static Vec3 point(ArchwayLayout.Assembly assembly, double a, double h, double d) {
        return DoorFixtures.point(new DoorLayout.Assembly(assembly.anchor(), assembly.facing(), DoorHingeSide.LEFT, DoorDesign.ROUND),
                a, h, d);
    }

    /** A ray straight through the archway at ({@code a}, {@code h}), from in front or from behind. */
    public static BlockHitResult through(GameTestHelper helper, ServerPlayer player, ArchwayLayout.Assembly assembly, double a,
                                         double h, boolean fromFront) {
        return ray(helper, player, assembly, point(assembly, a, h, fromFront ? -0.75 : 1.75),
                point(assembly, a, h, fromFront ? 1.75 : -0.75));
    }

    /** A ray across the passage at height {@code h} and depth {@code d}, from its middle towards the left jamb. */
    public static BlockHitResult leftJambInside(GameTestHelper helper, ServerPlayer player, ArchwayLayout.Assembly assembly,
                                                double h, double d) {
        return ray(helper, player, assembly, point(assembly, 1.0, h, d), point(assembly, -0.5, h, d));
    }

    /** The nearest hit of the segment on any cell's target shape as {@code player} sees it. */
    public static BlockHitResult ray(GameTestHelper helper, ServerPlayer player, ArchwayLayout.Assembly assembly, Vec3 from, Vec3 to) {
        BlockHitResult best = null;
        for (BlockPos cell : assembly.positions()) {
            BlockState state = helper.getLevel().getBlockState(cell);
            BlockHitResult hit = state.getShape(helper.getLevel(), cell, CollisionContext.of(player)).clip(from, to, cell);
            if (hit != null && hit.getType() == HitResult.Type.BLOCK
                    && (best == null || hit.getLocation().distanceToSqr(from) < best.getLocation().distanceToSqr(from))) {
                best = hit;
            }
        }
        if (best == null) {
            throw new GameTestAssertException("ray from " + from + " to " + to + " missed the archway");
        }
        return best;
    }

    /** The archway's anchor entity (fails the test if missing). */
    public static ArchwayBlockEntity anchorEntity(GameTestHelper helper, ArchwayLayout.Assembly assembly) {
        if (helper.getLevel().getBlockEntity(assembly.anchor()) instanceof ArchwayBlockEntity entity) {
            return entity;
        }
        throw new GameTestAssertException("no archway block entity at the anchor " + assembly.anchor());
    }

    /** Asserts all four cells are present, and exactly one archway entity, at the anchor. */
    public static void assertArchway(GameTestHelper helper, ArchwayLayout.Assembly assembly) {
        ArchwayBlock block = block(assembly.design());
        int entities = 0;
        for (ArchwayPart part : ArchwayPart.values()) {
            BlockState state = helper.getLevel().getBlockState(assembly.pos(part));
            check(assembly.isPart(state, block, part), part + " missing or wrong at " + assembly.pos(part) + ": " + state);
            entities += helper.getLevel().getBlockEntity(assembly.pos(part)) instanceof ArchwayBlockEntity ? 1 : 0;
        }
        check(entities == 1, "expected one archway entity, found " + entities);
        anchorEntity(helper, assembly);
    }

    /** Asserts no archway cell of any design is left in the assembly's cells. */
    public static void assertNoArchway(GameTestHelper helper, ArchwayLayout.Assembly assembly) {
        for (BlockPos pos : assembly.positions()) {
            BlockState state = helper.getLevel().getBlockState(pos);
            check(!(state.getBlock() instanceof ArchwayBlock), "archway cell left at " + pos + ": " + state);
        }
    }
}
