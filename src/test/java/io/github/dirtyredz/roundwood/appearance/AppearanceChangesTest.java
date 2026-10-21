package io.github.dirtyredz.roundwood.appearance;

import io.github.dirtyredz.roundwood.appearance.AppearanceChanges.Change;
import io.github.dirtyredz.roundwood.appearance.AppearanceChanges.Outcome;
import io.github.dirtyredz.roundwood.testsupport.MinecraftBootstrap;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The restyling rules and the family/kind acceptance, as pure functions with no Minecraft bootstrap. */
class AppearanceChangesTest {
    static {
        MinecraftBootstrap.ensure(); // DyeColor needs the game marked as bootstrapped
    }

    private static final ResourceLocation OAK = id("oak_planks");
    private static final ResourceLocation SPRUCE = id("spruce_planks");
    private static final ResourceLocation BRICKS = id("bricks");
    private static final ResourceLocation STONE_BRICKS = id("stone_bricks");

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("minecraft", path);
    }

    @Test
    void otherWoodChangesAndClearsPaint() {
        Appearance painted = Appearance.wood(OAK).painted(DyeColor.RED);
        Change change = AppearanceChanges.wood(painted, SPRUCE);
        assertEquals(Outcome.CHANGED, change.outcome());
        assertTrue(change.changed());
        assertEquals(Appearance.wood(SPRUCE), change.next());
        assertEquals(Optional.empty(), change.next().paint());
    }

    @Test
    void otherWoodOnBareWoodChanges() {
        Change change = AppearanceChanges.wood(Appearance.wood(OAK), SPRUCE);
        assertEquals(Outcome.CHANGED, change.outcome());
        assertEquals(Appearance.wood(SPRUCE), change.next());
    }

    @Test
    void sameWoodOnBareWoodIsUnchanged() {
        Appearance current = Appearance.wood(OAK);
        Change change = AppearanceChanges.wood(current, OAK);
        assertEquals(Outcome.UNCHANGED, change.outcome());
        assertFalse(change.changed());
        assertSame(current, change.next());
    }

    @Test
    void sameWoodOnPaintedWoodStripsThePaint() {
        Appearance painted = Appearance.wood(OAK).painted(DyeColor.BLUE);
        Change change = AppearanceChanges.wood(painted, OAK);
        assertEquals(Outcome.CHANGED, change.outcome());
        assertEquals(Appearance.wood(OAK), change.next());
        assertFalse(change.next().isPainted());
    }

    @Test
    void woodOverMasonryChanges() {
        Change change = AppearanceChanges.wood(Appearance.masonry(BRICKS), SPRUCE);
        assertEquals(Outcome.CHANGED, change.outcome());
        assertEquals(Appearance.wood(SPRUCE), change.next());
        assertTrue(change.next().isWood());
    }

    @Test
    void everyDyeChangesBareWoodKeepingTheWood() {
        assertEquals(16, DyeColor.values().length);
        for (DyeColor dye : DyeColor.values()) {
            Appearance current = Appearance.wood(SPRUCE);
            Change change = AppearanceChanges.paint(current, dye);
            assertEquals(Outcome.CHANGED, change.outcome(), dye.getName());
            assertEquals(SPRUCE, change.next().material(), dye.getName());
            assertEquals(Appearance.Kind.WOOD, change.next().kind(), dye.getName());
            assertEquals(Optional.of(dye), change.next().paint(), dye.getName());
        }
    }

    @Test
    void anotherDyeRepaintsPaintedWood() {
        Appearance red = Appearance.wood(OAK).painted(DyeColor.RED);
        Change change = AppearanceChanges.paint(red, DyeColor.BLUE);
        assertEquals(Outcome.CHANGED, change.outcome());
        assertEquals(Appearance.wood(OAK).painted(DyeColor.BLUE), change.next());
    }

    @Test
    void theSameDyeIsUnchanged() {
        for (DyeColor dye : DyeColor.values()) {
            Appearance current = Appearance.wood(OAK).painted(dye);
            Change change = AppearanceChanges.paint(current, dye);
            assertEquals(Outcome.UNCHANGED, change.outcome(), dye.getName());
            assertSame(current, change.next(), dye.getName());
        }
    }

    @Test
    void paintOnMasonryIsRefusedAndNothingChanges() {
        Appearance masonry = Appearance.masonry(BRICKS);
        for (DyeColor dye : DyeColor.values()) {
            Change change = AppearanceChanges.paint(masonry, dye);
            assertEquals(Outcome.REFUSED, change.outcome(), dye.getName());
            assertFalse(change.changed());
            assertSame(masonry, change.next(), dye.getName());
        }
    }

    @Test
    void otherMasonryChangesAndSameMasonryIsUnchanged() {
        Appearance bricks = Appearance.masonry(BRICKS);
        Change other = AppearanceChanges.masonry(bricks, STONE_BRICKS);
        assertEquals(Outcome.CHANGED, other.outcome());
        assertEquals(Appearance.masonry(STONE_BRICKS), other.next());
        Change same = AppearanceChanges.masonry(bricks, BRICKS);
        assertEquals(Outcome.UNCHANGED, same.outcome());
        assertSame(bricks, same.next());
    }

    @Test
    void masonryFromPaintedWoodClearsThePaint() {
        Appearance painted = Appearance.wood(OAK).painted(DyeColor.GREEN);
        Change change = AppearanceChanges.masonry(painted, BRICKS);
        assertEquals(Outcome.CHANGED, change.outcome());
        assertEquals(Appearance.masonry(BRICKS), change.next());
        assertFalse(change.next().isPainted());
        assertFalse(change.next().isWood());
    }

    @Test
    void familiesAcceptTheRightKinds() {
        for (AppearanceFamily family : new AppearanceFamily[] {AppearanceFamily.DOOR, AppearanceFamily.WINDOW}) {
            assertTrue(family.accepts(Appearance.Kind.WOOD), family.name());
            assertFalse(family.accepts(Appearance.Kind.MASONRY), family.name());
            assertTrue(family.accepts(Appearance.wood(OAK).painted(DyeColor.RED)), family.name());
            assertFalse(family.accepts(Appearance.masonry(BRICKS)), family.name());
        }
        assertTrue(AppearanceFamily.TRIM.accepts(Appearance.Kind.WOOD));
        assertTrue(AppearanceFamily.TRIM.accepts(Appearance.Kind.MASONRY));
        assertTrue(AppearanceFamily.TRIM.accepts(Appearance.wood(OAK)));
        assertTrue(AppearanceFamily.TRIM.accepts(Appearance.masonry(BRICKS)));
    }

    @Test
    void theConstructorDropsPaintOnMasonry() {
        Appearance masonry = new Appearance(Appearance.Kind.MASONRY, BRICKS, Optional.of(DyeColor.RED));
        assertEquals(Optional.empty(), masonry.paint());
        assertEquals(Appearance.masonry(BRICKS), masonry);
        assertEquals(Appearance.masonry(BRICKS), Appearance.masonry(BRICKS).painted(DyeColor.RED));
        Appearance wood = new Appearance(Appearance.Kind.WOOD, OAK, Optional.of(DyeColor.RED));
        assertEquals(Optional.of(DyeColor.RED), wood.paint());
    }
}
