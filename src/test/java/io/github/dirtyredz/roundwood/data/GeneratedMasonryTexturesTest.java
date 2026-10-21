package io.github.dirtyredz.roundwood.data;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The thin-mortar masonry textures added in 0.7.0: the door's thin radial face, the round-window trim
 * and the arch trim. Mortar is measured as the projection of a texel's colour onto the palette's
 * brick-to-mortar segment, so the checks hold whatever the exact grain of the artwork. Palette colours
 * are written out here, independently of the generator.
 */
class GeneratedMasonryTexturesTest {
    private static final Path DIR = Path.of("src/main/resources/assets/roundwood/textures/block");
    private static final List<String> MATERIALS = List.of("brick", "stone_brick", "deepslate_brick", "mud_brick");
    /** Per material: the brick colour, then the mortar colour (RGB). */
    private static final Map<String, int[][]> PALETTE = Map.of(
            "brick", new int[][] {{150, 74, 58}, {170, 160, 152}},
            "stone_brick", new int[][] {{126, 126, 126}, {84, 84, 86}},
            "deepslate_brick", new int[][] {{74, 74, 77}, {36, 36, 38}},
            "mud_brick", new int[][] {{146, 110, 80}, {100, 78, 66}});
    /** The door face has 24 bricks round its ring, the round window 12. */
    private static final int DOOR_JOINTS = 24;
    private static final int WINDOW_JOINTS = 12;
    private static final Set<Integer> HEAD_JOINTS = new TreeSet<>(List.of(0, 9, 18, 27, 36, 45, 54, 63));
    private static final List<Integer> JAMB_MORTAR_ROWS = List.of(32, 39, 46, 53, 59);

    private static BufferedImage read(String name) throws IOException {
        BufferedImage image = ImageIO.read(DIR.resolve(name + ".png").toFile());
        assertTrue(image != null, name + ".png must decode");
        return image;
    }

    /** 0 is pure brick colour, 1 pure mortar colour: the texel's projection onto the segment, clamped. */
    private static double mortar(BufferedImage image, int x, int y, String material) {
        int[][] palette = PALETTE.get(material);
        int argb = image.getRGB(x, y);
        int[] colour = {(argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF};
        double dot = 0;
        double length = 0;
        for (int channel = 0; channel < 3; channel++) {
            double axis = palette[1][channel] - palette[0][channel];
            dot += (colour[channel] - palette[0][channel]) * axis;
            length += axis * axis;
        }
        return Math.max(0, Math.min(1, dot / length));
    }

    /** Summed mortar weight of every texel whose centre lies at {@code inner..outer} from the given centre. */
    private static double bandMortar(BufferedImage image, String material, double centre, double inner, double outer) {
        double sum = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                double radius = Math.hypot(x + 0.5 - centre, y + 0.5 - centre);
                if (radius >= inner && radius <= outer) {
                    sum += mortar(image, x, y, material);
                }
            }
        }
        return sum;
    }

    @Test
    void thinFaceHasAboutHalfTheMortarOfTheFullFaceOnTheRing() throws IOException {
        for (String material : MATERIALS) {
            double full = bandMortar(read("trim_radial_" + material), material, 32, 28, 32);
            double thin = bandMortar(read("trim_radial_thin_" + material), material, 32, 28, 32);
            assertTrue(full > 0, material + ": the full face has mortar");
            double ratio = thin / full;
            assertTrue(ratio >= 0.30 && ratio <= 0.65, material + ": thin/full mortar " + ratio + " outside 0.30..0.65");
        }
    }

    @Test
    void theCrownBrickOfTheThinFaceIsIntact() throws IOException {
        for (String material : MATERIALS) {
            BufferedImage thin = read("trim_radial_thin_" + material);
            for (int[] texel : new int[][] {{31, 1}, {32, 1}, {31, 2}, {32, 2}}) {
                double weight = mortar(thin, texel[0], texel[1], material);
                assertTrue(weight < 0.3, material + ": crown texel " + texel[0] + "," + texel[1] + " reads as mortar (" + weight + ")");
            }
        }
    }

    /** The same physical joint width: mortar per joint per texel of band depth agrees between window and door. */
    @Test
    void roundWindowAndDoorFacesUseTheSameMortarWidth() throws IOException {
        for (String material : MATERIALS) {
            double door = bandMortar(read("trim_radial_thin_" + material), material, 32, 28, 32) / (DOOR_JOINTS * 4.0);
            double window = bandMortar(read("window_trim_round_" + material), material, 16, 12, 16) / (WINDOW_JOINTS * 4.0);
            assertTrue(door > 0 && window > 0, material + ": both faces have mortar");
            assertTrue(Math.abs(door - window) <= 0.35 * Math.max(door, window),
                    material + ": mortar per joint per texel differs, door " + door + " vs window " + window);
        }
    }

    @Test
    void archTrimUpperHalfIsTheThinFaceByteForByte() throws IOException {
        for (String material : MATERIALS) {
            BufferedImage arch = read("window_trim_arch_" + material);
            BufferedImage thin = read("trim_radial_thin_" + material);
            assertEquals(64, arch.getWidth(), material);
            assertEquals(64, arch.getHeight(), material);
            for (int y = 0; y < 32; y++) {
                for (int x = 0; x < 64; x++) {
                    assertEquals(thin.getRGB(x, y), arch.getRGB(x, y), material + ": texel " + x + "," + y);
                }
            }
        }
    }

    @Test
    void archTrimJambsRunCoursesAndTheSillShowsHeadJoints() throws IOException {
        for (String material : MATERIALS) {
            BufferedImage arch = read("window_trim_arch_" + material);
            for (int[] jamb : new int[][] {{0, 3}, {60, 63}}) {
                for (int y = 32; y <= 59; y++) {
                    boolean full = true;
                    for (int x = jamb[0]; x <= jamb[1]; x++) {
                        full &= mortar(arch, x, y, material) > 0.6;
                    }
                    assertEquals(JAMB_MORTAR_ROWS.contains(y), full,
                            material + ": jamb x=" + jamb[0] + ".." + jamb[1] + " row " + y + (full ? " is" : " is not") + " a mortar row");
                }
            }
            for (int y = 60; y <= 63; y++) {
                Set<Integer> joints = new TreeSet<>();
                for (int x = 0; x < 64; x++) {
                    if (mortar(arch, x, y, material) > 0.6) {
                        joints.add(x);
                    }
                }
                assertEquals(HEAD_JOINTS, joints, material + ": sill row " + y + " head joints");
            }
        }
    }
}
