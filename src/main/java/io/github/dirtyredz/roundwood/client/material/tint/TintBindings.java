package io.github.dirtyredz.roundwood.client.material.tint;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/** Session-stable indices: old compiled meshes must never resolve an index to a different donor. */
public final class TintBindings<D> {
    public record Binding<D>(D donor, int index) {}
    private final ConcurrentHashMap<Binding<D>, Integer> indices = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Integer, Binding<D>> bindings = new ConcurrentHashMap<>();
    private final AtomicInteger next = new AtomicInteger();
    public int bind(D donor, int index) {
        if (index < 0) return -1;
        return indices.computeIfAbsent(new Binding<>(donor, index), binding -> {
            int id = next.getAndIncrement();
            bindings.put(id, binding);
            return id;
        });
    }
    public Binding<D> get(int index) { return bindings.get(index); }
}
