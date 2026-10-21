// Door panels: one 32x32 image mapped once across the whole round leaf (one texel per model pixel).
// Eight vertical boards, four texels wide, each running the full height: a dark seam on every
// board's left column, a lit edge beside it, grain that drifts smoothly along the board, and the
// odd knot. Nothing runs horizontally, so the boards never restart mid-door.
import { hash, mix, shade, smooth1 } from './noise.mjs';

export const DOOR_PANEL_SIZE = 32;
const BOARD = 4;

export function doorPanel(palette, seed) {
  const { dark, mid, light } = palette;
  const px = [];
  const knots = [];
  for (let board = 0; board < DOOR_PANEL_SIZE / BOARD; board++) {
    if (hash(board, 1, seed) < 0.45) {
      knots.push({ x: board * BOARD + 2, y: 3 + Math.floor(hash(board, 2, seed) * 26) });
    }
  }
  for (let y = 0; y < DOOR_PANEL_SIZE; y++) {
    for (let x = 0; x < DOOR_PANEL_SIZE; x++) {
      const board = Math.floor(x / BOARD);
      const col = x % BOARD;
      const boardTone = 0.92 + hash(board, 0, seed) * 0.12;
      // Grain: each texel column has its own slow drift along the board, plus fine noise.
      const grain = (smooth1(y / 5 + col * 3.7, seed * 31 + x) - 0.5) * 0.12 + (hash(x, y, seed) - 0.5) * 0.05;
      let colour = shade(mid, boardTone + grain);
      if (col === 1) colour = mix(colour, light, 0.45); // lit edge beside the seam
      for (const knot of knots) {
        const d = Math.hypot(x + 0.5 - knot.x, (y + 0.5 - knot.y) * 0.7);
        if (d < 1.3 && col !== 0) colour = mix(colour, dark, 0.3);
      }
      if (col === 0) colour = shade(dark, 0.88 + hash(x, y, seed + 1) * 0.06); // seam: darkest, continuous top to bottom
      px.push(colour);
    }
  }
  return px;
}
