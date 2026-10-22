package io.github.dirtyredz.roundwood.showcase;

import io.github.dirtyredz.roundwood.showcase.section.BrickArchesSection;
import io.github.dirtyredz.roundwood.showcase.section.archway.ArchwaysSection;
import io.github.dirtyredz.roundwood.showcase.section.door.ArchedDoorsSection;
import io.github.dirtyredz.roundwood.showcase.section.door.CamouflageSection;
import io.github.dirtyredz.roundwood.showcase.section.door.DoorsSection;
import io.github.dirtyredz.roundwood.showcase.section.PaintsSection;
import io.github.dirtyredz.roundwood.showcase.section.door.RedstoneSection;
import io.github.dirtyredz.roundwood.showcase.section.door.TrimCombinationsSection;
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
            new DoorsSection(),
            new CamouflageSection(),
            new WindowsSection(),
            new WindowPositionsSection(),
            new WindowArchesSection(),
            new WindowCornersSection(),
            new WindowMuntinsSection(),
            new WindowTrimsSection(),
            new RedstoneSection(),
            new PaintsSection(),
            new TrimCombinationsSection(),
            new BrickArchesSection(),
            new WindowCirclesSection(),
            new WindowSillsSection(),
            new WindowEdgesSection(),
            new ArchedDoorsSection(),
            new ArchwaysSection());

    private ShowcaseSections() {
    }
}
