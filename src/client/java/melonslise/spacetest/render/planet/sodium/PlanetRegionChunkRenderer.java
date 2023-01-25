package melonslise.spacetest.render.planet.sodium;

import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gl.shader.*;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformFloat;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformFloat3v;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformInt;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.RegionChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.shader.*;
import melonslise.spacetest.SpaceTestCore;
import melonslise.spacetest.mixin.ShaderChunkRendererAccessor;
import melonslise.spacetest.planet.CubeFaceContext;
import melonslise.spacetest.planet.PlanetProperties;
import net.minecraft.util.Identifier;

public class PlanetRegionChunkRenderer extends RegionChunkRenderer
{
	public CubeFaceContext faceCtx;
	public PlanetProperties planetProps;

	public PlanetRegionChunkRenderer(RenderDevice device, ChunkVertexType vertexType)
	{
		super(device, vertexType);
	}

	public void savePlanetUniforms(CubeFaceContext faceCtx, PlanetProperties props)
	{
		this.faceCtx = faceCtx;
		this.planetProps = props;
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
				.link(shader -> new PlanetChunkShaderInterface(shader, options, this));
		}
		finally
		{
			vertShader.delete();
			fragShader.delete();
		}
	}

	public static class PlanetChunkShaderInterface extends ChunkShaderInterface
	{
		public final PlanetRegionChunkRenderer renderer;

		public GlUniformFloat3v corner;
		public GlUniformInt faceIndex;
		public GlUniformInt faceSize;
		public GlUniformFloat startRadius;
		public GlUniformFloat radiusRatio;

		public PlanetChunkShaderInterface(ShaderBindingContext ctx, ChunkShaderOptions options, PlanetRegionChunkRenderer renderer)
		{
			super(ctx, options);
			this.corner = ctx.bindUniform("Corner", GlUniformFloat3v::new);
			this.faceIndex = ctx.bindUniform("FaceIndex", GlUniformInt::new);
			this.faceSize = ctx.bindUniform("FaceSize", GlUniformInt::new);
			this.startRadius = ctx.bindUniform("StartRadius", GlUniformFloat::new);
			this.radiusRatio = ctx.bindUniform("RadiusRatio", GlUniformFloat::new);
			this.renderer = renderer;
		}

		@Override
		public void setDrawUniforms(GlMutableBuffer buffer)
		{
			super.setDrawUniforms(buffer);
			this.setPlanetUniforms(this.renderer.faceCtx, this.renderer.planetProps);
		}

		public void setPlanetUniforms(CubeFaceContext faceCtx, PlanetProperties planetProps)
		{
			this.corner.set(faceCtx.minX(), faceCtx.minY(), faceCtx.minZ());
			this.faceIndex.set(faceCtx.face().ordinal());
			this.faceSize.set(planetProps.getFaceSize() * 16);
			this.startRadius.set(planetProps.getStartRadius());
			this.radiusRatio.set(planetProps.getRadiusRatio());
		}
	}
}