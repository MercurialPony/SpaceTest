package melonslise.spacetest.mixin;

import melonslise.spacetest.compat.PlanetRendererFactory;
import melonslise.spacetest.render.PlanetWorldRenderer;
import melonslise.spacetest.render.planet.PlanetRenderer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WorldRenderer.class, priority = 1500)
public class WorldRendererMixin implements PlanetWorldRenderer
{
	private final PlanetRenderer planetRenderer = PlanetRendererFactory.createPlanetRenderer();

	@Override
	public PlanetRenderer getPlanetRenderer()
	{
		return this.planetRenderer;
	}

	@Inject(method = "setWorld", at = @At("TAIL"))
	private void setWorldInjectTail(ClientWorld world, CallbackInfo ci)
	{
		this.planetRenderer.init(world, (WorldRenderer) (Object) this);
	}

	@Inject(method = "close", at = @At("TAIL"))
	private void closeInjectTail(CallbackInfo ci)
	{
		this.planetRenderer.close();
	}
}