package melonslise.spacetest.render.planet.sodium;

import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.shader.*;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.RegionChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkFogMode;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderBindingPoints;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import melonslise.spacetest.SpaceTestCore;
import melonslise.spacetest.mixin.ShaderChunkRendererAccessor;
import melonslise.spacetest.planet.CubeFaceContext;
import melonslise.spacetest.planet.PlanetProperties;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class PlanetRegionChunkRenderer extends RegionChunkRenderer implements PlanetRenderOptions
{
	public CubeFaceContext faceCtx;
	public PlanetProperties planetProps;
	public BlockPos.Mutable sectionOrigin;

	public PlanetRegionChunkRenderer(RenderDevice device, ChunkVertexType vertexType)
	{
		super(device, vertexType);
		this.sectionOrigin = new BlockPos.Mutable();
	}

	public void savePlanetUniforms(CubeFaceContext faceCtx, PlanetProperties props)
	{
		this.faceCtx = faceCtx;
		this.planetProps = props;
	}

	@Override
	public CubeFaceContext faceCtx()
	{
		return this.faceCtx;
	}

	@Override
	public PlanetProperties planetProps()
	{
		return this.planetProps;
	}

	@Override
	protected GlProgram<ChunkShaderInterface> compileProgram(ChunkShaderOptions options)
	{
		options = new ChunkShaderOptions(ChunkFogMode.NONE, options.pass(), options.vertexType());

		ShaderChunkRendererAccessor accessor = (ShaderChunkRendererAccessor) this;

		GlProgram<ChunkShaderInterface> program = accessor.getPrograms().get(options);

		if (program == null)
		{
			accessor.getPrograms().put(options, program = this.createShader(SpaceTestCore.ID, "sodium/block_layer_opaque", options));
		}

		return program;
	}

	protected GlProgram<ChunkShaderInterface> createShader(String domain, String path, ChunkShaderOptions options)
	{
		ShaderConstants constants = options.constants();

		GlShader vertShader = ShaderLoader.loadShader(ShaderType.VERTEX, new Identifier(domain, path + ".vsh"), constants);
		GlShader fragShader = ShaderLoader.loadShader(ShaderType.FRAGMENT, new Identifier(domain, path + ".fsh"), constants);

		try
		{
			return GlProgram.builder(new Identifier(domain, "chunk_shader"))
				.attachShader(vertShader)
				.attachShader(fragShader)
				.bindAttribute("a_PosId", ChunkShaderBindingPoints.ATTRIBUTE_POSITION_ID)
				.bindAttribute("a_Color", ChunkShaderBindingPoints.ATTRIBUTE_COLOR)
				.bindAttribute("a_TexCoord", ChunkShaderBindingPoints.ATTRIBUTE_BLOCK_TEXTURE)
				.bindAttribute("a_LightCoord", ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_TEXTURE)
				.bindFragmentData("fragColor", ChunkShaderBindingPoints.FRAG_COLOR)
				.link(shader -> new PlanetChunkShaderInterface(shader, options, this, MinecraftClient.getInstance().gameRenderer.getCamera()::getPos));
		}
		finally
		{
			vertShader.delete();
			fragShader.delete();
		}
	}
}