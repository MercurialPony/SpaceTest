package melonslise.spacetest;

import melonslise.spacetest.init.StBlockEntityRenderers;
import melonslise.spacetest.init.StShaders;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.joml.Matrix4f;

@Environment(EnvType.CLIENT)
public class SpaceTestClient implements ClientModInitializer
{
	public static boolean isSodiumLoaded;

	public static Matrix4f modelViewMat;

	@Override
	public void onInitializeClient()
	{
		isSodiumLoaded = FabricLoader.getInstance().isModLoaded("sodium");

		StBlockEntityRenderers.register();
		StShaders.register();

		WorldRenderEvents.START.register(ctx -> modelViewMat = ctx.matrixStack().peek().getPositionMatrix());
	}
}