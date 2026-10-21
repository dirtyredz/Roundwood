// The five original 16x16 textures (unchanged since 0.1.0, byte-identical output): four painted
// plank panels for the painted windows (four horizontal boards with staggered butt joints) and
// brass for knobs and straps.
import { rng, shade } from './noise.mjs';

const SIZE = 16;

/** Painted window panel base colours, also the painted door/trim palettes' mid tone. */
export const PAINT = {
  green: [74, 112, 58],
  yellow: [212, 166, 52],
  red: [150, 46, 40],
  blue: [50, 86, 138],
};

function paintedPlanks(base, seed) {
  const rand = rng(seed);
  const px = [];
  const joints = [5, 11, 2, 9]; // butt joint column per board
  for (let y = 0; y < SIZE; y++) {
    const board = Math.floor(y / 4);
    const row = y % 4;
    const boardTone = 0.94 + rand() * 0.1;
    for (let x = 0; x < SIZE; x++) {
      let f = boardTone;
      // Grain: soft streaks that run along the board.
      f += Math.sin((x + board * 3) * 0.9 + row * 1.7) * 0.025 + (rand() - 0.5) * 0.05;
      if (row === 0) f *= 1.06; // lit top edge of each board
      if (row === 3) f *= 0.72; // shadowed seam under each board
      if (x === joints[board]) f *= 0.78; // end joint
      px.push(shade(base, f));
    }
  }
  return px;
}

function brass() {
  const rand = rng(77);
  const base = [196, 154, 62];
  const px = [];
  for (let y = 0; y < SIZE; y++) {
    for (let x = 0; x < SIZE; x++) {
      // Diagonal sheen with a little tarnish noise.
      const sheen = 0.85 + 0.3 * Math.cos(((x + y) / SIZE) * Math.PI) ** 2;
      const f = sheen + (rand() - 0.5) * 0.08;
      px.push(shade(base, (x + y) % 11 === 0 ? f * 1.12 : f));
    }
  }
  return px;
}

/** name -> { pixels, size } */
export function legacyTextures() {
  return {
    panel_green: { pixels: paintedPlanks(PAINT.green, 11), size: SIZE },
    panel_yellow: { pixels: paintedPlanks(PAINT.yellow, 23), size: SIZE },
    panel_red: { pixels: paintedPlanks(PAINT.red, 37), size: SIZE },
    panel_blue: { pixels: paintedPlanks(PAINT.blue, 41), size: SIZE },
    brass: { pixels: brass(), size: SIZE },
  };
}
