package io.github.dirtyredz.roundwood.showcase;

import io.github.dirtyredz.roundwood.showcase.section.MaterialsSection;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import static io.github.dirtyredz.roundwood.fixture.DoorFixtures.check;

/**
 * The minimum-gap guard of the showcase (0.13.1, dk-4350cd9e), proved on the built world rather than on
 * the layout arithmetic: whatever the sections' sums say, every sample must stand at least
 * {@link ShowcaseSpacing#GAP} clear blocks (in the floor plan, whatever the height) from every other,
 * and every label sign beside its own sample and clear of everything else.
 *
 * <p>The scan projects every non-air block that is not a standing label sign onto the floor plan and
 * groups blocks closer than the gap into one sample. A sample is then whatever the sections built
 * together (a window and its neighbour demo, a door and its lever, the materials wall with its wall
 * signs), and two samples that were closer than the gap would have merged: so the group count must
 * equal the number of standing signs plus the one wall, and each group must be the nearest of exactly
 * one sign (the wall of none). Each sign must stand within {@code |LABEL_OFFSET|} of its sample, at
 * least {@link #SIGN_CLEARANCE} from every other sample, and as far from every other sign.
 */
public final class SpacingChecks {
    /** A sign's distance to a sample other than its own, and to another sign, at least (two clear blocks). */
    public static final int SIGN_CLEARANCE = 3;

    private SpacingChecks() {
    }

    private record Spot(int r, int f) {
        long key() {
            return ((long) r << 32) ^ (f & 0xffffffffL);
        }

        int distance(Spot other) {
            return Math.max(Math.abs(r - other.r), Math.abs(f - other.f));
        }
    }

    public static void verifySpacing(ServerLevel level, ShowcaseFrame frame) {
        Map<Long, Spot> blocks = new HashMap<>();
        Map<Long, Spot> signs = new HashMap<>();
        for (int r = 0; r < ShowcaseFrame.WIDTH; r++) {
            for (int f = 0; f < ShowcaseFrame.DEPTH; f++) {
                for (int y = 0; y < ShowcaseFrame.HEIGHT; y++) {
                    BlockState state = level.getBlockState(frame.pos(r, y, f));
                    if (state.isAir()) {
                        continue;
                    }
                    Spot spot = new Spot(r, f);
                    (state.getBlock() instanceof StandingSignBlock ? signs : blocks).put(spot.key(), spot);
                }
            }
        }
        List<List<Spot>> samples = samples(blocks);
        Spot wall = new Spot(MaterialsSection.R0, MaterialsSection.F);
        check(blocks.containsKey(wall.key()), "no materials wall at " + wall);
        check(samples.size() == signs.size() + 1, samples.size() + " samples for " + signs.size()
                + " signs and the wall: two samples are closer than " + ShowcaseSpacing.GAP + " blocks, or one has no sign: "
                + describe(samples));
        List<Spot> signList = new ArrayList<>(signs.values());
        Map<List<Spot>, Integer> owners = new IdentityHashMap<>();
        for (Spot sign : signList) {
            List<Spot> nearest = null;
            int nearestDistance = Integer.MAX_VALUE;
            for (List<Spot> sample : samples) {
                int d = distance(sign, sample);
                if (d < nearestDistance) {
                    nearest = sample;
                    nearestDistance = d;
                }
            }
            check(nearestDistance <= -ShowcaseSpacing.LABEL_OFFSET, "sign at " + sign + " is " + nearestDistance + " from its sample");
            for (List<Spot> sample : samples) {
                check(sample == nearest || distance(sign, sample) >= SIGN_CLEARANCE,
                        "sign at " + sign + " is within " + distance(sign, sample) + " of another sample " + bounds(sample));
            }
            check(!nearest.contains(wall), "a standing sign belongs to the materials wall at " + sign);
            check(owners.merge(nearest, 1, Integer::sum) == 1, "two signs label one sample " + bounds(nearest));
            for (Spot other : signList) {
                check(other == sign || sign.distance(other) >= SIGN_CLEARANCE, "signs " + sign + " and " + other + " are too close");
            }
        }
    }

    /** Groups of blocks no further than the gap apart, by flood fill over the floor plan. */
    private static List<List<Spot>> samples(Map<Long, Spot> blocks) {
        List<List<Spot>> out = new ArrayList<>();
        Map<Long, Boolean> seen = new HashMap<>();
        for (Spot start : blocks.values()) {
            if (seen.put(start.key(), true) != null) {
                continue;
            }
            List<Spot> group = new ArrayList<>();
            List<Spot> queue = new ArrayList<>(List.of(start));
            while (!queue.isEmpty()) {
                Spot spot = queue.remove(queue.size() - 1);
                group.add(spot);
                for (int dr = -ShowcaseSpacing.GAP; dr <= ShowcaseSpacing.GAP; dr++) {
                    for (int df = -ShowcaseSpacing.GAP; df <= ShowcaseSpacing.GAP; df++) {
                        Spot near = blocks.get(new Spot(spot.r + dr, spot.f + df).key());
                        if (near != null && seen.put(near.key(), true) == null) {
                            queue.add(near);
                        }
                    }
                }
            }
            out.add(group);
        }
        return out;
    }

    private static int distance(Spot sign, List<Spot> sample) {
        int best = Integer.MAX_VALUE;
        for (Spot spot : sample) {
            best = Math.min(best, sign.distance(spot));
        }
        return best;
    }

    private static String bounds(List<Spot> sample) {
        int r0 = Integer.MAX_VALUE;
        int r1 = Integer.MIN_VALUE;
        int f0 = Integer.MAX_VALUE;
        int f1 = Integer.MIN_VALUE;
        for (Spot spot : sample) {
            r0 = Math.min(r0, spot.r);
            r1 = Math.max(r1, spot.r);
            f0 = Math.min(f0, spot.f);
            f1 = Math.max(f1, spot.f);
        }
        return "r " + r0 + ".." + r1 + " f " + f0 + ".." + f1;
    }

    private static String describe(List<List<Spot>> samples) {
        List<String> out = new ArrayList<>();
        samples.forEach(sample -> out.add(bounds(sample)));
        return String.join("; ", out);
    }
}
