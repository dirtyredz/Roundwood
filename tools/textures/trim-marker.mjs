import { hash, mix } from './noise.mjs';

/**
 * The window trim's source markers (16x16, opaque): stand-ins the client always replaces with the
 * trim's look, so the trim's quads can be told from the pane's oak wood after baking. Drawn as plain
 * light wood in case a resource pack ever shows them.
 */
export function trimMarker(palette, seed, edge) {
  const px = [];
  for (let y = 0; y < 16; y++) {
    for (let x = 0; x < 16; x++) {
      const grain = hash(edge ? y : x, 0, seed) * 0.6 + hash(x, y, seed + 1) * 0.4;
      px.push(mix(palette.mid, grain > 0.6 ? palette.light : palette.dark, Math.abs(grain - 0.5) * 0.6));
    }
  }
  return px;
}
