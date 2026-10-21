package io.github.dirtyredz.roundwood.showcase;

import io.github.dirtyredz.roundwood.showcase.section.BrickArchesSection;
import io.github.dirtyredz.roundwood.showcase.section.CamouflageSection;
import io.github.dirtyredz.roundwood.showcase.section.DoorsSection;
import io.github.dirtyredz.roundwood.showcase.section.PaintsSection;
import io.github.dirtyredz.roundwood.showcase.section.RedstoneSection;
import io.github.dirtyredz.roundwood.showcase.section.TrimCombinationsSection;
import io.github.dirtyredz.roundwood.showcase.section.WindowArchesSection;
import io.github.dirtyredz.roundwood.showcase.section.WindowCornersSection;
import io.github.dirtyredz.roundwood.showcase.section.WindowMuntinsSection;
import io.github.dirtyredz.roundwood.showcase.section.WindowPositionsSection;
import io.github.dirtyredz.roundwood.showcase.section.WindowTrimsSection;
import io.github.dirtyredz.roundwood.showcase.section.WindowsSection;

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
            new BrickArchesSection());

    private ShowcaseSections() {
    }
}
