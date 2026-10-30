package io.github.dirtyredz.roundwood.testsupport;

import java.nio.file.Path;

/**
 * Checked-in project files for tests that read sources, generated resources or fixtures. FML's JUnit launcher
 * runs in its own game directory (it writes config there), so paths resolve from the project directory Gradle
 * passes as {@code roundwood.projectDir}, not from the working directory.
 */
public final class ProjectFiles {
    private static final Path ROOT = Path.of(System.getProperty("roundwood.projectDir", "."));

    private ProjectFiles() {
    }

    public static Path path(String relative) {
        return ROOT.resolve(relative);
    }
}
