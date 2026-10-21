package io.github.dirtyredz.roundwood.data;

import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The generated block textures: manifest, sizes, preserved files, board, brick and neutral-grey structure. */
class GeneratedTexturesTest {
    private static final Path DIR = Path.of("src/main/resources/assets/roundwood/textures/block");
    private static final List<String> WOODS = List.of("oak", "spruce", "birch", "jungle", "acacia", "dark_oak",
            "mangrove", "cherry", "bamboo", "crimson", "warped");
    private static final List<String> DYES = List.of("white", "orange", "magenta", "light_blue", "yellow", "lime",
            "pink", "gray", "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black");
    /** Dyes whose boards and panels only appeared in 0.5.0. */
    private static final List<String> NEW_DYES = List.of("white", "orange", "magenta", "light_blue", "lime", "pink",
            "gray", "light_gray", "cyan", "purple", "brown", "black");
    private static final List<String> MASONRY = List.of("brick", "stone_brick", "deepslate_brick", "mud_brick");
    private static final double NEUTRAL_MEAN = 179.5;

    /** Every door panel: 11 woods, 16 dyes and the neutral boards used for unfamiliar woods. */
    private static List<String> doorPanels() {
        List<String> out = new ArrayList<>();
        WOODS.forEach(w -> out.add("door_panel_" + w));
        DYES.forEach(d -> out.add("door_panel_" + d));
        out.add("door_panel_neutral");
        return out;
    }

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
        TreeSet<String> expected = new TreeSet<>(List.of("brass", "window_glass"));
        expected.addAll(doorPanels());
        DYES.forEach(d -> expected.add("panel_" + d));
        for (String masonry : MASONRY) {
            expected.add("trim_radial_" + masonry);
            expected.add("trim_edge_" + masonry);
        }
        assertEquals(54, expected.size());
        TreeSet<String> actual = new TreeSet<>();
        try (Stream<Path> files = Files.list(DIR)) {
            files.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".png"))
                    .forEach(n -> actual.add(n.substring(0, n.length() - 4)));
        }
        assertEquals(expected, actual);
    }

    @Test
    void textureDimensionsMatchTheirRole() throws IOException {
        assertEquals(28, doorPanels().size());
        for (String panel : doorPanels()) {
            assertSize(panel, 32);
        }
        for (String dye : DYES) {
            assertSize("panel_" + dye, 16);
        }
        for (String masonry : MASONRY) {
            assertSize("trim_radial_" + masonry, 64);
            assertSize("trim_edge_" + masonry, 16);
        }
        assertSize("window_glass", 16);
        assertSize("brass", 16);
    }

    private static void assertSize(String name, int size) throws IOException {
        BufferedImage image = read(name);
        assertEquals(size, image.getWidth(), name + " width");
        assertEquals(size, image.getHeight(), name + " height");
    }

    @Test
    void the29PreservedTexturesAreByteForByteUnchanged() throws Exception {
        Map<String, String> hashes = Map.ofEntries(
                entry("brass", "3ec5bfd9eeb11e6a5ad2f0a1be62ab873cee072d662b12e4e2f2b9185fa3bc16"),
                entry("door_panel_acacia", "4f2d9649d123d0a27eb80d3cd3849c70ad340766d96e4761f428e01ecd874e02"),
                entry("door_panel_bamboo", "a97487691d64c02659bc5d69cf73d333f91342e9861f2578a2e3edf05fc18055"),
                entry("door_panel_birch", "3d064d3a9cfdb6c998cb8af80de57283c57fbbb7df747588065bf59cde4cdbd0"),
                entry("door_panel_blue", "6fa5cda337fdfd7e0234e0ae16be9c5032949b51ca1543a6d499f010a1014358"),
                entry("door_panel_cherry", "e38496398244669ed70406afdc556a4569cc075486bf2227de6260bfdf54fbcc"),
                entry("door_panel_crimson", "ae3e016d669fdaf8f6ccbdd65422376308429d7991444185bbda81079d29a228"),
                entry("door_panel_dark_oak", "e674731c59d00da1ba046009055aa62d2fba9f3d27c3d03010f74efa0cf4e088"),
                entry("door_panel_green", "615b0ab30b83c47f0ab24e07afdd4bd6ced877b87cd29b365444cb31ec257ec2"),
                entry("door_panel_jungle", "06440b80142c1a188775998c01faedf6c65a32a3789cdc8ce748cb2bd8a34c3a"),
                entry("door_panel_mangrove", "523bd7ecf99a58452574278a42f8b8bdf02bcace2541661d5dc95d976746d0c5"),
                entry("door_panel_oak", "9556ce700c3fcac1f819afbec5fc820361c0b9bcacc50211a6d1b18fe23fd9f2"),
                entry("door_panel_red", "690938e6ae5d3ec1b7b23aceb2cd2226f931315f142ce6435282c1ff55eb84db"),
                entry("door_panel_spruce", "c79ef30fdfe654d8faedba35c1675c4a96d78c08ff9157b212cdd78e99718bf2"),
                entry("door_panel_warped", "764933826f9303aa1fe3802a6e9a26d2d186dc73035d95311b3843b73ade9230"),
                entry("door_panel_yellow", "93423cc31d691acc184e7f734d69a81631fb9a6435e3d2e0aca261624be58ddd"),
                entry("panel_blue", "20bcd9cc9889e7c38d484a5354c646333ef1600088fb270d3a84d14af395174f"),
                entry("panel_green", "e38b41670723c40870421b1e40070a268c1eddd1ce05bac0151ecd826863ab92"),
                entry("panel_red", "b0f90c21cc399b804a5565d869f4bad20fa733a5c0607df7ca1452779ec1d496"),
                entry("panel_yellow", "9a767211b659a3147910c9d6eb2c1e9d71e792f70f73571c291140e35a8be249"),
                entry("trim_edge_brick", "66cfa72bef4f95b6f869adbda449b087b096a04da19f406cd2906faec33eec3f"),
                entry("trim_edge_deepslate_brick", "3e54d616a762e26f24d14110534439fe5e3ea240a908b222de7b42bf4b447e27"),
                entry("trim_edge_mud_brick", "d07ac8e7f944cda42d9bb38da6dfd7c305b0257b6a5681060636dc63fe715d9d"),
                entry("trim_edge_stone_brick", "8759c22f68cf2347e17ff771ff76a57df3e1ff8d1e864df9180626d98f9739c7"),
                entry("trim_radial_brick", "c0cdec65a33671ec542abbb77df224251f93a0e23d7c3917641f2cf11f29183f"),
                entry("trim_radial_deepslate_brick", "a1f1033a8d78bc2cd1d7ceced47a3bbe810f70c444c816547353a4c76d407326"),
                entry("trim_radial_mud_brick", "c30bdc6ddd2db9338da77ca3ae364a0c73c85f989f81a1c6327c644a9c57b52a"),
                entry("trim_radial_stone_brick", "afc402105feffdd9ac9167afdf49fc587c687649384a11bde4ca5272896afbf7"),
                entry("window_glass", "44d3d4526d1551bf4e26be13c35d350986d9a39f430a8f93cd54ace93a752236"));
        assertEquals(29, hashes.size());
        for (Map.Entry<String, String> entry : hashes.entrySet()) {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(DIR.resolve(entry.getKey() + ".png")));
            assertEquals(entry.getValue(), HexFormat.of().formatHex(digest), entry.getKey() + ".png changed");
        }
        // The 25 new files are exactly the 12 new dyes' boards and panels plus the neutral boards.
        assertEquals(25, NEW_DYES.size() * 2 + 1);
    }

    @Test
    void doorBoardsRunTheFullHeightWithoutRestartingOrCourses() throws IOException {
        for (String name : doorPanels()) {
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
                // A real joint line darkens nearly every column; grain noise reaches about half on a few rows.
                assertTrue(joints <= columns * 3 / 4, name + ": row " + y + " looks like a horizontal joint line");
                joints = 0;
            }
        }
    }

    /** The neutral boards are tinted at runtime, so they must be pure grey with the documented mean. */
    @Test
    void neutralBoardsArePureGreyWithTheDocumentedMean() throws IOException {
        BufferedImage image = read("door_panel_neutral");
        double sum = 0;
        for (int y = 0; y < 32; y++) {
            for (int x = 0; x < 32; x++) {
                int argb = image.getRGB(x, y);
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                assertTrue(r == g && g == b, "texel " + x + "," + y + " is not grey: " + r + "," + g + "," + b);
                sum += r;
            }
        }
        double mean = sum / (32 * 32);
        assertEquals(NEUTRAL_MEAN, mean, 1.0, "mean grey level must match DonorColors.NEUTRAL_MEAN");
    }

    @Test
    void paintedDoorPanelsAreOpaqueAndLegible() throws IOException {
        List<String> painted = new ArrayList<>(doorPanels());
        painted.remove("door_panel_neutral");
        assertEquals(27, painted.size());
        for (String name : painted) {
            BufferedImage image = read(name);
            double sum = 0;
            for (int y = 0; y < 32; y++) {
                for (int x = 0; x < 32; x++) {
                    assertEquals(255, alpha(image, x, y), name + ": translucent texel at " + x + "," + y);
                    sum += luminance(image, x, y);
                }
            }
            double mean = sum / (32 * 32);
            assertTrue(mean > 25 && mean < 235, name + ": average luminance " + mean + " is not legible");
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
