// The window glazing: 16x16 RGBA, faintly tinted translucent glass with two diagonal highlights.
// Unlike vanilla glass.png (opaque border, transparent middle) every texel is visible, so the round
// glazing and its arms read as glass wherever they sample.
import { hash } from './noise.mjs';

export const GLASS_SIZE = 16;

export function windowGlass(seed) {
  const px = [];
  for (let y = 0; y < GLASS_SIZE; y++) {
    for (let x = 0; x < GLASS_SIZE; x++) {
      const d = x + y;
      const streak = (d >= 6 && d <= 7 && x >= 2 && x <= 6) || (d >= 19 && d <= 21 && x >= 8 && x <= 13);
      if (streak) {
        px.push([246, 252, 255, 176]);
        continue;
      }
      const tone = Math.round(hash(x, y, seed) * 6);
      px.push([200 + tone, 224 + tone, 234 + tone, 92 + Math.round(hash(y, x, seed) * 12)]);
    }
  }
  return px;
}
