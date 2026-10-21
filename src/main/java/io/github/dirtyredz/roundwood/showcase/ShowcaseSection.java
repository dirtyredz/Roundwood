package io.github.dirtyredz.roundwood.showcase;

/**
 * One self-contained part of the showcase layout. A section owns a rectangle of the frame (declare
 * it in the class comment), builds only inside it, and goes through the mod's real placement paths.
 * Add a section to {@link ShowcaseSections} for every new block or feature.
 */
public interface ShowcaseSection {
    /** Short name used in reports and failures. */
    String id();

    void build(ShowcaseCanvas canvas);
}
