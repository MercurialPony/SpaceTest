package melonslise.spacetest.blockentity;

import melonslise.spacetest.client.render.PlanetRenderer;
import melonslise.spacetest.init.StBlockEntities;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.math.BlockPos;

public class PlanetBlockEntity extends BlockEntity
{
	@Environment(EnvType.CLIENT)
	public PlanetRenderer pr;

	public PlanetBlockEntity(BlockPos pos, BlockState state)
	{
		super(StBlockEntities.PLANET, pos, state);
	}


}