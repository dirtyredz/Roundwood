// Deterministic randomness and colour helpers shared by the texture generators.

/** Small deterministic PRNG (mulberry32): the same seed always gives the same sequence. */
export function rng(seed) {
  let a = seed >>> 0;
  return () => {
    a = (a + 0x6d2b79f5) >>> 0;
    let t = a;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

/** A stable value in [0, 1) for integer coordinates and a seed (no state, order-independent). */
export function hash(x, y, seed) {
  let h = Math.imul(x | 0, 374761393) + Math.imul(y | 0, 668265263) + Math.imul(seed | 0, 2246822519);
  h = Math.imul(h ^ (h >>> 13), 1274126177);
  h ^= h >>> 16;
  return (h >>> 0) / 4294967296;
}

/** Smooth 1D value noise along `t` (period-free), in [0, 1). */
export function smooth1(t, seed) {
  const i = Math.floor(t);
  const f = t - i;
  const s = f * f * (3 - 2 * f);
  return hash(i, 0, seed) * (1 - s) + hash(i + 1, 0, seed) * s;
}

export const clamp = (v) => Math.max(0, Math.min(255, Math.round(v)));
export const shade = ([r, g, b], f) => [clamp(r * f), clamp(g * f), clamp(b * f)];
export const mix = (a, b, t) => [clamp(a[0] + (b[0] - a[0]) * t), clamp(a[1] + (b[1] - a[1]) * t), clamp(a[2] + (b[2] - a[2]) * t)];
