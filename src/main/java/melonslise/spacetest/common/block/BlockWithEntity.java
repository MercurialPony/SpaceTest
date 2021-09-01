package melonslise.spacetest.common.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fmllegacy.RegistryObject;

public class BlockWithEntity<T extends BlockEntity> extends Block implements EntityBlock
{
	public final RegistryObject<BlockEntityType<T>> type;

	public BlockWithEntity(RegistryObject<BlockEntityType<T>> type, Properties props)
	{
		super(props);
		this.type = type;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
	{
		return this.type.get().create(pos, state);
	}

	@Override
	public RenderShape getRenderShape(BlockState state)
	{
		return RenderShape.ENTITYBLOCK_ANIMATED;
	}
}