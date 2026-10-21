package io.github.dirtyredz.roundwood.testsupport;

import net.minecraft.server.Bootstrap;

/**
 * Lets plain JUnit use the few Minecraft classes whose static initialisers insist the game was
 * bootstrapped ({@code DyeColor} reaches the registries through its tag keys). The first thing
 * {@link Bootstrap#bootStrap()} does is mark the game bootstrapped; its later steps need Forge's
 * transformed event classes, which only exist inside a real launch, so they may throw here. That is
 * fine: the flag is all these tests need, and they never read a registry.
 */
public final class MinecraftBootstrap {
    private static boolean done;

    private MinecraftBootstrap() {
    }

    /** Idempotent; call from a static initialiser or {@code @BeforeAll} before touching {@code DyeColor}. */
    public static synchronized void ensure() {
        if (done) {
            return;
        }
        done = true;
        try {
            Bootstrap.bootStrap();
        } catch (Throwable expectedOutsideARealLaunch) {
            // Registry population can fail without Forge's launch transformers; see the class comment.
        }
    }
}
