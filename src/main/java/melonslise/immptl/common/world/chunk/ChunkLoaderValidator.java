package melonslise.immptl.common.world.chunk;

import javax.annotation.Nonnull;

import melonslise.spacetest.SpaceTest;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.world.ForgeChunkManager;

public class ChunkLoaderValidator implements ForgeChunkManager.LoadingValidationCallback
{
	public void validateTickets(@Nonnull ServerLevel world, ForgeChunkManager.TicketHelper ticketHelper)
	{
		SpaceTest.LOGGER.info("Clearing old tickets.");
		for (BlockPos pos : ticketHelper.getBlockTickets().keySet())
			ticketHelper.removeAllTickets(pos);
	}
}