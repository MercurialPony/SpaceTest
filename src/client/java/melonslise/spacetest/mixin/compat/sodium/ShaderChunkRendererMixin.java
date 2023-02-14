package melonslise.spacetest.mixin.compat.sodium;

import me.jellysquid.mods.sodium.client.render.chunk.ShaderChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkFogMode;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import melonslise.spacetest.compat.sodium.CustomizableShaderChunkRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(value = ShaderChunkRenderer.class, remap = false)
public class ShaderChunkRendererMixin implements CustomizableShaderChunkRenderer
{
	@ModifyConstant(method = "compileProgram", constant = @Constant(stringValue = "blocks/block_layer_opaque"))
	private String compileProgramModifyConstantShaderPath(String constant)
	{
		return this.getShaderPath(constant);
	}

	@ModifyConstant(method = "createShader", constant = @Constant(stringValue = "sodium"))
	private String createShaderModifyConstantShaderDomain(String constant)
	{
		return this.getShaderDomain(constant);
	}

	@ModifyArgs(method = "createShader", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/gl/shader/GlProgram$Builder;link(Ljava/util/function/Function;)Lme/jellysquid/mods/sodium/client/gl/shader/GlProgram;"))
	private void createShaderModifyArgsLink(Args args, String path, ChunkShaderOptions options)
	{
		args.set(0, this.shaderInterfaceFactory(args.get(0), options));
	}

	@ModifyArg(method = "begin", at = @At(value = "INVOKE", target = "Lme/jellysquid/mods/sodium/client/render/chunk/shader/ChunkShaderOptions;<init>(Lme/jellysquid/mods/sodium/client/render/chunk/shader/ChunkFogMode;Lme/jellysquid/mods/sodium/client/render/chunk/passes/BlockRenderPass;Lme/jellysquid/mods/sodium/client/model/vertex/type/ChunkVertexType;)V"), index = 0)
	private ChunkFogMode beginModifyArgChunkFogMode(ChunkFogMode fog)
	{
		return this.getFogMode(fog);
	}
}