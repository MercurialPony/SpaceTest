package melonslise.spacetest.client.renderer.shader;

import java.io.IOException;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;

import melonslise.spacetest.client.util.UniformExtension;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ExtendedPostChain extends PostChain
{
	public final UniformExtension uniforms = new UniformExtension();

	public ExtendedPostChain(TextureManager texManager, ResourceManager resManager, RenderTarget target, ResourceLocation name) throws IOException, JsonSyntaxException
	{
		super(texManager, resManager, target, name);
	}

	@Override
	public void process(float frameTime)
	{
		if (frameTime < this.lastStamp)
		{
			this.time += 1f - this.lastStamp;
			this.time += frameTime;
		}
		else
			this.time += frameTime - this.lastStamp;

		for (this.lastStamp = frameTime; this.time > 20.0F; this.time -= 20.0F)
			;

		for (PostPass pass : this.passes)
		{
			EffectInstance effect = pass.getEffect();
			this.uniforms.upload(effect);
			pass.process(this.time / 20.0F);
		}
	}
}