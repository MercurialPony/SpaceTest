package melonslise.spacetest.block;

import melonslise.spacetest.blockentity.PlanetBlockEntity;
import melonslise.spacetest.client.render.blockentity.PlanetBlockEntityRenderer;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.api.PortalAPI;
import qouteall.imm_ptl.core.chunk_loading.ChunkLoader;
import qouteall.imm_ptl.core.chunk_loading.DimensionalChunkPos;

import java.util.function.Supplier;

public class SpecialBlock<T extends BlockEntity> extends Block implements BlockEntityProvider
{
	public final Supplier<BlockEntityType<T>> blockEntityType;

	public SpecialBlock(Supplier<BlockEntityType<T>> blockEntityType, AbstractBlock.Settings settings)
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
	public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack itemStack) // FIXME this is for testing only
	{
		if(placer instanceof ServerPlayerEntity player)
		{
			PortalAPI.addChunkLoaderForPlayer(player, new ChunkLoader(new DimensionalChunkPos(World.OVERWORLD, new ChunkPos(pos)), 11));
		}

		super.onPlaced(world, pos, state, placer, itemStack);
	}
}