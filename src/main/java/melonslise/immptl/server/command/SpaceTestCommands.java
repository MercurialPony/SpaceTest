package melonslise.immptl.server.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;

public class SpaceTestCommands {
    public SpaceTestCommands(CommandDispatcher<CommandSourceStack> dispatcher)
    {
        dispatcher.register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("spacetest")
                        .then(CommandDumpInfo.register())
        );
    }
}
