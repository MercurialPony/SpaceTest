package melonslise.spacetest.compat.sodium;

import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkCameraContext;
import me.jellysquid.mods.sodium.client.render.chunk.RegionChunkRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkFogMode;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ShaderBindingContext;
import melonslise.spacetest.SpaceTestCore;
import melonslise.spacetest.planet.CubeFaceContext;
import melonslise.spacetest.planet.PlanetProperties;

import java.util.function.Function;

public class SodiumPlanetRegionRenderer extends RegionChunkRenderer implements ExtendedRegionChunkRenderer, ExtendedShaderChunkRenderer
{
	private PlanetProperties planetProps;
	private CubeFaceContext faceCtx;

	public SodiumPlanetRegionRenderer(RenderDevice device, ChunkVertexType vertexType)
	{
		super(device, vertexType);
	}

	public void savePlanetProps(PlanetProperties planetProps)
	{
		this.planetProps = planetProps;
	}

	public void saveFaceCtx(CubeFaceContext faceCtx)
	{
		this.faceCtx = faceCtx;
	}

	@Override
	public void beginRender(ChunkCameraContext cameraCtx)
	{
		SodiumPlanetShaderInterface shaderInterface = (SodiumPlanetShaderInterface) this.activeProgram.getInterface();
		shaderInterface.setCameraUniform(cameraCtx);
		shaderInterface.setPlanetUniforms(this.planetProps);
		shaderInterface.setFaceUniforms(this.faceCtx);
	}

	@Override
	public boolean enableBlockFaceCulling(boolean original)
	{
		return false;
	}

	@Override
	public String getShaderDomain(String original)
	{
		return SpaceTestCore.ID;
	}

	@Override
	public String getShaderPath(String original)
	{
		return "sodium/block_layer_opaque";
	}

	@Override
	public Function<ShaderBindingContext, ChunkShaderInterface> shaderInterfaceFactory(Function<ShaderBindingContext, ChunkShaderInterface> original, ChunkShaderOptions options)
	{
		return ctx -> new SodiumPlanetShaderInterface(ctx, options);
	}

	@Override
	public ChunkFogMode getFogMode(ChunkFogMode original)
	{
		return ChunkFogMode.NONE;
	}
}