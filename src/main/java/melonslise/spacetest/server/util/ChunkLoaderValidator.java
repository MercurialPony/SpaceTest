package melonslise.spacetest.server.util;

import com.sun.istack.internal.NotNull;
import melonslise.spacetest.SpaceTest;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.world.ForgeChunkManager;

import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ChunkLoaderValidator implements ForgeChunkManager.LoadingValidationCallback {

    public void validateTickets(@NotNull ServerLevel world, ForgeChunkManager.TicketHelper ticketHelper) {
        SpaceTest.LOGGER.info("Clearing old tickets.");
        for (BlockPos pos : ticketHelper.getBlockTickets().keySet()) {
            ticketHelper.removeAllTickets(pos);
        }
    }
}
