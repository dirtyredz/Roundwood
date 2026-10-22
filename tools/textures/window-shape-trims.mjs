// 0.10.0 window attachment artwork for the new pieces: the top corners' head-and-jambs face, the
// single-edge pieces' straight runs and the arch sill's course. Thin mortar, two texels per model
// pixel, like the other window trims. New files only; the lower arches need no new art (they use the
// door ring's full radial face, trim_radial_thin_*, whose upper half is the arched window face's).
import { ARCH_FACE_SIZE, archWindowFace, brickColour, jointCoverage, withMortar } from './window-trims.mjs';

/** A single-edge piece's face: its own 16 model pixels at two texels each. */
export const SINGLE_FACE_SIZE = 32;
/** The sill's face spans the 2 x 2 window like the arched window's. */
export const SILL_FACE_SIZE = ARCH_FACE_SIZE;
/** A border band, in texels (2 model pixels). */
const BAND = 4;
/** One brick course of the sill, in texels (the sill's 2 model pixels). */
const COURSE = 4;
const BRICK = 8;

/**
 * The top corners' 64 x 64 face over the 2 x 2 window: the arched window face with its upper half
 * replaced by its lower half reflected top to bottom, so the head is a course of horizontal bricks
 * along the top edge and the jambs run straight down to the spring line. The lower half is the
 * arched face's own (jambs and sill), so bottom corners beside a top-corner window read the same.
 */
export function frameWindowFace(palette, seed) {
  const arch = archWindowFace(palette, seed);
  const px = [];
  for (let y = 0; y < ARCH_FACE_SIZE; y++) {
    const from = y < ARCH_FACE_SIZE / 2 ? ARCH_FACE_SIZE - 1 - y : y;
    for (let x = 0; x < ARCH_FACE_SIZE; x++) {
      px.push(arch[from * ARCH_FACE_SIZE + x]);
    }
  }
  return px;
}

function nearest(lines, at) {
  return Math.min(...lines.map((line) => Math.abs(at - line)));
}

/**
 * The single-edge pieces' 32 x 32 face: a straight run along every side, of which each piece shows
 * exactly one. The top and bottom runs are horizontal bricks across the full width; the side runs
 * stand between them. A 4-texel corner is its own brick in the horizontal run, so whichever run a
 * piece shows ends on a joint. The centre is never drawn.
 */
export function singleEdgeFace(palette, seed) {
  const joints = [BAND, 12, 20, SINGLE_FACE_SIZE - BAND];
  const px = [];
  for (let y = 0; y < SINGLE_FACE_SIZE; y++) {
    for (let x = 0; x < SINGLE_FACE_SIZE; x++) {
      const horizontal = y < BAND || y >= SINGLE_FACE_SIZE - BAND;
      const vertical = x < BAND || x >= SINGLE_FACE_SIZE - BAND;
      const along = horizontal ? x + 0.5 : y + 0.5;
      const run = horizontal ? (y < BAND ? 100 : 200) : vertical ? (x < BAND ? 300 : 400) : 0;
      const start = Math.max(0, ...joints.filter((j) => j <= along));
      const end = Math.min(SINGLE_FACE_SIZE, ...joints.filter((j) => j > along));
      const coverage = horizontal || vertical ? jointCoverage(nearest(joints, along)) : 1;
      const colour = brickColour(palette, run + start, (along - start) / (end - start), 30, 99, 0, x, y, seed);
      px.push(withMortar(colour, palette, coverage, x, y, seed));
    }
  }
  return px;
}

/**
 * The arch sill's 64 x 64 face over the 2 x 2 window: courses of 8-texel bricks, each course 4 texels
 * (the sill's height) and staggered by half a brick, with head joints only, so the sill, which always
 * covers exactly one course (texel rows 28..32 under the upper arches), shows one row of bricks and
 * no bed joint along its edges.
 */
export function sillFace(palette, seed) {
  const px = [];
  for (let y = 0; y < SILL_FACE_SIZE; y++) {
    const course = Math.floor(y / COURSE);
    const shift = (course % 2) * (BRICK / 2);
    for (let x = 0; x < SILL_FACE_SIZE; x++) {
      const along = x + 0.5 + shift;
      const index = Math.floor(along / BRICK);
      const across = (along % BRICK) / BRICK;
      const joint = Math.min(along % BRICK, BRICK - (along % BRICK));
      const colour = brickColour(palette, 500 + course * 16 + index, across, 30, 99, 0, x, y, seed);
      px.push(withMortar(colour, palette, jointCoverage(joint), x, y, seed));
    }
  }
  return px;
}
