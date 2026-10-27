package io.github.dirtyredz.roundwood.block.door.frame;

import io.github.dirtyredz.roundwood.appearance.Appearance;
import io.github.dirtyredz.roundwood.appearance.interaction.AppearanceActions;
import io.github.dirtyredz.roundwood.block.attachment.*;
import io.github.dirtyredz.roundwood.block.attachment.host.*;
import io.github.dirtyredz.roundwood.block.attachment.interaction.HeldTrimTool;
import io.github.dirtyredz.roundwood.block.door.adapter.*;
import io.github.dirtyredz.roundwood.block.door.assembly.DoorLayout;
import io.github.dirtyredz.roundwood.block.door.selection.DoorHitResolver;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import java.util.Optional;

/** Static frame regions win over door rings, camouflage and leaf toggles. */
public final class DoorArchInteraction {
    public record Hit(RoundDoorBlockEntity entity, DoorLayout.Assembly door, AttachmentFace face, boolean planar) {}
    private DoorArchInteraction() {}
    public static Optional<Hit> hit(BlockGetter level, BlockPos pos, BlockState state, BlockHitResult hit) {
        if(!(state.getBlock() instanceof AssemblyDoorBlock)) return Optional.empty();
        var door=DoorLayout.of(pos,state);
        if(level instanceof Level live && !live.isLoaded(door.anchor())) return Optional.empty();
        if(!(level.getBlockEntity(door.anchor()) instanceof RoundDoorBlockEntity entity) || entity.arch().isEmpty()) return Optional.empty();
        var frame=entity.arch().get();
        if(frame.product()!=DoorArchFitPlan.topFor(door.design())) return Optional.empty();
        var point=door.toAuthored(DoorHitResolver.insidePoint(hit)).add(0,0,door.depth().offset());
        if(DoorArchGeometry.selection(door.design(),frame,true).stream().noneMatch(b->point.x>=b.minX()-1e-6 && point.x<=b.maxX()+1e-6 && point.y>=b.minY()-1e-6 && point.y<=b.maxY()+1e-6 && point.z>=b.minZ()-1e-6 && point.z<=b.maxZ()+1e-6)) return Optional.empty();
        double back=frame.thickness().back(frame.depth()),front=frame.thickness().front(frame.depth());
        return Optional.of(new Hit(entity,door,AttachmentFace.at(point.z,(back+front)/2),hit.getDirection().getAxis()==door.facing().getAxis()));
    }
    public static InteractionResult use(Hit hit, Level level, BlockPos pos, Player player, InteractionHand hand) {
        var tool=HeldTrimTool.of(player,hand);
        if(hand==InteractionHand.MAIN_HAND && player.getMainHandItem().isEmpty() && !player.getOffhandItem().isEmpty()) return InteractionResult.PASS;
        var host=host(hit,pos);
        java.util.function.Supplier<Optional<TrimHost>> editable=()->mayEdit(hit,level,player)?Optional.of(host):Optional.empty();
        if(tool.is(HeldTrimTool.Kind.ATTACHMENT)) return tool.attachment(TrimSlot.PERIMETER)
                ?TrimFitting.fit(level,player,tool.stack(),hit.face(),editable):InteractionResult.FAIL;
        if(hit.planar() && tool.is(HeldTrimTool.Kind.SHEARS)) return TrimFitting.remove(level,player,hand,tool.stack(),hit.face(),editable);
        if(tool.is(HeldTrimTool.Kind.DONOR)) {
            boolean casing=hit.planar() && host.trims().has(hit.face());
            return AppearanceActions.apply(level,player,tool.donor().orElseThrow(),()->Optional.of(casing
                    ?TrimFitting.target(host,hit.face(),p->mayEdit(hit,level,p)):new AppearanceActions.Target(){
                        public Optional<Appearance> current(){return hit.entity().archEditable()?hit.entity().arch().map(DoorArchAttachment::appearance):Optional.empty();}
                        public boolean mayEdit(Player p){return DoorArchInteraction.mayEdit(hit,level,p);}
                        public void set(Appearance a){hit.entity().arch().ifPresent(frame->hit.entity().setArch(frame.withAppearance(a)));}
                        public BlockPos origin(){return pos;}
                    }));
        }
        return InteractionResult.CONSUME;
    }
    public static TrimHost host(Hit hit,BlockPos origin) {
        return TrimHost.of(TrimSlot.PERIMETER,()->hit.entity().arch().map(DoorArchAttachment::casings).orElse(FaceTrims.NONE),
                ()->hit.entity().archEditable() && hit.entity().arch().isPresent(),
                trims->hit.entity().arch().map(frame->hit.entity().setArch(frame.withCasings(trims))).orElse(false),origin);
    }
    private static boolean mayEdit(Hit hit,Level level,Player player) {
        return player.mayBuild() && hit.entity().archEditable() && hit.door().loaded(level)
                && hit.door().complete(level,hit.entity().getBlockState().getBlock()) && hit.door().mayEditAll(level,player);
    }
}
