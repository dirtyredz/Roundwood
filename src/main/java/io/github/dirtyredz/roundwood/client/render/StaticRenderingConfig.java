package io.github.dirtyredz.roundwood.client.render;

import net.minecraftforge.common.ForgeConfigSpec;

/** Requires a world reload when changed: compiled chunks and BER ownership switch together. */
public final class StaticRenderingConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.BooleanValue LEGACY;
    static {
        var builder = new ForgeConfigSpec.Builder();
        LEGACY = builder.comment("Use the legacy static block entity renderers. Exit and reopen the world after changing.")
                .define("legacyStaticRendering", false);
        SPEC = builder.build();
    }
    private StaticRenderingConfig() {}
    public static boolean legacy() { return LEGACY.get(); }
}
