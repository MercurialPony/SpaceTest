package melonslise.spacetest;

import melonslise.spacetest.init.SpaceDimension;
import melonslise.spacetest.init.StBlockEntities;
import melonslise.spacetest.init.StBlocks;
import melonslise.spacetest.init.StItems;
import net.fabricmc.api.ModInitializer;
import net.minecraft.util.Identifier;

// FIXME: monster light level stuff in the dimension type
public class SpaceTestCore implements ModInitializer
{
	public static final String ID = "spacetest";

	public static Identifier id(String name)
	{
		return new Identifier(ID, name);
	}

	public static String sid(String name)
	{
		return ID + ":" + name;
	}

	// public static final Logger LOGGER = LoggerFactory.getLogger(ID);

	@Override
	public void onInitialize()
	{
		StBlocks.register();
		StItems.register();
		StBlockEntities.register();
		SpaceDimension.registerParts();
	}
}