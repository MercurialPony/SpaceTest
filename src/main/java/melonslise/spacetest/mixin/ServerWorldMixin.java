package melonslise.spacetest.mixin;

import melonslise.spacetest.world.PlanetState;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin
{
	@Inject(at = @At("RETURN"), method = "<init>*")
	private void constructor(CallbackInfo ci)
	{
		ServerWorld world = (ServerWorld) (Object) this;

		world.getPersistentStateManager().getOrCreate(
			tag -> PlanetState.readNbt(world, tag),
			() -> new PlanetState(world),
			PlanetState.ID
		);
	}

}