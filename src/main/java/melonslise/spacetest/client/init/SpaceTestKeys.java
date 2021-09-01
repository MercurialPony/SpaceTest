package melonslise.spacetest.client.init;

import org.lwjgl.glfw.GLFW;

import melonslise.spacetest.SpaceTest;
import melonslise.spacetest.client.gui.ShaderInspectorScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fmlclient.registry.ClientRegistry;

public final class SpaceTestKeys
{
	public static final KeyMapping TWEAK = new KeyMapping("key." + SpaceTest.ID + ".tweak", GLFW.GLFW_KEY_Y, "key.category." + SpaceTest.ID);

	private SpaceTestKeys() {}

	public static void register()
	{
		ClientRegistry.registerKeyBinding(TWEAK);
	}

	public static void handle()
	{
		Minecraft mc = Minecraft.getInstance();

		while(TWEAK.consumeClick())
			mc.setScreen(new ShaderInspectorScreen(SpaceTestShaders.SHADERS));
	}
}