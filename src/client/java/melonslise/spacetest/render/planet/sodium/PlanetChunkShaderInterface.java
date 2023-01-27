package melonslise.spacetest.render.planet.sodium;

import me.jellysquid.mods.sodium.client.gl.buffer.GlMutableBuffer;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformFloat;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformFloat3v;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniformInt;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderInterface;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderOptions;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ShaderBindingContext;
import melonslise.spacetest.planet.CubeFaceContext;
import melonslise.spacetest.planet.PlanetProperties;
import net.minecraft.util.math.Vec3d;

import java.util.function.Supplier;

public class PlanetChunkShaderInterface extends ChunkShaderInterface
{
	public final PlanetRenderOptions renderOptions;
	public final Supplier<Vec3d> cameraPositionSupplier;

	public GlUniformFloat3v cameraPosition;
	public GlUniformFloat3v corner;
	public GlUniformInt faceIndex;
	public GlUniformInt faceSize;
	public GlUniformFloat startRadius;
	public GlUniformFloat radiusRatio;

	public PlanetChunkShaderInterface(ShaderBindingContext ctx, ChunkShaderOptions options, PlanetRenderOptions renderOptions, Supplier<Vec3d> cameraPositionSupplier)
	{
		super(ctx, options);
		this.cameraPosition = ctx.bindUniform("CameraPosition", GlUniformFloat3v::new);
		this.corner = ctx.bindUniform("Corner", GlUniformFloat3v::new);
		this.faceIndex = ctx.bindUniform("FaceIndex", GlUniformInt::new);
		this.faceSize = ctx.bindUniform("FaceSize", GlUniformInt::new);
		this.startRadius = ctx.bindUniform("StartRadius", GlUniformFloat::new);
		this.radiusRatio = ctx.bindUniform("RadiusRatio", GlUniformFloat::new);

		this.renderOptions = renderOptions;
		this.cameraPositionSupplier = cameraPositionSupplier;
	}

	@Override
	public void setDrawUniforms(GlMutableBuffer buffer)
	{
		super.setDrawUniforms(buffer);
		this.setPlanetUniforms(this.renderOptions.faceCtx(), this.renderOptions.planetProps(), cameraPositionSupplier.get());
	}

	public void setPlanetUniforms(CubeFaceContext faceCtx, PlanetProperties planetProps, Vec3d camPos)
	{
		this.cameraPosition.set((float) camPos.x, (float) camPos.y, (float) camPos.z);
		this.corner.set(faceCtx.minX(), faceCtx.minY(), faceCtx.minZ());
		this.faceIndex.set(faceCtx.face().ordinal());
		this.faceSize.set(planetProps.getFaceSize() * 16);
		this.startRadius.set(planetProps.getStartRadius());
		this.radiusRatio.set(planetProps.getRadiusRatio());
	}
}