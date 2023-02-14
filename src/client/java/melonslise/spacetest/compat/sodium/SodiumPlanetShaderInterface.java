package melonslise.spacetest.compat.sodium;

import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformFloat;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformFloat3v;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformInt;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkCameraContext;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ShaderBindingContext;
import melonslise.spacetest.core.planets.CubeFaceContext;
import melonslise.spacetest.core.planets.PlanetProperties;

public class SodiumPlanetShaderInterface extends ChunkShaderInterface
{
	public GlUniformFloat3v cameraPosition;

	public GlUniformFloat3v corner;
	public GlUniformInt faceIndex;

	public GlUniformInt faceSize;
	public GlUniformFloat startRadius;
	public GlUniformFloat radiusRatio;

	public SodiumPlanetShaderInterface(ShaderBindingContext ctx, ChunkShaderOptions options)
	{
		super(ctx, options);
		this.cameraPosition = ctx.bindUniform("CameraPosition", GlUniformFloat3v::new);

		this.corner = ctx.bindUniform("Corner", GlUniformFloat3v::new);
		this.faceIndex = ctx.bindUniform("FaceIndex", GlUniformInt::new);

		this.faceSize = ctx.bindUniform("FaceSize", GlUniformInt::new);
		this.startRadius = ctx.bindUniform("StartRadius", GlUniformFloat::new);
		this.radiusRatio = ctx.bindUniform("RadiusRatio", GlUniformFloat::new);
	}

	public void setCameraUniform(ChunkCameraContext cameraCtx)
	{
		this.cameraPosition.set(cameraCtx.posX, cameraCtx.posY, cameraCtx.posZ);
	}

	public void setFaceUniforms(CubeFaceContext faceCtx)
	{
		this.corner.set(faceCtx.minX(), faceCtx.minY(), faceCtx.minZ());
		this.faceIndex.set(faceCtx.face().ordinal());
	}

	public void setPlanetUniforms(PlanetProperties planetProps)
	{
		this.faceSize.set(planetProps.getFaceSize() * 16);
		this.startRadius.set(planetProps.getStartRadius());
		this.radiusRatio.set(planetProps.getRadiusRatio());
	}
}