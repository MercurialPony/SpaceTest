package melonslise.spacetest.client.event;

import melonslise.spacetest.SpaceTest;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SpaceTest.ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class SpaceTestClientForgeEvents
{
	/*
	public static ExtendedPostChain shader = null;

	public static ExtendedPostChain loadShader(ResourceLocation name)
	{
		Minecraft mc = Minecraft.getInstance();
		try
		{
			ExtendedPostChain shader = new ExtendedPostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), name);
			shader.resize(mc.getWindow().getWidth(), mc.getWindow().getHeight());
			return shader;
		}
		catch (JsonSyntaxException | IllegalArgumentException | SecurityException | IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	@SubscribeEvent
	public static void onRender(RenderWorldLastEvent e)
	{
		Minecraft mc = Minecraft.getInstance();
		if(mc.level == null || mc.isPaused()) // || e.phase == TickEvent.Phase.START)
			return;
		if(shader == null)
			shader = loadShader(new ResourceLocation(SpaceTest.ID, "shaders/post/test.json"));
		if(shader == null)
			return;
		Vec3 cam = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
		shader.projMat.load(e.getProjectionMatrix());
		System.out.println(e.getProjectionMatrix());
		e.getMatrixStack().pushPose();
		shader.modelViewMat.load(e.getMatrixStack().last().pose());
		shader.pos.set(-224.5f, 65f, -267.5f);
		// shader.pos.add((float) -cam.x, (float) -cam.y, (float) -cam.z);
		shader.rad = 1000f;
		RenderSystem.disableBlend();
		RenderSystem.disableDepthTest();
		RenderSystem.enableTexture();
		RenderSystem.resetTextureMatrix();
		shader.process(Minecraft.getInstance().getFrameTime());
		e.getMatrixStack().popPose();
	}
	*/

	/*
	@SubscribeEvent
	public static void onRenderWorld(RenderWorldLastEvent e)
	{
		Minecraft mc = Minecraft.getInstance();
		PoseStack mtx = e.getMatrixStack();
		BufferSource buf = mc.renderBuffers().bufferSource();
		Vec3 camPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
		mtx.pushPose();
		mtx.translate(-camPos.x, -camPos.y, -camPos.z);
		Vector3f pos = new Vector3f(-224.5f, 67f, -267.5f);
		mtx.translate(pos.x(), pos.y(), pos.z());
		billboardCircle(buf.getBuffer(RenderType.translucent()), mtx, 1f, 30);
		mtx.popPose();
		radius = 1f;
		SpaceTestClientForgeEvents.pos.load(pos);
		SpaceTestClientForgeEvents.pos.add((float) -camPos.x, (float) -camPos.y, (float) -camPos.z);
		buf.endBatch();
	}

	public static void billboardCircle(VertexConsumer bld, PoseStack mtx, float radius, int steps)
	{
		Matrix4f last = mtx.last().pose();
		bld.vertex(last, 0f, 0f, 0f).color(0f, 0f, 0f, 1f).uv(0f, 0f).overlayCoords(0).uv2(240).normal(0f, 1f, 0f).endVertex();
		bld.vertex(last, 0f, 1f, 0f).color(0f, 0f, 0f, 1f).uv(0f, 0f).overlayCoords(0).uv2(240).normal(0f, 1f, 0f).endVertex();
		bld.vertex(last, 1f, 1f, 1f).color(0f, 0f, 0f, 1f).uv(0f, 0f).overlayCoords(0).uv2(240).normal(0f, 1f, 0f).endVertex();
		bld.vertex(last, 1f, 0f, 1f).color(0f, 0f, 0f, 1f).uv(0f, 0f).overlayCoords(0).uv2(240).normal(0f, 1f, 0f).endVertex();
		Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
		Vec3 camPos = cam.getPosition();
		Vector3f left = cam.getLeftVector();
		Vector3f up = cam.getUpVector();
		Matrix4f last = mtx.last().pose();
		Vector3f v = new Vector3f();
		bld.vertex(last, 0f, 0f, 0f).endVertex();
		for(int a = 0; a <= steps; ++a)
		{
			float angle = a * Mth.PI * 2f / steps;
			Vector3f vLeft = left.copy();
			vLeft.mul(-Mth.sin(angle) * radius);
			Vector3f vUp = up.copy();
			vUp.mul(Mth.cos(angle) * radius);
			v.set(0f, 0f, 0f);
			v.add(vUp);
			v.add(vLeft);
			bld.vertex(last, v.x(), v.y(), v.z()).endVertex();
		}
	}

	// For debug
	public static void billboardXY(VertexConsumer bld, PoseStack mtx, Vector3f pos)
	{
		Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
		Vec3 camPos = cam.getPosition();
		Vector3f left = cam.getLeftVector();
		Vector3f up = cam.getUpVector();
		Matrix4f last = mtx.last().pose();
		bld.vertex(last, pos.x(), pos.y(), pos.z()).color(1f, 1f, 1f, 1f).normal(0f, 0f, 0f).endVertex();
		bld.vertex(last, pos.x() + up.x(), pos.y() + up.y(), pos.z() + up.z()).color(1f, 1f, 1f, 1f).normal(0f, 1f, 0f).endVertex();
		bld.vertex(last, pos.x(), pos.y(), pos.z()).color(1f, 1f, 1f, 1f).normal(0f, 0f, 0f).endVertex();
		bld.vertex(last, pos.x() + left.x(), pos.y() + left.y(), pos.z() + left.z()).color(1f, 1f, 1f, 1f).normal(1f, 0f, 0f).endVertex();
	}
	*/
}