package melonslise.spacetest.common.blockentity;

import com.sun.istack.internal.NotNull;
import melonslise.spacetest.SpaceTest;
import melonslise.spacetest.common.init.SpaceTestBlockEntities;
import melonslise.spacetest.common.util.DimBlockPos;
import melonslise.spacetest.common.util.DimChunkPos;
import melonslise.spacetest.common.util.Miscellaneous;
import melonslise.spacetest.server.util.ChunkLoaderManager;
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
	private DimBlockPos dimPos = null;

	public PlanetBlockEntity(BlockPos pos, BlockState state)
	{
		super(SpaceTestBlockEntities.PLANET.get(), pos, state);
		this.renderBB = new AABB(pos.getX() - 64, pos.getY() - 64, pos.getZ() - 64, pos.getX() + 65, pos.getY() + 65, pos.getZ() + 65);
	}

	@Override
	public AABB getRenderBoundingBox()
	{
		return this.renderBB;
	}

	@Override
	public void setLevel(@NotNull Level level) {
		super.setLevel(level);
		this.dimPos = new DimBlockPos(level.dimension(), this.getBlockPos());

		//SpaceTest.LOGGER.info("Server-side: PlanetBlockEntity#setLevel method called for planet at " + this.getBlockPos());
	}

	@Override
	public void setRemoved() {
		super.setRemoved();
		if (this.level != null) {
			if (!this.level.isClientSide()) {
				//SpaceTest.LOGGER.info("Server-side: PlanetBlockEntity#setRemoved method called for planet at "+this.getBlockPos());
				ChunkLoaderManager.DestroyChunkLoader(this.dimPos);
			}
		}
		else {
			SpaceTest.LOGGER.warn("Level for " + this + " was never set.");
		}
	}

	@Override
	public void clearRemoved() {
		super.clearRemoved();
		if (this.level!=null) {
			if (!this.level.isClientSide()) {
				//SpaceTest.LOGGER.info("Server-side: PlanetBlockEntity#clearRemoved method called for planet at "+this.getBlockPos());
				if (level instanceof ServerLevel) {
					//SpaceTestLogger.LOGGER.info("Server side. Planet block created at " + this.getBlockPos());
					ChunkPos thisChunkPos = new ChunkPos(this.getBlockPos());
					ChunkPos startCorner = new ChunkPos(thisChunkPos.x-10, thisChunkPos.z-10);
					ChunkLoaderManager.CreateChunkLoader(
							this.dimPos,
							new DimChunkPos(level.dimension(), startCorner),
							21, 21);
				}
			}
		}
	}

	@Override
	public void onChunkUnloaded() {
		super.onChunkUnloaded();
		//SpaceTest.LOGGER.info("Server-side: PlanetBlockEntity#onChunkUnloaded method called for planet at "+this.getBlockPos());
	}
}