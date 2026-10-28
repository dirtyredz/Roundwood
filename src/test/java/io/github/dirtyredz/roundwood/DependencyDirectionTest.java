package io.github.dirtyredz.roundwood;

import org.junit.jupiter.api.Test;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class DependencyDirectionTest {
    private static final Path ROOT = Path.of("src/main/java/io/github/dirtyredz/roundwood");

    @Test
    void windowsDoNotDependOnMigration() throws Exception {
        try (var files = Files.walk(ROOT.resolve("block/window"))) {
            for (var file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                assertFalse(Files.readString(file).contains("io.github.dirtyredz.roundwood.migration"), file.toString());
            }
        }
    }

    /** Shared assembly lifecycle and the static chunk model reach families only through their ports. */
    @Test
    void sharedStaticRenderingNamesNoHostFamily() throws Exception {
        var families = java.util.List.of("block.door", "block.arch", "block.archway", "block.trim", "block.window",
                "client.door", "client.arch", "client.trim", "client.window");
        for (var shared : java.util.List.of("block/assembly", "client/render/world")) {
            try (var files = Files.walk(ROOT.resolve(shared))) {
                for (var file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    var source = Files.readString(file);
                    for (var family : families)
                        assertFalse(source.contains("io.github.dirtyredz.roundwood." + family + "."), file + " names " + family);
                }
            }
        }
    }

    @Test
    void registryMigrationExceptionIsLimitedToRetiredAdapters() throws Exception {
        var imports = new java.util.HashSet<String>();
        try (var files = Files.walk(ROOT.resolve("registry"))) {
            for (var file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Files.readAllLines(file).stream()
                        .filter(line -> line.startsWith("import io.github.dirtyredz.roundwood.migration."))
                        .map(String::trim).forEach(imports::add);
            }
        }
        assertEquals(Set.of(
                "import io.github.dirtyredz.roundwood.migration.block.RetiredArchwayBlock;",
                "import io.github.dirtyredz.roundwood.migration.item.RetiredArchwayItem;",
                "import io.github.dirtyredz.roundwood.migration.block.RetiredRoundWindowBlock;",
                "import io.github.dirtyredz.roundwood.migration.block.RetiredOrientedWindowBlock;",
                "import io.github.dirtyredz.roundwood.migration.item.RetiredCrossWindowItem;"), imports);
    }
}
