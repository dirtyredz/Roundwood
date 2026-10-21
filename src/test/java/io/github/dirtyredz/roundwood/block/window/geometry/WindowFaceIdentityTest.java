package io.github.dirtyredz.roundwood.block.window.geometry;

import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import io.github.dirtyredz.roundwood.block.window.placement.WindowPosition;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * The window's face identity (0.8.0): which world side is the attachment FRONT. Pinned here on the
 * pose transforms the blocks use; the real blockstate rotations and mirrors (aliases included) are
 * swept in {@code WindowFaceIdentityGameTests}.
 */
class WindowFaceIdentityTest {
    @Test
    void frontAndBackAreOppositeSidesAlongTheNormal() {
        for (WindowPose pose : WindowPaneGeometryTest.allPoses()) {
            for (boolean reversed : new boolean[] {false, true}) {
                Direction front = WindowFaceIdentity.side(pose, reversed, AttachmentFace.FRONT);
                assertEquals(pose.normal(), front.getAxis());
                assertEquals(front.getOpposite(), WindowFaceIdentity.side(pose, reversed, AttachmentFace.BACK));
                assertEquals(reversed ? pose.front().getOpposite() : pose.front(), front);
                assertEquals(AttachmentFace.FRONT, WindowFaceIdentity.face(pose, reversed, front));
                assertEquals(AttachmentFace.BACK, WindowFaceIdentity.face(pose, reversed, front.getOpposite()));
                assertEquals(reversed, WindowFaceIdentity.reversed(pose, front));
            }
        }
        WindowPose south = WindowPose.upright(Direction.SOUTH, WindowDepth.CENTER);
        assertThrows(IllegalArgumentException.class, () -> WindowFaceIdentity.face(south, false, Direction.UP));
    }

    /** A centred round window turned 180 degrees keeps its axis, so its pose: only the bit can keep the looks apart. */
    @Test
    void aHalfTurnOfACentredRoundWindowTogglesTheBit() {
        for (Direction.Axis axis : new Direction.Axis[] {Direction.Axis.X, Direction.Axis.Z}) {
            WindowPose pose = WindowPosition.CENTER.pose(axis);
            Direction front = WindowFaceIdentity.front(pose, false);
            Direction turned = Rotation.CLOCKWISE_180.rotate(front);
            assertTrue(WindowFaceIdentity.reversed(pose, turned), axis + ": the trims must swap world sides");
        }
    }

    /** Oriented pieces turn their whole pose, so a rotation keeps the bit; a non-trivial mirror toggles it. */
    @Test
    void orientedRotationsKeepTheBitAndMirrorsToggleIt() {
        for (WindowPose pose : WindowPaneGeometryTest.allPoses()) {
            for (boolean reversed : new boolean[] {false, true}) {
                Direction front = WindowFaceIdentity.front(pose, reversed);
                for (int turns = 0; turns < 4; turns++) {
                    Rotation rotation = Rotation.values()[turns];
                    boolean after = WindowFaceIdentity.reversed(pose.rotateY(turns), rotation.rotate(front));
                    if (after != reversed) {
                        fail(pose + " turned " + turns + ": bit " + reversed + " became " + after);
                    }
                }
                for (Mirror mirror : new Mirror[] {Mirror.LEFT_RIGHT, Mirror.FRONT_BACK}) {
                    Direction.Axis axis = mirror == Mirror.LEFT_RIGHT ? Direction.Axis.Z : Direction.Axis.X;
                    WindowPose reflected = pose.mirror(axis);
                    boolean after = WindowFaceIdentity.reversed(reflected, mirror.mirror(front));
                    // WindowPose.mirror always reverses the geometric front (handedness is kept), so the bit always toggles.
                    if (after == reversed) {
                        fail(pose + " mirrored " + mirror + ": bit stayed " + reversed);
                    }
                }
            }
        }
    }

    /** Through any sequence, each slot stays on the world side its trim was fitted to (the identity never swaps looks). */
    @Test
    void anAsymmetricPairFollowsTheWorldSides() {
        WindowPose pose = WindowPose.upright(Direction.EAST, WindowDepth.NEGATIVE);
        boolean reversed = false;
        Direction brickSide = WindowFaceIdentity.side(pose, reversed, AttachmentFace.FRONT);
        for (int step = 0; step < 8; step++) {
            Direction front = WindowFaceIdentity.front(pose, reversed);
            if (step % 3 == 2) {
                WindowPose next = pose.mirror(Direction.Axis.X);
                reversed = WindowFaceIdentity.reversed(next, Mirror.FRONT_BACK.mirror(front));
                brickSide = Mirror.FRONT_BACK.mirror(brickSide);
                pose = next;
            } else {
                WindowPose next = pose.rotateY(1);
                reversed = WindowFaceIdentity.reversed(next, Rotation.CLOCKWISE_90.rotate(front));
                brickSide = Rotation.CLOCKWISE_90.rotate(brickSide);
                pose = next;
            }
            assertEquals(brickSide, WindowFaceIdentity.side(pose, reversed, AttachmentFace.FRONT), "step " + step);
        }
        assertFalse(brickSide.getAxis().isVertical());
    }
}
