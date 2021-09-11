package melonslise.spacetest.common.event;

import melonslise.spacetest.SpaceTest;
import melonslise.spacetest.common.init.SpaceTestChunkGenerators;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = SpaceTest.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class SpaceTestModEvents
{
	private SpaceTestModEvents() {}

	@SubscribeEvent
	public static void setup(FMLCommonSetupEvent e)
	{
		SpaceTestChunkGenerators.register();
	}
}