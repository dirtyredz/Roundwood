package io.github.dirtyredz.roundwood.migration.retired;

import io.github.dirtyredz.roundwood.migration.ChunkMigrationQueue;
import io.github.dirtyredz.roundwood.migration.item.ArchwayStacks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.minecraft.world.level.chunk.LevelChunk;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/** Loaded block containers use the bounded queue; player and entity inventories use their load/tick hooks. */
public final class ArchwayStackMigration {
    private static final ChunkMigrationQueue QUEUE = new ChunkMigrationQueue();
    private static final Map<ServerLevel, Queue<Entity>> ENTITIES = new ConcurrentHashMap<>();
    private ArchwayStackMigration() {}

    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide() || !(event.getChunk() instanceof LevelChunk chunk)) return;
        for (var pos : chunk.getBlockEntitiesPos()) QUEUE.add(event.getLevel(), pos, pos);
    }
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        QUEUE.drain(level, work -> {
            if (level.isLoaded(work.pos()) && level.getBlockEntity(work.pos()) instanceof Container container)
                convert(level, container, Vec3.atCenterOf(work.pos()));
        });
        var entities = ENTITIES.get(level);
        if (entities != null) for (int i = 0; i < ChunkMigrationQueue.PER_TICK; i++) {
            var entity = entities.poll();
            if (entity == null) break;
            // Join events can still be cancelled. Never split a stack before the holder actually exists.
            if (!entity.isAlive() || !entity.isAddedToLevel()) continue;
            if (entity instanceof Container container) convert(level, container, entity.position());
            if (entity instanceof ItemEntity item) {
                var outputs = ArchwayStacks.replacements(item.getItem());
                if (!outputs.isEmpty() && drop(level, item.position(), outputs.subList(1, outputs.size()))) item.setItem(outputs.get(0));
            }
        }
    }
    public static void onLevelUnload(LevelEvent.Unload event) {
        QUEUE.forget(event.getLevel());
        ENTITIES.remove(event.getLevel());
    }
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level() instanceof ServerLevel level
                && event.getEntity().tickCount % 20 == 0) {
            convert(level, event.getEntity().getInventory(), event.getEntity().position());
            convert(level, event.getEntity().getEnderChestInventory(), event.getEntity().position());
        }
    }
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (event.getEntity() instanceof Container || event.getEntity() instanceof ItemEntity)
            ENTITIES.computeIfAbsent(level, ignored -> new ConcurrentLinkedQueue<>()).add(event.getEntity());
    }
    public static boolean convert(ServerLevel level, Container container, Vec3 pos) {
        // Reading getItem would roll unopened loot without the opening player's luck/context.
        if (container instanceof net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity random
                && random.getLootTable() != null) return false;
        if (container instanceof net.minecraft.world.entity.vehicle.ContainerEntity vehicle && vehicle.getLootTable() != null) return false;
        return ArchwayStacks.convert(container, stacks -> drop(level, pos, stacks));
    }
    private static boolean drop(ServerLevel level, Vec3 pos, List<ItemStack> stacks) {
        List<ItemEntity> spawned = new ArrayList<>();
        for (var stack : stacks) {
            var item = new ItemEntity(level, pos.x, pos.y, pos.z, stack.copy());
            item.setDefaultPickUpDelay();
            if (!level.addFreshEntity(item)) {
                spawned.forEach(ItemEntity::discard);
                return false;
            }
            spawned.add(item);
        }
        return true;
    }
}
