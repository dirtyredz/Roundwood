// 0.9.0 Universal Trim icon artwork: the masonry face of the flat square frame, in the thin mortar of
// the other trims (two texels per model pixel, the same joint width). New files only.
//
// The frame is 16 model pixels across with a 2-pixel band, so the face is 32 x 32 texels and the band
// 4 texels wide; the open centre is never drawn and is filled with mortar. Straight courses follow
// the band: the top and bottom runs are one course of 8-texel bricks across the full width (staggered
// against each other), the sides one soldier course between them, so no radial joint ever shows.
import { brickColour, jointCoverage, withMortar } from './window-trims.mjs';

/** The square frame's face: 16 model pixels at two texels each. */
export const SQUARE_FACE_SIZE = 32;
/** The band, in texels (2 model pixels). */
export const SQUARE_BAND = 4;

const TOP_JOINTS = [4, 12, 20, 28];
const BOTTOM_JOINTS = [8, 16, 24];
const SIDE_JOINTS = [SQUARE_BAND, 16, SQUARE_FACE_SIZE - SQUARE_BAND];

/** Which run of the frame texel (x, y) is in: top, bottom, left, right, or the open centre. */
export function squareRun(x, y) {
  const far = SQUARE_FACE_SIZE - SQUARE_BAND;
  if (y < SQUARE_BAND) return 'top';
  if (y >= far) return 'bottom';
  if (x < SQUARE_BAND) return 'left';
  if (x >= far) return 'right';
  return 'centre';
}

function nearest(lines, at) {
  return Math.min(...lines.map((line) => Math.abs(at - line)));
}

/** Mortar coverage (0..1) of texel (x, y), and the brick it belongs to. */
export function squareJoint(x, y) {
  const run = squareRun(x, y);
  const cx = x + 0.5;
  const cy = y + 0.5;
  if (run === 'top' || run === 'bottom') {
    const joints = run === 'top' ? TOP_JOINTS : BOTTOM_JOINTS;
    const start = Math.max(0, ...joints.filter((j) => j <= cx));
    const end = Math.min(SQUARE_FACE_SIZE, ...joints.filter((j) => j > cx));
    return { run, coverage: jointCoverage(nearest(joints, cx)), index: start, across: (cx - start) / (end - start) };
  }
  if (run === 'left' || run === 'right') {
    const start = Math.max(...SIDE_JOINTS.filter((j) => j <= cy));
    const end = Math.min(...SIDE_JOINTS.filter((j) => j > cy));
    return { run, coverage: jointCoverage(nearest(SIDE_JOINTS, cy)), index: start, across: (cy - start) / (end - start) };
  }
  return { run, coverage: 1, index: 0, across: 0 };
}

/** The 32 x 32 square-frame masonry face for `palette`. */
export function squareFrameFace(palette, seed) {
  const runs = { top: 100, bottom: 200, left: 300, right: 400, centre: 0 };
  const px = [];
  for (let y = 0; y < SQUARE_FACE_SIZE; y++) {
    for (let x = 0; x < SQUARE_FACE_SIZE; x++) {
      const { run, coverage, index, across } = squareJoint(x, y);
      const colour = brickColour(palette, runs[run] + index, across, 30, 99, 0, x, y, seed);
      px.push(withMortar(colour, palette, coverage, x, y, seed));
    }
  }
  return px;
}
