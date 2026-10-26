package io.github.dirtyredz.roundwood.showcase;

import io.github.dirtyredz.roundwood.showcase.section.MaterialsSection;
import io.github.dirtyredz.roundwood.showcase.section.PiecesSection;
import io.github.dirtyredz.roundwood.showcase.section.door.CamouflageSection;
import io.github.dirtyredz.roundwood.showcase.section.door.RedstoneSection;
import io.github.dirtyredz.roundwood.showcase.section.window.WindowArchesSection;
import io.github.dirtyredz.roundwood.showcase.section.window.WindowCirclesSection;
import io.github.dirtyredz.roundwood.showcase.section.window.WindowCornersSection;
import io.github.dirtyredz.roundwood.showcase.section.window.WindowEdgesSection;
import io.github.dirtyredz.roundwood.showcase.section.window.WindowMuntinsSection;
import io.github.dirtyredz.roundwood.showcase.section.window.WindowPositionsSection;
import io.github.dirtyredz.roundwood.showcase.section.window.WindowSillsSection;
import io.github.dirtyredz.roundwood.showcase.section.window.WindowTrimsSection;
import io.github.dirtyredz.roundwood.showcase.section.window.WindowsSection;

import java.util.List;

/** The registry of showcase sections, built in order. New blocks and features add their section here. */
public final class ShowcaseSections {
    public static final List<ShowcaseSection> ALL = List.of(
            new PiecesSection(),
            new MaterialsSection(),
            new WindowPositionsSection(),
            new WindowArchesSection(),
            new WindowsSection(),
            new WindowCornersSection(),
            new WindowCirclesSection(),
            new WindowEdgesSection(),
            new WindowTrimsSection(),
            new WindowSillsSection(),
            new WindowMuntinsSection(),
            new CamouflageSection(),
            new RedstoneSection(),
            new io.github.dirtyredz.roundwood.showcase.section.arch.ArchPiecesSection(),
            new io.github.dirtyredz.roundwood.showcase.section.arch.ArchCompositionsSection());

    private ShowcaseSections() {
    }
}
