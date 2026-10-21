package io.github.dirtyredz.roundwood.showcase;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.github.dirtyredz.roundwood.Roundwood;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;

/** {@code /roundwood showcase}: operators (permission level 2) build the test layout in front of them. */
public final class ShowcaseCommand {
    public static final int PERMISSION_LEVEL = 2;

    private ShowcaseCommand() {
    }

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(Roundwood.MOD_ID)
                .then(Commands.literal("showcase")
                        .requires(source -> source.hasPermission(PERMISSION_LEVEL))
                        .executes(ShowcaseCommand::run)));
    }

    private static int run(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayerOrException();
        ShowcaseBuilder.Result result = ShowcaseBuilder.build(source.getLevel(), player.blockPosition(), player.getDirection());
        if (!result.built()) {
            source.sendFailure(Component.literal(result.message()));
            return 0;
        }
        source.sendSuccess(() -> Component.literal(result.message()), true);
        if (!result.problems().isEmpty()) {
            source.sendFailure(Component.literal(result.problems().size() + " placements failed: " + String.join("; ", result.problems())));
        }
        return 1;
    }
}
