package io.github.dirtyredz.roundwood.block.door.frame.fitting;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.item.AppearanceStacks;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceBlock;
import io.github.dirtyredz.roundwood.block.arch.adapter.ArchPieceItem;
import io.github.dirtyredz.roundwood.block.arch.assembly.ArchPieceDepth;
import io.github.dirtyredz.roundwood.block.arch.geometry.ArchPieceThickness;
import io.github.dirtyredz.roundwood.block.assembly.placement.AssemblyCapturedPlacement;
import io.github.dirtyredz.roundwood.block.door.adapter.AssemblyDoorBlock;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorBlockEntity;
import io.github.dirtyredz.roundwood.block.door.adapter.RoundDoorItem;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.frame.DoorArchFitPlan;
import io.github.dirtyredz.roundwood.block.door.hardware.DoorHardwareStacks;
import io.github.dirtyredz.roundwood.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.util.Optional;

/**
 * Click entry for fitting an arch top and a door together, in either order: resolves the clicked
 * source, hands it to the matching proposal ({@link DoorIntoArchProposal}, {@link ArchOntoDoorProposal}),
 * installs the result through the captured full-NBT placement, and announces it. {@link #fitAt} is the
 * silent showcase/command entry over the same proposal and transaction.
 */
public final class DoorArchFitting {
    private DoorArchFitting() {}
    /** Discover from the normal landing cell as well as an explicit component click. */
    private static Optional<BlockPos> sourcePos(UseOnContext context) {
        var level = context.getLevel();
        var clicked = context.getClickedPos();
        if (!level.isLoaded(clicked)) return Optional.empty();
        var source = level.getBlockState(clicked).getBlock();
        var item = context.getItemInHand().getItem();
        if (item instanceof RoundDoorItem && source instanceof ArchPieceBlock
                || item instanceof ArchPieceItem && source instanceof AssemblyDoorBlock) return Optional.of(clicked);
        var landing = new BlockPlaceContext(context).getClickedPos();
        if (item instanceof RoundDoorItem door) {
            var top = landing.above(DoorArchFitPlan.topOffset(door.door().design()));
            if (level.isLoaded(top) && level.getBlockState(top).getBlock() instanceof ArchPieceBlock) return Optional.of(top);
        } else if (item instanceof ArchPieceItem && level.isLoaded(landing)
                && level.getBlockState(landing).getBlock() instanceof AssemblyDoorBlock) {
            return Optional.of(landing);
        }
        return Optional.empty();
    }
    public static boolean claims(UseOnContext context) { return sourcePos(context).isPresent(); }
    public static Optional<DoorArchProposal> propose(UseOnContext context) {
        Level level = context.getLevel(); var player = context.getPlayer();
        if (!level.isLoaded(context.getClickedPos()) || player != null && (!player.mayBuild() || player.isSpectator())
                || !DoorHardwareStacks.readable(context.getItemInHand())) return Optional.empty();
        var sourcePos = sourcePos(context);
        if (sourcePos.isEmpty()) return Optional.empty();
        var source = level.getBlockState(sourcePos.get());
        if (context.getItemInHand().getItem() instanceof RoundDoorItem item && source.getBlock() instanceof ArchPieceBlock archBlock)
            return DoorIntoArchProposal.propose(context, item, archBlock, sourcePos.get(), source);
        if (context.getItemInHand().getItem() instanceof ArchPieceItem item && source.getBlock() instanceof AssemblyDoorBlock doorBlock)
            return ArchOntoDoorProposal.propose(context, item, doorBlock, sourcePos.get(), source);
        return Optional.empty();
    }
    /** Showcase/commands use the same proposal and write transaction, silently and without an item charge. */
    public static boolean fitAt(Level level, BlockPos pos, ArchPieceThickness thickness, ArchPieceDepth depth, Appearance appearance) {
        if (level.isClientSide || !level.isLoaded(pos) || !(level.getBlockState(pos).getBlock() instanceof AssemblyDoorBlock block)) return false;
        var door = DoorLayout.of(pos, level.getBlockState(pos));
        var item = ModBlocks.archPiece(DoorArchFitPlan.topFor(block.design()), thickness).asItem();
        var stack = AppearanceStacks.create(item, appearance);
        var point = Vec3.atLowerCornerOf(door.anchor()).add(.5, depth == ArchPieceDepth.MIDDLE ? .5 : depth == ArchPieceDepth.BACK ? .125 : .875, .5)
                .relative(door.facing().getOpposite(), .5);
        var hit = new BlockHitResult(point, door.facing().getOpposite(), door.anchor(), false);
        var context = new UseOnContext(level, null, InteractionHand.MAIN_HAND, stack, hit) {};
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
        var anchor = plan.fit().door().anchor();
        boolean installed = AssemblyCapturedPlacement.place(level, plan.states(), () -> {
            if (!(level.getBlockEntity(anchor) instanceof RoundDoorBlockEntity entity)) return false;
            entity.loadWithComponents(plan.anchorData().copy(), level.registryAccess());
            return entity.arch().filter(plan.attachment()::equals).isPresent();
        }, context.getPlayer(), context.getClickedFace(), () -> level.getBlockEntity(anchor).setChanged());
        if (!installed || !announce) return InteractionResult.CONSUME;
        var state = level.getBlockState(anchor); var player = context.getPlayer(); var sound = state.getSoundType(level, anchor, player);
        level.playSound(player, anchor, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume()+1)/2, sound.getPitch()*.8F);
        level.gameEvent(GameEvent.BLOCK_PLACE, anchor, GameEvent.Context.of(player, state));
        if (player == null || !player.getAbilities().instabuild) context.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
