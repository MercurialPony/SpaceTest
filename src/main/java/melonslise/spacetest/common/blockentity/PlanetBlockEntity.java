package melonslise.spacetest.common.blockentity;

import javax.annotation.Nonnull;

import melonslise.immptl.common.world.chunk.ChunkLoaderManager;
import melonslise.immptl.util.DimBlockPos;
import melonslise.immptl.util.DimChunkPos;
import melonslise.spacetest.SpaceTest;
import melonslise.spacetest.common.init.SpaceTestBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class PlanetBlockEntity extends BlockEntity
{
	public final AABB renderBB;
	private DimBlockPos dimPos;
	private DimChunkPos renderTargetStartCorner;
	private int renderXWidth;
	private int renderZWidth;

	public PlanetBlockEntity(BlockPos pos, BlockState state)
	{
		super(SpaceTestBlockEntities.PLANET.get(), pos, state);
		this.renderBB = new AABB(pos.getX() - 64, pos.getY() - 64, pos.getZ() - 64, pos.getX() + 65, pos.getY() + 65, pos.getZ() + 65);
		this.renderXWidth = 21;
		this.renderZWidth = 21;
		ChunkPos thisLoc = new ChunkPos(pos);
		this.renderTargetStartCorner = new DimChunkPos(Level.OVERWORLD,
				new ChunkPos(thisLoc.x-this.renderXWidth/2, thisLoc.z-this.renderZWidth/2));
	}

	@Override
	public AABB getRenderBoundingBox()
	{
		return this.renderBB;
	}

	@Override
	public void setLevel(@Nonnull Level level)
	{
		super.setLevel(level);
		this.dimPos = new DimBlockPos(level.dimension(), this.getBlockPos());

		// SpaceTest.LOGGER.info("Server-side: PlanetBlockEntity#setLevel method called
		// for planet at " + this.getBlockPos());
	}

	@Override
	public void setRemoved()
	{
		super.setRemoved();
		if (this.level != null)
		{
			if (!this.level.isClientSide())
			{
				// SpaceTest.LOGGER.info("Server-side: PlanetBlockEntity#setRemoved method called for planet at "+this.getBlockPos());
				ChunkLoaderManager.destroyChunkLoader(this.dimPos);
			}
		}
		else
		{
			SpaceTest.LOGGER.warn("Level for " + this + " was never set.");
		}
	}

	@Override
	public void clearRemoved()
	{
		super.clearRemoved();
		if (this.level != null && !this.level.isClientSide)
		{
			// SpaceTestLogger.LOGGER.info("Server side. Planet block created at " + this.getBlockPos());
			ChunkPos thisChunkPos = new ChunkPos(this.getBlockPos());
			ChunkPos startCorner = new ChunkPos(thisChunkPos.x - 10, thisChunkPos.z - 10);
			ChunkLoaderManager.createChunkLoader(this.dimPos, renderTargetStartCorner, this.renderXWidth, this.renderZWidth);
		}
	}
}