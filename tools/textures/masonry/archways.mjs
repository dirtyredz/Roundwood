// 0.12.0 archway artwork: the 64x64 masonry faces of the ordinary and keyhole archways (and of their
// casings, which cover the same frame), thin mortar at two texels per model pixel like the other trims.
// The face spans the whole 32 x 32 pixel assembly; texel row 0 is the top (model y 32), row 32 the
// spring line (model y 16). Contour-appropriate joints, never a stretched ring:
//   - the crown (rows 0..31): voussoirs radiating from the crown centre, an odd number over the half
//     circle so a keystone sits on top and a joint lands on each spring line;
//   - below the spring line, straight bed joints every COURSE rows across each jamb;
//   - the keyhole's jambs widen through its shoulders (rows 32..47) and stand 12 texels wide below:
//     each shoulder course is one corbel stone, and the wide courses alternate a split pair with one
//     long stone (a running bond), so the transition reads as deliberate stonework.
// The face is mirror-symmetric about the centre line. New files only.
import { brickColour, jointCoverage, withMortar } from '../window-trims.mjs';

/** The archway face: the 32 x 32 model pixel assembly at two texels each. */
export const ARCHWAY_FACE_SIZE = 64;
/** Texel row of the spring line, where the crown meets the jambs. */
const SPRING = 32;
/** The crown's centre, in texels. */
const CENTRE = 32;
/** Voussoirs over the half circle (odd: a keystone on top, joints on the spring lines). */
const VOUSSOIRS = 13;
/** Rows per straight course below the spring line. */
const COURSE = 8;
/** The keyhole's wide lower jamb, in texels from the outer edge (6 model pixels). */
const WIDE_JAMB = 12;
/** Texel row where the keyhole's shoulders end and its wide jambs begin (model y 8). */
const SHOULDER_FOOT = 48;
/** The crown ring's arrises, in texels from the centre: outer 32 (radius 16), inner 28 (radius 14). */
const OUTER_ARRIS = 31;
const INNER_ARRIS = 29;

/** Polar position of a texel centre about the crown centre: the voussoir, how far across it, the radius. */
function voussoir(x, y) {
  const dx = x + 0.5 - CENTRE;
  const dy = CENTRE - (y + 0.5);
  const width = Math.PI / VOUSSOIRS;
  const turns = (Math.atan2(dx, dy) + width / 2) / width;
  return { index: Math.floor(turns), across: turns - Math.floor(turns), radius: Math.hypot(dx, dy), width };
}

/** The crown's texel: radial voussoirs with thin joints. */
function crown(palette, x, y, seed) {
  const { index, across, radius, width } = voussoir(x, y);
  const colour = brickColour(palette, index, across, radius, OUTER_ARRIS, INNER_ARRIS, x, y, seed);
  return withMortar(colour, palette, jointCoverage(Math.min(across, 1 - across) * width * radius), x, y, seed);
}

/**
 * A jamb texel below the spring line. `left` is the texel's distance from its jamb's outer edge
 * (mirrored on the right), so both jambs carry the same stones.
 */
function jamb(palette, x, y, seed, keyhole) {
  const left = x < CENTRE ? x : ARCHWAY_FACE_SIZE - 1 - x;
  const side = x < CENTRE ? 0 : 1;
  const course = Math.floor((y - SPRING) / COURSE);
  const row = (y - SPRING) % COURSE;
  let stone = 0;
  let headJoint = 0;
  if (keyhole && y >= SHOULDER_FOOT && course % 2 === 0) {
    // the wide jamb's split course: two 6-texel stones, the head joint between them
    stone = left < WIDE_JAMB / 2 ? 0 : 1;
    headJoint = jointCoverage(Math.abs(left + 0.5 - WIDE_JAMB / 2));
  }
  const bedJoint = row === 0 ? 1 : 0;
  const index = 300 + course * 4 + side * 2 + stone;
  const colour = brickColour(palette, index, row / COURSE, 30, 99, 0, x, y, seed);
  return withMortar(colour, palette, Math.max(bedJoint, headJoint), x, y, seed);
}

/** An archway's 64 x 64 face: the ordinary archway's, or with `keyhole` its shouldered jambs. */
export function archwayFace(palette, seed, keyhole) {
  const px = [];
  for (let y = 0; y < ARCHWAY_FACE_SIZE; y++) {
    for (let x = 0; x < ARCHWAY_FACE_SIZE; x++) {
      px.push(y < SPRING ? crown(palette, x, y, seed) : jamb(palette, x, y, seed, keyhole));
    }
  }
  return px;
}
