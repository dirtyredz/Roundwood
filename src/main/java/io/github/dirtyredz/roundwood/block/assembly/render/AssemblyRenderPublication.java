package io.github.dirtyredz.roundwood.block.assembly.render;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

/** Publish first, refresh the anchor second, then invalidate the old/new footprint union. */
public final class AssemblyRenderPublication<S, P> {
    private volatile S current;
    private Set<P> footprint = Set.of();

    public S current() { return current; }
    public Set<P> footprint() { return footprint; }

    public void publish(S next, Set<P> cells, Runnable refresh, Consumer<P> dirty) {
        Set<P> affected = new LinkedHashSet<>(footprint);
        affected.addAll(cells);
        footprint = Set.copyOf(cells);
        current = next;
        refresh.run();
        affected.forEach(dirty);
    }
}
