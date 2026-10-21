// 0.7.0 trim artwork: thin-mortar masonry for door and window trims, the clearer window glass, and
// the window trim's source markers. The older artwork (radial-bricks.mjs, window-glass.mjs) is left
// exactly as it was so its bytes never change; everything here is new files.
//
// Masonry faces are mapped at two texels per model pixel, like the door ring's 64x64 face, so one
// mortar width (THIN_HALF_WIDTH texels either side of a joint line, half the old 0.85) is the same
// physical width on every trim. Joints are drawn with partial coverage (a texel is mixed towards the
// mortar by how much of it the joint line covers), so a joint narrower than one texel stays a
// continuous line instead of breaking into dots.
import { hash, mix, shade } from './noise.mjs';
import { RADIAL_SIZE } from './radial-bricks.mjs';
import { GLASS_SIZE, windowGlass } from './window-glass.mjs';

/** Half the width of a thin mortar joint, in texels (0.85 before 0.7.0). */
export const THIN_HALF_WIDTH = 0.425;
/** The round window trim's face: 16 model pixels at two texels each. */
export const ROUND_FACE_SIZE = 32;
/** Bricks around the round window's ring (the door ring's 24, scaled to its smaller circumference). */
export const ROUND_BRICKS = 12;
/** The arched window's face: the 32 x 32 assembly at two texels per pixel. */
export const ARCH_FACE_SIZE = 64;
/** Glass alpha outside the diagonal accents: 92..104 before, 24..36 now (accents keep 176). */
export const CLEAR_ALPHA_DROP = 68;

/** How much of texel-wide coverage a joint line at `distance` texels from the texel centre gives (0..1). */
export function jointCoverage(distance, halfWidth = THIN_HALF_WIDTH) {
  return Math.max(0, Math.min(1, halfWidth + 0.5 - distance));
}

/** Polar position of a texel centre in a `size` face with `bricks` wedges, the crown brick centred on the top. */
export function polar(x, y, size, bricks) {
  const c = size / 2;
  const dx = x + 0.5 - c;
  const dy = c - (y + 0.5);
  const width = (2 * Math.PI) / bricks;
  const turns = (Math.atan2(dx, dy) + width / 2) / width;
  const index = ((Math.floor(turns) % bricks) + bricks) % bricks;
  return { index, across: turns - Math.floor(turns), radius: Math.hypot(dx, dy), width };
}

/** Mortar coverage (0..1) of a texel in a radial face. */
export function radialMortar(x, y, size, bricks) {
  const { across, radius, width } = polar(x, y, size, bricks);
  return jointCoverage(Math.min(across, 1 - across) * width * radius);
}

function brickColour(palette, index, across, radius, outerArris, innerArris, x, y, seed) {
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

function withMortar(colour, palette, coverage, x, y, seed) {
  if (coverage <= 0) return colour;
  return mix(colour, shade(palette.mortar, 0.94 + hash(x, y, seed + 7) * 0.1), coverage);
}

/** A radial brick face of `size` texels with `bricks` wedges and thin joints. */
export function thinRadialFace(palette, seed, size = RADIAL_SIZE, bricks = 24) {
  const k = size / RADIAL_SIZE; // arris radii scale with the face
  const px = [];
  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      const { index, across, radius } = polar(x, y, size, bricks);
      const colour = brickColour(palette, index, across, radius, (61 / 2) * k, (57 / 2) * k, x, y, seed);
      px.push(withMortar(colour, palette, radialMortar(x, y, size, bricks), x, y, seed));
    }
  }
  return px;
}

/** Straight-course mortar coverage in the arched face's lower half (texel y >= 32), or null above it. */
export function courseMortar(x, y) {
  const half = ARCH_FACE_SIZE / 2;
  if (y < half) return null;
  if (y >= ARCH_FACE_SIZE - 4) {
    // the sill: one course of 8-texel bricks, symmetric about the middle, under a bed joint
    return [0, 9, 18, 27, 36, 45, 54, 63].includes(x) ? 1 : 0;
  }
  // the jambs: courses stacked from the spring line down to the sill, a bed joint every seven rows
  return [32, 39, 46, 53, 59].includes(y) ? 1 : 0;
}

/**
 * The arched window trim's 64x64 face over the whole 2 x 2 window: above the spring line the thin
 * radial face of the door ring (24 bricks, crown centred, same tones), below it straight jamb
 * courses down to a sill of horizontal bricks, so no lower circular quarter ever shows.
 */
export function archWindowFace(palette, seed) {
  const px = [];
  for (let y = 0; y < ARCH_FACE_SIZE; y++) {
    for (let x = 0; x < ARCH_FACE_SIZE; x++) {
      const course = courseMortar(x, y);
      if (course === null) {
        const { index, across, radius } = polar(x, y, ARCH_FACE_SIZE, 24);
        const colour = brickColour(palette, index, across, radius, 61 / 2, 57 / 2, x, y, seed);
        px.push(withMortar(colour, palette, radialMortar(x, y, ARCH_FACE_SIZE, 24), x, y, seed));
        continue;
      }
      const sill = y >= ARCH_FACE_SIZE - 4;
      const index = sill ? 100 + Math.floor((x + 9) / 9) : 200 + Math.floor((y - 32) / 7) * 2 + (x < 32 ? 0 : 1);
      const across = sill ? ((x + 9) % 9) / 9 : ((y - 32) % 7) / 7;
      const colour = brickColour(palette, index, across, 30, 99, 0, x, y, seed);
      px.push(withMortar(colour, palette, course, x, y, seed));
    }
  }
  return px;
}

/** The 0.7.0 window glazing: window_glass with only the non-accent texels made clearer. */
export function clearWindowGlass(seed) {
  return windowGlass(seed).map(([r, g, b, a]) => (a === 176 ? [r, g, b, a] : [r, g, b, a - CLEAR_ALPHA_DROP]));
}

export { GLASS_SIZE };

/**
 * The window trim's source markers (16x16, opaque): stand-ins the client always replaces with the
 * trim's look, so the trim's quads can be told from the pane's oak wood after baking. Drawn as plain
 * light wood in case a resource pack ever shows them.
 */
export function trimMarker(palette, seed, edge) {
  const px = [];
  for (let y = 0; y < 16; y++) {
    for (let x = 0; x < 16; x++) {
      const grain = hash(edge ? y : x, 0, seed) * 0.6 + hash(x, y, seed + 1) * 0.4;
      px.push(mix(palette.mid, grain > 0.6 ? palette.light : palette.dark, Math.abs(grain - 0.5) * 0.6));
    }
  }
  return px;
}
