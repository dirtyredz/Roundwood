package io.github.dirtyredz.roundwood.client.material.tint;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The pure colour arithmetic behind unfamiliar-wood door boards: the average of the opaque texels
 * of a NativeImage-ordered (ABGR) pixel array, and the tint that makes neutral boards land on it.
 */
class DonorColorsTest {
    /** A NativeImage pixel: alpha in the high byte, then blue, green, red in the low byte. */
    private static int abgr(int alpha, int red, int green, int blue) {
        return alpha << 24 | blue << 16 | green << 8 | red;
    }

    @Test
    void anOpaquePixelAveragesToItselfInRgbOrder() {
        assertEquals(0x102030, DonorColors.average(new int[] {abgr(255, 0x10, 0x20, 0x30)}));
        assertEquals(0xFF0000, DonorColors.average(new int[] {abgr(255, 255, 0, 0)}));
        assertEquals(0x0000FF, DonorColors.average(new int[] {abgr(255, 0, 0, 255)}));
    }

    @Test
    void channelsAreAveragedAndRounded() {
        assertEquals(0x204060, DonorColors.average(new int[] {abgr(255, 0x10, 0x30, 0x50), abgr(255, 0x30, 0x50, 0x70)}));
        // 10 and 11 average to 10.5, which rounds up.
        assertEquals(0x0B0B0B, DonorColors.average(new int[] {abgr(255, 10, 10, 10), abgr(255, 11, 11, 11)}));
        // 10, 10 and 11 average to 10.33, which rounds down.
        assertEquals(0x0A0A0A, DonorColors.average(new int[] {abgr(255, 10, 10, 10), abgr(255, 10, 10, 10), abgr(255, 11, 11, 11)}));
    }

    @Test
    void texelsBelowHalfAlphaAreIgnored() {
        int[] pixels = {abgr(255, 100, 100, 100), abgr(127, 250, 0, 0), abgr(0, 0, 250, 0)};
        assertEquals(0x646464, DonorColors.average(pixels));
        // 128 is the first alpha that counts.
        assertEquals(0xB43232, DonorColors.average(new int[] {abgr(128, 0xB4, 0x32, 0x32)}));
    }

    @Test
    void anAllTransparentOrEmptyImageFallsBackToOak() {
        assertEquals(0xA2824E, DonorColors.FALLBACK);
        assertEquals(DonorColors.FALLBACK, DonorColors.average(new int[0]));
        assertEquals(DonorColors.FALLBACK, DonorColors.average(new int[] {0, abgr(100, 1, 2, 3), abgr(127, 255, 255, 255)}));
    }

    @Test
    void averagingIsDeterministicAndIgnoresOrder() {
        int[] a = {abgr(255, 1, 2, 3), abgr(255, 200, 100, 50), abgr(0, 9, 9, 9), abgr(255, 77, 88, 99)};
        int[] b = {a[3], a[2], a[1], a[0]};
        assertEquals(DonorColors.average(a), DonorColors.average(a));
        assertEquals(DonorColors.average(a), DonorColors.average(b));
    }

    @Test
    void tintScalesEachChannelBy255OverTheNeutralMean() {
        // Oak's average over the real neutral mean: 162, 130, 78 -> 230.1, 184.7, 110.8.
        assertEquals(0xE6B96F, DonorColors.tintFor(0xA2824E, 179.5));
        // A mean of 255 leaves colours alone; 127.5 doubles them.
        assertEquals(0x102030, DonorColors.tintFor(0x102030, 255.0));
        assertEquals(0x204060, DonorColors.tintFor(0x102030, 127.5));
        assertEquals(0x000000, DonorColors.tintFor(0x000000, 179.5));
    }

    @Test
    void tintRoundsToTheNearestInteger() {
        // 1 * 255 / 2 = 127.5 rounds to 128; 1 * 255 / 3 = 85 exactly; 2 * 255 / 7 = 72.86 rounds to 73.
        assertEquals(0x800000, DonorColors.tintFor(0x010000, 2.0));
        assertEquals(0x005500, DonorColors.tintFor(0x000100, 3.0));
        assertEquals(0x000049, DonorColors.tintFor(0x000002, 7.0));
    }

    @Test
    void tintIsCappedAtFullIntensityPerChannel() {
        assertEquals(0xFFFFFF, DonorColors.tintFor(0xFFFFFF, 100.0));
        assertEquals(0xFF00FF, DonorColors.tintFor(0xC800C8, 100.0));
        assertEquals(0xFF0000, DonorColors.tintFor(0xC80000, 50.0));
    }

    @Test
    void aNeutralBoardTimesItsTintLandsOnTheDonorAverage() {
        double mean = DonorColors.NEUTRAL_MEAN;
        int donor = 0x7A5B3C;
        int tint = DonorColors.tintFor(donor, mean);
        int[] donorChannels = {(donor >> 16) & 0xFF, (donor >> 8) & 0xFF, donor & 0xFF};
        int[] tintChannels = {(tint >> 16) & 0xFF, (tint >> 8) & 0xFF, tint & 0xFF};
        for (int i = 0; i < 3; i++) {
            assertEquals(donorChannels[i], mean * tintChannels[i] / 255.0, 0.5 * mean / 255.0 + 1e-9, "channel " + i);
        }
    }
}
