package melonslise.spacetest.init;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import melonslise.spacetest.render.blockentity.PlanetBlockEntityRenderer;

@Environment(EnvType.CLIENT)
public final class StBlockEntityRenderers
{
	private StBlockEntityRenderers() {}

	public static void register()
	{
		BlockEntityRendererRegistry.register(StBlockEntities.PLANET, PlanetBlockEntityRenderer::new);
	}
}