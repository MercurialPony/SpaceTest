package melonslise.spacetest.compat.sodium;

import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkFogMode;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ShaderBindingContext;

import java.util.function.Function;

public interface ExtendedShaderChunkRenderer
{
	default String getShaderDomain(String original)
	{
		return original;
	}

	default String getShaderPath(String original)
	{
		return original;
	}

	default Function<ShaderBindingContext, ChunkShaderInterface> shaderInterfaceFactory(Function<ShaderBindingContext, ChunkShaderInterface> original, ChunkShaderOptions options)
	{
		return original;
	}

	default ChunkFogMode getFogMode(ChunkFogMode original)
	{
		return original;
	}
}