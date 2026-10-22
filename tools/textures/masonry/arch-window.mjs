// The arched window trim's 64x64 face (also the base of the top-corner and lower-arch faces).
import { brickColour, polar, radialMortar, withMortar } from './mortar.mjs';

/** The arched window's face: the 32 x 32 assembly at two texels per pixel. */
export const ARCH_FACE_SIZE = 64;

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
        const { index, across, radius } = polar(x, y, ARCH_FACE_SIZE / 2, ARCH_FACE_SIZE / 2, 24);
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

