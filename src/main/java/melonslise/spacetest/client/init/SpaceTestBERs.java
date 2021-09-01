package melonslise.spacetest.client.init;

import melonslise.spacetest.client.renderer.blockentity.BlackHoleBER;
import melonslise.spacetest.client.renderer.blockentity.PlanetBER;
import melonslise.spacetest.common.init.SpaceTestBlockEntities;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class SpaceTestBERs
{
	private SpaceTestBERs() {}

	public static void register()
	{
		BlockEntityRenderers.register(SpaceTestBlockEntities.BLACK_HOLE.get(), BlackHoleBER::new);
		BlockEntityRenderers.register(SpaceTestBlockEntities.PLANET.get(), PlanetBER::new);
	}
}