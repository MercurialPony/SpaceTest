package melonslise.immptl.common.world.chunk;

import melonslise.spacetest.SpaceTest;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.fmlserverevents.FMLServerStartedEvent;
import net.minecraftforge.fmlserverevents.FMLServerStoppedEvent;

public class ServerHelper {
    public static MinecraftServer server = null;
    public static int getTrueViewDistance()
    {
        if (server != null)
        {
            return Helpers.computeTrueViewDistance(server.getPlayerList().getViewDistance());
        }
        else
        {
            SpaceTest.LOGGER.warn("Attempted to get true view distance before server was ready!");
            return 0;
        }
    }

    public static void onServerStart(FMLServerStartedEvent event)
    {
        SpaceTest.LOGGER.info("ServerHelper received server start event.");
        server = event.getServer();
    }

    public static void onServerStop(FMLServerStoppedEvent event)
    {
        SpaceTest.LOGGER.info("ServerHelper received server stop event.");
        server = null;
    }
}
