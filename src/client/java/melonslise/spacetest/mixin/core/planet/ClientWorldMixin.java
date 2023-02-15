package melonslise.spacetest.mixin.core.planet;

import melonslise.spacetest.core.planet.network.PlanetPropertiesRequestPacket;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ClientWorld.class)
public class ClientWorldMixin
{
	@Inject(method = "<init>", at = @At("RETURN"))
	private void requestPlanetProperties(ClientPlayNetworkHandler networkHandler, ClientWorld.Properties properties, RegistryKey<World> registryRef, RegistryEntry<DimensionType> dimensionTypeEntry, int loadDistance, int simulationDistance, Supplier<Profiler> profiler, WorldRenderer worldRenderer, boolean debugWorld, long seed, CallbackInfo ci)
	{
		networkHandler.sendPacket(new CustomPayloadC2SPacket(PlanetPropertiesRequestPacket.ID, new PlanetPropertiesRequestPacket(registryRef)));
		// ClientPlayNetworking.send(PlanetPropertiesRequestPacket.ID, new PlanetPropertiesRequestPacket(registryRef));
	}
}