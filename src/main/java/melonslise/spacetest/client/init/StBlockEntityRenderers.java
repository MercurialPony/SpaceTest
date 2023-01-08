package melonslise.spacetest.client.init;

import melonslise.spacetest.client.render.blockentity.PlanetBlockEntityRenderer;
import melonslise.spacetest.init.StBlockEntities;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;

@Environment(EnvType.CLIENT)
public final class StBlockEntityRenderers
{
	private StBlockEntityRenderers() {}

	public static void register()
	{
		BlockEntityRendererRegistry.register(StBlockEntities.PLANET, PlanetBlockEntityRenderer::new);
	}
}