// Radial masonry for the brick frames: a 64x64 face mapped once across the whole 2x2 ring (two
// texels per model pixel) and a 16x16 granular edge texture for the ring's sides.
//
// The face is polar: 24 wedge-shaped bricks around the image centre, one centred on the crown (and,
// by symmetry, at the springing points and the base), with mortar joints that radiate from the
// centre. Each brick has its own tone, a lit leading edge, a shaded trailing edge, and speckle.
import { hash, mix, shade } from './noise.mjs';
import { brickColour, polar as polarAt } from './mortar.mjs';

export const RADIAL_SIZE = 64;
export const EDGE_SIZE = 16;
export const BRICKS = 24;
/** Half the width of a mortar joint, in texels measured along the arc. */
export const JOINT_HALF_WIDTH = 0.85;

/** Brick index (0 = the crown, clockwise) and position across it (0..1) for a texel centre. */
export function polar(x, y) {
  return polarAt(x, y, RADIAL_SIZE / 2, RADIAL_SIZE / 2, BRICKS);
}

/** True when the texel centre lies in a mortar joint. */
export function inJoint(x, y) {
  const { across, radius, width } = polar(x, y);
  return Math.min(across, 1 - across) * width * radius <= JOINT_HALF_WIDTH;
}

export function radialFace(palette, seed) {
  const { mortar } = palette;
  const px = [];
  for (let y = 0; y < RADIAL_SIZE; y++) {
    for (let x = 0; x < RADIAL_SIZE; x++) {
      const position = polar(x, y);
      if (inJoint(x, y)) {
        px.push(shade(mortar, 0.94 + hash(x, y, seed + 7) * 0.1));
        continue;
      }
      px.push(brickColour(palette, position, { outerArris: 61 / 2, innerArris: 57 / 2, x, y, seed }));
    }
  }
  return px;
}

/** The ring's sides: brick body with granular speckle and the odd mortar fleck. */
export function radialEdge(palette, seed) {
  const { brick, dark, light, mortar } = palette;
  const px = [];
  for (let y = 0; y < EDGE_SIZE; y++) {
    for (let x = 0; x < EDGE_SIZE; x++) {
      const grain = hash(x, y, seed + 11);
      let colour = shade(brick, 0.92 + hash(y, x, seed + 13) * 0.14);
      if (grain < 0.1) colour = mix(colour, dark, 0.5);
      else if (grain > 0.93) colour = mix(colour, light, 0.45);
      else if (grain > 0.9) colour = mix(colour, mortar, 0.6);
      px.push(colour);
    }
  }
  return px;
}
