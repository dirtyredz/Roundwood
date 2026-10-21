package io.github.dirtyredz.roundwood.block.window.geometry;

import io.github.dirtyredz.roundwood.block.attachment.AttachmentFace;
import net.minecraft.core.Direction;

/**
 * Which world side of a window is its attachment FRONT (0.8.0): {@link WindowPose#front()}, or its
 * opposite when the window's {@code face_reversed} bit is set. The pose alone cannot keep the two
 * trim slots apart: a round window turned 180 degrees keeps its axis (so its pose), and a mirrored
 * oriented piece reverses its geometric front to keep its handedness, while the trims must stay on
 * the world sides they were on. Rotations and mirrors therefore move the attachment front like any
 * other direction and recompute the bit against the new pose ({@link #reversed}). The bit changes
 * no geometry. Plain Java apart from {@link Direction}.
 */
public final class WindowFaceIdentity {
    private WindowFaceIdentity() {
    }

    /** The world direction the FRONT slot faces. */
    public static Direction front(WindowPose pose, boolean reversed) {
        return reversed ? pose.front().getOpposite() : pose.front();
    }

    /** The slot on the {@code side} of the pane ({@code side} lies along the pane's normal). */
    public static AttachmentFace face(WindowPose pose, boolean reversed, Direction side) {
        if (side.getAxis() != pose.normal()) {
            throw new IllegalArgumentException(side + " is not along the normal of " + pose);
        }
        return side == front(pose, reversed) ? AttachmentFace.FRONT : AttachmentFace.BACK;
    }

    /** The world side (along the normal) that {@code face} is on. */
    public static Direction side(WindowPose pose, boolean reversed, AttachmentFace face) {
        Direction front = front(pose, reversed);
        return face == AttachmentFace.FRONT ? front : front.getOpposite();
    }

    /** The bit that puts the FRONT slot on {@code attachmentFront} for a window posed {@code pose}. */
    public static boolean reversed(WindowPose pose, Direction attachmentFront) {
        if (attachmentFront.getAxis() != pose.normal()) {
            throw new IllegalArgumentException(attachmentFront + " is not along the normal of " + pose);
        }
        return attachmentFront != pose.front();
    }
}
