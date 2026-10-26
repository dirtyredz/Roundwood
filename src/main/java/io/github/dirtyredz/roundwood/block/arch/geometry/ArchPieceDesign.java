package io.github.dirtyredz.roundwood.block.arch.geometry;

/** Product names use height x width; geometry uses horizontal width and vertical height. */
public enum ArchPieceDesign {
    TOP_1X1("arch_top_1x1", 16, 16, 8, 8),
    TOP_1X2("arch_top_1x2", 32, 16, 16, 0),
    TOP_2X3("arch_top_2x3", 48, 32, 24, 0),
    KEYHOLE_SHOULDER("keyhole_shoulder", 16, 16, 0, 0);

    private final String id;
    private final ArchTopProfile top;

    ArchPieceDesign(String id, int width, int height, int radius, int spring) {
        this.id = id;
        this.top = radius == 0 ? null : new ArchTopProfile(width, height, radius, spring);
    }

    public String id() { return id; }
    public boolean shoulder() { return this == KEYHOLE_SHOULDER; }
    public ArchTopProfile top() {
        if (shoulder()) throw new IllegalStateException("A shoulder has no semicircular top");
        return top;
    }
    public boolean contains(KeyholeShoulderProfile.Side side, int u, int v) {
        return shoulder() ? KeyholeShoulderProfile.contains(side, u, v) : top.contains(u, v);
    }
    public int width() { return shoulder() ? 16 : top.width(); }
    public int height() { return shoulder() ? 16 : top.height(); }
    public int columns() { return width() / 16; }
    public int rows() { return height() / 16; }
}
