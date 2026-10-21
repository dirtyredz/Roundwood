// 0.8.0 dyed window glass: 16x16 RGBA, the dye colour with a faint tone variation. It carries the same
// diagonal marks as the clear glass (same mask, same alpha) drawn darker, so they read against the tint;
// the textures are independent of the frame paint colours.
import { hash } from './noise.mjs';
import { GLASS_SIZE } from './window-glass.mjs';

const clamp = (v) => Math.max(0, Math.min(255, v));

export function stainedWindowGlass(rgb, seed) {
  const px = [];
  for (let y = 0; y < GLASS_SIZE; y++) {
    for (let x = 0; x < GLASS_SIZE; x++) {
      const d = x + y;
      const streak = (d >= 6 && d <= 7 && x >= 2 && x <= 6) || (d >= 19 && d <= 21 && x >= 8 && x <= 13);
      if (streak) {
        px.push([...rgb.map((c) => Math.round(c * 0.55)), 176]);
        continue;
      }
      const tone = Math.round(hash(x, y, seed) * 6);
      px.push([...rgb.map((c) => clamp(c + tone)), 108 + Math.round(hash(y, x, seed) * 12)]);
    }
  }
  return px;
}
