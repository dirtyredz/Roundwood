package io.github.dirtyredz.roundwood.block.assembly.render;

/**
 * Family-owned immutable values an anchor adds to its {@link AssemblyRenderSnapshot} (a door's fitted
 * arch and camouflage). Records only: snapshot equality, and therefore chunk-quad caching, depends on
 * value equality. Families without extras publish {@link #NONE}.
 */
public interface AssemblyRenderExtras {
    AssemblyRenderExtras NONE = new AssemblyRenderExtras() {
        @Override public String toString() { return "AssemblyRenderExtras.NONE"; }
    };
}
