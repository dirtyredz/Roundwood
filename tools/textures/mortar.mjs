// Shared masonry primitives for the thin-mortar trim faces: the joint coverage, the polar brick
// position every radial face uses, and the brick and mortar colours. Masonry faces are mapped at two
// texels per model pixel, so one mortar width (THIN_HALF_WIDTH texels either side of a joint line,
// half the old 0.85) is the same physical width on every trim. Joints are drawn with partial coverage
// (a texel is mixed towards the mortar by how much of it the joint line covers), so a joint narrower
// than one texel stays a continuous line instead of breaking into dots.
import { hash, mix, shade } from './noise.mjs';

/** Half the width of a thin mortar joint, in texels (0.85 before 0.7.0). */
export const THIN_HALF_WIDTH = 0.425;

/** How much of texel-wide coverage a joint line at `distance` texels from the texel centre gives (0..1). */
export function jointCoverage(distance, halfWidth = THIN_HALF_WIDTH) {
  return Math.max(0, Math.min(1, halfWidth + 0.5 - distance));
}

/** Distance from `at` to the closest of the joint `lines`. */
export function nearest(lines, at) {
  return Math.min(...lines.map((line) => Math.abs(at - line)));
}

/**
 * Polar position of a texel centre about (cx, cy), with `count` bricks over `span` radians and the
 * crown brick centred on the top: the brick index, how far across it (0..1), the radius and the
 * brick's angular width. A full circle (the default span) wraps the index into 0..count-1; a partial
 * span (the archway's half circle) keeps the raw index, which is negative left of the crown.
 */
export function polar(x, y, cx, cy, count, span = 2 * Math.PI) {
  const dx = x + 0.5 - cx;
  const dy = cy - (y + 0.5);
  const width = span / count;
  const turns = (Math.atan2(dx, dy) + width / 2) / width;
  const whole = Math.floor(turns);
  const index = span === 2 * Math.PI ? ((whole % count) + count) % count : whole;
  return { index, across: turns - whole, radius: Math.hypot(dx, dy), width };
}

/** Mortar coverage (0..1) of a texel in a radial face of `size` texels with `bricks` wedges. */
export function radialMortar(x, y, size, bricks) {
  const { across, radius, width } = polar(x, y, size / 2, size / 2, bricks);
  return jointCoverage(Math.min(across, 1 - across) * width * radius);
}

export function brickColour(palette, { index, across, radius }, { outerArris, innerArris, x, y, seed }) {
  const { brick, dark, light } = palette;
  let colour = shade(brick, 0.9 + hash(index, 0, seed) * 0.2);
  if (across < 0.22) colour = mix(colour, light, 0.35);
  if (across > 0.8) colour = mix(colour, dark, 0.4);
  if (radius > outerArris) colour = mix(colour, light, 0.15);
  if (radius < innerArris) colour = mix(colour, dark, 0.2);
  const grain = hash(x, y, seed);
  if (grain < 0.08) colour = mix(colour, dark, 0.5);
  else if (grain > 0.95) colour = mix(colour, light, 0.4);
  return shade(colour, 0.97 + hash(y, x, seed + 3) * 0.06);
}

export function withMortar(colour, palette, coverage, x, y, seed) {
  if (coverage <= 0) return colour;
  return mix(colour, shade(palette.mortar, 0.94 + hash(x, y, seed + 7) * 0.1), coverage);
}
