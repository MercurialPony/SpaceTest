package melonslise.spacetest.mixin.compat.sodium;

import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = RenderSection.class, remap = false)
public class RenderSectionMixin
{
	@Redirect(method = "setData", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/SodiumWorldRenderer;onChunkRenderUpdated(IIILme/jellysquid/mods/sodium/client/render/chunk/data/ChunkRenderData;Lme/jellysquid/mods/sodium/client/render/chunk/data/ChunkRenderData;)V"))
	private void allowNullSodiumWorldRenderer(SodiumWorldRenderer wr, int x, int y, int z, ChunkRenderData meshBefore, ChunkRenderData meshAfter)
	{
		if(wr == null)
		{
			((RenderSection) (Object) this).setOcclusionData(meshAfter.getOcclusionData());
			return;
		}

		wr.onChunkRenderUpdated(x, y, z, meshBefore, meshAfter);
	}
}