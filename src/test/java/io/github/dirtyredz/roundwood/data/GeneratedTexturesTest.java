package io.github.dirtyredz.roundwood.data;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The generated block textures: manifest, sizes, preserved legacy files, board and brick structure. */
class GeneratedTexturesTest {
    private static final Path DIR = Path.of("src/main/resources/assets/roundwood/textures/block");
    private static final List<String> FINISHES = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "mangrove", "cherry", "bamboo", "crimson", "warped", "green", "yellow", "red", "blue");
    private static final List<String> MASONRY = List.of("brick", "stone_brick", "deepslate_brick", "mud_brick");
    private static final List<String> LEGACY = List.of("brass", "panel_green", "panel_yellow", "panel_red", "panel_blue");

    private static BufferedImage read(String name) throws IOException {
        BufferedImage image = ImageIO.read(DIR.resolve(name + ".png").toFile());
        assertTrue(image != null, name + ".png must decode");
        return image;
    }

    private static double luminance(BufferedImage image, int x, int y) {
        int argb = image.getRGB(x, y);
        return 0.3 * ((argb >> 16) & 0xFF) + 0.59 * ((argb >> 8) & 0xFF) + 0.11 * (argb & 0xFF);
    }

    private static int alpha(BufferedImage image, int x, int y) {
        return image.getRGB(x, y) >>> 24;
    }

    @Test
    void theTextureManifestIsExact() throws IOException {
        TreeSet<String> expected = new TreeSet<>(LEGACY);
        expected.add("window_glass");
        for (String finish : FINISHES) {
            expected.add("door_panel_" + finish);
        }
        for (String masonry : MASONRY) {
            expected.add("trim_radial_" + masonry);
            expected.add("trim_edge_" + masonry);
        }
        assertEquals(29, expected.size());
        TreeSet<String> actual = new TreeSet<>();
        try (Stream<Path> files = Files.list(DIR)) {
            files.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".png"))
                    .forEach(n -> actual.add(n.substring(0, n.length() - 4)));
        }
        assertEquals(expected, actual);
    }

    @Test
    void textureDimensionsMatchTheirRole() throws IOException {
        for (String finish : FINISHES) {
            assertSize("door_panel_" + finish, 32);
        }
        for (String masonry : MASONRY) {
            assertSize("trim_radial_" + masonry, 64);
            assertSize("trim_edge_" + masonry, 16);
        }
        assertSize("window_glass", 16);
        for (String legacy : LEGACY) {
            assertSize(legacy, 16);
        }
    }

    private static void assertSize(String name, int size) throws IOException {
        BufferedImage image = read(name);
        assertEquals(size, image.getWidth(), name + " width");
        assertEquals(size, image.getHeight(), name + " height");
    }

    @Test
    void legacyTexturesAreByteForByteUnchanged() throws Exception {
        Map<String, String> hashes = Map.of(
                "brass", "3ec5bfd9eeb11e6a5ad2f0a1be62ab873cee072d662b12e4e2f2b9185fa3bc16",
                "panel_blue", "20bcd9cc9889e7c38d484a5354c646333ef1600088fb270d3a84d14af395174f",
                "panel_green", "e38b41670723c40870421b1e40070a268c1eddd1ce05bac0151ecd826863ab92",
                "panel_red", "b0f90c21cc399b804a5565d869f4bad20fa733a5c0607df7ca1452779ec1d496",
                "panel_yellow", "9a767211b659a3147910c9d6eb2c1e9d71e792f70f73571c291140e35a8be249");
        assertEquals(LEGACY.size(), hashes.size());
        for (Map.Entry<String, String> entry : hashes.entrySet()) {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(DIR.resolve(entry.getKey() + ".png")));
            assertEquals(entry.getValue(), HexFormat.of().formatHex(digest), entry.getKey() + ".png changed");
        }
    }

    @Test
    void doorBoardsRunTheFullHeightWithoutRestartingOrCourses() throws IOException {
        for (String finish : FINISHES) {
            String name = "door_panel_" + finish;
            BufferedImage image = read(name);
            int seams = 0;
            for (int x = 0; x < 32; x += 4) {
                seams++;
                for (int y = 0; y < 32; y++) {
                    double seam = luminance(image, x, y);
                    assertTrue(seam < luminance(image, x + 2, y) && seam < luminance(image, x + 3, y),
                            name + ": seam column " + x + " not darker at row " + y + " (rows 15/16 mean a restart)");
                }
            }
            assertEquals(8, seams, name + ": board count");
            int columns = 0;
            for (int x = 0; x < 32; x++) {
                if (x % 4 != 0) {
                    columns++;
                }
            }
            int joints = 0;
            for (int y = 1; y < 31; y++) {
                for (int x = 0; x < 32; x++) {
                    if (x % 4 != 0 && luminance(image, x, y) < luminance(image, x, y - 1)
                            && luminance(image, x, y) < luminance(image, x, y + 1)) {
                        joints++;
                    }
                }
                assertTrue(joints <= columns / 2, name + ": row " + y + " looks like a horizontal joint line");
                joints = 0;
            }
        }
    }

    private static int[] sample(double radius, double degrees) {
        double theta = Math.toRadians(degrees);
        long x = Math.round(32 + radius * Math.sin(theta) - 0.5);
        long y = Math.round(32 - radius * Math.cos(theta) - 0.5);
        return new int[] {(int) x, (int) y};
    }

    @Test
    void radialFacesHaveMortarJointsBetweenEveryBrickAndSolidBricksOnTheMidlines() throws IOException {
        for (String masonry : MASONRY) {
            String name = "trim_radial_" + masonry;
            BufferedImage image = read(name);
            double jointSum = 0;
            double brickSum = 0;
            int direction = 0;
            for (int k = 0; k < 24; k++) {
                int[] joint = sample(30, (k + 0.5) * 15);
                int[] centre = sample(30, k * 15.0);
                double jointLum = luminance(image, joint[0], joint[1]);
                double centreLum = luminance(image, centre[0], centre[1]);
                double diff = jointLum - centreLum;
                assertTrue(Math.abs(diff) > 12, name + ": joint " + k + " indistinct from brick centre (" + diff + ")");
                if (direction == 0) {
                    direction = diff > 0 ? 1 : -1;
                }
                assertTrue(diff * direction > 0, name + ": joint " + k + " differs in the opposite direction");
                jointSum += jointLum;
                brickSum += centreLum;
            }
            double mortar = jointSum / 24;
            double brick = brickSum / 24;
            // The crown brick straddles the vertical midline, the quadrant boundaries are brick centres.
            for (int[] texel : new int[][] {{31, 2}, {32, 2}, {61, 31}, {61, 32}, {31, 61}, {32, 61}, {2, 31}, {2, 32}}) {
                double lum = luminance(image, texel[0], texel[1]);
                assertTrue(Math.abs(lum - brick) < Math.abs(lum - mortar),
                        name + ": texel " + texel[0] + "," + texel[1] + " reads as mortar, expected brick");
            }
        }
    }

    @Test
    void windowGlassIsVisibleYetTranslucentEverywhere() throws IOException {
        BufferedImage image = read("window_glass");
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int a = alpha(image, x, y);
                assertTrue(a >= 40 && a < 255, "alpha " + a + " at " + x + "," + y);
            }
        }
    }
}
