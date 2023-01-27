package melonslise.spacetest.render.planet.sodium;

import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.chunk.*;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkModelVertexFormats;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.world.WorldRendererExtended;
import melonslise.spacetest.SpaceTestClient;
import melonslise.spacetest.planet.CubeFaceContext;
import melonslise.spacetest.planet.CubemapFace;
import melonslise.spacetest.planet.PlanetProperties;
import melonslise.spacetest.render.planet.PlanetRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.joml.Matrix4f;
import qouteall.imm_ptl.core.ClientWorldLoader;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class SodiumPlanetRenderer implements PlanetRenderer
{
	public SodiumWorldRenderer wr;
	public PlanetProperties planetProps;

	public void init(RegistryKey<World> worldKey, PlanetProperties planetProps)
	{
		WorldRenderer wr = ClientWorldLoader.getWorldRenderer(worldKey);

		if (wr instanceof WorldRendererExtended wre)
		{
			this.wr = wre.getSodiumWorldRenderer();
			this.planetProps = planetProps;
		}
	}

	public void render(MatrixStack mtx, float tickDelta)
	{
		if (this.wr != null)
		{
			Vec3d camPos = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
			ChunkCameraContext camCtx = new ChunkCameraContext(camPos.x, camPos.y, camPos.z);

			Matrix4f modelView = new Matrix4f(SpaceTestClient.modelViewMat);
			modelView.translate((float) this.planetProps.getPosition().x, (float) this.planetProps.getPosition().y, (float) this.planetProps.getPosition().z);

			PlanetRegionChunkRenderer renderer = new PlanetRegionChunkRenderer(RenderDevice.INSTANCE, ChunkModelVertexFormats.DEFAULT);

			try
			{
				Method m = RenderSectionManager.class.getDeclaredMethod("getRenderSection", int.class, int.class, int.class);
				m.setAccessible(true);

				for (CubemapFace face : CubemapFace.values())
				{
					ChunkSectionPos origin = this.planetProps.getOrigin();

					ChunkRenderList chunkRenderList = new ChunkRenderList();

					for (int y = 0; y < 24; ++y)
					{
						RenderSection section = (RenderSection) m.invoke(this.wr.getRenderSectionManager(), origin.getX() + face.offsetX, origin.getY() + y, origin.getZ() + face.offsetZ);

						if (section != null)
						{
							chunkRenderList.add(section);
						}
					}

					for (BlockRenderPass pass : BlockRenderPass.VALUES)
					{
						pass.startDrawing();

						renderer.savePlanetUniforms(new CubeFaceContext(face, planetProps, 24), this.planetProps);

						try
						{
							renderer.render(new ChunkRenderMatrices(RenderSystem.getProjectionMatrix(), modelView), RenderDevice.INSTANCE.createCommandList(), chunkRenderList, pass, camCtx);
						}
						catch (Exception e)
						{
							System.out.println(e.getMessage());
						}

						pass.endDrawing();
					}
				}
			}
			catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e)
			{
				throw new RuntimeException(e);
			}

			renderer.delete();
		}
	}
}