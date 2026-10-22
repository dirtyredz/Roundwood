// 0.11.0 door casing artwork: the arched door casing's 64x64 masonry face, thin mortar, two texels
// per model pixel like the window trims. The casing (16 x 32 model pixels, straight jambs under a
// semicircular crown) lives in the left 32 texel columns; the right half is a copy. New files only.
import { brickColour, jointCoverage, withMortar } from './window-trims.mjs';

/** The casing face: a 32 x 32 model pixel area at two texels each. */
export const DOOR_CASING_FACE_SIZE = 64;
/** Texel row of the spring line, where the crown meets the jambs. */
const SPRING = 16;
/** Bricks around the full crown circle. */
const CROWN_BRICKS = 12;
/** Bed joints of the straight jambs, in texel rows. */
const JAMB_JOINTS = [16, 23, 30, 37, 44, 51, 58];

/** Polar position of a texel centre about (cx, cy), the crown brick centred on the top. */
function polarAbout(x, y, cx, cy, bricks) {
  const dx = x + 0.5 - cx;
  const dy = cy - (y + 0.5);
  const width = (2 * Math.PI) / bricks;
  const turns = (Math.atan2(dx, dy) + width / 2) / width;
  const index = ((Math.floor(turns) % bricks) + bricks) % bricks;
  return { index, across: turns - Math.floor(turns), radius: Math.hypot(dx, dy), width };
}

/** The arched door casing's 64 x 64 face: radial crown over straight jamb courses, left half copied right. */
export function archedCasingFace(palette, seed) {
  const px = [];
  for (let y = 0; y < DOOR_CASING_FACE_SIZE; y++) {
    for (let x = 0; x < DOOR_CASING_FACE_SIZE; x++) {
      const xl = x % 32;
      if (y < SPRING) {
        const { index, across, radius, width } = polarAbout(xl, y, 16, 16, CROWN_BRICKS);
        const colour = brickColour(palette, index, across, radius, 15.5, 12.5, x, y, seed);
        const coverage = jointCoverage(Math.min(across, 1 - across) * width * radius);
        px.push(withMortar(colour, palette, coverage, x, y, seed));
        continue;
      }
      const coverage = JAMB_JOINTS.includes(y) ? 1 : 0;
      const index = 200 + Math.floor((y - SPRING) / 7) * 2 + (xl < 16 ? 0 : 1);
      const across = ((y - SPRING) % 7) / 7;
      const colour = brickColour(palette, index, across, 30, 99, 0, x, y, seed);
      px.push(withMortar(colour, palette, coverage, x, y, seed));
    }
  }
  return px;
}
