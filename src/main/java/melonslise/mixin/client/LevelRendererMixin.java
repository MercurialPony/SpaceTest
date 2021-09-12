package melonslise.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;

import melonslise.spacetest.client.init.SpaceTestShaders;
import melonslise.spacetest.client.renderer.shader.ExtendedPostChain;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin
{
	@Inject(
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/LevelRenderer;renderDebug(Lnet/minecraft/client/Camera;)V"),
		method = "renderLevel(Lcom/mojang/blaze3d/vertex/PoseStack;FJZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/GameRenderer;Lnet/minecraft/client/renderer/LightTexture;Lcom/mojang/math/Matrix4f;)V")
	public void renderLevel(PoseStack mtx, float frameTime, long nanoTime, boolean renderOutline, Camera camera, GameRenderer gameRenderer, LightTexture light, Matrix4f projMat, CallbackInfo ci)
	{
		renderPreFabulous(mtx);
	}

	private static void renderPreFabulous(PoseStack mtx)
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