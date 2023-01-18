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
	}
}