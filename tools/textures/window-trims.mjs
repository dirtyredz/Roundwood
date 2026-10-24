// 0.7.0 trim artwork: the round window trim's thin-mortar face and the window trim's source markers.
// The older artwork (radial-bricks.mjs, window-glass.mjs) is left exactly as it was so its bytes never
// change. The mortar primitives live in masonry/mortar.mjs, the arched face in masonry/arch-window.mjs.
import { hash, mix } from './noise.mjs';
import { RADIAL_SIZE } from './radial-bricks.mjs';
import { brickColour, polar, radialMortar, withMortar } from './mortar.mjs';

/** The round window trim's face: 16 model pixels at two texels each. */
export const ROUND_FACE_SIZE = 32;
/** Bricks around the round window's ring (the door ring's 24, scaled to its smaller circumference). */
export const ROUND_BRICKS = 12;

/** A radial brick face of `size` texels with `bricks` wedges and thin joints. */
export function thinRadialFace(palette, seed, size = RADIAL_SIZE, bricks = 24) {
  const k = size / RADIAL_SIZE; // arris radii scale with the face
  const px = [];
  for (let y = 0; y < size; y++) {
    for (let x = 0; x < size; x++) {
      const { index, across, radius } = polar(x, y, size / 2, size / 2, bricks);
      const colour = brickColour(palette, { index: index, across: across, radius: radius }, { outerArris: (61 / 2) * k, innerArris: (57 / 2) * k, x, y, seed });
      px.push(withMortar(colour, palette, radialMortar(x, y, size, bricks), x, y, seed));
    }
  }
  return px;
}

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
