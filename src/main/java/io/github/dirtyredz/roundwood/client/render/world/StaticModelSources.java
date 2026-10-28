package io.github.dirtyredz.roundwood.client.render.world;

import net.minecraft.world.level.block.Block;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

/** Immutable registered-block to {@link StaticModelSource} bindings supplied by the client composition root. */
public final class StaticModelSources {
    private final Map<Block, StaticModelSource> sources;

    private StaticModelSources(Map<Block, StaticModelSource> sources) {
        this.sources = Collections.unmodifiableMap(new LinkedHashMap<>(sources));
    }

    public static Builder builder() { return new Builder(); }

    public Optional<StaticModelSource> find(Block block) {
        return Optional.ofNullable(sources.get(block));
    }

    public void forEach(BiConsumer<? super Block, ? super StaticModelSource> action) {
        sources.forEach(action);
    }

    public static final class Builder {
        private final Map<Block, StaticModelSource> sources = new LinkedHashMap<>();
        private Builder() {}
        public Builder bind(Block block, StaticModelSource source) {
            if (sources.putIfAbsent(block, source) != null) throw new IllegalStateException("Duplicate static source: " + block);
            return this;
        }
        public Builder bindAll(Iterable<? extends Block> blocks, StaticModelSource source) {
            blocks.forEach(block -> bind(block, source));
            return this;
        }
        public StaticModelSources build() { return new StaticModelSources(sources); }
    }
}
