package melonslise.spacetest.client.renderer.dimension;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import melonslise.spacetest.SpaceTest;
import melonslise.spacetest.client.init.SpaceTestShaders;
import melonslise.spacetest.client.renderer.shader.ExtendedShaderInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public class SpaceEffects extends DimensionSpecialEffects
{
	public static ResourceLocation ID = new ResourceLocation(SpaceTest.ID, "space");

	public SpaceEffects()
	{
		super(Float.NaN, false, SkyType.NONE, true, false);
		this.setSkyRenderHandler(this::renderSky);
	}

	public void renderSky(int ticks, float frameTime, PoseStack mtx, ClientLevel world, Minecraft mc)
	{
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		RenderSystem.depthMask(false);
		// RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
		RenderSystem.setShader(SpaceTestShaders::getSpaceSky);
		// RenderSystem.setShaderTexture(0, new ResourceLocation("textures/environment/end_sky.png"));
		Tesselator tesselator = Tesselator.getInstance();
		BufferBuilder bufferbuilder = tesselator.getBuilder();
		
		Matrix4f modelView = mtx.last().pose();
		Matrix4f invModelView = modelView.copy();
		invModelView.invert();

		ExtendedShaderInstance shader = SpaceTestShaders.getSpaceSky();
		shader.safeGetUniform("ModelViewInverseMat").set(invModelView);

		// all squares are drawn from bottom left -> bottom right -> top right -> top left

		// bottom
		bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		bufferbuilder.vertex(modelView, -100f, -100f, -100f).uv(0f, 0f).endVertex();
		bufferbuilder.vertex(modelView, -100f, -100f, 100f).uv(1f, 0f).endVertex();
		bufferbuilder.vertex(modelView, 100f, -100f, 100f).uv(1f, 1f).endVertex();
		bufferbuilder.vertex(modelView, 100f, -100f, -100f).uv(0f, 1f).endVertex();
		tesselator.end();

		// top
		bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		bufferbuilder.vertex(modelView, -100f, 100f, 100f).uv(0f, 0f).endVertex();
		bufferbuilder.vertex(modelView, -100f, 100f, -100f).uv(1f, 0f).endVertex();
		bufferbuilder.vertex(modelView, 100f, 100f, -100f).uv(1f, 1f).endVertex();
		bufferbuilder.vertex(modelView, 100f, 100f, 100f).uv(0f, 1f).endVertex();
		tesselator.end();

		// north
		bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		bufferbuilder.vertex(modelView, -100f, -100f, -100f).uv(0f, 0f).endVertex();
		bufferbuilder.vertex(modelView, 100f, -100f, -100f).uv(1f, 0f).endVertex();
		bufferbuilder.vertex(modelView, 100f, 100f, -100f).uv(1f, 1f).endVertex();
		bufferbuilder.vertex(modelView, -100f, 100f, -100f).uv(0f, 1f).endVertex();
		tesselator.end();

		// east
		bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		bufferbuilder.vertex(modelView, 100f, -100f, -100f).uv(0f, 0f).endVertex();
		bufferbuilder.vertex(modelView, 100f, -100f, 100f).uv(1f, 0f).endVertex();
		bufferbuilder.vertex(modelView, 100f, 100f, 100f).uv(1f, 1f).endVertex();
		bufferbuilder.vertex(modelView, 100f, 100f, -100f).uv(0f, 1f).endVertex();
		tesselator.end();

		// south
		bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		bufferbuilder.vertex(modelView, 100f, -100f, 100f).uv(0f, 0f).endVertex();
		bufferbuilder.vertex(modelView, -100f, -100f, 100f).uv(1f, 0f).endVertex();
		bufferbuilder.vertex(modelView, -100f, 100f, 100f).uv(1f, 1f).endVertex();
		bufferbuilder.vertex(modelView, 100f, 100f, 100f).uv(0f, 1f).endVertex();
		tesselator.end();

		// west
		bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
		bufferbuilder.vertex(modelView, -100f, -100f, 100f).uv(0f, 0f).endVertex();
		bufferbuilder.vertex(modelView, -100f, -100f, -100f).uv(1f, 0f).endVertex();
		bufferbuilder.vertex(modelView, -100f, 100f, -100f).uv(1f, 1f).endVertex();
		bufferbuilder.vertex(modelView, -100f, 100f, 100f).uv(0f, 1f).endVertex();
		tesselator.end();

		RenderSystem.depthMask(true);
		RenderSystem.enableTexture();
		RenderSystem.disableBlend();
	}

	@Override
	public Vec3 getBrightnessDependentFogColor(Vec3 fogColor, float frameTime)
	{
		return fogColor;
	}

	@Override
	public boolean isFoggyAt(int x, int y)
	{
		return false;
	}

	@Override
	public float[] getSunriseColor(float p_108872_, float p_108873_)
	{
		return null;
	}
}