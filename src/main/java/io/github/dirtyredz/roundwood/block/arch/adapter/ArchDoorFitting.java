package io.github.dirtyredz.roundwood.block.arch.adapter;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import java.util.function.Function;
import java.util.function.Predicate;

/** Arch adapters know only this port; bootstrap supplies the door-owned implementation. */
public final class ArchDoorFitting {
    private static Predicate<UseOnContext> claims = context -> false;
    private static Function<UseOnContext, InteractionResult> use = context -> InteractionResult.PASS;
    private ArchDoorFitting() {}
    public static void bind(Predicate<UseOnContext> claim, Function<UseOnContext, InteractionResult> action) { claims = claim; use = action; }
    public static boolean claims(UseOnContext context) { return claims.test(context); }
    public static InteractionResult use(UseOnContext context) { return use.apply(context); }
}
