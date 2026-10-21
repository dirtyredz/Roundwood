// Radial masonry for the brick frames: a 64x64 face mapped once across the whole 2x2 ring (two
// texels per model pixel) and a 16x16 granular edge texture for the ring's sides.
//
// The face is polar: 24 wedge-shaped bricks around the image centre, one centred on the crown (and,
// by symmetry, at the springing points and the base), with mortar joints that radiate from the
// centre. Each brick has its own tone, a lit leading edge, a shaded trailing edge, and speckle.
import { hash, mix, shade } from './noise.mjs';

export const RADIAL_SIZE = 64;
export const EDGE_SIZE = 16;
export const BRICKS = 24;
/** Half the width of a mortar joint, in texels measured along the arc. */
export const JOINT_HALF_WIDTH = 0.85;

/** Brick index (0 = the crown, clockwise) and position across it (0..1) for a texel centre. */
export function polar(x, y) {
  const c = RADIAL_SIZE / 2;
  const dx = x + 0.5 - c;
  const dy = c - (y + 0.5);
  const width = (2 * Math.PI) / BRICKS;
  const turns = (Math.atan2(dx, dy) + width / 2) / width; // clockwise from the top, crown centred
  const index = ((Math.floor(turns) % BRICKS) + BRICKS) % BRICKS;
  return { index, across: turns - Math.floor(turns), radius: Math.hypot(dx, dy), width };
}

/** True when the texel centre lies in a mortar joint. */
export function inJoint(x, y) {
  const { across, radius, width } = polar(x, y);
  return Math.min(across, 1 - across) * width * radius <= JOINT_HALF_WIDTH;
}

export function radialFace(palette, seed) {
  const { brick, dark, light, mortar } = palette;
  const px = [];
  for (let y = 0; y < RADIAL_SIZE; y++) {
    for (let x = 0; x < RADIAL_SIZE; x++) {
      const { index, across, radius } = polar(x, y);
      if (inJoint(x, y)) {
        px.push(shade(mortar, 0.94 + hash(x, y, seed + 7) * 0.1));
        continue;
      }
      let colour = shade(brick, 0.9 + hash(index, 0, seed) * 0.2);
      if (across < 0.22) colour = mix(colour, light, 0.35); // lit leading edge
      if (across > 0.8) colour = mix(colour, dark, 0.4); // shaded trailing edge
      if (radius > 61 / 2) colour = mix(colour, light, 0.15); // outer arris catches the light
      if (radius < 57 / 2) colour = mix(colour, dark, 0.2); // inner arris in shadow
      const grain = hash(x, y, seed);
      if (grain < 0.08) colour = mix(colour, dark, 0.5);
      else if (grain > 0.95) colour = mix(colour, light, 0.4);
      px.push(shade(colour, 0.97 + hash(y, x, seed + 3) * 0.06));
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
