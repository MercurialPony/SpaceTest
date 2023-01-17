package melonslise.spacetest.client;

import melonslise.spacetest.client.init.StBlockEntityRenderers;
import melonslise.spacetest.client.init.StShaders;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class SpaceTestClient implements ClientModInitializer
{
	@Override
	public void onInitializeClient()
	{
		StBlockEntityRenderers.register();
		StShaders.register();

		/*
		WorldRenderEvents.START.register(ctx ->
		{
			MinecraftClient client = MinecraftClient.getInstance();
			double fov = Math.max(client.gameRenderer.getFov(ctx.camera(), ctx.tickDelta(), true), client.options.getFov().getValue().doubleValue());
			planetCuller.update(ctx.matrixStack().peek().getPositionMatrix(), client.gameRenderer.getBasicProjectionMatrix(fov));
		});
		 */
	}
}