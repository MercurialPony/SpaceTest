package melonslise.spacetest.common.blockentity;

import melonslise.spacetest.common.init.SpaceTestBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class TestBlockEntity extends BlockEntity
{
	public final AABB renderBB;

	public TestBlockEntity(BlockPos pos, BlockState state)
	{
		super(SpaceTestBlockEntities.TEST.get(), pos, state);
		this.renderBB = new AABB(pos.getX() - 64, pos.getY() - 64, pos.getZ() - 64, pos.getX() + 65, pos.getY() + 65, pos.getZ() + 65);
	}

	@Override
	public AABB getRenderBoundingBox()
	{
		return this.renderBB;
	}
}