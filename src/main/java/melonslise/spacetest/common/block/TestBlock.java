package melonslise.spacetest.common.block;

import melonslise.spacetest.common.init.SpaceTestBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TestBlock extends Block implements EntityBlock
{
	public TestBlock(Properties props)
	{
		super(props);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return SpaceTestBlockEntities.TEST.get().create(pos, state);
	}

	@Override
	public RenderShape getRenderShape(BlockState state)
	{
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}

	
}