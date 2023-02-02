package melonslise.spacetest.mixin;

import melonslise.spacetest.world.PersistentPlanetState;
import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerWorld.class)
public class ServerWorldMixin
{
	@Inject(at = @At("RETURN"), method = "<init>*")
	private void constructor(CallbackInfo ci)
	{
		ServerWorld world = (ServerWorld) (Object) this;

		world.getPersistentStateManager().getOrCreate(
			tag -> PersistentPlanetState.readNbt(world, tag),
			() -> new PersistentPlanetState(world),
			PersistentPlanetState.ID
		);
	}

}