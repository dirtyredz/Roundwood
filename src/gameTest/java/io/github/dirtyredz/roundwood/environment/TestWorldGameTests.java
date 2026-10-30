package io.github.dirtyredz.roundwood.environment;

import io.github.dirtyredz.roundwood.Roundwood;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Guards the world every other GameTest runs in (gradle/gametest-world.gradle): superflat with no
 * structures and no natural spawning, so a plot holds only what its test builds. Normal terrain put the plots in caves, where
 * falling gravel, flowing water and cave mobs broke unrelated tests at random.
 */
@GameTestHolder(Roundwood.MOD_ID)
@PrefixGameTestTemplate(false)
@net.neoforged.fml.common.EventBusSubscriber(modid = Roundwood.MOD_ID)
public final class TestWorldGameTests {
    /** Natural spawning off before any batch runs; tests spawn every mob they use. */
    @net.neoforged.bus.api.SubscribeEvent
    public static void quiet(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        event.getServer().getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, event.getServer());
    }

    @GameTest(template = "empty", batch = "roundwood_test_world")
    public static void theTestWorldIsSuperflatWithoutStructuresOrNaturalSpawning(GameTestHelper helper) {
        var level = helper.getLevel();
        helper.assertTrue(level.getChunkSource().getGenerator() instanceof FlatLevelSource, "GameTest world is not superflat: "
                + level.getChunkSource().getGenerator().getClass().getSimpleName() + " (server.properties not written?)");
        var server = level.getServer();
        helper.assertTrue(!server.getWorldData().worldGenOptions().generateStructures(), "GameTest world generates structures");
        // The GameTest server keeps MinecraftServer's always-on spawn flags, so the game rule is what stops spawning.
        helper.assertTrue(!level.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING), "GameTest world spawns mobs naturally ("
                + server.getClass().getSimpleName() + ")");
        helper.succeed();
    }
}
