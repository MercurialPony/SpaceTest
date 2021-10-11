package melonslise.spacetest.common.blockentity;

import melonslise.immptl.client.ClientImmutableRenderLoader;
import melonslise.immptl.client.PlayerViewManager;
import melonslise.immptl.common.world.chunk.RenderSideSplitter;
import melonslise.immptl.util.DimBlockPos;
import melonslise.immptl.util.DimChunkPos;
import melonslise.spacetest.SpaceTest;
import melonslise.spacetest.common.init.SpaceTestBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PlanetBlockEntity extends BlockEntity implements BlockEntityWithClientRenderLoader
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
	}

	@Override
	public void setRemoved()
	{
		super.setRemoved();
		if (this.level != null)
		{
			// SpaceTest.LOGGER.info("Server-side: PlanetBlockEntity#setRemoved method called for planet at "+this.getBlockPos());
			RenderSideSplitter.removeImmutableRender(this.dimPos, this.level.isClientSide);
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
		if (this.level != null)
		{
			// SpaceTestLogger.LOGGER.info("Server side. Planet block created at " + this.getBlockPos());
			ChunkPos thisChunkPos = new ChunkPos(this.getBlockPos());
			ChunkPos startCorner = new ChunkPos(thisChunkPos.x - 10, thisChunkPos.z - 10);
			RenderSideSplitter.addImmutableRender(this.dimPos, renderTargetStartCorner, this.renderXWidth, this.renderZWidth, this.level.isClientSide);
		}
		else
		{
			SpaceTest.LOGGER.warn("Level for " + this + " was never set.");
		}
	}

	@Override
	@Nullable
	public ClientImmutableRenderLoader getRenderLoader()
	{
		if (this.level != null)
		{
			if (this.level.isClientSide)
			{
				return PlayerViewManager.getImmutableRenderLoader(this.dimPos);
			}
		}
		return null;
	}
}