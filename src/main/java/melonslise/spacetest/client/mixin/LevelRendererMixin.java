package melonslise.spacetest.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;

import melonslise.spacetest.client.init.SpaceTestShaders;
import melonslise.spacetest.client.renderer.shader.ExtendedPostChain;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;

// Mixin not available yet... Use js coremods!
public class LevelRendererMixin
{
	public static void renderPreFabulous(PoseStack mtx)
	{
		ExtendedPostChain shaderChain = SpaceTestShaders.getAtmosphere();
		EffectInstance shader = shaderChain.getMainShader();

		if(shader == null)
			return;

		Minecraft mc = Minecraft.getInstance();

		Vector3f cam = new Vector3f(mc.gameRenderer.getMainCamera().getPosition());

		Matrix4f projInv = RenderSystem.getProjectionMatrix().copy();
		projInv.invert();

		Matrix4f viewInv = mtx.last().pose().copy();
		viewInv.invert();

		shader.safeGetUniform("CameraPosition").set(cam.x(), cam.y(), cam.z());
		shader.safeGetUniform("ProjInverseMat").set(projInv);
		shader.safeGetUniform("ViewInverseMat").set(viewInv);

		//shader.safeGetUniform("PlanetRadius").set(60f);
		//shader.safeGetUniform("AtmosphereRadius").set(90f);
		//shader.safeGetUniform("DirToSun").set(0f, 1f, 0f);
		shaderChain.process(mc.getFrameTime());
		mc.getMainRenderTarget().bindWrite(false);
	}
}