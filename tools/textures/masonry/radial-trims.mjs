// Thin radial masonry faces for door rings and round window trims.
import { RADIAL_SIZE } from './radial-bricks.mjs';
import { brickColour, polar, radialMortar, withMortar } from '../mortar.mjs';

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
