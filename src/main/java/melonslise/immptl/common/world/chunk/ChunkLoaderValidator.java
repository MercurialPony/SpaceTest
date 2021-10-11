package melonslise.immptl.common.world.chunk;

import melonslise.spacetest.SpaceTest;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.world.ForgeChunkManager;

import javax.annotation.Nonnull;

public class ChunkLoaderValidator implements ForgeChunkManager.LoadingValidationCallback
{
	public void validateTickets(@Nonnull ServerLevel world, ForgeChunkManager.TicketHelper ticketHelper)
	{
		SpaceTest.LOGGER.info("Clearing old RenderLoader tickets.");
		int i = 0;
		for (BlockPos pos : ticketHelper.getBlockTickets().keySet()) {
			ticketHelper.removeAllTickets(pos);
			i++;
		}
		SpaceTest.LOGGER.info("Cleared old tickets for " + i + " blocks.");
	}
}