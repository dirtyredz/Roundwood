package io.github.dirtyredz.roundwood.block.window.pane;

import io.github.dirtyredz.roundwood.block.window.geometry.face.WindowPattern;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import java.util.Objects;

/** Window-owned port for retired pane custody; migration supplies it during mod bootstrap. */
public final class RetiredCrossWindows {
    public enum Result { CONVERTED, DEFERRED, UNCHANGED }

    public interface Handler {
        Result convert(Level level, BlockPos pos);
        ItemStack stack(WindowPaneBlock block, @Nullable RoundWindowBlockEntity entity);
    }

    private static Handler handler;

    private RetiredCrossWindows() { }

    public static void register(Handler implementation) {
        handler = Objects.requireNonNull(implementation);
    }

    public static Handler handler() {
        return Objects.requireNonNull(handler, "Retired cross handler must be registered at bootstrap");
    }

    public static boolean isRetired(BlockState state) {
        return state.getBlock() instanceof WindowPaneBlock window && window.design().pattern() == WindowPattern.CROSS;
    }
}
