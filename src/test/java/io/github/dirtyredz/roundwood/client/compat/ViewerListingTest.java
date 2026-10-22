package io.github.dirtyredz.roundwood.client.compat;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guard: the recipe-viewer adapters never add per-look entries or cycling previews, so every item
 * (both archways included) is listed once, as its oak default. The matching in-game guard is
 * ContentGameTests.everyItemIncludingBothArchwaysIsListedOnceAsItsOakDefault.
 */
class ViewerListingTest {
    private static final Path COMPAT = Path.of("src/main/java/io/github/dirtyredz/roundwood/client/compat");

    @Test
    void noViewerAdapterAddsStacksOrCyclesLooks() throws Exception {
        try (Stream<Path> files = Files.walk(COMPAT)) {
            var sources = files.filter(p -> p.toString().endsWith(".java")).toList();
            assertEquals(2, sources.size(), "unexpected viewer adapter files: " + sources);
            for (Path source : sources) {
                String text = Files.readString(source);
                assertFalse(text.contains("addEmiStack"), source + " adds extra index entries");
                assertFalse(text.contains("Cycling"), source + " cycles looks");
                assertFalse(text.contains("AppearanceSamples"), source + " previews sample looks");
                assertTrue(text.contains("ModItems"), source + " does not cover the canonical items");
            }
        }
    }
}
