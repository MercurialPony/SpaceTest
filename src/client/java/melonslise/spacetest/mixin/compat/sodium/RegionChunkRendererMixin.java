package melonslise.spacetest.mixin.compat.sodium;

import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkCameraContext;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import me.jellysquid.mods.sodium.client.render.chunk.RegionChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import melonslise.spacetest.compat.sodium.CustomizableRegionChunkRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RegionChunkRenderer.class, remap = false)
public class RegionChunkRendererMixin implements CustomizableRegionChunkRenderer // use ModifyExpressionValue from MixinExtras?
{
	@Shadow
	@Final
	@Mutable
	private boolean isBlockFaceCullingEnabled;

	@Inject(method = "<init>", at = @At("RETURN"))
	private void modifyBlockFaceCulling(RenderDevice device, ChunkVertexType vertexType, CallbackInfo ci)
	{
		this.isBlockFaceCullingEnabled = this.enableBlockFaceCulling(this.isBlockFaceCullingEnabled);
	}

	@Inject(method = "render", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/gl/shader/GlProgram;getInterface()Ljava/lang/Object;"))
	private void addBeginRender(ChunkRenderMatrices matrices, CommandList commandList, ChunkRenderList list, BlockRenderPass pass, ChunkCameraContext camera, CallbackInfo ci)
	{
		this.beginRender(camera);
	}
}