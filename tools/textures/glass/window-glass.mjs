// The window glazing: 16x16 RGBA, faintly tinted translucent glass with two diagonal highlights.
// Unlike vanilla glass.png (opaque border, transparent middle) every texel is visible, so the round
// glazing and its arms read as glass wherever they sample.
import { hash } from '../noise.mjs';

export const GLASS_SIZE = 16;

/** The same diagonal accent mask for clear, historical and dyed glazing. */
export function glassStreak(x, y) {
  const d = x + y;
  return (d >= 6 && d <= 7 && x >= 2 && x <= 6) || (d >= 19 && d <= 21 && x >= 8 && x <= 13);
}

export function windowGlass(seed) {
  const px = [];
  for (let y = 0; y < GLASS_SIZE; y++) {
    for (let x = 0; x < GLASS_SIZE; x++) {
      if (glassStreak(x, y)) {
        px.push([246, 252, 255, 176]);
        continue;
      }
      const tone = Math.round(hash(x, y, seed) * 6);
      px.push([200 + tone, 224 + tone, 234 + tone, 92 + Math.round(hash(y, x, seed) * 12)]);
    }
  }
  return px;
}

/** Glass alpha drop outside the diagonal accents: 92..104 before, 24..36 now (accents keep 176). */
export const CLEAR_ALPHA_DROP = 68;

/** The 0.7.0 window glazing: window_glass with only the non-accent texels made clearer. */
export function clearWindowGlass(seed) {
  return windowGlass(seed).map(([r, g, b, a]) => (a === 176 ? [r, g, b, a] : [r, g, b, a - CLEAR_ALPHA_DROP]));
}
