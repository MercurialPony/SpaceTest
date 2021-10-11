package melonslise.immptl.server.command;

import com.mojang.brigadier.builder.ArgumentBuilder;
import melonslise.immptl.common.world.chunk.RenderLoaderManager;
import melonslise.spacetest.SpaceTest;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

public class CommandDumpInfo {
    static ArgumentBuilder<CommandSourceStack, ?> register()
    {
        return Commands.literal("dump")
                .requires(cs -> cs.hasPermission(2))
                .then(Commands.literal("manager")
                        .executes(ctx -> {
                            RenderLoaderManager.dumpMapsToLog();
                            return 0;
                        }))
                .then(Commands.literal("player")
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(ctx -> {
                                    SpaceTest.LOGGER.info("Executing player command.");
                                    ServerPlayer player = EntityArgument.getPlayer(ctx, "target");
                                    return RenderLoaderManager.dumpPlayerManager(player);
                                })));
    }
}
