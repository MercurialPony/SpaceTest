package melonslise.spacetest.block;

import melonslise.spacetest.blockentity.PlanetBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class PlanetBlock<T extends PlanetBlockEntity> extends Block implements BlockEntityProvider
{
	public final Supplier<BlockEntityType<T>> blockEntityType;

	public PlanetBlock(Supplier<BlockEntityType<T>> blockEntityType, AbstractBlock.Settings settings)
	{
		super(settings);
		this.blockEntityType = blockEntityType;
	}

	@Nullable
	@Override
	public BlockEntity createBlockEntity(BlockPos pos, BlockState state)
	{
		return this.blockEntityType.get().instantiate(pos, state);
	}

	@Override
	public BlockRenderType getRenderType(BlockState state)
	{
		return BlockRenderType.ENTITYBLOCK_ANIMATED;
	}

	@Override
	public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack)
	{
		((PlanetBlockEntity) world.getBlockEntity(pos)).placed(pos, placer);

		super.onPlaced(world, pos, state, placer, itemStack);
	}

	@Nullable
	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type)
	{
		return PlanetBlockEntity::tick;
	}
}