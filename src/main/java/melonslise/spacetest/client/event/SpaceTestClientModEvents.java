package melonslise.spacetest.client.event;

import melonslise.spacetest.SpaceTest;
import melonslise.spacetest.client.init.SpaceTestBERs;
import melonslise.spacetest.client.init.SpaceTestDimensionEffects;
import melonslise.spacetest.client.init.SpaceTestKeys;
import melonslise.spacetest.client.init.SpaceTestShaders;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = SpaceTest.ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class SpaceTestClientModEvents
{
	@SubscribeEvent
	public static void setup(FMLClientSetupEvent e)
	{
		SpaceTestBERs.register();
		SpaceTestKeys.register();
		SpaceTestDimensionEffects.register();
	}

	@SubscribeEvent
	public static void registerReloadListeners(RegisterClientReloadListenersEvent e)
	{
		e.registerReloadListener(new SpaceTestShaders());
	}
}