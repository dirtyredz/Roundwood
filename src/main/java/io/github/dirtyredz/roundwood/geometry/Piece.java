package io.github.dirtyredz.roundwood.geometry;

import java.util.List;

/** A box painted with one model texture variable (for example {@code "trim"}). */
public record Piece(Box box, String texture) {
    public Piece rotateY(int quarterTurns) {
        return new Piece(box.rotateY(quarterTurns), texture);
    }

    public static List<Box> boxes(List<Piece> pieces) {
        return pieces.stream().map(Piece::box).toList();
    }
}
