package melonslise.spacetest;

import melonslise.spacetest.init.StBlockEntityRenderers;
import melonslise.spacetest.init.StShaders;
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