package io.github.dirtyredz.roundwood.block.arch.geometry;

/** Product names use height x width; geometry uses horizontal width and vertical height. */
public enum ArchPieceDesign {
    TOP_1X1("arch_top_1x1", 16, 16, 6),
    TOP_1X2("arch_top_1x2", 32, 16, 14),
    TOP_2X3("arch_top_2x3", 48, 32, 22);

    private final String id;
    private final ArchTopProfile top;

    ArchPieceDesign(String id, int width, int height, int radius) {
        this.id = id;
        this.top = new ArchTopProfile(width, height, radius);
    }

    public String id() { return id; }
    public ArchTopProfile top() { return top; }
    public int width() { return top.width(); }
    public int height() { return top.height(); }
    public int columns() { return width() / 16; }
    public int rows() { return height() / 16; }
}
