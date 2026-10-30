package io.github.dirtyredz.roundwood.fixture;

import net.minecraft.core.HolderLookup;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** The running GameTest server's registries, for block-entity and stack (de)serialization in fixtures and checks. */
public final class TestRegistries {
    private TestRegistries() {
    }

    public static HolderLookup.Provider get() {
        return ServerLifecycleHooks.getCurrentServer().registryAccess();
    }
}
