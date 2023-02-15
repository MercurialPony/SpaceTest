package melonslise.spacetest.mixin.core.planet;

import melonslise.spacetest.core.planet.PlanetProperties;
import melonslise.spacetest.core.planet.world.PlanetWorld;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(World.class)
public class WorldMixin implements PlanetWorld
{
	private PlanetProperties planetProps;

	@Override
	public PlanetProperties getPlanetProperties()
	{
		return this.planetProps;
	}

	@Override
	public void setPlanetProperties(PlanetProperties props)
	{
		this.planetProps = props;
	}
}