package io.github.dirtyredz.roundwood.showcase;

import io.github.dirtyredz.roundwood.showcase.section.CamouflageSection;
import io.github.dirtyredz.roundwood.showcase.section.DoorsSection;
import io.github.dirtyredz.roundwood.showcase.section.RedstoneSection;
import io.github.dirtyredz.roundwood.showcase.section.WindowsSection;

import java.util.List;

/** The registry of showcase sections, built in order. New blocks and features add their section here. */
public final class ShowcaseSections {
    public static final List<ShowcaseSection> ALL = List.of(
            new DoorsSection(),
            new CamouflageSection(),
            new WindowsSection(),
            new RedstoneSection());

    private ShowcaseSections() {
    }
}
