package melonslise.spacetest.mixin.core.planet;

import melonslise.spacetest.core.planet.BasicPlanetProperties;
import melonslise.spacetest.core.planet.PlanetProperties;
import melonslise.spacetest.core.planet.world.PlanetWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(World.class)
public class WorldMixin implements PlanetWorld
{
	// FIXME: this is not synced!
	private PlanetProperties planetProps;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void constructorInjectReturn(MutableWorldProperties properties, RegistryKey<World> registryRef, RegistryEntry<DimensionType> dimension, Supplier<Profiler> profiler, boolean isClient, boolean debugWorld, long seed, int maxChainedNeighborUpdates, CallbackInfo ci)
	{
		World world = (World) (Object) this;

		int bottom = dimension.value().minY();

		this.planetProps = new BasicPlanetProperties(
			ChunkSectionPos.from(0, ChunkSectionPos.getSectionCoord(bottom), 0),
			10,
			world.getSeaLevel() - bottom);
	}

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