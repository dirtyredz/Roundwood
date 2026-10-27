package io.github.dirtyredz.roundwood.block.door.frame;

import io.github.dirtyredz.roundwood.appearance.AppearanceNbt;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.arch.adapter.*;
import io.github.dirtyredz.roundwood.block.arch.assembly.*;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness;
import io.github.dirtyredz.roundwood.block.arch.trim.ArchPieceTrimNbt;
import io.github.dirtyredz.roundwood.block.attachment.FaceTrims;
import io.github.dirtyredz.roundwood.block.assembly.placement.*;
import io.github.dirtyredz.roundwood.block.door.adapter.*;
import io.github.dirtyredz.roundwood.block.door.assembly.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import java.util.*;

/** Both ordinary-click orders share this read-only proposal and full-NBT transaction. */
public final class DoorArchFitting {
    public record Proposal(DoorArchFitPlan fit, AssemblyDoorBlock block, DoorArchAttachment attachment,
                           Map<BlockPos, BlockState> states, CompoundTag anchorData, boolean intoArch) {}
    private DoorArchFitting() {}
    public static boolean claims(UseOnContext context) {
        if (!context.getLevel().isLoaded(context.getClickedPos())) return false;
        var state = context.getLevel().getBlockState(context.getClickedPos());
        return context.getItemInHand().getItem() instanceof RoundDoorItem && state.getBlock() instanceof ArchPieceBlock
                || context.getItemInHand().getItem() instanceof ArchPieceItem && state.getBlock() instanceof AssemblyDoorBlock;
    }
    public static Optional<Proposal> propose(UseOnContext context) {
        Level level = context.getLevel(); var player = context.getPlayer();
        if (!level.isLoaded(context.getClickedPos()) || player != null && (!player.mayBuild() || player.isSpectator())
                || !readable(context.getItemInHand())) return Optional.empty();
        var source = level.getBlockState(context.getClickedPos());
        DoorArchFitPlan fit; AssemblyDoorBlock block; DoorArchAttachment attachment; CompoundTag data;
        boolean intoArch; boolean open; boolean powered;
        if (context.getItemInHand().getItem() instanceof RoundDoorItem item && source.getBlock() instanceof ArchPieceBlock archBlock) {
            if (archBlock.design().shoulder() || !source.getValue(ArchPieceBlock.CELL).in(archBlock.design())) return Optional.empty();
            var arch = ArchPieceLayout.of(context.getClickedPos(), source);
            if (!arch.loaded(level) || !arch.complete(level, archBlock) || context.getHorizontalDirection() != arch.facing()
                    || !(level.getBlockEntity(arch.anchor()) instanceof ArchPieceBlockEntity entity) || !entity.trimsEditable()) return Optional.empty();
            block = item.door();
            var anchor = arch.anchor().below(DoorArchFitPlan.topOffset(block.design()));
            for (var edge : List.of(anchor.relative(arch.facing().getCounterClockWise()), anchor.relative(arch.facing().getClockWise(), block.design().columns())))
                if (!level.isLoaded(edge) || !level.isLoaded(edge.above())) return Optional.empty();
            var hinge = DoorHingePlacement.choose(block, new BlockPlaceContext(context), anchor, arch.facing(), block.design().columns());
            var candidate = DoorArchFitPlan.fromArch(block.design(), arch, hinge);
            if (candidate.isEmpty() || !knownArchEntity(entity)) return Optional.empty();
            fit = candidate.get();
            attachment = new DoorArchAttachment(arch.design(), arch.thickness(), arch.depth(), entity.appearance(), entity.trims());
            data = new CompoundTag(); AppearanceNbt.write(data, AppearanceStacks.read(context.getItemInHand()));
            powered = fit.door().positions().stream().filter(level::isLoaded).anyMatch(level::hasNeighborSignal); open = powered; intoArch = true;
        } else if (context.getItemInHand().getItem() instanceof ArchPieceItem item && source.getBlock() instanceof AssemblyDoorBlock doorBlock) {
            var door = DoorLayout.of(context.getClickedPos(), source); block = doorBlock;
            if (item.piece().design() != DoorArchFitPlan.topFor(block.design()) || !door.loaded(level) || !door.complete(level, block)
                    || !(level.getBlockEntity(door.anchor()) instanceof RoundDoorBlockEntity entity)
                    || !entity.archEditable() || entity.arch().isPresent() || !entity.trimsEditable()) return Optional.empty();
            var local = context.getClickLocation().subtract(net.minecraft.world.phys.Vec3.atLowerCornerOf(context.getClickedPos()));
            var depth = item.piece().thickness() == ArchPieceThickness.FULL ? ArchPieceDepth.FRONT
                    : ArchPieceDepth.target(door.facing(), context.getClickedFace(), local.x, local.y, local.z);
            var candidate = DoorArchFitPlan.fromDoor(door, item.piece().thickness(), depth, entity.camouflage().isPresent());
            if (candidate.isEmpty()) return Optional.empty();
            fit = candidate.get(); attachment = new DoorArchAttachment(item.piece().design(), item.piece().thickness(), depth,
                    AppearanceStacks.read(context.getItemInHand()), FaceTrims.NONE);
            data = entity.saveWithFullMetadata(); open = source.getValue(DoorBlock.OPEN); powered = source.getValue(DoorBlock.POWERED); intoArch = false;
        } else return Optional.empty();
        var cells = fit.door().positions();
        for (var pos : cells) if (!AssemblyPlacementPlan.inWorld(level, pos)) return Optional.empty();
        var states = new LinkedHashMap<BlockPos, BlockState>();
        var collision = player == null ? CollisionContext.empty() : CollisionContext.of(player);
        for (var part : fit.door().parts()) {
            var pos = fit.door().pos(part); var previous = level.getBlockState(pos);
            if (player != null && (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, context.getClickedFace(), context.getItemInHand()))) return Optional.empty();
            if (intoArch && !fit.arch().positions().contains(pos) && !previous.canBeReplaced()) return Optional.empty();
            var next = fit.door().stateFor(part, block, open, powered);
            if (!level.isUnobstructed(next, pos, collision) || part.row() == 0 && !next.canSurvive(level, pos)) return Optional.empty();
            states.put(pos, next);
        }
        data.put(DoorArchStore.TAG, DoorArchStore.encode(attachment));
        return Optional.of(new Proposal(fit, block, attachment, Collections.unmodifiableMap(states), data, intoArch));
    }
    private static boolean readable(ItemStack stack) {
        if (stack.isEmpty()) return false;
        var all = stack.serializeNBT();
        if (all.contains("ForgeCaps") && !all.getCompound("ForgeCaps").isEmpty()) return false;
        var tag = stack.getTag(); if (tag == null) return true;
        if (!Set.of(AppearanceNbt.TAG).containsAll(tag.getAllKeys())) return false;
        return !tag.contains(AppearanceNbt.TAG) || AppearanceNbt.decode(tag.getCompound(AppearanceNbt.TAG))
                .map(a -> a.equals(AppearanceStacks.read(stack)) && AppearanceNbt.encode(a).equals(tag.get(AppearanceNbt.TAG))).orElse(false);
    }
    private static boolean knownArchEntity(ArchPieceBlockEntity entity) {
        return Set.of(AppearanceNbt.TAG, ArchPieceTrimNbt.TAG).containsAll(entity.saveWithoutMetadata().getAllKeys());
    }
    /** Showcase/commands use the same proposal and write transaction, silently and without an item charge. */
    public static boolean fitAt(Level level, BlockPos pos, ArchPieceThickness thickness, ArchPieceDepth depth,
                                io.github.dirtyredz.roundwood.appearance.Appearance appearance) {
        if (level.isClientSide || !level.isLoaded(pos) || !(level.getBlockState(pos).getBlock() instanceof AssemblyDoorBlock block)
                || !DoorArchClearance.accepts(block.design(), thickness, depth)) return false;
        var door = DoorLayout.of(pos, level.getBlockState(pos));
        var item = io.github.dirtyredz.roundwood.registry.ModBlocks.archPiece(DoorArchFitPlan.topFor(block.design()), thickness).asItem();
        var stack = AppearanceStacks.create(item, appearance);
        var point = net.minecraft.world.phys.Vec3.atLowerCornerOf(door.anchor()).add(.5, depth == ArchPieceDepth.MIDDLE ? .5 : .875, .5)
                .relative(door.facing().getOpposite(), .5);
        var hit = new net.minecraft.world.phys.BlockHitResult(point, door.facing().getOpposite(), door.anchor(), false);
        var context = new UseOnContext(level, null, net.minecraft.world.InteractionHand.MAIN_HAND, stack, hit) {};
        var planned = propose(context);
        if (planned.isEmpty() || planned.get().attachment().depth() != depth) return false;
        use(context, false);
        return level.getBlockEntity(door.anchor()) instanceof RoundDoorBlockEntity entity && entity.arch().filter(planned.get().attachment()::equals).isPresent();
    }
    public static InteractionResult use(UseOnContext context) { return use(context, true); }
    private static InteractionResult use(UseOnContext context, boolean announce) {
        if (!claims(context)) return InteractionResult.PASS;
        var proposal = propose(context);
        if (proposal.isEmpty()) {
            if (!context.getLevel().isClientSide && context.getPlayer() != null)
                context.getPlayer().displayClientMessage(Component.translatable("message.roundwood.arch_fit_refused"), true);
            return InteractionResult.CONSUME;
        }
        if (context.getLevel().isClientSide) return InteractionResult.SUCCESS;
        var level = context.getLevel(); var plan = proposal.get();
        if (!propose(context).filter(plan::equals).isPresent() || level.restoringBlockSnapshots) return InteractionResult.CONSUME;
        // Own only these snapshots. Outer item-use capture receives no duplicate placement event.
        int mark = level.capturedBlockSnapshots.size(); boolean capturing = level.captureBlockSnapshots;
        if (mark != 0) return InteractionResult.CONSUME;
        var before = plan.states().keySet().stream().map(p -> BlockSnapshot.create(level.dimension(), level, p, Block.UPDATE_ALL)).toList();
        boolean success;
        level.captureBlockSnapshots = true;
        try {
            success = AssemblyWriteTransaction.write(level, plan.states(), () -> {
                if (!(level.getBlockEntity(plan.fit().door().anchor()) instanceof RoundDoorBlockEntity entity)) return false;
                entity.load(plan.anchorData().copy());
                return entity.arch().filter(plan.attachment()::equals).isPresent();
            }, () -> context.getPlayer() == null || !ForgeEventFactory.onMultiBlockPlace(context.getPlayer(), before, context.getClickedFace()));
        } finally {
            level.captureBlockSnapshots = capturing;
            if (level.capturedBlockSnapshots.size() > mark) level.capturedBlockSnapshots.subList(mark, level.capturedBlockSnapshots.size()).clear();
        }
        if (!success) return InteractionResult.CONSUME;
        var anchor = plan.fit().door().anchor(); level.getBlockEntity(anchor).setChanged();
        for (var snapshot : before) level.markAndNotifyBlock(snapshot.getPos(), level.getChunkAt(snapshot.getPos()),
                snapshot.getReplacedBlock(), level.getBlockState(snapshot.getPos()), Block.UPDATE_ALL, Block.UPDATE_LIMIT);
        if (!announce) return InteractionResult.CONSUME;
        var state = level.getBlockState(anchor); var player = context.getPlayer(); var sound = state.getSoundType(level, anchor, player);
        level.playSound(player, anchor, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume()+1)/2, sound.getPitch()*.8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, anchor, GameEvent.Context.of(player, state));
        if (player == null || !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
