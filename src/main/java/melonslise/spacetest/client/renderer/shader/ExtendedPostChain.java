package melonslise.spacetest.client.renderer.shader;

import com.google.gson.JsonSyntaxException;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.shaders.Uniform;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EffectInstance;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.io.IOException;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class ExtendedPostChain extends PostChain implements IShader
{
	public ExtendedPostChain(TextureManager texManager, ResourceManager resManager, RenderTarget target, ResourceLocation name) throws JsonSyntaxException, IOException
	{
		super(texManager, resManager, target, name);
	}

	public ExtendedPostChain(String domain, String name) throws JsonSyntaxException, IOException
	{
		this(Minecraft.getInstance().getTextureManager(), Minecraft.getInstance().getResourceManager(), Minecraft.getInstance().getMainRenderTarget(), new ResourceLocation(domain, "shaders/post/" + name + ".json"));
		this.resize(Minecraft.getInstance().getWindow().getWidth(), Minecraft.getInstance().getWindow().getHeight());
	}

	public static ExtendedPostChain create(String domain, String name)
	{
		try
		{
			return new ExtendedPostChain(domain, name);
		}
		catch (JsonSyntaxException | IOException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	public EffectInstance getMainShader()
	{
		return this.passes.get(0).getEffect();
	}

	@Override
	public List<Uniform> getUniforms()
	{
		return this.getMainShader().uniforms;
	}

	@Override
	public PostPass addPass(String name, RenderTarget in, RenderTarget out) throws IOException
	{
		PostPass pass = new PostPass(this.resourceManager, name, in, out);
		pass.effect = new ExtendedEffectInstance(this.resourceManager, name);
		this.passes.add(this.passes.size(), pass);
		return pass;
	}

	@Override
	public void process(float frameTime)
	{
		Window w = Minecraft.getInstance().getWindow();
		if(this.screenWidth != w.getWidth() || this.screenHeight != w.getHeight())
			this.resize(w.getWidth(), w.getHeight());
		super.process(frameTime);
	}
}