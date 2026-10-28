package io.github.dirtyredz.roundwood.client.render.world;

import net.minecraft.world.level.block.Block;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Immutable block-class to {@link StaticModelSource} bindings; a block inherits its nearest superclass's source. */
public final class StaticModelSources {
    private final Map<Class<? extends Block>, StaticModelSource> sources;

    private StaticModelSources(Map<Class<? extends Block>, StaticModelSource> sources) {
        this.sources = Map.copyOf(sources);
    }

    public static Builder builder() { return new Builder(); }

    public Optional<StaticModelSource> find(Block block) {
        for (Class<?> type = block.getClass(); type != null && Block.class.isAssignableFrom(type); type = type.getSuperclass()) {
            var source = sources.get(type);
            if (source != null) return Optional.of(source);
        }
        return Optional.empty();
    }

    public static final class Builder {
        private final Map<Class<? extends Block>, StaticModelSource> sources = new LinkedHashMap<>();
        private Builder() {}
        public Builder bind(Class<? extends Block> family, StaticModelSource source) {
            if (sources.putIfAbsent(family, source) != null) throw new IllegalStateException("Duplicate static source: " + family);
            return this;
        }
        public StaticModelSources build() { return new StaticModelSources(sources); }
    }
}
