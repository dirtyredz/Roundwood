package io.github.dirtyredz.roundwood.data;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The 0.10.0 window attachment artwork joins the 0.7.0 art without a seam: the lower arches draw the
 * door ring's full radial face, whose upper half must be pixel for pixel the arched window face the
 * upper arches draw (else a full circle of four masonry arches breaks at its diameter); the top
 * corners' face is the arched face's straight lower half reflected above an unchanged lower half.
 */
class WindowShapeArtworkTest {
    private static final Path TEXTURES = Path.of("src/main/resources/assets/roundwood/textures/block");
    private static final List<String> MASONRY = List.of("brick", "stone_brick", "mud_brick", "deepslate_brick");

    private static BufferedImage image(String name) {
        try {
            return ImageIO.read(TEXTURES.resolve(name + ".png").toFile());
        } catch (IOException e) {
            throw new AssertionError("cannot read " + name, e);
        }
    }

    @Test
    void theLowerArchCircleContinuesTheUpperArchesRadialFace() {
        for (String material : MASONRY) {
            BufferedImage arch = image("window_trim_arch_" + material);
            BufferedImage circle = image("trim_radial_thin_" + material);
            assertEquals(64, arch.getWidth());
            assertEquals(64, circle.getWidth());
            for (int y = 0; y < 32; y++) {
                for (int x = 0; x < 64; x++) {
                    if (arch.getRGB(x, y) != circle.getRGB(x, y)) {
                        throw new AssertionError(material + ": upper radial texel " + x + "," + y + " differs");
                    }
                }
            }
        }
    }

    @Test
    void theTopCornerFaceIsTheArchedLowerHalfReflected() {
        for (String material : MASONRY) {
            BufferedImage arch = image("window_trim_arch_" + material);
            BufferedImage frame = image("window_trim_frame_" + material);
            assertEquals(64, frame.getWidth());
            assertEquals(64, frame.getHeight());
            for (int y = 0; y < 64; y++) {
                int from = y < 32 ? 63 - y : y;
                for (int x = 0; x < 64; x++) {
                    if (arch.getRGB(x, from) != frame.getRGB(x, y)) {
                        throw new AssertionError(material + ": frame texel " + x + "," + y + " is not arch row " + from);
                    }
                }
            }
            assertEquals(32, image("window_trim_single_" + material).getWidth(), "a single edge's face spans 16 pixels");
            assertEquals(64, image("window_sill_" + material).getWidth(), "the sill spans the 2 x 2 window");
        }
    }
}
