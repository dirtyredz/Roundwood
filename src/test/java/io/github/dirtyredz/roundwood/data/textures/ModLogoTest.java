package io.github.dirtyredz.roundwood.data.textures;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ModLogoTest {
    @Test
    void modLogoIsSquareTransparentAndCrispAtLauncherSize() throws Exception {
        var resources = io.github.dirtyredz.roundwood.testsupport.ProjectFiles.path("src/main/resources");
        assertTrue(Files.readString(resources.resolve("META-INF/neoforge.mods.toml"))
                .contains("logoFile=\"roundwood-logo.png\""));
        var image = ImageIO.read(resources.resolve("roundwood-logo.png").toFile());
        assertNotNull(image);
        assertEquals(256, image.getWidth());
        assertEquals(256, image.getHeight());
        assertTrue(image.getColorModel().hasAlpha());
        assertEquals(0, image.getRGB(0, 0) >>> 24);
        assertEquals(255, image.getRGB(128, 128) >>> 24);
        for (int y = 0; y < 256; y++) {
            for (int x = 0; x < 256; x++) {
                assertEquals(image.getRGB(x / 8 * 8, y / 8 * 8), image.getRGB(x, y),
                        "Every master pixel must scale to a solid 8x8 tile");
            }
        }
    }
}
