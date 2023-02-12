package melonslise.spacetest.mixin;

import net.minecraft.server.world.ServerWorld;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(ServerWorld.class)
public class ServerWorldMixin
{
	/*
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
	 */

}