package melonslise.spacetest.client.renderer.blockentity;

import java.util.ArrayList;
import java.util.List;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;

import melonslise.spacetest.client.init.SpaceTestRenderTypes;
import melonslise.spacetest.client.init.SpaceTestShaders;
import melonslise.spacetest.client.renderer.shader.ExtendedShaderInstance;
import melonslise.spacetest.common.blockentity.PlanetBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.RenderChunk;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlanetBER implements BlockEntityRenderer<PlanetBlockEntity>
{
	public PlanetBER(BlockEntityRendererProvider.Context ctx)
	{
		
	}

	@Override
	public void render(PlanetBlockEntity be, float frameTime, PoseStack mtx, MultiBufferSource bufSrc, int light, int overlay)
	{
		Minecraft mc = Minecraft.getInstance();
		Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
		BlockPos pos = be.getBlockPos();
		mtx.pushPose();

		float rot = (mc.level.getGameTime() + frameTime) / 600f;
		mtx.mulPose(Vector3f.XP.rotation(rot));
		mtx.mulPose(Vector3f.YP.rotation(rot));
		mtx.mulPose(Vector3f.ZP.rotation(rot));
		// mtx.scale(2f, 2f, 2f);
		mtx.translate(-pos.getX() + camPos.x, -pos.getY() + camPos.y, -pos.getZ() + camPos.z);

		final int xChunks = 10, zChunks = 10;
		final int maxU = (xChunks * 2 + 1) * 16, maxV = (zChunks * 2 + 1) * 16;

		ChunkPos chPos = new ChunkPos(pos);
		BlockPos cornerPos = new ChunkPos(chPos.x - xChunks, chPos.z - zChunks).getWorldPosition();

		BlockPos tr = pos.subtract(cornerPos);
		mtx.translate(tr.getX(), tr.getY(), tr.getZ());

		List<RenderChunk> chunks = new ArrayList<>(9 * 16);
		for(int x = -xChunks; x <= xChunks; ++x)
			for(int z = -zChunks; z <= zChunks; ++z)
				for(int y = 0; y < 16; ++y)
				{
					RenderChunk chunk = mc.levelRenderer.viewArea.getRenderChunkAt(new ChunkPos(chPos.x + x, chPos.z + z).getWorldPosition().atY(y * 16));
					chunks.add(chunk);
				}

		ExtendedShaderInstance shader;
		shader = SpaceTestShaders.getSolidPlanet();
		shader.safeGetUniform("CameraPosition").set((float) camPos.x, (float) camPos.y, (float) camPos.z);
		shader.safeGetUniform("Corner").set((float) cornerPos.getX(), (float) cornerPos.getY(), (float) cornerPos.getZ());
		shader.safeGetUniform("MaxUV").set((float) maxU, (float) maxV);

		shader = SpaceTestShaders.getCutoutPlanet();
		shader.safeGetUniform("CameraPosition").set((float) camPos.x, (float) camPos.y, (float) camPos.z);
		shader.safeGetUniform("Corner").set((float) cornerPos.getX(), (float) cornerPos.getY(), (float) cornerPos.getZ());
		shader.safeGetUniform("MaxUV").set((float) maxU, (float) maxV);

		shader = SpaceTestShaders.getTranslucentPlanet();
		shader.safeGetUniform("CameraPosition").set((float) camPos.x, (float) camPos.y, (float) camPos.z);
		shader.safeGetUniform("Corner").set((float) cornerPos.getX(), (float) cornerPos.getY(), (float) cornerPos.getZ());
		shader.safeGetUniform("MaxUV").set((float) maxU, (float) maxV);

		renderChunks(chunks, RenderType.solid(), SpaceTestRenderTypes.SOLID_PLANET, mtx, camPos.x, camPos.y, camPos.z, RenderSystem.getProjectionMatrix());
		renderChunks(chunks, RenderType.cutout(), SpaceTestRenderTypes.CUTOUT_PLANET, mtx, camPos.x, camPos.y, camPos.z, RenderSystem.getProjectionMatrix());
		renderChunks(chunks, RenderType.cutoutMipped(), SpaceTestRenderTypes.CUTOUT_PLANET, mtx, camPos.x, camPos.y, camPos.z, RenderSystem.getProjectionMatrix());
		renderChunks(chunks, RenderType.translucent(), SpaceTestRenderTypes.TRANSLUCENT_PLANET, mtx, camPos.x, camPos.y, camPos.z, RenderSystem.getProjectionMatrix());

		mtx.popPose();

		EffectInstance postShader = SpaceTestShaders.getAtmosphere().getMainShader();
		postShader.safeGetUniform("Center").set(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f);
	}

	public static void renderChunks(List<RenderChunk> chunks, RenderType layer, RenderType newLayer, PoseStack mtx, double camX, double camY, double camZ, Matrix4f proj)
	{
		// RenderSystem.assertThread(RenderSystem::isOnRenderThread);
		// Minecraft mc = Minecraft.getInstance();
		// LevelRenderer renderer = mc.levelRenderer;
		newLayer.setupRenderState();
		/*
		if (layer == RenderType.translucent())
		{
			mc.getProfiler().push("translucent_sort");
			double d0 = camX - renderer.xTransparentOld;
			double d1 = camY - renderer.yTransparentOld;
			double d2 = camZ - renderer.zTransparentOld;
			if (d0 * d0 + d1 * d1 + d2 * d2 > 1.0D)
			{
				renderer.xTransparentOld = camX;
				renderer.yTransparentOld = camY;
				renderer.zTransparentOld = camZ;
				int j = 0;

				for (LevelRenderer.RenderChunkInfo chunkInfo : renderer.renderChunks)
					if (j < 15 && chunkInfo.chunk.resortTransparency(layer, renderer.getChunkRenderDispatcher()))
						++j;
			}

			mc.getProfiler().pop();
		}
		*/

		// mc.getProfiler().push("filterempty");
		// mc.getProfiler().popPush(() -> "render_" + layer);
		// boolean opaque = layer != RenderType.translucent();
		// ObjectListIterator<LevelRenderer.RenderChunkInfo> iterator = renderer.renderChunks.listIterator(opaque ? 0 : renderer.renderChunks.size());
		VertexFormat format = layer.format();
		ShaderInstance shader = RenderSystem.getShader();
		BufferUploader.reset();

		for (int k = 0; k < 12; ++k)
		{
			int i = RenderSystem.getShaderTexture(k);
			shader.setSampler("Sampler" + k, i);
		}

		if (shader.MODEL_VIEW_MATRIX != null)
			shader.MODEL_VIEW_MATRIX.set(mtx.last().pose());

		if (shader.PROJECTION_MATRIX != null)
			shader.PROJECTION_MATRIX.set(proj);

		if (shader.COLOR_MODULATOR != null)
			shader.COLOR_MODULATOR.set(RenderSystem.getShaderColor());

		if (shader.FOG_START != null)
			shader.FOG_START.set(RenderSystem.getShaderFogStart());

		if (shader.FOG_END != null)
			shader.FOG_END.set(RenderSystem.getShaderFogEnd());

		if (shader.FOG_COLOR != null)
			shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());

		if (shader.TEXTURE_MATRIX != null)
			shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());

		if (shader.GAME_TIME != null)
			shader.GAME_TIME.set(RenderSystem.getShaderGameTime());

		RenderSystem.setupShaderLights(shader);
		shader.apply();
		boolean clear = false;

		for(RenderChunk chunk : chunks)
		{
			if(chunk.getCompiledChunk().isEmpty(layer))
				continue;
			BlockPos pos = chunk.getOrigin();
			VertexBuffer buf = chunk.getBuffer(layer);
			if (shader.CHUNK_OFFSET != null)
			{
				shader.CHUNK_OFFSET.set((float) (pos.getX() - camX), (float) (pos.getY() - camY), (float) (pos.getZ() - camZ));
				shader.CHUNK_OFFSET.upload();
			}

			buf.drawChunkLayer();
			clear = true;
		}

		/*
		while (true)
		{
			if(opaque && !iterator.hasNext() || !opaque && !iterator.hasPrevious())
				break;

			LevelRenderer.RenderChunkInfo chunkInfo = opaque ? iterator.next() : iterator.previous();
			ChunkRenderDispatcher.RenderChunk chunk = chunkInfo.chunk;
			if (chunk.getCompiledChunk().isEmpty(layer))
				continue;
			BlockPos pos = chunk.getOrigin();
			if(!chunkOrigins.contains(pos))
				continue;
			VertexBuffer buf = chunk.getBuffer(layer);
			if (uniform != null)
			{
				uniform.set((float) (pos.getX() - camX), (float) (pos.getY() - camY), (float) (pos.getZ() - camZ));
				uniform.upload();
			}

			buf.drawChunkLayer();
			clear = true;
		}
		*/

		if (shader.CHUNK_OFFSET != null)
			shader.CHUNK_OFFSET.set(Vector3f.ZERO);

		shader.clear();
		if (clear)
			format.clearBufferState();

		VertexBuffer.unbind();
		VertexBuffer.unbindVertexArray();
		// mc.getProfiler().pop();
		newLayer.clearRenderState();
	}

	@Override
	public int getViewDistance()
	{
		return 256;
	}

	@Override
	public boolean shouldRenderOffScreen(PlanetBlockEntity be)
	{
		return true;
	}
}