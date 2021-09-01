package melonslise.spacetest.common.blockentity;

import melonslise.spacetest.common.init.SpaceTestBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class BlackHoleBlockEntity extends BlockEntity
{
	public final AABB renderBB;

	public BlackHoleBlockEntity(BlockPos pos, BlockState state)
	{
		super(SpaceTestBlockEntities.BLACK_HOLE.get(), pos, state);
		this.renderBB = new AABB(pos.getX() - 32, pos.getY() - 32, pos.getZ() - 32, pos.getX() + 33, pos.getY() + 33, pos.getZ() + 33);
	}

	@Override
	public AABB getRenderBoundingBox()
	{
		return this.renderBB;
	}
}