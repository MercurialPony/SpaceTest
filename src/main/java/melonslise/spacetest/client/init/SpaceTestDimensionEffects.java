package melonslise.spacetest.client.init;

import melonslise.spacetest.client.renderer.dimension.SpaceEffects;
import net.minecraft.client.renderer.DimensionSpecialEffects;

public final class SpaceTestDimensionEffects
{
	private SpaceTestDimensionEffects() {}

	public static void register()
	{
		DimensionSpecialEffects.EFFECTS.put(SpaceEffects.ID, new SpaceEffects());
	}
}