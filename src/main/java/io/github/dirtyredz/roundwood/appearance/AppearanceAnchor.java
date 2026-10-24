package io.github.dirtyredz.roundwood.appearance;

/** The appearance-only view required by assembly placement and restyling. */
public interface AppearanceAnchor {
    Appearance appearance();
    void setAppearance(Appearance appearance);
}
